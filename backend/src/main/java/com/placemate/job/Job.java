package com.placemate.job;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.placemate.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "jobs")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 10000)
    private String description;

    @Column(nullable = false, length = 150)
    private String companyName;

    @Column(nullable = false, length = 150)
    private String location;

    @Column(nullable = false, length = 40)
    private String employmentType;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMin;

    @Column(precision = 12, scale = 2)
    private BigDecimal salaryMax;

    @Column(precision = 4, scale = 2)
    private BigDecimal minCgpa;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_required_skills", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "skill", nullable = false, length = 60)
    private Set<String> requiredSkills = new HashSet<>();

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobStatus status = JobStatus.PENDING;

    @Column(length = 1000)
    private String reviewNote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Job() {}

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = JobStatus.PENDING;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCompanyName() { return companyName; }
    public String getLocation() { return location; }
    public String getEmploymentType() { return employmentType; }
    public BigDecimal getSalaryMin() { return salaryMin; }
    public BigDecimal getSalaryMax() { return salaryMax; }
    public BigDecimal getMinCgpa() { return minCgpa; }
    public Set<String> getRequiredSkills() { return requiredSkills; }
    public LocalDate getDeadline() { return deadline; }
    public JobStatus getStatus() { return status; }
    public String getReviewNote() { return reviewNote; }
    public User getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateFrom(JobRequest request) {
        title = request.title().trim();
        description = request.description().trim();
        companyName = request.companyName().trim();
        location = request.location().trim();
        employmentType = request.employmentType().trim().toUpperCase();
        salaryMin = request.salaryMin();
        salaryMax = request.salaryMax();
        minCgpa = request.minCgpa();
        requiredSkills = request.requiredSkills() == null ? new HashSet<>() :
                request.requiredSkills().stream().filter(s -> s != null && !s.isBlank())
                        .map(String::trim).collect(java.util.stream.Collectors.toSet());
        deadline = request.deadline();
        status = JobStatus.PENDING;
        reviewNote = null;
    }

    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }
    public void setStatus(JobStatus status) { this.status = status; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
}
