package com.example.sikka.Service;

import com.example.sikka.Repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegistrationFollowupCleanupService {
    private final NotificationRepository notificationRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cancelLegacyPendingFollowups() {
        notificationRepository.cancelPendingRegistrationFollowups("أُلغيت المتابعة المؤجلة؛ يُرسل الترحيب ورمز التفعيل مرة واحدة عند التسجيل.");
    }
}
