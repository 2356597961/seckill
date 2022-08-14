package com.xiaoliu.seckill.mq;

import com.alibaba.fastjson.JSON;
import com.xiaoliu.seckill.dao.StockLogDOMapper;
import com.xiaoliu.seckill.error.BusinessException;
import com.xiaoliu.seckill.pojo.StockLogDO;
import com.xiaoliu.seckill.service.OrderService;
import com.xiaoliu.seckill.util.IdWorker;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
/**
 * Created by xiaoliu on 2022/4/18.
 */
@Component
public class MqProducer {

    private DefaultMQProducer producer;

    private TransactionMQProducer transactionMQProducer;

    @Value("${mq.nameserver.addr}")
    private String nameAddr;

    @Value("${mq.topicname}")
    private String topicName;


    @Autowired
    private OrderService orderService;

    @Resource
    private StockLogDOMapper stockLogDOMapper;

    @Resource
    private RedisTemplate redisTemplate;

    @PostConstruct
    public void init() throws MQClientException {
        //做mq producer的初始化
        producer = new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr(nameAddr);
        producer.start();

        transactionMQProducer = new TransactionMQProducer("transaction_producer_group");
        transactionMQProducer.setNamesrvAddr(nameAddr);
        transactionMQProducer.setRetryTimesWhenSendFailed(3);
        transactionMQProducer.start();

        transactionMQProducer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                //真正要做的事  创建订单
                Integer itemId = (Integer) ((Map)arg).get("itemId");
                Integer promoId = (Integer) ((Map)arg).get("promoId");
                Integer userId = (Integer) ((Map)arg).get("userId");
                Integer amount = (Integer) ((Map)arg).get("amount");
                String stockLogId = (String) ((Map)arg).get("stockLogId");
                try {
                    orderService.createOrder(userId,itemId,promoId,amount,stockLogId);  //这里发生异常了，流水号永远为1，这个redis就会减掉库存，但实际却没有。
                } catch (BusinessException e) {  //自己决定不需要重新发送消息的异常
                    e.printStackTrace();
                    //设置对应的stockLog为回滚状态
                    StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
                    stockLogDO.setStatus(3);
                    stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                //根据是否扣减库存成功，来判断要返回COMMIT,ROLLBACK还是继续UNKNOWN
                String jsonString  = new String(msg.getBody());
                Map<String,Object>map = JSON.parseObject(jsonString, Map.class);
                Integer itemId = (Integer) map.get("itemId");
                Integer promoId = (Integer) map.get("promoId");
                Integer userId = (Integer) map.get("userId");
                Integer amount = (Integer) map.get("amount");
                String stockLogId = (String) map.get("stockLogId");
                StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);  //流水状态
                if(stockLogDO == null){
                    try {
                        orderService.createOrder(userId,itemId,promoId,amount,stockLogId);
                    } catch (BusinessException e) {
                        e.printStackTrace();
                        //设置对应的stockLog为回滚状态
                        StockLogDO stockLogDO01 = stockLogDOMapper.selectByPrimaryKey(stockLogId);
                        stockLogDO01.setStatus(3);
                        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO01);
                        return LocalTransactionState.ROLLBACK_MESSAGE;
                    }
                }
                if(stockLogDO.getStatus().intValue() == 2){
                    return LocalTransactionState.COMMIT_MESSAGE;
                }else if(stockLogDO.getStatus().intValue() == 1){
                    try {
                        orderService.createOrder(userId,itemId,promoId,amount,stockLogId);
                    } catch (BusinessException e) {
                        e.printStackTrace();
                        //设置对应的stockLog为回滚状态
                        StockLogDO stockLogDO01 = stockLogDOMapper.selectByPrimaryKey(stockLogId);
                        stockLogDO01.setStatus(3);
                        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO01);
                        return LocalTransactionState.ROLLBACK_MESSAGE;
                    }
                }else {
                    return LocalTransactionState.ROLLBACK_MESSAGE;
                }
                return LocalTransactionState.UNKNOW;  //返回UNKNOW时，最大尝试5次
            }
        });
    }

    //事务型同步库存扣减消息
    public boolean transactionAsyncReduceStock(Integer userId,Integer itemId,Integer promoId,Integer amount,String stockLogId) {
        Map<String,Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId",itemId);
        bodyMap.put("amount",amount);
        bodyMap.put("stockLogId",stockLogId);
        bodyMap.put("orderId",String.valueOf(new IdWorker().nextId()));
        bodyMap.put("userId",userId);
        bodyMap.put("promoId",promoId);

        Map<String,Object> argsMap = new HashMap<>();
        argsMap.put("itemId",itemId);
        argsMap.put("amount",amount);
        argsMap.put("userId",userId);
        argsMap.put("promoId",promoId);
        argsMap.put("stockLogId",stockLogId);

        Message message = new Message(topicName,"increase",
                JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        TransactionSendResult sendResult = null;
        try {

            sendResult = transactionMQProducer.sendMessageInTransaction(message,argsMap);
            System.out.println("发送的状态"+sendResult.getLocalTransactionState());
        } catch (MQClientException e) {
            /*e.printStackTrace();
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "网络卡顿,请尝试");*/
            return false;
        }
        if(sendResult.getLocalTransactionState() == LocalTransactionState.ROLLBACK_MESSAGE){
            return false;
        }else if(sendResult.getLocalTransactionState() == LocalTransactionState.COMMIT_MESSAGE){
            return true;
        }else{
            return false;
        }

    }

    //异步库存扣减消息
    public boolean asyncReduceStock(Integer itemId,Integer amount)  {
        Map<String,Object> bodyMap = new HashMap<>();
        bodyMap.put("itemId",itemId);
        bodyMap.put("amount",amount);

        Message message = new Message(topicName,"increase",
                JSON.toJSON(bodyMap).toString().getBytes(Charset.forName("UTF-8")));
        try {
            producer.send(message);
        } catch (MQClientException e) {
            e.printStackTrace();
            return false;
        } catch (RemotingException e) {
            e.printStackTrace();
            return false;
        } catch (MQBrokerException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
