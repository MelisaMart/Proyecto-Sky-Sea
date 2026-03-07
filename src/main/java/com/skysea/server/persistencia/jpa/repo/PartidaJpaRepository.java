package com.skysea.server.persistencia.jpa.repo;

import com.skysea.server.persistencia.jpa.entity.PartidaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartidaJpaRepository extends JpaRepository<PartidaEntity, String> {
    Optional<PartidaEntity> findFirstByActivaTrueOrderByFechaCreacionDesc();

    @Modifying
    @Query("update PartidaEntity p set p.activa = false where p.activa = true")
    int desactivarTodasLasActivas();

    @Modifying
    @Query("update PartidaEntity p set p.activa = false where p.activa = true and p.id <> :idActual")
    int desactivarActivasExcepto(@Param("idActual") String idActual);
}
