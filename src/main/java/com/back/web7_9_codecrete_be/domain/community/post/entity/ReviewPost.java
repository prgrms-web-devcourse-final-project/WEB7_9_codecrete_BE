package com.back.web7_9_codecrete_be.domain.community.post.entity;

import com.back.web7_9_codecrete_be.domain.community.post.dto.request.ReviewPostMultipartRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.request.ReviewPostUpdateMultipartRequest;
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

    @ElementCollection
    @CollectionTable(
            name = "review_tag",
            joinColumns = @JoinColumn(name = "review_post_id")
    )
    @Column(name = "tag", length = 30)
    private List<String> tags = new ArrayList<>();

    public static ReviewPost create(Post post, ReviewPostMultipartRequest req) {
        ReviewPost reviewPost = new ReviewPost();
        reviewPost.post = post;
        reviewPost.rating = req.getRating();
        reviewPost.tags = req.getTags();
        return reviewPost;
    }

    public void update(ReviewPostUpdateMultipartRequest req) {
        this.rating = req.getRating();
        this.tags.clear();
        if (req.getTags() != null) {
            this.tags.addAll(req.getTags());
        }
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
