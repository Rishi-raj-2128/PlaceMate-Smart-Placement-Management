package com.placemate.auth;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.placemate.auth.dto.*;
import com.placemate.user.*;
@Service
public class AuthService {
 private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
 public AuthService(UserRepository users,PasswordEncoder encoder,JwtService jwt){this.users=users;this.encoder=encoder;this.jwt=jwt;}
 @Transactional public AuthResponse register(RegisterRequest r) {
  Role role=parseRole(r.role()); String email=r.email().trim().toLowerCase(Locale.ROOT);
  if(users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists.");
  User user=new User(r.fullName().trim(),email,encoder.encode(r.password()),role);
  if(role==Role.STUDENT) {
   if(blank(r.enrollmentNumber())||blank(r.department())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enrollment number and department are required for student registration.");
   user.setEnrollmentNumber(r.enrollmentNumber().trim()); user.setDepartment(r.department().trim());
  } else {
   if(blank(r.companyName())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Company name is required for recruiter registration.");
   user.setCompanyName(r.companyName().trim());
   if(!blank(r.companyWebsite())) user.setCompanyWebsite(r.companyWebsite().trim());
  }
  return response(users.save(user));
 }
 @Transactional(readOnly=true) public AuthResponse login(LoginRequest r) {
  String email=r.email().trim().toLowerCase(Locale.ROOT);
  User u=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password."));
  if(!encoder.matches(r.password(),u.getPassword())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password.");
  return response(u);
 }
 @Transactional(readOnly=true) public AuthResponse.UserSummary currentUser(String email) {
  User u=users.findByEmailIgnoreCase(email).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User account not found."));
  return summary(u);
 }
 private AuthResponse response(User u){return AuthResponse.from(jwt.generateToken(u),jwt.getExpirationMs(),u);}
 private AuthResponse.UserSummary summary(User u){return new AuthResponse.UserSummary(u.getId(),u.getFullName(),u.getEmail(),u.getRole(),u.getEnrollmentNumber(),u.getDepartment(),u.getCompanyName(),u.getCompanyWebsite());}
 private boolean blank(String s){return s==null||s.isBlank();}
 private Role parseRole(String s) {
  try {Role role=Role.valueOf(s.trim().toUpperCase(Locale.ROOT)); if(role==Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Admin accounts are provisioned by the placement team."); return role;}
  catch(IllegalArgumentException ex){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Role must be STUDENT or RECRUITER.");}
 }
}
