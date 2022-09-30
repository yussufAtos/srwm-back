package com.afp.iris.sr.wm.config.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Bean
    public Docket privateApiDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
        		.groupName("private")
        		.select()
        		.apis(RequestHandlerSelectors.withMethodAnnotation(PrivateApi.class))
                .paths(PathSelectors.any())
                .build();
    }
    
    @Bean
    public Docket publicApiDocket() {
        return new Docket(DocumentationType.SWAGGER_2)
        		.groupName("public")
        		.select()
        		.apis(RequestHandlerSelectors.withMethodAnnotation(PublicApi.class))
                .paths(PathSelectors.any())
                .build();
    }
}
