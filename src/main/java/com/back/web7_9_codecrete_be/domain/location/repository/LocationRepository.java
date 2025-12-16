package com.back.web7_9_codecrete_be.domain.location.repository;

import com.back.web7_9_codecrete_be.domain.location.entity.Location;
import com.back.web7_9_codecrete_be.domain.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {


    Location findByUser(User user);

    boolean existsByUser(User user);
}
