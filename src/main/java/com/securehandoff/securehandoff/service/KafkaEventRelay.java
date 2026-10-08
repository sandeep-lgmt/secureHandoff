package com.securehandoff.securehandoff.service;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.securehandoff.securehandoff.config.KafkaConfig;
import com.securehandoff.securehandoff.event.EscalationEvent;
import com.securehandoff.securehandoff.event.ReleaseEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Forwards domain events to Kafka only AFTER the database transaction that produced them
 * has committed, so a rolled-back transaction can never leak an event.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventRelay {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEscalation(EscalationEvent event) {
        send(KafkaConfig.ESCALATION_TOPIC, String.valueOf(event.ownerId()), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRelease(ReleaseEvent event) {
        send(KafkaConfig.RELEASE_TOPIC, String.valueOf(event.ownerId()), event);
    }

    private void send(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload).whenComplete((result, error) -> {
            if (error != null) {
                log.error("Failed to publish event to topic {}: {}", topic, error.getMessage());
            }
        });
    }
}
