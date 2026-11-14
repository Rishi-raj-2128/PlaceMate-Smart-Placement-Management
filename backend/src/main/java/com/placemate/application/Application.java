package com.placemate.application;

import java.time.Instant;
import com.placemate.job.Job;
import com.placemate.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "job_applications",
    uniqueConstraints = @UniqueConstraint(name = "uk_application_student_job", columnNames = {"student_id", "job_id"}))
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(length = 1000)
    private String recruiterNote;

    @Column(nullable = false, updatable = false)
    private Instant appliedAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Application() {}

    public Application(User student, Job job) {
        this.student = student;
        this.job = job;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        appliedAt = now;
        updatedAt = now;
        if (status == null) status = ApplicationStatus.APPLIED;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public User getStudent() { return student; }
    public Job getJob() { return job; }
    public ApplicationStatus getStatus() { return status; }
    public String getRecruiterNote() { return recruiterNote; }
    public Instant getAppliedAt() { return appliedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateStatus(ApplicationStatus status, String note) {
        this.status = status;
        this.recruiterNote = note == null || note.isBlank() ? null : note.trim();
    }
}
