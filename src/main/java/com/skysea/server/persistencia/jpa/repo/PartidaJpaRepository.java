package com.skysea.server.persistencia.jpa.repo;

import com.skysea.server.persistencia.jpa.entity.PartidaEntity;
import com.skysea.server.logica.model.Equipo;
import com.skysea.server.logica.model.EstadoPartida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PartidaJpaRepository extends JpaRepository<PartidaEntity, String> {
    interface PartidaReanudableProjection {
        String getId();
      EstadoPartida getEstado();
      Equipo getEquipo();
        int getTurnoNumero();
        LocalDateTime getFechaCreacion();
    }

    Optional<PartidaEntity> findFirstByActivaTrueOrderByFechaCreacionDesc();

    @Query("""
            select p.id as id,
                   p.estado as estado,
                   j.equipo as equipo,
                   p.turnoNumero as turnoNumero,
                   p.fechaCreacion as fechaCreacion
            from PartidaEntity p
            join p.jugadores j
            where upper(j.nombre) = upper(:nombre)
              and p.isReanudable = true
            order by p.fechaCreacion desc
            """)
    List<PartidaReanudableProjection> findReanudablesByNombre(@Param("nombre") String nombre);

    @Query("""
            select case when count(j) > 0 then true else false end
            from JugadorEntity j
            join j.partida p
            where upper(j.nombre) = upper(:nombre)
              and p.activa = true
              and (
                p.estado = com.skysea.server.logica.model.EstadoPartida.EN_JUEGO
                or p.estado = com.skysea.server.logica.model.EstadoPartida.ESPERANDO_RIVAL
              )
            """)
    boolean existsNombreEnPartidaActiva(@Param("nombre") String nombre);

    @Modifying
    @Query("update PartidaEntity p set p.activa = false where p.activa = true")
    int desactivarTodasLasActivas();

    @Modifying
    @Query("update PartidaEntity p set p.activa = false where p.activa = true and p.id <> :idActual")
    int desactivarActivasExcepto(@Param("idActual") String idActual);
}
