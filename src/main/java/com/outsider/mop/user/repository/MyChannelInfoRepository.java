package com.outsider.mop.user.repository;

import com.outsider.mop.user.dto.MyChannelInfoViewModel;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;


@Repository
public interface MyChannelInfoRepository extends ReactiveMongoRepository<MyChannelInfoViewModel, Long> {
    Mono<MyChannelInfoViewModel> findById(Long id);
}
