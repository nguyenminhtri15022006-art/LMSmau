package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.GatewayAuthentication;
import com.lms.courseservice.dto.request.CompleteLessonRequest;
import com.lms.courseservice.dto.response.CourseProgressResponse;
import com.lms.courseservice.entity.mysql.LessonProgress;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import com.lms.courseservice.repository.mysql.LessonProgressRepository;
import com.lms.courseservice.service.CourseStructureService;
import com.lms.courseservice.service.LessonProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LessonProgressServiceImpl implements LessonProgressService {

    private final LessonProgressRepository progressRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final CourseStructureService structureService;

    @Override
    public void completeLesson(String courseId, String lessonId, CompleteLessonRequest req) {
        String studentId = GatewayAuthentication.currentUserId();

        if (!enrollmentRepo.existsByUserIdAndCourseId(studentId, courseId)) {
            throw new CourseException(ErrorCode.NOT_ENROLLED);
        }

        if (!structureService.isLessonInCourse(courseId, lessonId)) {
            throw new CourseException(ErrorCode.LESSON_NOT_BELONG_TO_COURSE);
        }

        LessonProgress progress = progressRepo
                .findByStudentIdAndLessonId(studentId, lessonId)
                .orElse(LessonProgress.builder()
                        .studentId(studentId)
                        .courseId(courseId)
                        .lessonId(lessonId)
                        .lessonType(req.getLessonType())
                        .build());

        if (!progress.isCompleted()) {
            progress.setCompleted(true);
        }
        progress.setLastAccessed(LocalDateTime.now());
        progressRepo.save(progress);
    }

    @Override
    public CourseProgressResponse getCourseProgress(String courseId) {
        String studentId = GatewayAuthentication.currentUserId();

        int totalLessons = structureService.countLessons(courseId);

        List<LessonProgress> records = progressRepo.findByStudentIdAndCourseId(studentId, courseId);

        List<String> completedIds = records.stream()
                .filter(LessonProgress::isCompleted)
                .map(LessonProgress::getLessonId)
                .toList();

        double percent = totalLessons > 0
                ? (completedIds.size() * 100.0 / totalLessons)
                : 0.0;

        return CourseProgressResponse.builder()
                .courseId(courseId)
                .totalLessons(totalLessons)
                .completedLessons(completedIds.size())
                .percentComplete(percent)
                .completedLessonIds(completedIds)
                .build();
    }

    @Override
    public double getAverageProgress(String courseId) {
        int totalLessons = structureService.countLessons(courseId);
        long totalStudents = enrollmentRepo.countByCourseId(courseId);

        if (totalLessons == 0 || totalStudents == 0) return 0.0;

        long totalCompleted = progressRepo.countByCourseIdAndCompletedTrue(courseId);
        double avg = (totalCompleted * 100.0) / (totalLessons * totalStudents);
        return Math.min(avg, 100.0);
    }
}