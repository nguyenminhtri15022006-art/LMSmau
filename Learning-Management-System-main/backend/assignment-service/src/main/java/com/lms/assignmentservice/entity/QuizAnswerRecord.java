package com.lms.assignmentservice.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "quiz_answer_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder

public class QuizAnswerRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    QuizAttempt quizAttempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    Question question;

    // Lưu dạng JSON array: [1, 3] (danh sách answer_id student chọn)
    // Dùng @Convert thay vì @JdbcTypeCode để kiểm soát serialization tốt hơn
    // IMPORTANT: Nếu FE không gửi, Builder set null
    // → Converter.convertToDatabaseColumn(null) = "[]"
    // → Khi load: Converter.convertToEntityAttribute("[]") = List.of()
    // → Học viên đã chọn sẽ được lưu đúng!
    @Convert(converter = AnswerIdsConverter.class)
    @Column(name = "selected_answer_ids", nullable = false, columnDefinition = "JSON")
    private List<Integer> selectedAnswerIds;

    @Column(name = "earned_score", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal earnedScore = BigDecimal.ZERO;

    // ==================== Converter ====================

    @Converter
    public static class AnswerIdsConverter
            implements AttributeConverter<List<Integer>, String> {

        private static final ObjectMapper MAPPER =
                new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(List<Integer> attribute) {
            if (attribute == null) return "[]";
            try {
                return MAPPER.writeValueAsString(attribute);
            } catch (Exception e) {
                throw new IllegalArgumentException("Không thể serialize selectedAnswerIds", e);
            }
        }

        @Override
        public List<Integer> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) return List.of();
            try {
                return MAPPER.readValue(dbData, new TypeReference<>() {
                });
            } catch (Exception e) {
                throw new IllegalArgumentException("Không thể deserialize selectedAnswerIds", e);
            }
        }
    }


}
