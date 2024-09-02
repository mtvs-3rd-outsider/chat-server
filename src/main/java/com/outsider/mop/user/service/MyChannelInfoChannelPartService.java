package com.outsider.mop.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.outsider.mop.user.dto.MyChannelInfoViewModel;
import com.outsider.mop.user.repository.MyChannelInfoRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
@Service
public class MyChannelInfoChannelPartService {
    private final MyChannelInfoRepository repository;
    private final ObjectMapper mapper;

    public MyChannelInfoChannelPartService(MyChannelInfoRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @KafkaListener(topics = "dbserver1.forecasthub.my_channel")
    @Transactional
    public Mono<Void> consumeMyChannel(ConsumerRecord<String, String> record) throws JsonProcessingException {
        String consumedValue = record.value();
        var jsonNode = mapper.readTree(consumedValue);
        JsonNode payload = jsonNode.path("payload");
        String op = payload.get("op").toString().substring(1, 2);
        JsonNode after = payload.path("after");
        JsonNode before = payload.path("before");

        switch (op) {
            case "c":
                return handleCreate(after);
            case "u":
                return handleUpdate(after);
            case "d":
                return handleDelete(before);
            default:
                return Mono.empty();
        }
    }

    private Mono<Void> handleCreate(JsonNode after) {
        MyChannelInfoViewModel mongoChannel = MyChannelInfoViewModel.builder()
                .id(after.get("id").asLong())
                .bio(after.has("bio") ? after.get("bio").asText() : null)
                .displayName(after.get("display_name").asText())
                .website(after.has("website") ? after.get("website").asText() : null)
                .followersCount(after.get("followers_count").asInt())
                .followingCount(after.get("following_count").asInt())
                .build();

        return repository.save(mongoChannel)
                .then();
    }

    private Mono<Void> handleUpdate(JsonNode after) {
        Long id = after.get("id").asLong();

        return repository.findById(id)
                .flatMap(existingChannel -> {
                    MyChannelInfoViewModel updatedChannel = MyChannelInfoViewModel.builder()
                            .username(String.valueOf(after.get("user_id").asLong()))
                            .bio(after.has("bio") ? after.get("bio").asText() : existingChannel.getBio())
                            .displayName(after.get("display_name").asText())
                            .website(after.has("website") ? after.get("website").asText() : existingChannel.getWebsite())
                            .followersCount(after.get("followers_count").asInt())
                            .followingCount(after.get("following_count").asInt())
                            .build();

                    return repository.save(updatedChannel);
                })
                .then();
    }

    private Mono<Void> handleDelete(JsonNode before) {
        Long id = before.get("id").asLong();

        return repository.deleteById(id)
                .then();
    }
}
