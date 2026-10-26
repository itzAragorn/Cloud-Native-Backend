package com.bancocloud.usuarios_service.messaging.producer;

import com.bancocloud.usuarios_service.messaging.event.UsuarioEstadoCambiadoEvent;
import com.bancocloud.usuarios_service.messaging.event.UsuarioRegistradoEvent;
import com.bancocloud.usuarios_service.service.UsuarioEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Envía a RabbitMQ los eventos de usuarios solo después del commit,
 * para no anunciar cambios que terminaron en rollback.
 */
@Component
@RequiredArgsConstructor
public class UsuarioEventListener {

    private final UsuarioEventPublisher publisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUsuarioRegistrado(UsuarioRegistradoEvent event) {
        publisher.publicarUsuarioRegistrado(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEstadoCambiado(UsuarioEstadoCambiadoEvent event) {
        publisher.publicarEstadoCambiado(event);
    }
}
