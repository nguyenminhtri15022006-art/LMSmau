package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.response.AssignmentLessonCountResponse;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.repository.ProblemRepository;
import com.lms.assignmentservice.repository.QuizRepository;
import com.lms.assignmentservice.repository.httpClient.CourseClient;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.AssignmentLessonSummaryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AssignmentLessonSummaryServiceImpl implements AssignmentLessonSummaryService {

    ProblemRepository problemRepository;
    QuizRepository quizRepository;
    CourseClient courseClient;
    AssignmentAuthorizationService authorizationService;

    @Override
    public List<AssignmentLessonCountResponse> getLessonCounts(String courseId) {
        if (!authorizationService.isAdminOrInstructor()) {
            Boolean enrolled = courseClient.checkEnrollment(courseId, authorizationService.currentUserId());
            if (!Boolean.TRUE.equals(enrolled)) {
                throw new AssignmentException(ErrorCode.NOT_ENROLLED);
            }
        }

        Map<String, AssignmentLessonCountResponse> countsByLessonId = new LinkedHashMap<>();
        problemRepository.countPublicGroupByLessonId(courseId).forEach(row -> {
            String lessonId = String.valueOf(row[0]);
            countsByLessonId.put(lessonId, AssignmentLessonCountResponse.builder()
                    .lessonId(lessonId)
                    .codingCount((Long) row[1])
                    .quizCount(0)
                    .build());
        });

        quizRepository.countPublishedGroupByLessonId(courseId).forEach(row -> {
            String lessonId = String.valueOf(row[0]);
            AssignmentLessonCountResponse current = countsByLessonId.get(lessonId);
            if (current == null) {
                countsByLessonId.put(lessonId, AssignmentLessonCountResponse.builder()
                        .lessonId(lessonId)
                        .codingCount(0)
                        .quizCount((Long) row[1])
                        .build());
            } else {
                current.setQuizCount((Long) row[1]);
            }
        });

        return List.copyOf(countsByLessonId.values());
    }
}
