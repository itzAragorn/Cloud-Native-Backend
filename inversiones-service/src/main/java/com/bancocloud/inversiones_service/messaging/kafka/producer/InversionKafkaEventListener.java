package com.bancocloud.inversiones_service.messaging.kafka.producer;

import com.bancocloud.inversiones_service.messaging.event.InversionCreadaEvent;
import com.bancocloud.inversiones_service.service.InversionStreamPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía a Kafka las inversiones solo después del commit, igual que el listener de RabbitMQ
 * ({@code messaging.producer.InversionEventListener}), pero por separado: cada canal tiene su
 * propio listener y un error en uno no impide que el otro publique.
 */
@Component
@RequiredArgsConstructor
public class InversionKafkaEventListener {

    private final InversionStreamPublisher streamPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInversionCreada(InversionCreadaEvent event) {
        streamPublisher.publicarInversionRegistrada(event);
    }
}
