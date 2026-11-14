package com.placemate.interview;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.placemate.application.*;
import com.placemate.notification.NotificationService;
import com.placemate.user.*;

@Service
public class InterviewService {
    private final InterviewRepository interviews;
    private final ApplicationRepository applications;
    private final UserRepository users;
    private final NotificationService notifications;
    public InterviewService(InterviewRepository interviews,ApplicationRepository applications,UserRepository users,NotificationService notifications){
        this.interviews=interviews;this.applications=applications;this.users=users;this.notifications=notifications;
    }
    @Transactional
    public InterviewResponse schedule(String email,Long applicationId,InterviewRequest request){
        User recruiter=require(email,Role.RECRUITER);
        Application app=applications.findById(applicationId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Application not found."));
        if(!app.getJob().getCreatedBy().getId().equals(recruiter.getId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You can only schedule interviews for applicants to your own jobs.");
        if(app.getStatus()==ApplicationStatus.REJECTED)throw new ResponseStatusException(HttpStatus.CONFLICT,"A rejected application cannot be scheduled for interview.");
        Interview interview=interviews.save(new Interview(app,recruiter,request));
        notifications.create(app.getStudent(),"INTERVIEW","Interview scheduled",
            "Your interview for "+app.getJob().getTitle()+" at "+app.getJob().getCompanyName()+" has been scheduled.",
            "/interviews");
        return InterviewResponse.from(interview);
    }
    @Transactional(readOnly=true)
    public List<InterviewResponse> mine(String email){User u=require(email,Role.STUDENT);return interviews.findByApplication_Student_IdOrderByScheduledAtAsc(u.getId()).stream().map(InterviewResponse::from).toList();}
    @Transactional(readOnly=true)
    public List<InterviewResponse> recruiterList(String email){User u=require(email,Role.RECRUITER);return interviews.findByCreatedBy_IdOrderByScheduledAtAsc(u.getId()).stream().map(InterviewResponse::from).toList();}
    @Transactional
    public InterviewResponse updateStatus(String email,Long id,InterviewStatus status){
        User u=require(email,Role.RECRUITER);Interview i=interviews.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Interview not found."));
        if(!i.getCreatedBy().getId().equals(u.getId()))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You can only update interviews you scheduled.");
        i.setStatus(status);
        notifications.create(i.getApplication().getStudent(),"INTERVIEW_UPDATE","Interview updated",
            "Your interview for "+i.getApplication().getJob().getTitle()+" is now "+status.name().toLowerCase()+".","/interviews");
        return InterviewResponse.from(interviews.save(i));
    }
    private User require(String email,Role role){
        User u=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account not found."));
        if(u.getRole()!=role)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Your account does not have permission for this action.");
        return u;
    }
}
