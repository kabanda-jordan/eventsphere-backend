package com.eventsphere.repository;

import com.eventsphere.entity.Event;
import com.eventsphere.entity.Registration;
import com.eventsphere.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    boolean existsByEventAndStudent(Event event, Student student);

    long countByEvent(Event event);

    long countByStudent(Student student);

    long countByEventId(Long eventId);

    void deleteByEventAndStudent(Event event, Student student);
}
