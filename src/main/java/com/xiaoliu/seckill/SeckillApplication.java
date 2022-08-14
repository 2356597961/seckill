package com.xiaoliu.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Created with IDEA
 *
 * @Auther:XIAOLIU
 * @Date: 2022/08/10/20:57
 * @Description:
 */
@SpringBootApplication
@MapperScan("com.xiaoliu.seckill.dao")
public class SeckillApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class,args);
    }
}
