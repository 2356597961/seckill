package com.xiaoliu.seckill.controller;

import com.xiaoliu.seckill.page.HtmlService;
import com.xiaoliu.seckill.response.CommonReturnType;
import com.xiaoliu.seckill.service.PromoService;
import com.xiaoliu.seckill.viewobject.PromoVO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Created by xiaoliu on 2022/4/18.
 */
@RestController
@RequestMapping("/promo")
@CrossOrigin(origins = {"*"},allowCredentials = "true")
public class PromoController extends BaseController {

    @Resource
    private HtmlService htmlService;
    @Resource
    private PromoService promoService;

    @Transactional(rollbackFor = Exception.class)
    @PostMapping("/addPromo")
    public CommonReturnType addPromo(@RequestBody PromoVO promoVO) throws Exception {
        //添加秒杀活动
        promoService.addPromo(promoVO);
        //生成静态页
        Boolean aBoolean = htmlService.writerPage(promoVO.getItemId(),promoVO);
        if (!aBoolean){
            return CommonReturnType.create(null,"false");
        }
        return CommonReturnType.create(null);
    }
}
