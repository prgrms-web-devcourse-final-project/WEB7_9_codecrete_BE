package com.back.web7_9_codecrete_be.domain.users.repository;

import com.back.web7_9_codecrete_be.domain.users.entity.User;
import com.back.web7_9_codecrete_be.domain.users.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    boolean existsByNickname(String nickname);
    List<User> findByIsDeletedTrueAndStatusAndDeletedDateBefore(
            UserStatus status,
            LocalDateTime time
    );
    Optional<User> findByEmailAndIsDeletedTrue(String email);
}
