package com.bancocloud.analitica_service.repository;

import com.bancocloud.analitica_service.model.ValorCuotaHistorico;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ValorCuotaHistoricoRepository extends JpaRepository<ValorCuotaHistorico, Long> {

    List<ValorCuotaHistorico> findByFondoIdOrderByFechaAscIdAsc(Long fondoId);

    Optional<ValorCuotaHistorico> findFirstByFondoIdOrderByFechaDescIdDesc(Long fondoId);

    @Query("select distinct v.fondoId from ValorCuotaHistorico v")
    List<Long> fondosConValorCuota();
}
