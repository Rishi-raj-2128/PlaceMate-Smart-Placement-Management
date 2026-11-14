package com.placemate.admin;

import java.time.Instant;
import com.placemate.user.User;
import com.placemate.user.Role;

public record AdminUserResponse(Long id,String fullName,String email,Role role,String enrollmentNumber,
 String department,String companyName,String companyWebsite,boolean companyVerified,Instant createdAt) {
 public static AdminUserResponse from(User u){return new AdminUserResponse(u.getId(),u.getFullName(),u.getEmail(),u.getRole(),
  u.getEnrollmentNumber(),u.getDepartment(),u.getCompanyName(),u.getCompanyWebsite(),u.isCompanyVerified(),u.getCreatedAt());}
}
