package com.example.restaurant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI restaurantOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("餐厅点餐系统 API")
                        .version("1.0.0")
                        .description("餐厅点餐系统后端接口文档")
                        .license(new License().name("Course Design")));
    }
}
