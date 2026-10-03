package com.securehandoff.securehandoff.service;

import org.springframework.kafka.core.KafkaTemplate;
import com.securehandoff.config.KafkaConfig;
import com.securehandoff.event.EscalationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EscaltionProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishEscalation(EscalationEvent event) {
        // Keyed by ownerId so all events for the same owner land on the same partition,
        // preserving per-owner ordering.
        kafkaTemplate.send(KafkaConfig.ESCALATION_TOPIC, event.ownerId().toString(), event);
    }

}
