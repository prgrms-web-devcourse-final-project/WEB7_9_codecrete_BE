package com.back.web7_9_codecrete_be.global.storage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FileDeleteQueueRepository extends JpaRepository<FileDeleteQueue, Long> {
    List<FileDeleteQueue> findAllByDeleteAtBefore(LocalDateTime now);
}
