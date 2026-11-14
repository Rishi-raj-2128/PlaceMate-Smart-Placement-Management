package com.placemate.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.placemate.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock private NotificationRepository repository;
    @InjectMocks private NotificationService service;

    @Test
    void createPersistsNotificationForRecipient() {
        User recipient=mock(User.class);

        service.create(recipient,"APPLICATION","Application received","A student applied.","/applications");

        ArgumentCaptor<Notification> captor=ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        Notification saved=captor.getValue();
        assertSame(recipient,saved.getUser());
        assertEquals("APPLICATION",saved.getType());
        assertEquals("Application received",saved.getTitle());
        assertEquals("A student applied.",saved.getMessage());
        assertEquals("/applications",saved.getTargetUrl());
    }

    @Test
    void createIgnoresNullRecipient() {
        service.create(null,"APPLICATION","Title","Message","/applications");

        verifyNoInteractions(repository);
    }

    @Test
    void listReturnsOnlyRepositoryResultsAsNotificationResponses() {
        Notification notification=notification(12L,"INTERVIEW","Interview scheduled","Check your schedule.","/interviews");
        when(repository.findTop100ByUser_IdOrderByCreatedAtDesc(4L)).thenReturn(List.of(notification));

        List<NotificationResponse> result=service.list(4L);

        assertEquals(1,result.size());
        assertEquals(12L,result.get(0).id());
        assertEquals("INTERVIEW",result.get(0).type());
        assertEquals("Interview scheduled",result.get(0).title());
        assertEquals("/interviews",result.get(0).targetUrl());
        verify(repository).findTop100ByUser_IdOrderByCreatedAtDesc(4L);
    }

    @Test
    void unreadCountIsScopedToRequestedUser() {
        when(repository.countByUser_IdAndReadAtIsNull(4L)).thenReturn(3L);

        assertEquals(3L,service.unreadCount(4L));

        verify(repository).countByUser_IdAndReadAtIsNull(4L);
    }

    @Test
    void markReadUpdatesAndReturnsNotificationOwnedByUser() {
        Notification notification=notification(12L,"APPLICATION","New application","Review it.","/applications");
        when(repository.findByIdAndUser_Id(12L,4L)).thenReturn(Optional.of(notification));
        when(repository.save(notification)).thenReturn(notification);

        NotificationResponse result=service.markRead(4L,12L);

        verify(notification).markRead();
        verify(repository).save(notification);
        assertEquals(12L,result.id());
    }

    @Test
    void markReadCannotAccessAnotherUsersNotification() {
        when(repository.findByIdAndUser_Id(12L,4L)).thenReturn(Optional.empty());

        ResponseStatusException error=assertThrows(ResponseStatusException.class,
            ()->service.markRead(4L,12L));

        assertEquals(HttpStatus.NOT_FOUND,error.getStatusCode());
        verify(repository,never()).save(any());
    }

    @Test
    void markAllReadMarksOnlyUnreadNotificationsReturnedForUser() {
        Notification first=mock(Notification.class);
        Notification second=mock(Notification.class);
        when(repository.findByUser_IdAndReadAtIsNull(4L)).thenReturn(List.of(first,second));

        service.markAllRead(4L);

        verify(first).markRead();
        verify(second).markRead();
        verify(repository).findByUser_IdAndReadAtIsNull(4L);
        verify(repository,never()).save(any());
    }

    private Notification notification(Long id,String type,String title,String message,String targetUrl) {
        Notification notification=mock(Notification.class);
        when(notification.getId()).thenReturn(id);
        when(notification.getType()).thenReturn(type);
        when(notification.getTitle()).thenReturn(title);
        when(notification.getMessage()).thenReturn(message);
        when(notification.getTargetUrl()).thenReturn(targetUrl);
        when(notification.getCreatedAt()).thenReturn(Instant.parse("2026-11-14T12:00:00Z"));
        return notification;
    }
}
