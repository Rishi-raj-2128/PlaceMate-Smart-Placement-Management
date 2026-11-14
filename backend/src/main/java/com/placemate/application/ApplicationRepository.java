package com.placemate.application;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByStudentIdAndJobId(Long studentId, Long jobId);
    List<Application> findByStudentIdOrderByAppliedAtDesc(Long studentId);
    List<Application> findByJob_IdAndJob_CreatedBy_IdOrderByAppliedAtDesc(Long jobId, Long recruiterId);
}
