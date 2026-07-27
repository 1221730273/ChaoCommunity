package com.ljc.chaocommunity.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ChaoCommunity论坛后端接口文档")
                        .version("1.0.0")
                        .description("社区论坛后台API")
                        .contact(new Contact().name("ljc"))
                );
    }
}