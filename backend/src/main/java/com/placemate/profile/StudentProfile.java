package com.placemate.profile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.placemate.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "student_profiles", uniqueConstraints = @UniqueConstraint(name = "uk_student_profile_user", columnNames = "user_id"))
public class StudentProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(precision = 4, scale = 2)
    private BigDecimal cgpa;

    private Integer graduationYear;

    @Column(length = 120)
    private String degree;

    @Column(length = 120)
    private String branch;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "student_profile_skills", joinColumns = @JoinColumn(name = "profile_id"))
    @Column(name = "skill", nullable = false, length = 60)
    private Set<String> skills = new HashSet<>();

    @Column(length = 500)
    private String resumeUrl;

    @Column(nullable = false)
    private Instant updatedAt;

    protected StudentProfile() {}

    public StudentProfile(User user) { this.user = user; }

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public BigDecimal getCgpa() { return cgpa; }
    public Integer getGraduationYear() { return graduationYear; }
    public String getDegree() { return degree; }
    public String getBranch() { return branch; }
    public Set<String> getSkills() { return skills; }
    public String getResumeUrl() { return resumeUrl; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(StudentProfileRequest request) {
        cgpa = request.cgpa();
        graduationYear = request.graduationYear();
        degree = clean(request.degree());
        branch = clean(request.branch());
        skills = request.skills() == null ? new HashSet<>() :
            request.skills().stream().filter(s -> s != null && !s.isBlank())
                .map(String::trim).collect(java.util.stream.Collectors.toCollection(HashSet::new));
        resumeUrl = clean(request.resumeUrl());
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
