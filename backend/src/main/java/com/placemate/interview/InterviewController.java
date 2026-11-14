package com.placemate.interview;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class InterviewController {
    private final InterviewService service;
    public InterviewController(InterviewService service){this.service=service;}
    @PostMapping("/recruiter/applications/{applicationId}/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewResponse schedule(Authentication auth,@PathVariable Long applicationId,@Valid @RequestBody InterviewRequest request){return service.schedule(auth.getName(),applicationId,request);}
    @GetMapping("/student/interviews")
    public List<InterviewResponse> student(Authentication auth){return service.mine(auth.getName());}
    @GetMapping("/recruiter/interviews")
    public List<InterviewResponse> recruiter(Authentication auth){return service.recruiterList(auth.getName());}
    @PatchMapping("/recruiter/interviews/{id}/status")
    public InterviewResponse update(Authentication auth,@PathVariable Long id,@RequestParam InterviewStatus status){return service.updateStatus(auth.getName(),id,status);}
}
