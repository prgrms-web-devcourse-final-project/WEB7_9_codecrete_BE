package com.back.web7_9_codecrete_be.domain.concerts.entity;

import com.back.web7_9_codecrete_be.domain.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@RequiredArgsConstructor
@Table(name = "concert_like")
public class ConcertLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_like_id")
    private Long concertLikeId;

    @ManyToOne(fetch = FetchType.LAZY)
    private Concert concert;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @CreationTimestamp
    private LocalDateTime createDate;

    public ConcertLike(Concert concert, User user) {
        this.concert = concert;
        this.user = user;
    }
}
