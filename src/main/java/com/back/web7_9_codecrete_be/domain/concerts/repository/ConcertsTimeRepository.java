package com.back.web7_9_codecrete_be.domain.concerts.repository;

import com.back.web7_9_codecrete_be.domain.concerts.entity.ConcertsTimeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConcertsTimeRepository extends JpaRepository<ConcertsTimeEntity, Long> {
}
