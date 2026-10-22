package com.example.sikka.Repository;

import com.example.sikka.Model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Notification findNotificationById(Integer id);

    List<Notification> findAllByAccount_IdAndChannelAndStatusOrderByCreatedAtDescIdDesc(Integer accountId, String channel, String status);

    long countByAccount_IdAndChannelAndStatusAndReadAtIsNull(Integer accountId, String channel, String status);

}