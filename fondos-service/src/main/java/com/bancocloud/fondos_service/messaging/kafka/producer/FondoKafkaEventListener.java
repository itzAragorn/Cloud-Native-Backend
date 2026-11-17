package com.bancocloud.fondos_service.messaging.kafka.producer;

import com.bancocloud.fondos_service.messaging.event.ValorCuotaActualizadoEvent;
import com.bancocloud.fondos_service.service.ValorCuotaStreamPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía a Kafka los cambios de valor cuota solo después del commit, igual que el listener de
 * RabbitMQ ({@code messaging.producer.FondoEventListener}), pero por separado: cada canal tiene
 * su propio listener y un error en uno no impide que el otro publique.
 */
@Component
@RequiredArgsConstructor
public class FondoKafkaEventListener {

    private final ValorCuotaStreamPublisher streamPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onValorCuotaActualizado(ValorCuotaActualizadoEvent event) {
        streamPublisher.publicarValorCuota(event);
    }
}
