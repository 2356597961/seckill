package com.xiaoliu.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.xiaoliu.seckill.config.NettyWebSocketServer;
import com.xiaoliu.seckill.controller.ItemController;
import com.xiaoliu.seckill.dao.ItemStockDOMapper;
import com.xiaoliu.seckill.dao.OrderDOMapper;
import com.xiaoliu.seckill.error.BusinessException;
import com.xiaoliu.seckill.pojo.OrderDO;
import com.xiaoliu.seckill.service.ItemService;
import com.xiaoliu.seckill.service.model.ItemModel;
import com.xiaoliu.seckill.service.model.OrderModel;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Created by xioaliu on 2022/4/23.
 */
@Component
public class MqConsumer {

    private DefaultMQPushConsumer consumer;
    @Value("${mq.nameserver.addr}")
    private String nameAddr;

    @Value("${mq.topicname}")
    private String topicName;

    @Resource
    private ItemStockDOMapper itemStockDOMapper;

    @Resource
    private ItemController itemController;

    @Resource
    private OrderDOMapper orderDOMapper;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private ItemService itemService;

    @Resource
    private NettyWebSocketServer nettyWebSocketServer;

    @PostConstruct
    public void init() throws MQClientException {
        consumer = new DefaultMQPushConsumer("stock_consumer_group");
        consumer.setNamesrvAddr(nameAddr);
        consumer.subscribe(topicName,"*");

        consumer.registerMessageListener(new MessageListenerConcurrently() {  //消费消息
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                //这里做消失的幂等性（强一致：数据库实现。唯一索引，记录），弱：redis的set实现，要有唯一的标识，存在一定的时间。
                //实现库存真正到数据库内扣减的逻辑
                //3.订单入库
                Message msg = msgs.get(0);
                String jsonString  = new String(msg.getBody());
                Map<String,Object>map = JSON.parseObject(jsonString, Map.class);
                Integer itemId = (Integer) map.get("itemId");
                Integer amount = (Integer) map.get("amount");
                String orderId =(String) map.get("orderId");
                Integer userId =(Integer) map.get("userId");
                Integer promoId = (Integer)map.get("promoId");
                ItemModel itemModel = itemController.getItemModel(itemId); //天生幂等
                if (!redisTemplate.opsForSet().isMember("orderId",orderId)) {
                    try {
                        OrderModel orderModel = new OrderModel();
                        orderModel.setUserId(userId);
                        orderModel.setItemId(itemId);
                        orderModel.setAmount(amount);
                        if (promoId != null) {
                            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());  //是秒杀的时候的价格
                        } else {
                            orderModel.setItemPrice(itemModel.getPrice());  //不是秒杀的时候的价格
                        }
                        orderModel.setPromoId(promoId);
                        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));  //乘以个数
                        OrderDO orderDO = convertFromOrderModel(orderModel);
                        orderDO.setId(orderId);
                        orderDOMapper.insertSelective(orderDO);
                        nettyWebSocketServer.sendMessage("下单成功",userId.toString());
                        redisTemplate.opsForSet().add("orderId", orderId);
                    }catch (Exception e){
                        e.printStackTrace();
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }

                //加上商品的销量
                if (!redisTemplate.opsForSet().isMember("itemIdSale",orderId)) {
                    try {
                        itemService.increaseSales(itemId, amount);
                        redisTemplate.opsForSet().add("itemIdSale", orderId);
                    } catch (BusinessException e) {
                        e.printStackTrace();
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;  //重试
                    }
                }
                if (!redisTemplate.opsForSet().isMember("itemIdStock",orderId)) {
                    try {
                        itemStockDOMapper.decreaseStock(itemId, amount);
                        redisTemplate.opsForSet().isMember("itemIdStock",orderId);
                    }catch (Exception e){
                        e.printStackTrace();
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS; //消费成功
            }
        });

        consumer.start();

    }
    private OrderDO convertFromOrderModel(OrderModel orderModel){
        if(orderModel == null){
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel,orderDO);
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());
        return orderDO;
    }
}
