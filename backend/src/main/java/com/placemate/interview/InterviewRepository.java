package com.placemate.interview;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview,Long> {
    List<Interview> findByApplication_Student_IdOrderByScheduledAtAsc(Long studentId);
    List<Interview> findByCreatedBy_IdOrderByScheduledAtAsc(Long recruiterId);
}
