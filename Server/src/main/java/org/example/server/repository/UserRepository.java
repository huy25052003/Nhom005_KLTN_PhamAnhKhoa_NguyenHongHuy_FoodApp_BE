package org.example.server.repository;

import org.example.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findByRolesContaining(String role);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
}