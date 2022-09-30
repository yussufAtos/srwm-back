package com.afp.iris.sr.wm;

import com.afp.iris.sr.wm.config.AppProperties;
import com.afp.iris.sr.wm.config.monitoring.SoftConfigurations;
import org.jasig.cas.client.boot.configuration.EnableCasClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableConfigurationProperties({ AppProperties.class, SoftConfigurations.class })
@SpringBootApplication
@EnableSwagger2
@EnableCasClient
public class WmApplication implements WebMvcConfigurer {

    public static void main(String[] args) {
        SpringApplicationBuilder app = new SpringApplicationBuilder(WmApplication.class);
        app.build().addListeners(new ApplicationPidFileWriter());
        app.run(args);
    }
}
