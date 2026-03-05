package org.com.story.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.story.dto.request.CategoryRequest;
import org.com.story.dto.response.CategoryResponse;
import org.com.story.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Category Controller", description = "Quản lý thể loại truyện")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories", description = "Lấy danh sách tất cả thể loại (public)")
    public List<CategoryResponse> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create category (Admin)", description = "Tạo thể loại mới (chỉ Admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public CategoryResponse createCategory(@Valid @RequestBody CategoryRequest request) {
        return categoryService.createCategory(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update category (Admin)", description = "Cập nhật thể loại (chỉ Admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public CategoryResponse updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete category (Admin)", description = "Xóa thể loại (chỉ Admin)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public void deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
    }
}

