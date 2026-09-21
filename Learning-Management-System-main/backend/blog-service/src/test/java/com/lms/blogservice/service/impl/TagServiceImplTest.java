package com.lms.blogservice.service.impl;

import com.lms.blogservice.dto.request.CreateTagRequest;
import com.lms.blogservice.dto.request.UpdateTagRequest;
import com.lms.blogservice.dto.response.TagResponse;
import com.lms.blogservice.entity.Tag;
import com.lms.blogservice.exception.BlogException;
import com.lms.blogservice.exception.ErrorCode;
import com.lms.blogservice.mapper.TagMapper;
import com.lms.blogservice.repository.TagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    TagRepository tagRepository;
    @Mock
    TagMapper tagMapper;

    @InjectMocks
    TagServiceImpl tagService;

    @Test
    void createTagRejectsDuplicateName() {
        CreateTagRequest request = CreateTagRequest.builder().name("Java").slug("java").build();
        when(tagRepository.existsByName("Java")).thenReturn(true);

        assertThatThrownBy(() -> tagService.createTag(request))
                .isInstanceOf(BlogException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TAG_NAME_EXISTED);

        verify(tagRepository, never()).save(any());
    }

    @Test
    void createTagRejectsDuplicateSlug() {
        CreateTagRequest request = CreateTagRequest.builder().name("Java").slug("java").build();
        when(tagRepository.existsByName("Java")).thenReturn(false);
        when(tagRepository.existsBySlug("java")).thenReturn(true);

        assertThatThrownBy(() -> tagService.createTag(request))
                .isInstanceOf(BlogException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TAG_SLUG_EXISTED);
    }

    @Test
    void updateTagSavesWhenNameAndSlugAreUnique() {
        Tag tag = Tag.builder().id(1L).name("Java").slug("java").build();
        UpdateTagRequest request = UpdateTagRequest.builder().name("Spring").slug("spring").build();
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByName("Spring")).thenReturn(false);
        when(tagRepository.existsBySlug("spring")).thenReturn(false);
        when(tagRepository.save(tag)).thenReturn(tag);
        when(tagMapper.toTagResponse(tag)).thenReturn(TagResponse.builder().id(1L).name("Spring").slug("spring").build());

        TagResponse response = tagService.updateTag(1L, request);

        assertThat(response.getSlug()).isEqualTo("spring");
        verify(tagMapper).updateTag(tag, request);
    }

    @Test
    void deleteTagRejectsTagUsedByPost() {
        Tag tag = Tag.builder().id(1L).name("Java").slug("java").build();
        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));
        when(tagRepository.isTagUsedByAnyPost(1L)).thenReturn(true);

        assertThatThrownBy(() -> tagService.deleteTag(1L))
                .isInstanceOf(BlogException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CANNOT_DELETE_TAG);

        verify(tagRepository, never()).delete(tag);
    }

    @Test
    void searchTagsMapsResults() {
        Tag tag = Tag.builder().id(1L).name("Java").slug("java").build();
        when(tagRepository.findByNameContainingIgnoreCase("ja")).thenReturn(List.of(tag));
        when(tagMapper.toTagResponse(tag)).thenReturn(TagResponse.builder().id(1L).name("Java").build());

        var results = tagService.searchTags("ja");

        assertThat(results).singleElement()
                .extracting(TagResponse::getName)
                .isEqualTo("Java");
    }
}
