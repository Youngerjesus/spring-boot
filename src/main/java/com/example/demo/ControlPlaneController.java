package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class ControlPlaneController {
    @Autowired
    ApplicationEventPublisher applicationEventPublisher;

    @GetMapping("/block")
    public String block(){
        AvailabilityChangeEvent.publish(applicationEventPublisher, this, ReadinessState.REFUSING_TRAFFIC);
        return "Blocked Requests";
    }

    @GetMapping("/turnoff")
    public String turnoff(){
        AvailabilityChangeEvent.publish(applicationEventPublisher, this, LivenessState.BROKEN);
        return "Broken";
    }

    @Async
    @EventListener
    public void readinessStateChanged(AvailabilityChangeEvent<ReadinessState> readiness) throws InterruptedException {
      log.info("State is changed to " + readiness.getState());
      if(readiness.getState() == ReadinessState.REFUSING_TRAFFIC){
          Thread.sleep(5000L);
          AvailabilityChangeEvent.publish(applicationEventPublisher, this, ReadinessState.ACCEPTING_TRAFFIC);
      }
    }
}
