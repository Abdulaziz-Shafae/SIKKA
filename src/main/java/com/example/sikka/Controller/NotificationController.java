package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/get/my-notifications/{accountId}")
    public ResponseEntity<?> getMyNotifications(@PathVariable Integer accountId) {
        return ResponseEntity.status(200).body(notificationService.getMyNotifications(accountId));
    }

    @GetMapping("/get/details/{accountId}/{notificationId}")
    public ResponseEntity<?> getNotificationDetails(@PathVariable Integer accountId, @PathVariable Integer notificationId) {
        return ResponseEntity.status(200).body(notificationService.getNotificationDetails(accountId, notificationId));
    }

    @DeleteMapping("/delete/{accountId}/{notificationId}")
    public ResponseEntity<?> deleteNotification(@PathVariable Integer accountId, @PathVariable Integer notificationId) {
        notificationService.deleteNotification(accountId, notificationId);
        return ResponseEntity.status(200).body(new ApiResponse("Notification deleted successfully"));
    }

    @PutMapping("/read/{accountId}/{notificationId}")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Integer accountId, @PathVariable Integer notificationId) {
        notificationService.markNotificationAsRead(accountId, notificationId);
        return ResponseEntity.status(200).body(new ApiResponse("Notification marked as read successfully"));
    }

    @GetMapping("/unread-count/{accountId}")
    public ResponseEntity<?> getUnreadNotificationsCount(@PathVariable Integer accountId) {
        return ResponseEntity.status(200).body(notificationService.getUnreadNotificationsCount(accountId));
    }
}
