package com.placemate.job;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByStatusOrderByCreatedAtDesc(JobStatus status);
    List<Job> findByCreatedByIdOrderByCreatedAtDesc(Long recruiterId);
    List<Job> findByStatusOrderByCreatedAtAsc(JobStatus status);
    long countByStatus(JobStatus status);
}
