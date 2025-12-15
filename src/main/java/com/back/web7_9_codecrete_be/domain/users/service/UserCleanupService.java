package com.back.web7_9_codecrete_be.domain.users.service;

import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.entity.UserStatus;
import com.back.web7_9_codecrete_be.domain.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCleanupService {

    private final UserRepository userRepository;

    @Transactional
    public void cleanup(LocalDateTime time) {
        List<User> targets =
                userRepository.findByIsDeletedTrueAndStatusAndDeletedDateBefore(UserStatus.DELETED, time);

        userRepository.deleteAll(targets);
    }
}
