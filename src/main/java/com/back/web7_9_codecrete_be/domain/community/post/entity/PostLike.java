package com.back.web7_9_codecrete_be.domain.community.post.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(
        name = "post_like",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"post_id", "user_id"})
        }
)
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_like_id")
    private Long postLikeId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Builder
    private PostLike(Long postId, Long userId) {
        this.postId = postId;
        this.userId = userId;
    }

    // 좋아요 생성
    public static PostLike create(Long postId, Long userId) {
        return PostLike.builder()
                .postId(postId)
                .userId(userId)
                .build();
    }
}
