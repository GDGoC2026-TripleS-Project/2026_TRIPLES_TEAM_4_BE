package com.gdg.unimatebackend.notification.repository;

import com.gdg.unimatebackend.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByEventKey(String eventKey);
}
