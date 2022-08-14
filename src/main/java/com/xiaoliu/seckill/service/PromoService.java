package com.xiaoliu.seckill.service;


import com.xiaoliu.seckill.error.BusinessException;
import com.xiaoliu.seckill.service.model.PromoModel;
import com.xiaoliu.seckill.viewobject.PromoVO;

/**
 * Created by xiaoliu on 2022/4/22.
 */
public interface PromoService {
    //根据itemid获取即将进行的或正在进行的秒杀活动
    PromoModel getPromoByItemId(Integer itemId);

    //活动发布
    void publishPromo(Integer promoId) throws BusinessException;

    //生成秒杀用的令牌
    String generateSecondKillToken(Integer promoId, Integer itemId, Integer userId);

    void addPromo(PromoVO promoVO) throws BusinessException;
}
