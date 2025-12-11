package com.back.web7_9_codecrete_be.domain.users.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(nullable = false, unique = true, length = 20)
    private String nickname;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false)
    private LocalDate birth;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @CreatedDate
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted;

    @Builder
    public User(String email,
                String nickname,
                String password,
                LocalDate birth,
                String profileImage) {

        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.birth = birth;
        this.profileImage = profileImage;

        // 기본값 세팅
        this.role = Role.USER;
        this.status = UserStatus.ACTIVE;
        this.isDeleted = false;
        this.createdDate = LocalDateTime.now();
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}

