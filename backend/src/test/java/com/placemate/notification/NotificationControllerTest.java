package com.placemate.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import com.placemate.user.User;
import com.placemate.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

class NotificationControllerTest {
    private NotificationService notifications;
    private UserRepository users;
    private NotificationController controller;

    @BeforeEach
    void setUp() {
        notifications=mock(NotificationService.class);
        users=mock(UserRepository.class);
        controller=new NotificationController(notifications,users);
    }

    @Test
    void listUsesAuthenticatedUsersId() {
        User user=user(41L);
        Authentication auth=auth("student@example.com");
        NotificationResponse notification=new NotificationResponse(9L,"APPLICATION","New application","A student applied.","/applications",null,null);
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));
        when(notifications.list(41L)).thenReturn(List.of(notification));

        List<NotificationResponse> result=controller.list(auth);

        assertEquals(1,result.size());
        assertEquals(9L,result.get(0).id());
        verify(notifications).list(41L);
    }

    @Test
    void unreadCountUsesAuthenticatedUsersId() {
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user(41L)));
        when(notifications.unreadCount(41L)).thenReturn(2L);

        assertEquals(2L,controller.unreadCount(auth("student@example.com")));

        verify(notifications).unreadCount(41L);
    }

    @Test
    void markReadUsesAuthenticatedUsersIdAndNotificationId() {
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user(41L)));
        NotificationResponse response=new NotificationResponse(9L,"INTERVIEW","Interview","Scheduled.","/interviews",null,null);
        when(notifications.markRead(41L,9L)).thenReturn(response);

        NotificationResponse result=controller.read(auth("student@example.com"),9L);

        assertEquals(9L,result.id());
        verify(notifications).markRead(41L,9L);
    }

    @Test
    void markAllReadUsesAuthenticatedUsersId() {
        when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user(41L)));

        assertDoesNotThrow(()->controller.readAll(auth("student@example.com")));

        verify(notifications).markAllRead(41L);
    }

    @Test
    void unknownAuthenticatedAccountIsRejected() {
        when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        ResponseStatusException error=assertThrows(ResponseStatusException.class,
            ()->controller.list(auth("missing@example.com")));

        assertEquals(HttpStatus.UNAUTHORIZED,error.getStatusCode());
        verifyNoInteractions(notifications);
    }

    private Authentication auth(String email) {
        Authentication auth=mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }

    private User user(Long id) {
        User user=mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}
