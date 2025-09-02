package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.Cart;
import org.example.server.entity.CartItem;
import org.example.server.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping
    public ResponseEntity<Cart> getMyCart(Authentication auth) {
        return ResponseEntity.ok(cartService.getCart(auth));
    }

    @PostMapping("/items")
    public ResponseEntity<CartItem> addItem(Authentication auth, @RequestParam Long productId, @RequestParam(defaultValue = "1") int quantity) {
        return ResponseEntity.ok(cartService.addItem(auth, productId, Math.max(quantity, 1)));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<CartItem> updateQty(Authentication auth, @PathVariable Long itemId, @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateItem(auth, itemId, Math.max(quantity, 1)));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> remove(Authentication auth, @PathVariable Long itemId) {
        cartService.removeItem(auth, itemId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(Authentication auth) {
        cartService.clear(auth);
        return ResponseEntity.noContent().build();
    }
}
