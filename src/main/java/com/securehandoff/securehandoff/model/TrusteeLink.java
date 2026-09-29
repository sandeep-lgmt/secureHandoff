package com.securehandoff.securehandoff.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
@Entity
@Table(name = "trustee_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrusteeLink {
     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // Nullable until the invited person registers/accepts and is linked to a real User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trustee_user_id")
    private User trusteeUser;

    @Column(nullable = false)
    private String trusteeEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TrusteeStatus status = TrusteeStatus.PENDING;

    @Column(nullable = false, unique = true)
    private String inviteToken;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant respondedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public enum TrusteeStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        REVOKED
    }

}
