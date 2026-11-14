package com.placemate.notification;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.placemate.user.User;
import com.placemate.user.UserRepository;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notifications;
    private final UserRepository users;
    public NotificationController(NotificationService notifications,UserRepository users){this.notifications=notifications;this.users=users;}

    @GetMapping
    public List<NotificationResponse> list(Authentication auth){return notifications.list(current(auth).getId());}
    @GetMapping("/unread-count")
    public long unreadCount(Authentication auth){return notifications.unreadCount(current(auth).getId());}
    @PatchMapping("/{id}/read")
    public NotificationResponse read(Authentication auth,@PathVariable Long id){return notifications.markRead(current(auth).getId(),id);}
    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void readAll(Authentication auth){notifications.markAllRead(current(auth).getId());}
    private User current(Authentication auth){
        return users.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account not found."));
    }
}
