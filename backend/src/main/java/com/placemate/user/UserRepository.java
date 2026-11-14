package com.placemate.user;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface UserRepository extends JpaRepository<User,Long> {
 Optional<User> findByEmailIgnoreCase(String email);
 boolean existsByEmailIgnoreCase(String email);
 java.util.List<User> findByRoleOrderByCreatedAtDesc(Role role);
 long countByRole(Role role);
}
