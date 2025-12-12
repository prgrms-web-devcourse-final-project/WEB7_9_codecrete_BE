package com.back.web7_9_codecrete_be.domain.email.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "verification_code")
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expireAt;

    @Builder
    public VerificationCode(String email, String code, LocalDateTime expireAt) {
        this.email = email;
        this.code = code;
        this.expireAt = expireAt;
    }

    public boolean isExpired() {
        return expireAt.isBefore(LocalDateTime.now());
    }
}
