package org.seba.oksign;

import org.seba.oksign.config.OksignProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(OksignProperties.class)
@SpringBootApplication
public class OksignApplication {

    public static void main(String[] args) {
        SpringApplication.run(OksignApplication.class, args);
    }

}
