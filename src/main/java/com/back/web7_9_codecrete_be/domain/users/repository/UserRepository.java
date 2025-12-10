package com.back.web7_9_codecrete_be.domain.users.repository;

import com.back.web7_9_codecrete_be.domain.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
