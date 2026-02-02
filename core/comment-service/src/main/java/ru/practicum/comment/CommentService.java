package ru.practicum.comment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "interaction.client")
@EnableDiscoveryClient
public class CommentService {
    public static void main(String[] args) {
        SpringApplication.run(CommentService.class, args);
    }
}
