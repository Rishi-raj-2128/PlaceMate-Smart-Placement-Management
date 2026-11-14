package com.placemate.notification;

import java.time.Instant;
import com.placemate.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user_created", columnList = "user_id, createdAt")
})
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 40)
    private String type;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(nullable = false, length = 1000)
    private String message;
    @Column(length = 300)
    private String targetUrl;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    private Instant readAt;
    protected Notification() {}
    public Notification(User user, String type, String title, String message, String targetUrl) {
        this.user=user; this.type=type; this.title=title; this.message=message; this.targetUrl=targetUrl;
    }
    @PrePersist void onCreate(){createdAt=Instant.now();}
    public Long getId(){return id;}
    public User getUser(){return user;}
    public String getType(){return type;}
    public String getTitle(){return title;}
    public String getMessage(){return message;}
    public String getTargetUrl(){return targetUrl;}
    public Instant getCreatedAt(){return createdAt;}
    public Instant getReadAt(){return readAt;}
    public void markRead(){if(readAt==null)readAt=Instant.now();}
}
