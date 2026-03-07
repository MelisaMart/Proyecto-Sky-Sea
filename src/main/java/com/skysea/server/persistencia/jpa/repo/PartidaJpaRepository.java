package com.skysea.server.persistencia.jpa.repo;

import com.skysea.server.persistencia.jpa.entity.PartidaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartidaJpaRepository extends JpaRepository<PartidaEntity, String> {
    Optional<PartidaEntity> findFirstByActivaTrueOrderByFechaCreacionDesc();
}
