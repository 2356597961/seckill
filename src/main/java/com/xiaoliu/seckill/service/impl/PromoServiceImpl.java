package com.xiaoliu.seckill.service.impl;


import com.xiaoliu.seckill.controller.ItemController;
import com.xiaoliu.seckill.dao.PromoDOMapper;
import com.xiaoliu.seckill.error.BusinessException;
import com.xiaoliu.seckill.error.EmBusinessError;
import com.xiaoliu.seckill.pojo.PromoDO;
import com.xiaoliu.seckill.service.ItemService;
import com.xiaoliu.seckill.service.PromoService;
import com.xiaoliu.seckill.service.UserService;
import com.xiaoliu.seckill.service.model.ItemModel;
import com.xiaoliu.seckill.service.model.PromoModel;
import com.xiaoliu.seckill.service.model.UserModel;
import com.xiaoliu.seckill.viewobject.PromoVO;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiaoliu on 2022/4/18.
 */
@Service
public class PromoServiceImpl implements PromoService {

    @Resource
    private PromoDOMapper promoDOMapper;

    @Autowired
    private ItemService itemService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    @Resource
    private ItemController itemController;

    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        //获取对应商品的秒杀活动信息
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);

        //dataobject->model
        PromoModel promoModel = convertFromDataObject(promoDO);
        if(promoModel == null){
            return null;
        }

        //判断当前时间是否秒杀活动即将开始或正在进行
        if(promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else{
            promoModel.setStatus(2);
        }
        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) throws BusinessException{
        //通过活动id获取活动
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if(promoDO.getItemId() == null || promoDO.getItemId().intValue() == 0){
            throw new BusinessException(EmBusinessError.NO_EXIST,"不存在相应的秒杀活动");
        }
        PromoModel promoModel = convertFromDataObject(promoDO);
        if (promoModel.getStartDate().isBeforeNow()){
            throw new BusinessException(null,"秒杀活动开始时间不能早于当前时间");
        }
        if (promoModel.getEndDate().isBeforeNow()){
            throw new BusinessException(null,"秒杀活动的结束时间不能早于当前时间");
        }
        if (promoModel.getStartDate().isAfter(promoModel.getEndDate())){
            throw new BusinessException(null,"秒杀活动的开始时间不能晚于结束时间时间");
        }
        ItemModel itemModel = itemController.getItemModel(promoDO.getItemId());  //多级缓存中取

        //todo 将库存同步到redis内(改进加上活动的标识)
        redisTemplate.opsForValue().set("promo_item_stock_"+itemModel.getId(), itemModel.getStock());

        //将大闸的限制数字设到redis内
        redisTemplate.opsForValue().set("promo_door_count_"+promoId,itemModel.getStock().intValue() * 5);

    }

    @Override
    public String generateSecondKillToken(Integer promoId,Integer itemId,Integer userId) {

        //判断是否库存已售罄，若对应的售罄key存在，则直接返回下单失败
        if(redisTemplate.hasKey("promo_item_stock_invalid_"+itemId)){
            return null;
        }
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);

        //dataobject->model
        PromoModel promoModel = convertFromDataObject(promoDO);
        if(promoModel == null){
            return null;
        }

        //判断当前时间是否秒杀活动即将开始或正在进行
        if(promoModel.getStartDate().isAfterNow()){
            promoModel.setStatus(1);
        }else if(promoModel.getEndDate().isBeforeNow()){
            promoModel.setStatus(3);
        }else{
            promoModel.setStatus(2);
        }
        //判断活动是否正在进行
        if(promoModel.getStatus().intValue() != 2){
            return null;
        }
        //判断item信息是否存在
        ItemModel itemModel = itemController.getItemModel(itemId);
        if(itemModel == null){
            return null;
        }
        //判断用户信息是否存在
        UserModel userModel = userService.getUserByIdInCache(userId);
        if(userModel == null){
            return null;
        }
        //会存在一个问题就是如果这个用户登录了，而且拼命的得到令牌，其他用户就得不到了，这个需要注意，可以这样就是一个用户只能一个令牌
        //或者对应是否可以一个用户和商品，有了就不减了
        //获取秒杀大闸的count数量
        //为什么大闸的数目要超过平常的数量呢，因为可能会有人生成了相应的秒杀临牌了，但没有下单，所以要比总的库存数量大
        // todo 限制抢一次一个活动的一个商品的一个用户
        if (redisTemplate.opsForValue().get("promo_token_"+promoId+"_userid_"+userId+"_itemid_"+itemId)==null) {
            long result = redisTemplate.opsForValue().increment("promo_door_count_" + promoId, -1);  //减1后的值
            if(result < 0){
                return null;
            }
        }
        //生成token并且存入redis内并给一个5分钟的有效期
        String token = UUID.randomUUID().toString().replace("-","");

        redisTemplate.opsForValue().set("promo_token_"+promoId+"_userid_"+userId+"_itemid_"+itemId,token);
        redisTemplate.expire("promo_token_"+promoId+"_userid_"+userId+"_itemid_"+itemId,5, TimeUnit.MINUTES);

        return token;
    }

    @Override
    public void addPromo(PromoVO promoVO) throws BusinessException{
        //根据秒杀的名称和商品的id查看是否已经存在了
        PromoDO promoDO = new PromoDO();
        BeanUtils.copyProperties(promoVO,promoDO);
        PromoModel promoModel = convertFromDataObject(promoDO);
        if (promoModel.getStartDate().isBeforeNow()){
            throw new BusinessException(null,"秒杀活动开始时间不能早于当前时间");
        }
        if (promoModel.getEndDate().isBeforeNow()){
            throw new BusinessException(null,"秒杀活动的结束时间不能早于当前时间");
        }
        if (promoModel.getStartDate().isAfter(promoModel.getEndDate())){
            throw new BusinessException(null,"秒杀活动的开始时间不能晚于结束时间时间");
        }
        promoDOMapper.insert(promoDO);
    }

    private PromoModel convertFromDataObject(PromoDO promoDO){
        if(promoDO == null){
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO,promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartDate(new DateTime(promoDO.getStartDate()));
        promoModel.setEndDate(new DateTime(promoDO.getEndDate()));
        return promoModel;
    }
}
