package com.eventsphere.repository;

import com.eventsphere.entity.Event;
import com.eventsphere.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("""
            select e from Event e
            where (:status is null or e.status = :status)
              and (
                    :search is null
                    or :search = ''
                    or lower(e.title) like lower(concat('%', :search, '%'))
                    or lower(e.location) like lower(concat('%', :search, '%'))
              )
            """)
    Page<Event> search(@Param("search") String search, @Param("status") EventStatus status, Pageable pageable);

    List<Event> findByEventDateAfterOrderByEventDateAsc(LocalDateTime now, Pageable pageable);

    List<Event> findByEventDateBeforeOrderByEventDateDesc(LocalDateTime now, Pageable pageable);

    long countByStatus(EventStatus status);
}
