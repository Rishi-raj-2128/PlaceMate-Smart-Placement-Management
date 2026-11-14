package com.placemate.config;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.placemate.auth.JwtAuthenticationFilter;
@Configuration
public class SecurityConfig {
 private final JwtAuthenticationFilter jwtFilter;
 public SecurityConfig(JwtAuthenticationFilter jwtFilter){this.jwtFilter=jwtFilter;}
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http)throws Exception {
  return http.csrf(csrf->csrf.disable()).cors(Customizer.withDefaults())
   .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .formLogin(f->f.disable()).httpBasic(b->b.disable())
   .authorizeHttpRequests(a->a.requestMatchers("/api/auth/**","/api/health","/actuator/health").permitAll()
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/recruiter/**").hasRole("RECRUITER")
    .requestMatchers("/api/student/**").hasRole("STUDENT").anyRequest().authenticated())
   .addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class).build();
 }
}
