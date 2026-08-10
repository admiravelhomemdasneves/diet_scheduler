package com.dietscheduler.backend.preferences;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "user_taste", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "taste_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTaste {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "taste_id", nullable = false)
    private UUID tasteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TastePreference preference;
}
