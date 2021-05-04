package com.example.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConstructorBinding
@ConfigurationProperties("my")
public class MyProperties {

    private String message;

    public String getMessage() {
        return message;
    }

    public MyProperties(String message) {
        this.message = message;
    }
}
