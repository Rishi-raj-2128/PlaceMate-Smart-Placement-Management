package com.placemate.notification;

import java.time.Instant;

public record NotificationResponse(Long id,String type,String title,String message,String targetUrl,Instant createdAt,Instant readAt) {
    public static NotificationResponse from(Notification n){
        return new NotificationResponse(n.getId(),n.getType(),n.getTitle(),n.getMessage(),n.getTargetUrl(),n.getCreatedAt(),n.getReadAt());
    }
}
