package org.example.server.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String fullName;
    private Double heightCm;
    private Double weightKg;
    private String gender;
    private String allergies;
    private String dietaryPreference;
    private Integer targetCalories;
    private String activityLevel;
    private LocalDate birthDate;
}
