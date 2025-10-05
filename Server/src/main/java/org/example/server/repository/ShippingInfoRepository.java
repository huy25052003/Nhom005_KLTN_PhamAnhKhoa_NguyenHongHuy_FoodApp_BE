package org.example.server.repository;

import org.example.server.entity.ShippingInfo;
import org.example.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShippingInfoRepository extends JpaRepository<ShippingInfo, Long> {
    Optional<ShippingInfo> findByUser(User user);
    Optional<ShippingInfo> findFirstByUserId(Long userId);
}
