package com.securehandoff.securehandoff.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "release_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class ReleaseRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private int requiredConfirmations; // e.g. 2, computed as min(2, totalAcceptedTrustees) at creation time

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant quorumMetAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public enum Status {
        PENDING,
        QUORUM_MET,
        EXPIRED
    }

}
