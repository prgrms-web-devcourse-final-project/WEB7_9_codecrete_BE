package com.back.web7_9_codecrete_be.domain.community.post.entity;

import com.back.web7_9_codecrete_be.domain.community.comment.entity.Comment;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "nickname", nullable = false, length = 20)
    private String nickname;

    @Column(nullable = false, length = 30)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostCategory category;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "modified_date", nullable = false)
    private LocalDateTime modifiedDate;

    // 연관관계
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private ReviewPost reviewPost;

    public void addReviewPost(ReviewPost reviewPost) {
        this.reviewPost = reviewPost;
    }

    @OneToOne(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private JoinPost joinPost;

    public void addJoinPost(JoinPost joinPost) {
        this.joinPost = joinPost;
    }

    @Builder
    private Post(Long userId, String nickname, String title, String content, PostCategory category) {
        this.userId = userId;
        this.nickname = nickname;
        this.title = title;
        this.content = content;
        this.category = category;
    }

    public static Post create(Long userId, String nickname, String title, String content, PostCategory category) {
        return Post.builder()
                .userId(userId)
                .nickname(nickname)
                .title(title)
                .content(content)
                .category(category)
                .build();
    }

    // 수정
    public void update(String title, String content, PostCategory category) {
        this.title = title;
        this.content = content;
        this.category = category;
    }
}
