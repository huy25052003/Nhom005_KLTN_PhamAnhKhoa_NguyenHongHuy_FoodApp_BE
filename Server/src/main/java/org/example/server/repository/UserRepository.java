package org.example.server.repository;

import org.example.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.lang.String;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r LIKE %:role%")
    List<User> findByRolesContaining(@Param("role") String role);
}