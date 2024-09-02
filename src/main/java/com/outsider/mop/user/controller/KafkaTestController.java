package com.outsider.mop.user.controller;

import com.outsider.mop.user.service.KafkaProducer;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class KafkaTestController {

    private final KafkaProducer kafkaProducer;

    public KafkaTestController(KafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @PostMapping("/send")
    public Mono<String> sendMessage(@RequestParam String message) {
        return Mono.fromRunnable(() -> kafkaProducer.sendMessage("test-topic", message))
                .then(Mono.just("Message sent: " + message));
    }
}
