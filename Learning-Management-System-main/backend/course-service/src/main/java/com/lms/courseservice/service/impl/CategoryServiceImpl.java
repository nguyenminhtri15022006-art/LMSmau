package com.lms.courseservice.service.impl;

import com.lms.courseservice.dto.request.CreateCategoryRequest;
import com.lms.courseservice.dto.request.UpdateCategoryRequest;
import com.lms.courseservice.dto.response.CategoryResponse;
import com.lms.courseservice.entity.mysql.Category;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.mapper.CategoryMapper;
import com.lms.courseservice.repository.mysql.CategoryRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.service.CategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryServiceImpl implements CategoryService {

    CategoryRepository categoryRepository;
    CourseRepository courseRepository;
    CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        //Kiem tra trung ten
        if(categoryRepository.existsByName(request.getName()))
            throw new CourseException(ErrorCode.CATEGORY_NAME_EXISTED);

        if(request.getSlug() != null
                && categoryRepository.existsBySlug(request.getSlug()))
            throw new CourseException(ErrorCode.SLUG_ALREADY_EXISTS);

        Category category = categoryMapper.toCategory(request);

        return categoryMapper.toCategoryResponse(
                categoryRepository.save(category));
    }

    @Override
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CourseException(ErrorCode.CATEGORY_NOT_FOUND));

        // Kiểm tra tên trùng với category khác
        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByName(request.getName()))
            throw new CourseException(ErrorCode.CATEGORY_NAME_EXISTED);

        // Kiem tra trung slug
        if (request.getSlug() != null
                && !request.getSlug().equals(category.getSlug())
                && categoryRepository.existsBySlug(request.getSlug()))
            throw new CourseException(ErrorCode.SLUG_ALREADY_EXISTS);

        categoryMapper.updateCategory(category, request);

        return categoryMapper.toCategoryResponse(
                categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CourseException(ErrorCode.CATEGORY_NOT_FOUND));

        // Kiểm tra còn khóa học không
        if (!category.getCourses().isEmpty())
            throw new CourseException(ErrorCode.CANNOT_DELETE_CATEGORY);

        categoryRepository.delete(category);
    }
}
