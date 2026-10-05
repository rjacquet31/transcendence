package com.transcendance.backend.undercover.entity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.transcendance.backend.undercover.entity.UndercoverParticipantEntity;

@Repository
public interface UndercoverParticipantRepository extends JpaRepository<UndercoverParticipantEntity, Long> {

}
