package com.placemate.user;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name="users", uniqueConstraints=@UniqueConstraint(name="uk_users_email", columnNames="email"))
public class User {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,length=120) private String fullName;
 @Column(nullable=false,length=190) private String email;
 @Column(nullable=false) private String password;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Role role;
 @Column(length=80) private String enrollmentNumber;
 @Column(length=100) private String department;
 @Column(length=150) private String companyName;
 @Column(length=255) private String companyWebsite;
 @Column(nullable=false,updatable=false) private Instant createdAt;
 protected User() {}
 public User(String fullName,String email,String password,Role role) {
  this.fullName=fullName; this.email=email; this.password=password; this.role=role;
 }
 @PrePersist void onCreate(){ createdAt=Instant.now(); }
 public Long getId(){return id;} public String getFullName(){return fullName;}
 public String getEmail(){return email;} public String getPassword(){return password;}
 public Role getRole(){return role;} public String getEnrollmentNumber(){return enrollmentNumber;}
 public String getDepartment(){return department;} public String getCompanyName(){return companyName;}
 public String getCompanyWebsite(){return companyWebsite;} public Instant getCreatedAt(){return createdAt;}
 public void setEnrollmentNumber(String v){enrollmentNumber=v;}
 public void setDepartment(String v){department=v;}
 public void setCompanyName(String v){companyName=v;}
 public void setCompanyWebsite(String v){companyWebsite=v;}
}
