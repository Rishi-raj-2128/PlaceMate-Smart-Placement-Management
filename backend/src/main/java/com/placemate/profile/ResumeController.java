package com.placemate.profile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.*;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
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
        byte[] bytes=file.getBytes();
        validateFileContent(bytes,ext);
        Files.createDirectories(root);
        String stored=student.getId()+"-"+UUID.randomUUID()+ext;
        Path destination=root.resolve(stored).normalize();
        if(!destination.startsWith(root))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid file name.");
        Files.copy(new ByteArrayInputStream(bytes),destination,StandardCopyOption.REPLACE_EXISTING);
        StudentProfile profile=profiles.findByUser_Id(student.getId()).orElseGet(()->profiles.save(new StudentProfile(student)));
        profile.setResumeUrl(stored);
        profiles.save(profile);
        return ResponseEntity.ok(java.util.Map.of("fileName",original,"stored",true,"downloadEndpoint","/api/student/resume"));
    }

    @GetMapping("/student/resume")
    public ResponseEntity<Resource> studentResume(Authentication auth) throws IOException {
        User student=current(auth,Role.STUDENT);
        StudentProfile profile=profiles.findByUser_Id(student.getId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"No resume uploaded."));
        return download(profile.getResumeUrl(),student.getId());
    }

    @GetMapping("/recruiter/applications/{applicationId}/resume")
    @Transactional(readOnly=true)
    public ResponseEntity<Resource> recruiterResume(Authentication auth,@PathVariable Long applicationId) throws IOException {
        User recruiter=current(auth,Role.RECRUITER);
        Application application=applications.findById(applicationId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Application not found."));
        if(!application.getJob().getCreatedBy().getId().equals(recruiter.getId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You can only access resumes for applicants to your own jobs.");
        Long studentId=application.getStudent().getId();
        StudentProfile profile=profiles.findByUser_Id(studentId).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Student profile not found."));
        return download(profile.getResumeUrl(),studentId);
    }

    private void validateFileContent(byte[] bytes,String extension) throws IOException {
        if(".pdf".equals(extension)) {
            if(bytes.length<5||bytes[0]!='%'||bytes[1]!='P'||bytes[2]!='D'||bytes[3]!='F'||bytes[4]!='-')
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The uploaded file is not a valid PDF document.");
            return;
        }
        if(".doc".equals(extension)) {
            byte[] signature={(byte)0xD0,(byte)0xCF,0x11,(byte)0xE0,(byte)0xA1,(byte)0xB1,0x1A,(byte)0xE1};
            if(bytes.length<signature.length)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The uploaded file is not a valid DOC document.");
            for(int i=0;i<signature.length;i++)if(bytes[i]!=signature[i])
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The uploaded file is not a valid DOC document.");
            return;
        }
        boolean contentTypes=false;
        boolean document=false;
        try(ZipInputStream zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) {
                if("[Content_Types].xml".equals(entry.getName()))contentTypes=true;
                if("word/document.xml".equals(entry.getName()))document=true;
                if(contentTypes&&document)break;
            }
        } catch(IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The uploaded file is not a valid DOCX document.");
        }
        if(!contentTypes||!document)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The uploaded file is not a valid DOCX document.");
    }

    private ResponseEntity<Resource> download(String fileName,Long ownerId) throws IOException {
        if(fileName==null||fileName.isBlank())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"No resume uploaded.");
        if(fileName.startsWith("http://")||fileName.startsWith("https://")) {
            try {
                URI uri=URI.create(fileName);
                String scheme=uri.getScheme();
                if(uri.getHost()==null||uri.getUserInfo()!=null||!("http".equalsIgnoreCase(scheme)||"https".equalsIgnoreCase(scheme)))
                    throw new IllegalArgumentException("Invalid resume URL");
                return ResponseEntity.status(HttpStatus.FOUND).location(uri).build();
            } catch(IllegalArgumentException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"The saved resume URL is invalid.");
            }
        }
        String safe=Paths.get(fileName).getFileName().toString();
        String expectedPrefix=ownerId+"-";
        if(!safe.equals(fileName)||!safe.startsWith(expectedPrefix)||
            !safe.matches(java.util.regex.Pattern.quote(expectedPrefix)+"[0-9a-fA-F-]{36}\\.(pdf|doc|docx)"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Resume file not found.");
        Path file=root.resolve(safe).normalize();
        if(!file.startsWith(root)||!Files.isRegularFile(file))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Resume file not found.");
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
