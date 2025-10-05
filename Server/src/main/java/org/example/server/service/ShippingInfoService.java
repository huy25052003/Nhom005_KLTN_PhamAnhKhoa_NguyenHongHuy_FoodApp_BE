package org.example.server.service;

import lombok.RequiredArgsConstructor;
import org.example.server.entity.ShippingInfo;
import org.example.server.entity.User;
import org.example.server.repository.ShippingInfoRepository;
import org.example.server.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShippingInfoService {
    private final ShippingInfoRepository shippingRepo;
    private final UserRepository userRepo;

    private User requireUser(Authentication auth) {
        return userRepo.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional(readOnly = true)
    public ShippingInfo getMy(Authentication auth) {
        User u = requireUser(auth);
        return shippingRepo.findByUser(u).orElseGet(() -> {
            ShippingInfo s = new ShippingInfo();
            s.setUser(u);
            return s;
        });
    }

    @Transactional
    public ShippingInfo upsertMy(Authentication auth, Map<String, Object> body) {
        User u = requireUser(auth);
        ShippingInfo s = shippingRepo.findByUser(u).orElse(null);
        if (s == null) {
            s = new ShippingInfo();
            s.setUser(u);
        }
        // map body
        if (body.containsKey("phone"))       s.setPhone(str(body.get("phone")));
        if (body.containsKey("addressLine")) s.setAddressLine(str(body.get("addressLine")));
        if (body.containsKey("city"))        s.setCity(str(body.get("city")));
        if (body.containsKey("note"))        s.setNote(str(body.get("note")));

        if (isBlank(s.getPhone()) || isBlank(s.getAddressLine()) || isBlank(s.getCity())) {
            throw new IllegalArgumentException("phone, addressLine, city are required");
        }
        return shippingRepo.save(s);
    }

    @Transactional
    public ShippingInfo snapshotForOrder(User user) {
        ShippingInfo def = shippingRepo.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Bạn chưa cấu hình địa chỉ giao hàng"));
        ShippingInfo snap = ShippingInfo.builder()
                .phone(def.getPhone())
                .addressLine(def.getAddressLine())
                .city(def.getCity())
                .note(def.getNote())
                .build();
        return shippingRepo.save(snap);
    }

    private static String str(Object v) { return v == null ? null : String.valueOf(v).trim(); }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
