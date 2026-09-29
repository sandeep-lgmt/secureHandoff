package com.securehandoff.securehandoff.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "release_confirmations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"release_request_id", "trustee_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReleaseConfirmation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "release_request_id", nullable = false)
    private ReleaseRequest releaseRequest;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "trustee_id", nullable = false)
    private User trustee;

    @Column(nullable = false, updatable = false)
    private Instant confirmedAt;

    @PrePersist
    void onCreate() {
        this.confirmedAt = Instant.now();
    }

}
