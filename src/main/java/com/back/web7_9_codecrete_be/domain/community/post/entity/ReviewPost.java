package com.back.web7_9_codecrete_be.domain.community.post.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "review_post")
public class ReviewPost {

    @Id
    private Long postId;

    @Column(name = "concert_id", nullable = false)
    private Long concertId;

    @Column(nullable = false)
    private Integer rating; // 0~5

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "post_id")
    private Post post;

    @OneToMany(
            mappedBy = "reviewPost",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ReviewImage> images = new ArrayList<>();

    public static ReviewPost create(Post post, Long concertId, Integer rating) {
        ReviewPost review = new ReviewPost();
        review.post = post;
        review.postId = post.getPostId();
        review.concertId = concertId;
        review.rating = rating;
        return review;
    }

    public void updateRating(Integer rating) {
        this.rating = rating;
    }

    public void addImage(ReviewImage image) {
        images.add(image);
    }

    public void clearImages() {
        images.clear();
    }

    public void removeImage(ReviewImage image) {
        images.remove(image);
    }
}
