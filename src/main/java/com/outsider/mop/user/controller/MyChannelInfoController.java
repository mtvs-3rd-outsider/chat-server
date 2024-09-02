package com.outsider.mop.user.controller;


import com.outsider.mop.user.dto.CustomUserInfoDTO;
import com.outsider.mop.user.dto.MyChannelInfoViewModel;
import com.outsider.mop.user.repository.MyChannelInfoRepository;
import com.outsider.mop.util.UserId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;


@RestController
@RequestMapping("/api/v1/my-channel")
public class MyChannelInfoController {

    private final MyChannelInfoRepository myChannelInfoRepository;

    // 생성자 주입을 사용하여 레포지토리를 주입합니다.
    public MyChannelInfoController(MyChannelInfoRepository myChannelInfoRepository) {
        this.myChannelInfoRepository = myChannelInfoRepository;
    }

    @GetMapping("/{userId}")
    public Mono<ResponseEntity<MyChannelInfoViewModel>> getUserData(@PathVariable Long userId) {
        // 레포지토리를 사용하여 데이터베이스에서 사용자 데이터를 가져옵니다.
        return myChannelInfoRepository.findById(userId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
