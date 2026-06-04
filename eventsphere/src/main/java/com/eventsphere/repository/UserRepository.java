package com.eventsphere.repository;

import com.eventsphere.entity.User;
import com.eventsphere.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Optional<User> findByRefreshToken(String refreshToken);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findByRole(Role role, Pageable pageable);

    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (
                :search is null or :search = ''
                or lower(u.username) like lower(concat('%', :search, '%'))
                or lower(u.email)    like lower(concat('%', :search, '%'))
                or lower(u.fullName) like lower(concat('%', :search, '%'))
              )
            """)
    Page<User> search(@Param("search") String search,
                      @Param("role") Role role,
                      Pageable pageable);
}
