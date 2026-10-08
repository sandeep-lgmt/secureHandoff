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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

 
@Entity
@Table(name = "check_in_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

  public class CheckInConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    private User owner;

    @Column(nullable = false)
    @Builder.Default
    private int frequencyDays = 14;

    @Column(nullable = false)
    private Instant lastCheckInAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CheckInStatus status = CheckInStatus.ACTIVE;

    public enum CheckInStatus {
        ACTIVE,     // checking in on schedule
        OVERDUE,    // missed a check-in; escalation event has been fired
        RELEASED    // consensus reached, packets released to trustees
    }

}
