package com.placemate.interview;

import java.time.Instant;

public record InterviewResponse(Long id,Long applicationId,Long jobId,String jobTitle,String companyName,
    Long studentId,String studentName,String studentEmail,Instant scheduledAt,Integer durationMinutes,
    String mode,String meetingLink,String location,String notes,InterviewStatus status,Instant createdAt) {
    public static InterviewResponse from(Interview i){
        var a=i.getApplication();var s=a.getStudent();var j=a.getJob();
        return new InterviewResponse(i.getId(),a.getId(),j.getId(),j.getTitle(),j.getCompanyName(),
            s.getId(),s.getFullName(),s.getEmail(),i.getScheduledAt(),i.getDurationMinutes(),i.getMode(),
            i.getMeetingLink(),i.getLocation(),i.getNotes(),i.getStatus(),i.getCreatedAt());
    }
}
