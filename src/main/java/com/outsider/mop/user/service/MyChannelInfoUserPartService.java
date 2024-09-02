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

import static com.outsider.mop.util.Formatter.formatJoinDate;

@Service
public class MyChannelInfoUserPartService {

    private MyChannelInfoRepository repository;
    private final ObjectMapper mapper;

    public MyChannelInfoUserPartService(MyChannelInfoRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }
    @KafkaListener(topics = "dbserver1.forecasthub.user")
    @Transactional
    public Mono<Void> consumeUser(ConsumerRecord<String, String> record) throws JsonProcessingException {
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
        MyChannelInfoViewModel mongoUser = MyChannelInfoViewModel.builder()
                .id(after.get("user_id").asLong()) // "user_id" 필드를 사용하여 id 설정
                .name(after.get("user_name").asText()) // "user_name" 필드를 사용하여 이름 설정
                .userEmail(after.get("user_email").asText()) // "user_email"을 username에 설정
                .birthdate(after.has("birthday") ? formatJoinDate(after, "birthday")  : null) // "birthday"를 사용하여 생년월일 설정
                .joinedDate(formatJoinDate(after, "join_date")) // "join_date"를 문자열로 변환하여 설정
                .gender(after.has("user_gender") ? after.get("user_gender").asText() : null) // "user_gender"를 사용하여 성별 설정
                .points(new BigDecimal(Base64.getDecoder().decode(after.get("user_point").asText())[0] & 0xFF)) // Base64 디코딩하여 정수 값으로 변환 후 points 설정
                .tier_level(after.get("tier_level").asInt()) // "tier_level"을 tier_level에 설정
                .transactions(0) // transactions 필드는 초기값으로 설정 (데이터가 없는 경우)
                .profitRate(null) // profitRate는 초기값으로 null 설정
                .positionValue(null) // positionValue는 초기값으로 null 설정
                .tradeCount(0) // tradeCount는 초기값으로 설정
                .build();

        return repository.save(mongoUser)
        .then();
    }

    private Mono<Void> handleUpdate(JsonNode after) {
        Long userId = after.get("user_id").asLong();
        return repository.findById(userId)
                .flatMap(existingUser -> {
                    MyChannelInfoViewModel updatedUser = MyChannelInfoViewModel.builder()
                            .name(after.get("user_name").asText())
                            .userEmail(after.get("user_email").asText())
                            .location(after.has("user_location") ? after.get("user_location").asText() : existingUser.getLocation())
                            .birthdate(after.has("birthday") ? formatJoinDate(after, "birthday"): existingUser.getBirthdate())
                            .joinedDate(after.has("join_date") ? formatJoinDate(after, "join_date"): existingUser.getJoinedDate())
                            .gender(after.has("user_gender") ? after.get("user_gender").asText() : existingUser.getGender())
                            .points(new BigDecimal(Base64.getDecoder().decode(after.get("user_point").asText())[0] & 0xFF))
                            .tier_level(after.get("tier_level").asInt())
                            .build();

                    return repository.save(updatedUser);
                })
                .then();
    }

    private Mono<Void> handleDelete(JsonNode before) {
        Long userId = before.get("user_id").asLong();

        return repository.deleteById(userId)
                .then();
    }

}