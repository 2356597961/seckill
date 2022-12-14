package com.xiaoliu.seckill.controller;

import com.google.common.util.concurrent.RateLimiter;
import com.xiaoliu.seckill.error.BusinessException;
import com.xiaoliu.seckill.error.EmBusinessError;
import com.xiaoliu.seckill.mq.MqProducer;
import com.xiaoliu.seckill.response.CommonReturnType;
import com.xiaoliu.seckill.service.ItemService;
import com.xiaoliu.seckill.service.OrderService;
import com.xiaoliu.seckill.service.PromoService;
import com.xiaoliu.seckill.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Created by xiaoliu on 2022/4/25.
 */
@Controller("order")
@RequestMapping("/order")
@CrossOrigin(origins = {"*"}, allowCredentials = "true")
public class OrderController extends BaseController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private MqProducer mqProducer;

    @Autowired
    private ItemService itemService;

    @Autowired
    private PromoService promoService;

    private ExecutorService executorService;

    @Resource
    private RateLimiter rateLimiter;

    @PostConstruct
    public void init() {
        /**
         * 不推荐使用这个线程池，因为会阻塞队列是无界的会压垮服务器
         */
        //executorService = Executors.newFixedThreadPool(20);
        executorService =
                new ThreadPoolExecutor(20, 100, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100));

        /* orderCreateRateLimiter = RateLimiter.create(300);*/

    }

    //生成验证码
    /*@RequestMapping(value = "/generateverifycode",method = {RequestMethod.GET,RequestMethod.POST})
    @ResponseBody
    public void generateverifycode(HttpServletResponse response) throws BusinessException, IOException {
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能生成验证码");
        }
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能生成验证码");
        }

        Map<String,Object> map = CodeUtil.generateCodeAndPic();

        redisTemplate.opsForValue().set("verify_code_"+userModel.getId(),map.get("code"));
        redisTemplate.expire("verify_code_"+userModel.getId(),10,TimeUnit.MINUTES);

        ImageIO.write((RenderedImage) map.get("codePic"), "jpeg", response.getOutputStream());


    }*/


    //生成秒杀令牌
    @RequestMapping(value = "/generatetoken", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType generatetoken(@RequestParam(name = "itemId") Integer itemId,
                                          @RequestParam(name = "promoId") Integer promoId,
                                          @RequestHeader(value = "Authorization") String authorization) throws BusinessException {
        //根据token获取用户信息
        /*String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
        }*/
        String userId = null;
        try {
            //解析令牌
            Map<String, Object> tokenMap = JwtTokenUtil.parseToken(authorization);
            userId = tokenMap.get("userId").toString();
        } catch (Exception e) {
            return CommonReturnType.create(null, "令牌无效！");
        }
        //获取用户的登陆信息
        String token = (String) redisTemplate.opsForValue().get(userId);
        if (token == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户登录过期，不能下单，请重新登录");
        }
        //获取用户的登陆信息
        /*UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
        }*/

        //通过verifycode验证验证码的有效性
        /*String redisVerifyCode = (String) redisTemplate.opsForValue().get("verify_code_"+userModel.getId());
        if(StringUtils.isEmpty(redisVerifyCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"请求非法");
        }
        if(!redisVerifyCode.equalsIgnoreCase(verifyCode)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"请求非法，验证码错误");
        }*/

        //获取秒杀访问令牌
        String promoToken = promoService.generateSecondKillToken(promoId, itemId, Integer.parseInt(userId));

        if (promoToken == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "生成令牌失败");
        }
        //返回对应的结果
        return CommonReturnType.create(promoToken);
    }

    //封装下单请求
    @RequestMapping(value = "/createorder", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name = "itemId") Integer itemId,
                                        @RequestParam(name = "amount") Integer amount,
                                        @RequestParam(name = "promoId", required = false) Integer promoId,
                                        @RequestParam(name = "promoToken", required = false) String promoToken,
                                        @RequestHeader(value = "Authorization") String authorization) throws BusinessException {

        //todo 本地限流
        //limiter.acquire() 表示消费一个令牌。当桶中有足够的令牌时，则直接返回0，否则阻塞，直到
        // 有可用的令牌数才返回，返回的值为阻塞的时间
        if (rateLimiter.acquire() > 0) {  //表示要等待
            throw new BusinessException(EmBusinessError.RATELIMIT);
        }


        /*String token = httpServletRequest.getParameterMap().get("token")[0];
        if(StringUtils.isEmpty(token)){
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN,"用户还未登陆，不能下单");
        }*/
        String userId = null;
        try {
            Map<String, Object> tokenMap = JwtTokenUtil.parseToken(authorization);
            userId = String.valueOf(tokenMap.get("userId"));
        } catch (Exception e) {
            return CommonReturnType.create(null, "令牌无效！");
        }
        //获取用户的登陆信息
        String token = (String) redisTemplate.opsForValue().get(userId);
        if (token == null) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户登录过期，不能下单，请重新登录");
        }
        //校验秒杀令牌是否正确
        if (promoId != null) {
            String inRedisPromoToken = (String) redisTemplate.opsForValue().get("promo_token_" + promoId + "_userid_" + userId + "_itemid_" + itemId);
            if (inRedisPromoToken == null) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }
            if (!org.apache.commons.lang3.StringUtils.equals(promoToken, inRedisPromoToken)) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "秒杀令牌校验失败");
            }
        }

        //同步调用线程池的submit方法
        //拥塞窗口为20的等待队列，用来队列化泄洪
        String finalUserId = userId;
        Future<Object> future = executorService.submit(new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                //一人一单
                if (redisTemplate.opsForValue().get("promoid_" + promoId + "_userid_" + finalUserId + "_itemid_" + itemId)!=null) {
                    //加入库存流水init状态
                    String stockLogId = itemService.initStockLog(itemId, amount);


                    //再去完成对应的下单事务型消息机制
                    if (!mqProducer.transactionAsyncReduceStock(Integer.parseInt(finalUserId), itemId, promoId, amount, stockLogId)) {  //推送消失
                        throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "您已下过单或网络卡顿请稍等");
                    }
                }

                return null;
            }
        });

        try {
            future.get();
        } catch (InterruptedException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR,"请尝试");
        } catch (ExecutionException e) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR,"您已下过单或网络卡顿请稍等");
        }
        return CommonReturnType.create(null);  //创建订单成功
    }
}
