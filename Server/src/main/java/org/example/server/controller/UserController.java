package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.User;
import org.example.server.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService UserService;

    @GetMapping
    public ResponseEntity<Page<User>> listUsers(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<User> users = UserService.listUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = UserService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<User> updateUserRoles(@PathVariable Long id, @RequestBody Set<String> roles) {
        User updatedUser = UserService.updateUserRoles(id, roles);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication authentication) {
        String currentAdminUsername = authentication.getName();
        UserService.deleteUser(id, currentAdminUsername);
        return ResponseEntity.noContent().build();
    }


}
