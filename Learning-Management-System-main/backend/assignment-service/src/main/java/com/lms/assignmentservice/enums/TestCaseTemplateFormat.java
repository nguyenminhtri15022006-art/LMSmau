package com.lms.assignmentservice.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum TestCaseTemplateFormat {
    XLSX(
            "templates/testcase-import/testcases-template.xlsx.b64",
            "testcases-template.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ),
    CSV(
            "templates/testcase-import/testcases-template.csv",
            "testcases-template.csv",
            "text/csv"
    );

    String resourcePath;
    String filename;
    String mediaType;
}
