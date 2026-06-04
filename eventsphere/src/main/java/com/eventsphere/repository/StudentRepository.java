package com.eventsphere.repository;

import com.eventsphere.entity.Student;
import com.eventsphere.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByUser(User user);

    Optional<Student> findByUserUsername(String username);

    Optional<Student> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);

    @Query("""
            select s from Student s
            join s.user u
            where lower(u.fullName) like lower(concat('%', :search, '%'))
               or lower(u.username) like lower(concat('%', :search, '%'))
               or lower(s.studentId) like lower(concat('%', :search, '%'))
            """)
    Page<Student> search(@Param("search") String search, Pageable pageable);
}
