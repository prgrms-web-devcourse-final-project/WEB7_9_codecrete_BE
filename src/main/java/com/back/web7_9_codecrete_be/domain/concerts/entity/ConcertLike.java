package com.back.web7_9_codecrete_be.domain.concerts.entity;

import com.back.web7_9_codecrete_be.domain.users.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Getter
@RequiredArgsConstructor
@Table(name = "concert_like")
public class ConcertLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "concert_like_id")
    private Long concertLikeId;

    @ManyToOne
    private Concert concert;

    @ManyToOne
    private User user;
}
