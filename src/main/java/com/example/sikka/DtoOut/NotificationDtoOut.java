package com.example.sikka.DtoOut;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationDtoOut {

    private Integer id;
    private Integer visitId;
    private String channel;
    private String type;
    private String title;
    private String message;
    private String status;
    private LocalDateTime scheduledAt;
    private LocalDateTime createdAt;
}