package com.xiaoliu.test;

import com.xiaoliu.seckill.SeckillApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;

/**
 * Created with IDEA
 *
 * @Auther:XIAOLIU
 * @Date: 2022/08/12/22:20
 * @Description:
 */
@SpringBootTest(classes = SeckillApplication.class)
@RunWith(SpringRunner.class)
public class test {

    @Resource
    private DefaultRedisScript defaultRedisScript;

    @Resource
    private RedisTemplate redisTemplate;

    @Test
    public void  test01(){
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> valus = new ArrayList<>();
        keys.add("promoid_"+1+"_userid_"+1+"_itemid_"+1);  //用户是否存在
        keys.add("promo_item_stock_"+1);  //库存扣减
        keys.add("promo_item_stock_invalid_"+1);  //卖完的标志
        valus.add(String.valueOf(1));  //扣减数目
        valus.add("true");
        Object execute = redisTemplate.execute(defaultRedisScript, keys);
        System.out.println(execute);
    }
}
