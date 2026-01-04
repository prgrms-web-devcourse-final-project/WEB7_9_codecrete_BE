package com.back.web7_9_codecrete_be.domain.community.post.entity;

import com.back.web7_9_codecrete_be.domain.community.post.dto.request.JoinPostCreateRequest;
import com.back.web7_9_codecrete_be.domain.community.post.dto.request.JoinPostUpdateRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "join_post")
public class JoinPost {

    @Id
    private Long postId;

    @Column(nullable = false)
    private Long concertId;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false)
    private Integer currentParticipants = 1;

    @Enumerated(EnumType.STRING)
    private GenderPreference genderPreference;

    private Integer ageRangeMin;
    private Integer ageRangeMax;

    private LocalDateTime meetingAt;

    @Column(length = 100)
    private String meetingPlace;

    @Enumerated(EnumType.STRING)
    private JoinStatus status;

    @ElementCollection
    @CollectionTable(
            name = "join_activity_tag",
            joinColumns = @JoinColumn(name = "join_post_id")
    )
    @Column(name = "tag")
    private List<String> activityTags = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "post_id")
    private Post post;

    public static JoinPost create(Post post, JoinPostCreateRequest req) {
        JoinPost joinPost = new JoinPost();
        joinPost.post = post;
        joinPost.postId = post.getPostId();
        joinPost.concertId = req.getConcertId();
        joinPost.maxParticipants = req.getMaxParticipants();
        joinPost.genderPreference = req.getGenderPreference();
        joinPost.ageRangeMin = req.getAgeRangeMin();
        joinPost.ageRangeMax = req.getAgeRangeMax();
        joinPost.meetingAt = req.getMeetingAt();
        joinPost.meetingPlace = req.getMeetingPlace();
        joinPost.activityTags = req.getActivityTags();
        joinPost.status = JoinStatus.OPEN;
        return joinPost;
    }

    public void update(JoinPostUpdateRequest req) {
        this.maxParticipants = req.getMaxParticipants();
        this.genderPreference = req.getGenderPreference();
        this.ageRangeMin = req.getAgeRangeMin();
        this.ageRangeMax = req.getAgeRangeMax();
        this.meetingAt = req.getMeetingAt();
        this.meetingPlace = req.getMeetingPlace();
        this.activityTags = req.getActivityTags();
    }

    public void close() {
        this.status = JoinStatus.CLOSED;
    }
}

