package com.lms.courseservice.repository.mysql;

import com.lms.courseservice.entity.mysql.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement,Long> {
    Page<Announcement> findByCourseId(String courseId, Pageable pageable);
}
