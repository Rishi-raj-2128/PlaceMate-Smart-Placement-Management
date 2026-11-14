package com.placemate.profile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.placemate.application.Application;
import com.placemate.application.ApplicationRepository;
import com.placemate.user.*;

@RestController
@RequestMapping("/api")
public class ResumeController {
    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private final UserRepository users;
    private final StudentProfileRepository profiles;
    private final ApplicationRepository applications;
    private final Path root;

    public ResumeController(UserRepository users,StudentProfileRepository profiles,ApplicationRepository applications,
        @Value("${placemate.upload-dir:uploads/resumes}") String uploadDir) {
        this.users=users;this.profiles=profiles;this.applications=applications;
        this.root=Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostMapping(value="/student/profile/resume",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(Authentication auth,@RequestPart("file") MultipartFile file) throws IOException {
        User student=current(auth,Role.STUDENT);
        if(file==null||file.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Choose a resume file.");
        if(file.getSize()>MAX_SIZE)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Resume must be 5 MB or smaller.");
        String original=file.getOriginalFilename()==null?"resume":Paths.get(file.getOriginalFilename()).getFileName().toString();
        String lower=original.toLowerCase(Locale.ROOT);
        String ext=lower.endsWith(".pdf")?".pdf":lower.endsWith(".docx")?".docx":lower.endsWith(".doc")?".doc":null;
        if(ext==null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only PDF, DOC, and DOCX files are supported.");
        Files.createDirectories(root);
        String stored=student.getId()+"-"+UUID.randomUUID()+ext;
        Path destination=root.resolve(stored).normalize();
        if(!destination.startsWith(root))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid file name.");
        try(var input=file.getInputStream()){Files.copy(input,destination,StandardCopyOption.REPLACE_EXISTING);}
        StudentProfile profile=profiles.findByUser_Id(student.getId()).orElseGet(()->profiles.save(new StudentProfile(student)));
        profile.setResumeUrl(stored);
        profiles.save(profile);
        return ResponseEntity.ok(java.util.Map.of("fileName",original,"stored",true,"downloadEndpoint","/api/student/resume"));
    }

    @GetMapping("/student/resume")
    public ResponseEntity<Resource> studentResume(Authentication auth) throws IOException {
        User student=current(auth,Role.STUDENT);
        StudentProfile profile=profiles.findByUser_Id(student.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"No resume uploaded."));
        return download(profile.getResumeUrl());
    }

    @GetMapping("/recruiter/applications/{applicationId}/resume")
    public ResponseEntity<Resource> recruiterResume(Authentication auth,@PathVariable Long applicationId) throws IOException {
        User recruiter=current(auth,Role.RECRUITER);
        Application application=applications.findById(applicationId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Application not found."));
        if(!application.getJob().getCreatedBy().getId().equals(recruiter.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You can only access resumes for applicants to your own jobs.");
        StudentProfile profile=profiles.findByUser_Id(application.getStudent().getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Student profile not found."));
        return download(profile.getResumeUrl());
    }

    private ResponseEntity<Resource> download(String fileName) throws IOException {
        if(fileName==null||fileName.isBlank())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"No resume uploaded.");
        Path file=root.resolve(Paths.get(fileName).getFileName().toString()).normalize();
        if(!file.startsWith(root)||!Files.exists(file))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Resume file not found.");
        Resource resource=new UrlResource(file.toUri());
        String type=Files.probeContentType(file);
        MediaType media=type==null?MediaType.APPLICATION_OCTET_STREAM:MediaType.parseMediaType(type);
        return ResponseEntity.ok().contentType(media).header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(file.getFileName().toString()).build().toString()).body(resource);
    }
    private User current(Authentication auth,Role role){
        User u=users.findByEmailIgnoreCase(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Account not found."));
        if(u.getRole()!=role)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Your account does not have permission for this action.");
        return u;
    }
}
