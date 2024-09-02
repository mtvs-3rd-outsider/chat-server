package com.outsider.mop.util;

import com.outsider.mop.user.dto.Authority;
import com.outsider.mop.user.dto.CustomUserInfoDTO;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class UserIdAspect {

    @Around("execution(* com.outsider.mop..*Controller.*(.., @UserId (*), ..)) || execution(* com.outsider.mop..*Service.*(.., @UserId (*), ..))")
    public Object injectUserId(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        // 메서드 시그니처와 파라미터 정보를 가져옵니다.
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Object[] args = proceedingJoinPoint.getArgs();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        // ServerWebExchange 찾기
        Mono<ServerWebExchange> exchangeMono = findServerWebExchange(args);

        // ServerWebExchange가 있을 경우
        return exchangeMono.flatMap(exchange -> {
            Mono<Long> userIdMono = Mono.justOrEmpty(exchange.<Long>getAttribute("userId")).defaultIfEmpty(0L);
            Mono<String> roleStrMono = Mono.justOrEmpty(exchange.getAttribute("role"));
            Mono<String> emailMono = Mono.justOrEmpty(exchange.getAttribute("email"));
            Mono<String> userNameMono = Mono.justOrEmpty(exchange.getAttribute("userName"));

            return Mono.zip(userIdMono, roleStrMono, emailMono, userNameMono)
                    .flatMap(tuple -> {
                        Long userId = tuple.getT1();
                        String roleStr = tuple.getT2();
                        String email = tuple.getT3();
                        String userName = tuple.getT4();

                        Authority role = null;
                        if (roleStr != null) {
                            role = Authority.valueOf(roleStr.toUpperCase());
                        }

                        // 매개변수의 어노테이션 확인 후 userId 주입
                        for (int i = 0; i < args.length; i++) {
                            for (Annotation annotation : parameterAnnotations[i]) {
                                if (annotation instanceof UserId) {
                                    if (args[i] instanceof CustomUserInfoDTO) {
                                        CustomUserInfoDTO customUserInfoDTO = (CustomUserInfoDTO) args[i];
                                        customUserInfoDTO.setUserId(userId);
                                        customUserInfoDTO.setRole(role);
                                        customUserInfoDTO.setEmail(email);
                                        customUserInfoDTO.setUsername(userName);
                                    } else {
                                        return Mono.error(new IllegalArgumentException("Expected CustomUserInfoDTO, but found " + args[i].getClass().getName()));
                                    }
                                }
                            }
                        }

                        try {
                            Object result = proceedingJoinPoint.proceed(args);
                            if (result instanceof Mono) {
                                return (Mono<?>) result;
                            } else {
                                return Mono.justOrEmpty(result);
                            }
                        } catch (Throwable throwable) {
                            return Mono.error(throwable);
                        }
                    });
        }).defaultIfEmpty(proceedingJoinPoint.proceed(args)).onErrorResume(e -> Mono.error(new RuntimeException("AOP Processing failed", e)));
    }

    private Mono<ServerWebExchange> findServerWebExchange(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof ServerWebExchange) {
                return Mono.just((ServerWebExchange) arg);
            }
        }
        return Mono.empty();
    }
}
