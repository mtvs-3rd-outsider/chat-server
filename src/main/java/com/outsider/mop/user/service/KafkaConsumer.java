package com.outsider.mop.user.service;


import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class KafkaConsumer {



    @KafkaListener(topics = "test-topic", groupId = "test-group")
    public void listen(String message) {
        processMessage(message)
                .subscribe(
                        success -> System.out.println("Processed message successfully"),
                        error -> System.err.println("Failed to process message: " + error.getMessage())
                );
    }

    private Mono<Void> processMessage(String message) {
        return Mono.fromRunnable(() -> {
            // Process the message here in a non-blocking way if possible
            System.out.println("Received message: " + message);
            // Example: Save the message to a database or perform some other operations
        });
    }
}
