package org.com.story.service;

import org.com.story.dto.request.CategoryRequest;
import org.com.story.dto.response.CategoryResponse;

import java.util.List;

public interface ServiceCategory {
    List<CategoryResponse> getAllCategories();
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(Long id, CategoryRequest request);
    void deleteCategory(Long id);
}

