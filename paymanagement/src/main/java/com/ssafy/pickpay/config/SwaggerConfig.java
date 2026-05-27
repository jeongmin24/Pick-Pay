package com.ssafy.pickpay.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PickPay API 명세서")
                        .description("SSAFY PickPay 프로젝트의 유저 및 결제 관련 API 명세서입니다.")
                        .version("v1.0.0"));
    }
}