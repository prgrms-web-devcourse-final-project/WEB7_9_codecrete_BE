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

    // 소셜 로그인 사용자는 password가 없을 수 있으므로 nullable = true
    @Column(length = 100)
    private String password;

    // 소셜 로그인 시 생년월일을 불러오지 못하므로 nullable = true
    @Column(nullable = true)
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

    // 소셜 로그인용 컬럼 추가
    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 20)
    private SocialType socialType;

    @Column(name = "social_id", length = 100)
    private String socialId;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserSetting userSetting;

    @Builder
    public User(String email,
                String nickname,
                String password,
                LocalDate birth,
                String profileImage,
                SocialType socialType,
                String socialId) {

        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.birth = birth;
        this.profileImage = profileImage;
        this.socialType = socialType;
        this.socialId = socialId;

        // 기본값 세팅
        this.role = Role.USER;
        this.status = UserStatus.ACTIVE;
        this.isDeleted = false;
        this.createdDate = LocalDateTime.now();
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void updateBirth(LocalDate birth) {
        this.birth = birth;
    }

    public void softDelete() {
        this.isDeleted = true;
        this.status = UserStatus.DELETED;
        this.deletedDate = LocalDateTime.now();
    }

    public void promoteToAdmin() {
        this.role = Role.ADMIN;
    }

    public void restore() {
        this.isDeleted = false;
        this.status = UserStatus.ACTIVE;
        this.deletedDate = null;
    }

    public void initSetting() {
        this.userSetting = new UserSetting(this);
    }
}