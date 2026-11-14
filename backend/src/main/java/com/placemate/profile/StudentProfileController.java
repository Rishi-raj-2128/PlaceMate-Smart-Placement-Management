package com.placemate.profile;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/student")
public class StudentProfileController {
    private final StudentProfileService service;

    public StudentProfileController(StudentProfileService service) { this.service = service; }

    @GetMapping("/profile")
    public StudentProfileResponse profile(Authentication auth) {
        return service.getProfile(auth.getName());
    }

    @PutMapping("/profile")
    public StudentProfileResponse update(Authentication auth, @Valid @RequestBody StudentProfileRequest request) {
        return service.updateProfile(auth.getName(), request);
    }

    @GetMapping("/jobs/matches")
    public List<JobMatchResponse> matches(Authentication auth) {
        return service.matches(auth.getName());
    }
}
