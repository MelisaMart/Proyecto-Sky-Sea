package com.skysea.server.persistencia.jpa;

import com.skysea.server.logica.model.Partida;
import com.skysea.server.logica.model.EstadoPartida;
import com.skysea.server.persistencia.dao.IPartidaDAO;
import com.skysea.server.persistencia.jpa.entity.PartidaEntity;
import com.skysea.server.persistencia.jpa.mapper.PartidaJpaMapper;
import com.skysea.server.persistencia.jpa.repo.PartidaJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
    public synchronized void save(Partida partida) {
        PartidaEntity entity = mapper.toEntity(partida);
        partidaRepository.findById(partida.getIdPartida()).ifPresent(existing -> {
            entity.setFechaCreacion(existing.getFechaCreacion());
            entity.setVersion(existing.getVersion());
        });
        if (entity.getFechaCreacion() == null) {
            entity.setFechaCreacion(LocalDateTime.now());
        }
        entity.setActiva(partida.getEstado() != EstadoPartida.FINALIZADA);
        partidaRepository.save(entity);
    }

    @Override
    @Transactional
    public synchronized void reset() {
        partidaRepository.deleteAllInBatch();
        save(new Partida());
    }
}
