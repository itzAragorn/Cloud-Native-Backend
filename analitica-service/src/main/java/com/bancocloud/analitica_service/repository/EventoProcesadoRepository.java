package com.bancocloud.analitica_service.repository;

import com.bancocloud.analitica_service.model.EventoProcesado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventoProcesadoRepository extends JpaRepository<EventoProcesado, String> {
}
