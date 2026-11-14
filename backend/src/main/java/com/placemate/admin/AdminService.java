package com.placemate.admin;

import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.placemate.application.*;
import com.placemate.job.*;
import com.placemate.user.*;

@Service
public class AdminService {
 private final UserRepository users; private final JobRepository jobs; private final ApplicationRepository applications;
 public AdminService(UserRepository users,JobRepository jobs,ApplicationRepository applications){this.users=users;this.jobs=jobs;this.applications=applications;}

 @Transactional(readOnly=true)
 public AdminOverviewResponse overview(){
  List<User> recruiters=users.findByRoleOrderByCreatedAtDesc(Role.RECRUITER);
  long companies=recruiters.stream().map(User::getCompanyName).filter(Objects::nonNull).map(String::trim).filter(s->!s.isBlank()).map(s->s.toLowerCase(Locale.ROOT)).distinct().count();
  return new AdminOverviewResponse(users.countByRole(Role.STUDENT),recruiters.size(),companies,
   jobs.countByStatus(JobStatus.PENDING),jobs.countByStatus(JobStatus.APPROVED),applications.count(),
   applications.countByStatus(ApplicationStatus.SHORTLISTED),applications.countByStatus(ApplicationStatus.HIRED));
 }
 @Transactional(readOnly=true)
 public List<AdminUserResponse> users(String role){
  Role parsed;
  try{parsed=Role.valueOf(role.toUpperCase(Locale.ROOT));}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Role must be STUDENT or RECRUITER.");}
  if(parsed==Role.ADMIN)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Admin accounts are not listed through this endpoint.");
  return users.findByRoleOrderByCreatedAtDesc(parsed).stream().map(AdminUserResponse::from).toList();
 }
 @Transactional(readOnly=true)
 public List<CompanySummary> companies(){
  Map<String,List<User>> grouped=users.findByRoleOrderByCreatedAtDesc(Role.RECRUITER).stream()
   .filter(u->u.getCompanyName()!=null&&!u.getCompanyName().isBlank())
   .collect(Collectors.groupingBy(u->u.getCompanyName().trim().toLowerCase(Locale.ROOT),LinkedHashMap::new,Collectors.toList()));
  return grouped.values().stream().map(group->{
   User first=group.get(0);long verified=group.stream().filter(User::isCompanyVerified).count();
   return new CompanySummary(first.getCompanyName(),first.getCompanyWebsite(),group.size(),verified,verified==group.size());
  }).sorted(Comparator.comparing(CompanySummary::companyName,String.CASE_INSENSITIVE_ORDER)).toList();
 }
 @Transactional(readOnly=true)
 public List<ApplicationResponse> applications(){return applications.findAll().stream().sorted(Comparator.comparing(Application::getAppliedAt).reversed()).map(ApplicationResponse::from).toList();}
 @Transactional(readOnly=true)
 public List<DepartmentPlacementReport> placements(){
  Map<String,Long> totals=applications.findAll().stream().filter(a->a.getStatus()==ApplicationStatus.HIRED)
   .collect(Collectors.groupingBy(a->{String d=a.getStudent().getDepartment();return d==null||d.isBlank()?"Not specified":d;},Collectors.mapping(a->a.getStudent().getId(),Collectors.collectingAndThen(Collectors.toSet(),Set::size))).entrySet().stream()
   .collect(Collectors.toMap(Map.Entry::getKey,e->e.getValue().longValue()));
  return totals.entrySet().stream().map(e->new DepartmentPlacementReport(e.getKey(),e.getValue()))
   .sorted(Comparator.comparingLong(DepartmentPlacementReport::hiredStudents).reversed()).toList();
 }
 @Transactional
 public AdminUserResponse verifyCompany(Long recruiterId,boolean verified){
  User recruiter=users.findByRoleOrderByCreatedAtDesc(Role.RECRUITER).stream().filter(u->u.getId().equals(recruiterId)).findFirst()
   .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Recruiter account not found."));
  recruiter.setCompanyVerified(verified);return AdminUserResponse.from(users.save(recruiter));
 }
}
