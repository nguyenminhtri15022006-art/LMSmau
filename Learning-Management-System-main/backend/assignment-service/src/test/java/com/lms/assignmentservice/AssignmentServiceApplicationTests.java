package com.lms.assignmentservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires local MySQL/MongoDB infrastructure; run with the Docker stack for full-context verification.")
class AssignmentServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
