package com.back.web7_9_codecrete_be.domain.plans.repository;

import com.back.web7_9_codecrete_be.domain.plans.entity.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface RouteRepository extends JpaRepository<Route, Long> {

}