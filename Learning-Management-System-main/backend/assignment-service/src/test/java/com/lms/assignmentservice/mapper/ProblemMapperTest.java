package com.lms.assignmentservice.mapper;

import com.lms.assignmentservice.dto.response.ProblemListResponse;
import com.lms.assignmentservice.entity.Problem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemMapperTest {

    ProblemMapper mapper = new ProblemMapper();

    @Test
    void problemListResponseIncludesTotalSubmit() {
        Problem problem = Problem.builder()
                .problemId(10)
                .totalSubmit(7)
                .totalAccepted(3)
                .build();

        ProblemListResponse response = mapper.toProblemListResponse(problem);

        assertThat(response.getTotalSubmit()).isEqualTo(7);
        assertThat(response.getAcceptanceRate()).isEqualTo(43);
    }
}
