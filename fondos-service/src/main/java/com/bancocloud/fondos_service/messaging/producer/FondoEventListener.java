package com.bancocloud.fondos_service.messaging.producer;

import com.bancocloud.fondos_service.messaging.event.ValorCuotaActualizadoEvent;
import com.bancocloud.fondos_service.service.FondoEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía a RabbitMQ los eventos de fondos solo después del commit,
 * para no anunciar cambios de valor cuota que terminaron en rollback.
 */
@Component
@RequiredArgsConstructor
public class FondoEventListener {

    private final FondoEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onValorCuotaActualizado(ValorCuotaActualizadoEvent event) {
        publisher.publicarValorCuotaActualizado(event);
    }
}
