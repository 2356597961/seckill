package com.xiaoliu.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Created with IDEA
 *
 * @Auther:XIAOLIU
 * @Date: 2022/08/10/20:59
 * @Description:
 */
@EnableSwagger2
@Configuration
public class SwaggerConfig {

    @Bean
    public Docket createRestApi(){
       return new Docket(DocumentationType.SWAGGER_2)
               .apiInfo(apiInfo())
               .select()
               .apis(RequestHandlerSelectors.basePackage("com.xiaoliu.seckill.controller"))
               .paths(PathSelectors.any())
               .build();
    }

    private ApiInfo apiInfo() {
       return new ApiInfoBuilder()
               .title("秒杀系统")
               .contact(new Contact("xiaoliu","http://xiaoliuyl.com","2356597961@qq.com"))
               .description("秒杀系统的api文档")
               .version("1.0.0")
               .build();
    }
}
