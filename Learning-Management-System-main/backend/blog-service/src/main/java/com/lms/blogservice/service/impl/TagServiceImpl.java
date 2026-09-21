package com.lms.blogservice.service.impl;

import com.lms.blogservice.dto.request.CreateTagRequest;
import com.lms.blogservice.dto.request.UpdateTagRequest;
import com.lms.blogservice.dto.response.TagResponse;
import com.lms.blogservice.entity.Tag;
import com.lms.blogservice.exception.BlogException;
import com.lms.blogservice.exception.ErrorCode;
import com.lms.blogservice.mapper.TagMapper;
import com.lms.blogservice.repository.TagRepository;
import com.lms.blogservice.service.TagService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TagServiceImpl implements TagService {

    TagRepository tagRepository;
    TagMapper tagMapper;

    @Override
    public List<TagResponse> getAllTags() {
        return tagRepository.findAll()
                .stream()
                .map(tagMapper::toTagResponse)
                .toList();
    }

    @Override
    public TagResponse getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BlogException(ErrorCode.TAG_NOT_FOUND));
        return tagMapper.toTagResponse(tag);
    }

    @Override
    public List<TagResponse> searchTags(String keyword) {
        return tagRepository.findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(tagMapper::toTagResponse)
                .toList();
    }

    @Override
    public TagResponse createTag(CreateTagRequest request) {

        if (tagRepository.existsByName(request.getName()))
            throw new BlogException(ErrorCode.TAG_NAME_EXISTED);

        if (tagRepository.existsBySlug(request.getSlug()))
            throw new BlogException(ErrorCode.TAG_SLUG_EXISTED);

        return tagMapper.toTagResponse(
                tagRepository.save(tagMapper.toTag(request)));
    }

    @Override
    public TagResponse updateTag(Long id, UpdateTagRequest request) {

        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BlogException(ErrorCode.TAG_NOT_FOUND));

        if (!tag.getName().equals(request.getName())
                && tagRepository.existsByName(request.getName()))
            throw new BlogException(ErrorCode.TAG_NAME_EXISTED);

        if (!tag.getSlug().equals(request.getSlug())
                && tagRepository.existsBySlug(request.getSlug()))
            throw new BlogException(ErrorCode.TAG_SLUG_EXISTED);

        tagMapper.updateTag(tag, request);
        return tagMapper.toTagResponse(tagRepository.save(tag));
    }

    @Override
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new BlogException(ErrorCode.TAG_NOT_FOUND));

        if (tagRepository.isTagUsedByAnyPost(id))
            throw new BlogException(ErrorCode.CANNOT_DELETE_TAG);

        tagRepository.delete(tag);
    }
}