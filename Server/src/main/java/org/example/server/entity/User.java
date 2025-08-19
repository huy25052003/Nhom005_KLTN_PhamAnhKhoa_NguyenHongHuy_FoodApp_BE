package org.example.server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table (name = "users" , uniqueConstraints = @UniqueConstraint(columnNames = "username"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length = 100)
    private String username;

    @Column(nullable=false, length = 200)
    private String password; // đã mã hoá BCrypt

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<String> roles;


}
