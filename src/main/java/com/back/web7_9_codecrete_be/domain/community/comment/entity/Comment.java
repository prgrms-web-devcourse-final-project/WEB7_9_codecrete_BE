package com.back.web7_9_codecrete_be.domain.community.comment.entity;

import com.back.web7_9_codecrete_be.domain.community.post.entity.Post;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 500)
    private String content;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;

    @Builder
    private Comment(Post post, Long userId, String content) {
        this.post = post;
        this.userId = userId;
        this.content = content;
    }

    // 생성
    public static Comment create(Post post, Long userId, String content) {
        return Comment.builder()
                .post(post)
                .userId(userId)
                .content(content)
                .build();
    }

    // 수정
    public void update(String content) {
        this.content = content;
    }
}
