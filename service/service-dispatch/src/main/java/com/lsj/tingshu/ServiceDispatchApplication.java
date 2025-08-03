package com.lsj.tingshu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ServiceDispatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ServiceDispatchApplication.class, args);
    }

}
