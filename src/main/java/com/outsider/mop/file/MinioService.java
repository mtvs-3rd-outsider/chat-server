package com.outsider.mop.file;


import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.ErrorResponseException;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class MinioService {

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    private static final char separator = '_';

    public Mono<Boolean> doesFileExist(String filename) {
        return Mono.fromCallable(() -> {
            try {
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(filename)
                                .build()
                );
                return true;
            } catch (ErrorResponseException e) {
                // 파일이 존재하지 않을 때
                return false;
            }
        }).subscribeOn(Schedulers.boundedElastic()); // 블로킹 작업을 별도의 스레드 풀에서 수행
    }

    public Mono<String> uploadFile(FilePart filePart) {
        return Mono.fromCallable(() -> {
            StringBuilder sb = new StringBuilder();
            sb.append(UUID.randomUUID());
            sb.append(separator);
            sb.append(filePart.filename());

            boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!isExist) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            return sb.toString();
        }).flatMap(objectName -> {
            // 비동기적으로 FilePart를 DataBuffer로 변환 후 MinIO에 업로드
            return DataBufferUtils.join(filePart.content())
                    .publishOn(Schedulers.boundedElastic())
                    .flatMap(dataBuffer -> {
                        Path tempFile = null;
                        try {
                            tempFile = Files.createTempFile("upload-", filePart.filename());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            Files.write(tempFile, dataBuffer.asByteBuffer().array());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        Path finalTempFile = tempFile;
                        return Mono.fromCallable(() -> {
                            minioClient.putObject(
                                    PutObjectArgs.builder()
                                            .bucket(bucketName)
                                            .object(objectName)
                                            .stream(Files.newInputStream(finalTempFile), Files.size(finalTempFile), -1)
                                            .contentType(Files.probeContentType(finalTempFile))
                                            .build()
                            );
                            Files.delete(finalTempFile);
                            return objectName;
                        });
                    });
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<String> getFileUrl(String filename) {
        return Mono.fromCallable(() -> {
            try {
                // 객체가 존재하는지 확인
                minioClient.statObject(
                        StatObjectArgs.builder()
                                .bucket(bucketName)
                                .object(filename)
                                .build()
                );

                // 객체가 존재하면 프리사인드 URL 생성
                return minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucketName)
                                .object(filename)
                                .expiry(24 * 60 * 60) // 24시간 유효
                                .build()
                );
            } catch (ErrorResponseException e) {
                // MinIO 관련 오류 처리
                throw new RuntimeException("Error accessing MinIO: " + e.getMessage());
            } catch (Exception e) {
                // 기타 예외 처리
                throw new RuntimeException("An unexpected error occurred: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic()); // 블로킹 호출을 별도의 스레드 풀에서 처리
        }
}