package com.skysea.server.persistencia.jpa;

import com.skysea.server.logica.model.Partida;
import com.skysea.server.persistencia.dao.IPartidaDAO;
import com.skysea.server.persistencia.jpa.entity.PartidaEntity;
import com.skysea.server.persistencia.jpa.mapper.PartidaJpaMapper;
import com.skysea.server.persistencia.jpa.repo.PartidaJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MySqlPartidaDAO implements IPartidaDAO {

    private final PartidaJpaRepository partidaRepository;
    private final PartidaJpaMapper mapper;

    public MySqlPartidaDAO(PartidaJpaRepository partidaRepository, PartidaJpaMapper mapper) {
        this.partidaRepository = partidaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public synchronized Partida loadActiva() {
        return partidaRepository.findFirstByActivaTrueOrderByFechaCreacionDesc()
                .map(mapper::toDomain)
                .orElseGet(() -> {
                    Partida nueva = new Partida();
                    save(nueva);
                    return nueva;
                });
    }

    @Override
    @Transactional
    public synchronized Optional<Partida> loadById(String idPartida) {
        if (idPartida == null || idPartida.isBlank()) {
            return Optional.empty();
        }
        return partidaRepository.findById(idPartida).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public synchronized List<PartidaReanudableInfo> findReanudablesByNombre(String nombreJugador) {
        if (nombreJugador == null || nombreJugador.isBlank()) {
            return List.of();
        }
        return partidaRepository.findReanudablesByNombre(nombreJugador).stream()
                .map(p -> new PartidaReanudableInfo(
                        p.getId(),
                        p.getEstado() != null ? p.getEstado().name() : null,
                        p.getEquipo() != null ? p.getEquipo().name() : null,
                        p.getTurnoNumero(),
                        p.getFechaCreacion() != null ? p.getFechaCreacion().toString() : null
                ))
                .toList();
    }

    @Override
    @Transactional
    public synchronized void save(Partida partida) {
        // En cualquier guardado mantenemos solo una partida activa: la actual.
        // Esto conserva historial (FINALIZADA y anteriores) sin borrar filas.
        partidaRepository.desactivarActivasExcepto(partida.getIdPartida());

        PartidaEntity entity = mapper.toEntity(partida);
        partidaRepository.findById(partida.getIdPartida()).ifPresent(existing -> {
            entity.setFechaCreacion(existing.getFechaCreacion());
            entity.setVersion(existing.getVersion());
        });
        if (entity.getFechaCreacion() == null) {
            entity.setFechaCreacion(LocalDateTime.now());
        }
        // Mantener la partida finalizada como "activa" para que state2/board
        // sigan apuntando al mismo juego y el frontend pueda mostrar fin de partida.
        // La creacion de una nueva partida queda a cargo de reset() o join() al detectar FINALIZADA.
        entity.setActiva(true);
        partidaRepository.save(entity);
    }

    @Override
    @Transactional
    public synchronized void reset() {
        // No se elimina historial: solo se cierra la activa y se abre una nueva.
        partidaRepository.desactivarTodasLasActivas();
        save(new Partida());
    }
}
