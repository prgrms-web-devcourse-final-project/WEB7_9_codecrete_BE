package com.back.web7_9_codecrete_be.domain.email.repository;

import com.back.web7_9_codecrete_be.domain.email.entity.VerifiedEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerifiedEmailRepository extends JpaRepository<VerifiedEmail, String> {
    boolean existsByEmail(String email);
    void deleteByEmail(String email);
}
