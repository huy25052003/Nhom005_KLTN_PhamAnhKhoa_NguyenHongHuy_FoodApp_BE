package org.example.server.controller;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.ShippingInfo;
import org.example.server.service.ShippingInfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shipping/me")
@RequiredArgsConstructor
public class ShippingInfoController {
    private final ShippingInfoService shippingService;

    @GetMapping
    public ResponseEntity<ShippingInfo> getMy(Authentication auth) {
        return ResponseEntity.ok(shippingService.getMy(auth));
    }

    @PutMapping
    public ResponseEntity<ShippingInfo> upsertMy(Authentication auth, @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(shippingService.upsertMy(auth, body));
    }
}
