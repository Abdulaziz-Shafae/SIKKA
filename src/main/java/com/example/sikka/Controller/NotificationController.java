package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoOut.NotificationDtoOut;
import com.example.sikka.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/get/my-notifications/{accountId}")
    public ResponseEntity<List<NotificationDtoOut>> getMyNotifications(@PathVariable Integer accountId) {
        return ResponseEntity.status(200).body(notificationService.getMyNotifications(accountId));
    }

    @GetMapping("/get/details/{accountId}/{notificationId}")
    public ResponseEntity<NotificationDtoOut> getNotificationDetails(@PathVariable Integer accountId, @PathVariable Integer notificationId) {
        return ResponseEntity.status(200).body(notificationService.getNotificationDetails(accountId, notificationId));
    }

    @DeleteMapping("/delete/{accountId}/{notificationId}")
    public ResponseEntity<ApiResponse> deleteNotification(@PathVariable Integer accountId, @PathVariable Integer notificationId) {
        notificationService.deleteNotification(accountId, notificationId);
        return ResponseEntity.status(200).body(new ApiResponse("Notification deleted successfully"));
    }
}