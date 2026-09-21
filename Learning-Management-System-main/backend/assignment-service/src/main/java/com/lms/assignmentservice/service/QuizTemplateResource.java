package com.lms.assignmentservice.service;

import org.springframework.core.io.Resource;

public record QuizTemplateResource(Resource resource, String filename, String mediaType) {}
