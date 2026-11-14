package com.placemate.interview;

import java.time.Instant;
import com.placemate.application.Application;
import com.placemate.user.User;
import jakarta.persistence.*;

@Entity
@Table(name="interviews", indexes=@Index(name="idx_interview_application",columnList="application_id"))
public class Interview {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="application_id",nullable=false) private Application application;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="created_by_id",nullable=false) private User createdBy;
    @Column(nullable=false) private Instant scheduledAt;
    @Column(nullable=false) private Integer durationMinutes;
    @Column(nullable=false,length=30) private String mode;
    @Column(length=500) private String meetingLink;
    @Column(length=250) private String location;
    @Column(length=1000) private String notes;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private InterviewStatus status=InterviewStatus.SCHEDULED;
    @Column(nullable=false,updatable=false) private Instant createdAt;
    @Column(nullable=false) private Instant updatedAt;
    protected Interview(){}
    public Interview(Application application,User recruiter,InterviewRequest request){
        this.application=application;this.createdBy=recruiter;updateFrom(request);
    }
    @PrePersist void create(){Instant now=Instant.now();createdAt=now;updatedAt=now;}
    @PreUpdate void update(){updatedAt=Instant.now();}
    public Long getId(){return id;} public Application getApplication(){return application;}
    public User getCreatedBy(){return createdBy;} public Instant getScheduledAt(){return scheduledAt;}
    public Integer getDurationMinutes(){return durationMinutes;} public String getMode(){return mode;}
    public String getMeetingLink(){return meetingLink;} public String getLocation(){return location;}
    public String getNotes(){return notes;} public InterviewStatus getStatus(){return status;}
    public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
    public void updateFrom(InterviewRequest r){
        scheduledAt=r.scheduledAt();durationMinutes=r.durationMinutes();mode=r.mode().trim().toUpperCase();
        meetingLink=clean(r.meetingLink());location=clean(r.location());notes=clean(r.notes());
    }
    public void setStatus(InterviewStatus status){this.status=status;}
    private String clean(String s){return s==null||s.isBlank()?null:s.trim();}
}
