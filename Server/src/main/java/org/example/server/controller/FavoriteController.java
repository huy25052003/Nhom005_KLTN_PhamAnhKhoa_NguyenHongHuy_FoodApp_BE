package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Product;
import org.example.server.service.FavoriteService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    @PostMapping("/{productId}/toggle")
    public ResponseEntity<Map<String,Object>> toggle(Authentication auth, @PathVariable Long productId) {
        boolean favorite = favoriteService.toggle(auth, productId);
        return ResponseEntity.ok(Map.of("favorite", favorite));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<Void> add(Authentication auth, @PathVariable Long productId) {
        favoriteService.add(auth, productId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> remove(Authentication auth, @PathVariable Long productId) {
        favoriteService.remove(auth, productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Map<String,Object>> stat(Authentication auth, @PathVariable Long productId) {
        return ResponseEntity.ok(favoriteService.stat(auth, productId));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<Product>> my(Authentication auth,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(favoriteService.myFavorites(auth, page, size));
    }
}
