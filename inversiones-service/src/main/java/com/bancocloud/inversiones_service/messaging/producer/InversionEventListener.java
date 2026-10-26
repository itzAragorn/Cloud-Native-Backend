package com.bancocloud.inversiones_service.messaging.producer;

import com.bancocloud.inversiones_service.messaging.event.InversionCreadaEvent;
import com.bancocloud.inversiones_service.service.InversionEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía a RabbitMQ los eventos de inversiones solo después del commit,
 * para no anunciar inversiones que terminaron en rollback.
 */
@Component
@RequiredArgsConstructor
public class InversionEventListener {

    private final InversionEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInversionCreada(InversionCreadaEvent event) {
        publisher.publicarInversionCreada(event);
    }
}
