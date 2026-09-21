package com.lms.courseservice.service.impl;

import com.lms.courseservice.dto.request.CreateCategoryRequest;
import com.lms.courseservice.dto.request.UpdateCategoryRequest;
import com.lms.courseservice.dto.response.CategoryResponse;
import com.lms.courseservice.entity.mysql.Category;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.mapper.CategoryMapper;
import com.lms.courseservice.repository.mysql.CategoryRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    CategoryRepository categoryRepository;
    @Mock
    CourseRepository courseRepository;
    @Mock
    CategoryMapper categoryMapper;

    @InjectMocks
    CategoryServiceImpl categoryService;

    @Test
    void createCategoryRejectsDuplicateName() {
        CreateCategoryRequest request = CreateCategoryRequest.builder().name("Backend").slug("backend").build();
        when(categoryRepository.existsByName("Backend")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(request))
                .isInstanceOf(CourseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CATEGORY_NAME_EXISTED);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createCategorySavesMappedCategoryWhenUnique() {
        CreateCategoryRequest request = CreateCategoryRequest.builder().name("Backend").slug("backend").build();
        Category category = Category.builder().name("Backend").slug("backend").build();
        CategoryResponse response = CategoryResponse.builder().id(1L).name("Backend").slug("backend").build();
        when(categoryRepository.existsByName("Backend")).thenReturn(false);
        when(categoryRepository.existsBySlug("backend")).thenReturn(false);
        when(categoryMapper.toCategory(request)).thenReturn(category);
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toCategoryResponse(category)).thenReturn(response);

        CategoryResponse actual = categoryService.createCategory(request);

        assertThat(actual.getName()).isEqualTo("Backend");
    }

    @Test
    void updateCategoryRejectsDuplicateSlugFromAnotherCategory() {
        Category category = Category.builder().id(1L).name("Backend").slug("backend").build();
        UpdateCategoryRequest request = UpdateCategoryRequest.builder().name("Backend").slug("api").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsBySlug("api")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(1L, request))
                .isInstanceOf(CourseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SLUG_ALREADY_EXISTS);
    }

    @Test
    void deleteCategoryRejectsCategoryWithCourses() {
        Category category = Category.builder()
                .id(1L)
                .name("Backend")
                .courses(new ArrayList<>(List.of(Course.builder().id("course-1").build())))
                .build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(CourseException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CANNOT_DELETE_CATEGORY);

        verify(categoryRepository, never()).delete(category);
    }
}
