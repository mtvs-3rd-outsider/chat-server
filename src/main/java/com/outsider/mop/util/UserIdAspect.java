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
    public Mono<Object> injectUserId(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        // 메서드 시그니처와 매개변수 정보 가져오기
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = methodSignature.getMethod();
        Object[] args = proceedingJoinPoint.getArgs();
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();

        // ServerWebExchange를 인자로 직접 받아오기
        ServerWebExchange exchange = findServerWebExchange(args);
        if (exchange == null) {
            return Mono.error(new IllegalStateException("ServerWebExchange not found in method arguments"));
        }

        // HTTP 헤더에서 사용자 정보 가져오기
        String userIdHeader = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String roleStr = exchange.getRequest().getHeaders().getFirst("X-User-Role");
        String email = exchange.getRequest().getHeaders().getFirst("X-User-Email");
        String userName = exchange.getRequest().getHeaders().getFirst("X-User-Name");

        Long userId = userIdHeader != null ? Long.parseLong(userIdHeader) : 0L;
        Authority role = roleStr != null ? Authority.valueOf(roleStr.toUpperCase()) : null;

        // 매개변수의 어노테이션 확인 후 CustomUserInfoDTO에 사용자 정보 주입
        for (int i = 0; i < args.length; i++) {
            for (Annotation annotation : parameterAnnotations[i]) {
                if (annotation instanceof UserId && args[i] instanceof CustomUserInfoDTO) {
                    CustomUserInfoDTO customUserInfoDTO = (CustomUserInfoDTO) args[i];
                    customUserInfoDTO.setUserId(userId);
                    customUserInfoDTO.setRole(role);
                    customUserInfoDTO.setEmail(email);
                    customUserInfoDTO.setUsername(userName);
                }
            }
        }

        // 수정된 인자들로 메서드 실행
        return Mono.defer(() -> {
            try {
                Object result = proceedingJoinPoint.proceed(args);
                if (result instanceof Mono) {
                    return (Mono<Object>) result;
                } else {
                    return Mono.justOrEmpty(result);
                }
            } catch (Throwable throwable) {
                return Mono.error(throwable);
            }
        });
    }

    private ServerWebExchange findServerWebExchange(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof ServerWebExchange) {
                return (ServerWebExchange) arg;
            }
        }
        return null;
    }
}
