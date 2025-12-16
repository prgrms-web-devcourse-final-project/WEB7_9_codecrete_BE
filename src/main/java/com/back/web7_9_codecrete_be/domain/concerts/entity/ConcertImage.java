package com.back.web7_9_codecrete_be.domain.concerts.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
public class ConcertImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Concert concert;

    @Column(name = "image_url")
    private String imageUrl;

    public ConcertImage(Concert concert, String imageUrl) {
        this.concert = concert;
        this.imageUrl = imageUrl;
    }
}
