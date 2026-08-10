package com.dietscheduler.backend.preferences;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Shared reference table. Also used for choice-based ingredient exclusions (e.g. "no pork"). */
@Entity
@Table(name = "taste", uniqueConstraints = @UniqueConstraint(columnNames = {"type", "name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Taste {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TasteType type;

    @Column(nullable = false)
    private String name;
}
