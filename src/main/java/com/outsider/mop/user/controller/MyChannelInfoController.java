package com.outsider.mop.user.controller;


import com.outsider.mop.user.dto.CustomUserInfoDTO;
import com.outsider.mop.user.dto.MyChannelInfoResponseDTO;
import com.outsider.mop.util.UserId;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/my-channel")
public class MyChannelInfoController {

    @GetMapping("/{userId}")
    public Mono<ResponseEntity<MyChannelInfoResponseDTO>> getUserData(@PathVariable String userId ,@UserId CustomUserInfoDTO id) {
        System.out.println(id);
        MyChannelInfoResponseDTO user = new MyChannelInfoResponseDTO(
                "홍길동",
                "honggildong",
                "This is a short bio about the user.",
                "Earth",
                "master-of-prediction.com",
                "November 7, 1987",
                "Joined November 2010",
                "남",
                30000,
                100,
                "200%",
                "200p",
                200,
                100,
                200
        );
        return Mono.just(ResponseEntity.ok(user));
    }
}
