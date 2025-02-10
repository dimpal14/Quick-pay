package com.wp.org.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wp.org.entity.Notification;

@Repository
public interface NotificationRepo extends JpaRepository<Notification,Long> {
}
