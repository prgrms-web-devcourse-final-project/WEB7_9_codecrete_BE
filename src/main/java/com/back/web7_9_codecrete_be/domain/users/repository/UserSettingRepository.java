package com.back.web7_9_codecrete_be.domain.users.repository;

import com.back.web7_9_codecrete_be.domain.users.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
}
