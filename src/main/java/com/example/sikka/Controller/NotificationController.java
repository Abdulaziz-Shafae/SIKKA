package com.example.sikka.Controller;

import com.example.sikka.Api.ApiResponse;
import com.example.sikka.DtoOut.NotificationDtoOut;
import jakarta.validation.Valid;
import com.example.sikka.DtoIn.RespondDelayDtoIn;
import com.example.sikka.Service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import com.example.sikka.Model.Account;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification")
public class NotificationController {

    private final NotificationService notificationService;
    private final com.example.sikka.Service.RideNotificationActionService rideNotificationActionService;

    @GetMapping("/get/my-notifications")
    public ResponseEntity<?> getMyNotifications(@AuthenticationPrincipal Account account) {
        return ResponseEntity.status(200).body(notificationService.getMyNotifications(account.getId()));
    }

    @GetMapping("/get/details/{notificationId}")
    public ResponseEntity<?> getNotificationDetails(@AuthenticationPrincipal Account account, @PathVariable Integer notificationId) {
        return ResponseEntity.status(200).body(notificationService.getNotificationDetails(account.getId(), notificationId));
    }

    @DeleteMapping("/delete/{notificationId}")
    public ResponseEntity<?> deleteNotification(@AuthenticationPrincipal Account account, @PathVariable Integer notificationId) {
        notificationService.deleteNotification(account.getId(), notificationId);
        return ResponseEntity.status(200).body(new ApiResponse("Notification deleted successfully"));
    }

    @PutMapping("/read/{notificationId}")
    public ResponseEntity<?> markNotificationAsRead(@AuthenticationPrincipal Account account, @PathVariable Integer notificationId) {
        notificationService.markNotificationAsRead(account.getId(), notificationId);
        return ResponseEntity.status(200).body(new ApiResponse("Notification marked as read successfully"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadNotificationsCount(@AuthenticationPrincipal Account account) {
        return ResponseEntity.status(200).body(notificationService.getUnreadNotificationsCount(account.getId()));
    }

    @PutMapping("/ride-action/{notificationId}/{action}")
    public ResponseEntity<?> applyRideNotificationAction(@AuthenticationPrincipal Account account, @PathVariable Integer notificationId, @PathVariable String action) {
        return ResponseEntity.ok(new ApiResponse(rideNotificationActionService.applyRideNotificationAction(account.getId(), notificationId, action)));
    }

    @PutMapping("/respond-delay/{notificationId}")
    public ResponseEntity<NotificationDtoOut> respondToDelay(@AuthenticationPrincipal Account account, @PathVariable Integer notificationId, @RequestBody @Valid RespondDelayDtoIn respondDelayDtoIn) {
        return ResponseEntity.status(200).body(notificationService.respondToDelay(account.getId(), notificationId, respondDelayDtoIn));
    }
    @PostMapping("/check-delay/{activitySessionId}")
    public ResponseEntity<?> checkActivityDelay(@AuthenticationPrincipal Account account, @PathVariable Integer activitySessionId) {
        NotificationDtoOut result = notificationService.checkActivityDelay(account.getId(), activitySessionId);
        if (result == null) {
            return ResponseEntity.status(200).body(new com.example.sikka.Api.ApiResponse("No activity delay detected"));
        }
        return ResponseEntity.status(200).body(result);
    }

}