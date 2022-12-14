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

        consumer.registerMessageListener(new MessageListenerConcurrently() {  //????????????
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
                //?????????????????????????????????????????????????????????????????????????????????????????????redis???set?????????????????????????????????????????????????????????
                //????????????????????????????????????????????????
                //3.????????????
                Message msg = msgs.get(0);
                String jsonString  = new String(msg.getBody());
                Map<String,Object>map = JSON.parseObject(jsonString, Map.class);
                Integer itemId = (Integer) map.get("itemId");
                Integer amount = (Integer) map.get("amount");
                String orderId =(String) map.get("orderId");
                Integer userId =(Integer) map.get("userId");
                Integer promoId = (Integer)map.get("promoId");
                ItemModel itemModel = itemController.getItemModel(itemId); //????????????
                if (!redisTemplate.opsForSet().isMember("orderId",orderId)) {
                    try {
                        OrderModel orderModel = new OrderModel();
                        orderModel.setUserId(userId);
                        orderModel.setItemId(itemId);
                        orderModel.setAmount(amount);
                        if (promoId != null) {
                            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());  //???????????????????????????
                        } else {
                            orderModel.setItemPrice(itemModel.getPrice());  //??????????????????????????????
                        }
                        orderModel.setPromoId(promoId);
                        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));  //????????????
                        OrderDO orderDO = convertFromOrderModel(orderModel);
                        orderDO.setId(orderId);
                        orderDOMapper.insertSelective(orderDO);
                        nettyWebSocketServer.sendMessage("????????????",userId.toString());
                        redisTemplate.opsForSet().add("orderId", orderId);
                    }catch (Exception e){
                        e.printStackTrace();
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }

                //?????????????????????
                if (!redisTemplate.opsForSet().isMember("itemIdSale",orderId)) {
                    try {
                        itemService.increaseSales(itemId, amount);
                        redisTemplate.opsForSet().add("itemIdSale", orderId);
                    } catch (BusinessException e) {
                        e.printStackTrace();
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;  //??????
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
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS; //????????????
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
