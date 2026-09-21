package com.lms.blogservice.service;

import com.lms.blogservice.dto.request.CreateTagRequest;
import com.lms.blogservice.dto.request.UpdateTagRequest;
import com.lms.blogservice.dto.response.TagResponse;

import java.util.List;

public interface TagService {
    List<TagResponse> getAllTags();
    TagResponse getTagById(Long id);
    List<TagResponse> searchTags(String keyword);
    TagResponse createTag(CreateTagRequest request);
    TagResponse updateTag(Long id, UpdateTagRequest request);
    void deleteTag(Long id);
}
