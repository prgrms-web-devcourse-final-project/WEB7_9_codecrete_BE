package com.back.web7_9_codecrete_be.domain.email.repository;

import com.back.web7_9_codecrete_be.domain.email.entity.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findByEmail(String email);

    void deleteByEmail(String email);
}
