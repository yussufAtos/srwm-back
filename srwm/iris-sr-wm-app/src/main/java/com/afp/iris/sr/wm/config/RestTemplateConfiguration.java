package com.afp.iris.sr.wm.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfiguration {

    @Bean
    public RestTemplate cmsRestTemplate() {
        return new RestTemplateBuilder().build();
    }

    @Bean
    public RestTemplate scomRestTemplate() {
        return new RestTemplateBuilder().build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
