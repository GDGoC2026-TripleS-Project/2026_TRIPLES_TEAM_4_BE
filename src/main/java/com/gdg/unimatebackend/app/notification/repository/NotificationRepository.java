package com.gdg.unimatebackend.app.notification.repository;

import com.gdg.unimatebackend.app.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByEventKey(String eventKey);
}
