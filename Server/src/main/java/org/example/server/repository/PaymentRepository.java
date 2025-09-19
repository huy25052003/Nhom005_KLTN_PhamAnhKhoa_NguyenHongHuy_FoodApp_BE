package org.example.server.repository;

import org.example.server.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Payment findByPayosOrderId(String payosOrderId);
}