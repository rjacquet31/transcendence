package com.transcendance.backend.undercover.entity.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.transcendance.backend.undercover.entity.UndercoverPlayerEntity;

@Repository
public interface UndercoverPlayerRepository extends JpaRepository<UndercoverPlayerEntity, UUID> {

}
