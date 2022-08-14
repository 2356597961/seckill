package com.xiaoliu.seckill.service.impl;


import com.xiaoliu.seckill.controller.ItemController;
import com.xiaoliu.seckill.dao.OrderDOMapper;
import com.xiaoliu.seckill.dao.SequenceDOMapper;
import com.xiaoliu.seckill.dao.StockLogDOMapper;
import com.xiaoliu.seckill.error.BusinessException;
import com.xiaoliu.seckill.error.EmBusinessError;
import com.xiaoliu.seckill.pojo.StockLogDO;
import com.xiaoliu.seckill.service.ItemService;
import com.xiaoliu.seckill.service.OrderService;
import com.xiaoliu.seckill.service.UserService;
import com.xiaoliu.seckill.service.model.ItemModel;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;

/**
 * Created by xiaoliu on 2022/04/18.
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private SequenceDOMapper sequenceDOMapper;

    @Resource
    private ItemService itemService;

    @Resource
    private UserService userService;

    @Resource
    private OrderDOMapper orderDOMapper;

    @Resource
    private StockLogDOMapper stockLogDOMapper;

    @Resource
    private ItemController itemController;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private DefaultRedisScript defaultRedisScript;

    @Resource
    private RedissonClient redissonClient;


    /**
     * 分布式锁
     * @param userId
     * @param itemId
     * @param promoId
     * @param amount
     * @param stockLogId
     * @return
     * @throws BusinessException
     */
    @Override
    @Transactional
    public void createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) throws BusinessException {
        //1.校验下单状态,下单的商品是否存在，用户是否合法，购买数量是否正确
        //ItemModel itemModel = itemService.getItemById(itemId);
        ItemModel itemModel = itemController.getItemModel(itemId);
        if(itemModel == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商品信息不存在");
        }

//
//        UserModel userModel = userService.getUserByIdInCache(userId);
//        if(userModel == null){
//            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"用户信息不存在");
//        }
        if(amount <= 0 || amount > 99){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"数量信息不正确");
        }

        //校验活动信息
//        if(promoId != null){
//            //（1）校验对应活动是否存在这个适用商品
//            if(promoId.intValue() != itemModel.getPromoModel().getId()){
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息不正确");
//                //（2）校验活动是否正在进行中
//            }else if(itemModel.getPromoModel().getStatus().intValue() != 2) {
//                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"活动信息还未开始");
//            }
//        }
        //一人一单（分布式锁）
        //RLock lock = redissonClient.getLock(promoId + userId + itemId.toString());
        try {
            //boolean b = lock.tryLock(10, TimeUnit.MINUTES);
            //if (b) {
                ArrayList<String> keys = new ArrayList<>();
                keys.add("promoid_" + promoId + "_userid_" + userId + "_itemid_" + itemId);  //用户是否存在
                keys.add("promo_item_stock_" + itemId);  //库存扣减
                keys.add("promo_item_stock_invalid_" + itemId);  //卖完的标志
                //AVGE是object...,因此不能是list,可以多个，使用逗号隔开
                Long result = (Long) redisTemplate.execute(defaultRedisScript, keys, amount);
                System.out.println("脚本执行结果"+result);
                if (result == 1) {
                    throw new BusinessException(EmBusinessError.AlReadySale, "您已下过单");
                }
                if (result == 0) {
                    throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
                }
                //设置库存流水状态为成功(天生幂等)
                StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);  //基于不同的行锁没有锁之间的竞争，还是很快的
                if (stockLogDO == null) {
                    throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
                }
                stockLogDO.setStatus(2);
                stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);
           // }
        }catch (BusinessException e) {
            //手动回滚实物
            //TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            //释放锁
            //lock.unlock();
            //抛出异常
            //throw new NullPointerException();
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "发生错误");
        }
        /*} catch (InterruptedException e) {
            //释放锁
            lock.unlock();
            e.printStackTrace();
        }*/
        /*if (redisTemplate.opsForValue().get("promoid_"+promoId+"_userid_"+userId+"_itemid_"+itemId)!=null){
            throw new BusinessException(null,"已下过单");
        }else {
            //2.落单减库存（redis中的库存）,幂等性
            boolean result = itemService.decreaseStock(itemId,amount);  //这里使用分布式锁的话，就不用考虑超卖的情况了
            if(!result){
                throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
            }
            redisTemplate.opsForValue().set("promoid_"+promoId+"_userid_"+userId+"_itemid_"+itemId,userId,24, TimeUnit.HOURS);
        }*/

        /**
         * 订单产生，减库存消息消费采用异步化，也可以采用订单的异步化，可以使用websocket进行通知下单成功
         * 这里的话会有重复消费的问题，解决方法，在发送订单的id,然后判断是否已经消费过了，没有就消费，有的话
         */
        /*//3.订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setUserId(userId);
        orderModel.setItemId(itemId);
        orderModel.setAmount(amount);
        if(promoId != null){
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        }else{
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setPromoId(promoId);
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));  //乘以个数

        //生成交易流水号,订单号
        //orderModel.setId(generateOrderNo());  //数据库
        orderModel.setId(String.valueOf(new IdWorker().nextId()));  //雪花算法
        OrderDO orderDO = convertFromOrderModel(orderModel);
        orderDOMapper.insertSelective(orderDO);

        //加上商品的销量
        itemService.increaseSales(itemId,amount);*/

        /*//设置库存流水状态为成功(天生幂等)
        StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);  //基于不同的行锁没有锁之间的竞争，还是很快的
        if(stockLogDO == null){
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }
        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);*/

        /*TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit(){
                    //异步更新库存
                    boolean mqResult = itemService.asyncDecreaseStock(itemId,amount);
                    if(!mqResult){
                        itemService.increaseStock(itemId,amount);
                        throw new BusinessException(EmBusinessError.MQ_SEND_FAIL);
                    }
                }

        });*/



        //4.返回前端
        //return orderModel;
        return;
    }


    /*@Transactional(propagation = Propagation.REQUIRES_NEW)
    String generateOrderNo(){
        //订单号有16位
        StringBuilder stringBuilder = new StringBuilder();
        //前8位为时间信息，年月日
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-","");
        stringBuilder.append(nowDate);

        //中间6位为自增序列
        //获取当前sequence
        int sequence = 0;
        SequenceDO sequenceDO =  sequenceDOMapper.getSequenceByName("order_info");
        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue() + sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        String sequenceStr = String.valueOf(sequence);
        for(int i = 0; i < 6-sequenceStr.length();i++){
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);


        //最后2位为分库分表位,暂时写死
        stringBuilder.append("00");

        return stringBuilder.toString();
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
    }*/
}
