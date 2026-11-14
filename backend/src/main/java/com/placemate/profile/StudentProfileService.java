package com.placemate.profile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.placemate.job.*;
import com.placemate.user.*;

@Service
public class StudentProfileService {
    private final StudentProfileRepository profiles;
    private final UserRepository users;
    private final JobRepository jobs;

    public StudentProfileService(StudentProfileRepository profiles, UserRepository users, JobRepository jobs) {
        this.profiles = profiles;
        this.users = users;
        this.jobs = jobs;
    }

    @Transactional
    public StudentProfileResponse getProfile(String email) {
        User student = requireStudent(email);
        return StudentProfileResponse.from(getOrCreate(student));
    }

    @Transactional
    public StudentProfileResponse updateProfile(String email, StudentProfileRequest request) {
        User student = requireStudent(email);
        String resumeReference = request.resumeUrl();
        if (resumeReference != null && !resumeReference.isBlank()
                && !resumeReference.startsWith("http://") && !resumeReference.startsWith("https://")
                && !resumeReference.startsWith(student.getId() + "-")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded resume references must belong to your own account.");
        }
        StudentProfile profile = getOrCreate(student);
        profile.update(request);
        return StudentProfileResponse.from(profiles.save(profile));
    }

    @Transactional(readOnly = true)
    public List<JobMatchResponse> matches(String email) {
        User student = requireStudent(email);
        StudentProfile profile = profiles.findByUser_Id(student.getId()).orElse(null);
        return jobs.findByStatusOrderByCreatedAtDesc(JobStatus.APPROVED).stream()
            .filter(job -> job.getDeadline() == null || !job.getDeadline().isBefore(LocalDate.now()))
            .map(job -> calculate(job, profile))
            .sorted((a, b) -> {
                int eligibleOrder = Boolean.compare(b.eligible(), a.eligible());
                return eligibleOrder != 0 ? eligibleOrder : Integer.compare(b.matchPercentage(), a.matchPercentage());
            }).toList();
    }

    private JobMatchResponse calculate(Job job, StudentProfile profile) {
        List<String> reasons = new ArrayList<>();
        boolean cgpaKnown = profile != null && profile.getCgpa() != null;
        boolean skillsKnown = profile != null && !profile.getSkills().isEmpty();
        boolean cgpaPass = job.getMinCgpa() == null || (cgpaKnown && profile.getCgpa().compareTo(job.getMinCgpa()) >= 0);
        int skillCount = job.getRequiredSkills().size();
        int matchedSkills = 0;
        if (skillCount > 0 && skillsKnown) {
            for (String required : job.getRequiredSkills()) {
                boolean found = profile.getSkills().stream().anyMatch(skill -> skill.equalsIgnoreCase(required));
                if (found) matchedSkills++;
            }
        }
        boolean skillsPass = skillCount == 0 || (skillsKnown && matchedSkills == skillCount);
        if (job.getMinCgpa() != null) {
            if (!cgpaKnown) reasons.add("Add your CGPA to check the minimum requirement of " + job.getMinCgpa() + ".");
            else if (!cgpaPass) reasons.add("Your CGPA is below the required " + job.getMinCgpa() + ".");
            else reasons.add("Your CGPA meets the minimum requirement.");
        } else reasons.add("No minimum CGPA specified.");
        if (skillCount > 0) {
            if (!skillsKnown) reasons.add("Add your skills to compare against the required skills.");
            else if (matchedSkills == skillCount) reasons.add("You have all required skills.");
            else reasons.add("Matched " + matchedSkills + " of " + skillCount + " required skills.");
        } else reasons.add("No specific skills listed by the recruiter.");

        int cgpaScore = job.getMinCgpa() == null ? 100 : cgpaKnown ? (cgpaPass ? 100 : 0) : 0;
        int skillScore = skillCount == 0 ? 100 : skillsKnown ? (int)Math.round(100.0 * matchedSkills / skillCount) : 0;
        int score = (int)Math.round(cgpaScore * 0.5 + skillScore * 0.5);
        if (!cgpaKnown || (skillCount > 0 && !skillsKnown)) {
            reasons.add("Complete your profile for a more accurate match result.");
        }
        return new JobMatchResponse(JobResponse.from(job), cgpaPass && skillsPass, score, reasons);
    }

    private StudentProfile getOrCreate(User student) {
        return profiles.findByUser_Id(student.getId()).orElseGet(() -> profiles.save(new StudentProfile(student)));
    }

    private User requireStudent(String email) {
        User user = users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found."));
        if (user.getRole() != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Student access is required.");
        }
        return user;
    }
}
