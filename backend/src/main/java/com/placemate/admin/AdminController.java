package com.placemate.admin;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import com.placemate.application.ApplicationResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
 private final AdminService service;
 public AdminController(AdminService service){this.service=service;}
 @GetMapping("/overview") public AdminOverviewResponse overview(){return service.overview();}
 @GetMapping("/users") public List<AdminUserResponse> users(@RequestParam(defaultValue="STUDENT") String role){return service.users(role);}
 @GetMapping("/companies") public List<CompanySummary> companies(){return service.companies();}
 @GetMapping("/applications") public List<ApplicationResponse> applications(){return service.applications();}
 @GetMapping("/reports/placements") public List<DepartmentPlacementReport> placements(){return service.placements();}
 @PatchMapping("/recruiters/{id}/company-verification")
 public AdminUserResponse verify(@PathVariable Long id,@Valid @RequestBody CompanyVerificationRequest request){return service.verifyCompany(id,request.verified());}
}
