package com.placemate.notification;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.placemate.user.User;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    public NotificationService(NotificationRepository repository){this.repository=repository;}

    @Transactional
    public void create(User recipient,String type,String title,String message,String targetUrl){
        if(recipient!=null) repository.save(new Notification(recipient,type,title,message,targetUrl));
    }
    @Transactional(readOnly=true)
    public List<NotificationResponse> list(Long userId){
        return repository.findTop100ByUser_IdOrderByCreatedAtDesc(userId).stream().map(NotificationResponse::from).toList();
    }
    @Transactional(readOnly=true)
    public long unreadCount(Long userId){return repository.countByUser_IdAndReadAtIsNull(userId);}
    @Transactional
    public NotificationResponse markRead(Long userId,Long id){
        Notification n=repository.findByIdAndUser_Id(id,userId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Notification not found."));
        n.markRead();return NotificationResponse.from(repository.save(n));
    }
    @Transactional
    public void markAllRead(Long userId){
        repository.findByUser_IdAndReadAtIsNull(userId).forEach(Notification::markRead);
    }
}
