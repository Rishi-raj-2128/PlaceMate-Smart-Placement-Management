package com.placemate.auth.dto;
import com.placemate.user.Role;
import com.placemate.user.User;
public record AuthResponse(String token,String tokenType,long expiresIn,UserSummary user) {
 public static AuthResponse from(String token,long expiresIn,User u) {
  return new AuthResponse(token,"Bearer",expiresIn,new UserSummary(u.getId(),u.getFullName(),u.getEmail(),u.getRole(),u.getEnrollmentNumber(),u.getDepartment(),u.getCompanyName(),u.getCompanyWebsite()));
 }
 public record UserSummary(Long id,String fullName,String email,Role role,String enrollmentNumber,String department,String companyName,String companyWebsite) {}
}
