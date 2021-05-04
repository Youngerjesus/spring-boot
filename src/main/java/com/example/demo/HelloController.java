package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
public class HelloController {

    @Autowired
    ApplicationAvailability applicationAvailability;

    @Autowired
    MyProperties myProperties;

    @GetMapping("/hello")
    public String hello() throws UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();
        return "Application is now " + applicationAvailability.getLivenessState() + " " + applicationAvailability.getReadinessState() + " "
                + "host-address: " + localHost.getHostAddress() + " "
                + "host-name: " + localHost.getHostName() + " "
                + "my-message:" + myProperties.getMessage();
    }
}
