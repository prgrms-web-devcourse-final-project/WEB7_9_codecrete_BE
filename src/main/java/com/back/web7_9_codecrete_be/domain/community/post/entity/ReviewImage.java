package com.back.web7_9_codecrete_be.domain.community.post.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "review_image")
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private ReviewPost reviewPost;

    public static ReviewImage create(ReviewPost reviewPost, String imageUrl) {
        ReviewImage image = new ReviewImage();
        image.reviewPost = reviewPost;
        image.imageUrl = imageUrl;
        return image;
    }
}
