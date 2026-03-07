package com.skysea.server.persistencia.config;

import com.skysea.server.persistencia.dao.IGameStateDAO;
import com.skysea.server.persistencia.memory.InMemoryGameStateDAO;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DaoConfig {

    @Bean
    public IGameStateDAO gameStateDAO() {
        return new InMemoryGameStateDAO();
    }
}
