package com.guo.guopicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guo.guopicturebackend.annotation.AuthCheck;
import com.guo.guopicturebackend.common.BaseResponse;
import com.guo.guopicturebackend.common.DeleteRequest;
import com.guo.guopicturebackend.common.ResultUtils;
import com.guo.guopicturebackend.constant.UserConstant;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.exception.ErrorCode;
import com.guo.guopicturebackend.exception.ThrowUtils;
import com.guo.guopicturebackend.model.dto.picture.PictureCategoryAddRequest;
import com.guo.guopicturebackend.model.dto.picture.PictureCategoryUpdateRequest;
import com.guo.guopicturebackend.model.dto.picture.PictureDictionaryPageRequest;
import com.guo.guopicturebackend.model.entity.PictureCategory;
import com.guo.guopicturebackend.service.PictureCategoryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/picture/category/admin")
public class PictureCategoryAdminController {

    @Resource
    private PictureCategoryService pictureCategoryService;

    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<PictureCategory>> listPage(@RequestBody PictureDictionaryPageRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = request.getCurrent() > 0 ? request.getCurrent() : 1;
        long size = request.getPageSize() > 0 ? request.getPageSize() : 10;
        Page<PictureCategory> page = pictureCategoryService.listPage(current, size);
        return ResultUtils.success(page);
    }

    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> add(@RequestBody PictureCategoryAddRequest addRequest) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);
        long id = pictureCategoryService.addCategory(addRequest.getCategoryName(), addRequest.getSortOrder());
        return ResultUtils.success(id);
    }

    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> update(@RequestBody PictureCategoryUpdateRequest updateRequest) {
        ThrowUtils.throwIf(updateRequest == null || updateRequest.getId() == null, ErrorCode.PARAMS_ERROR);
        pictureCategoryService.updateCategory(updateRequest.getId(), updateRequest.getCategoryName(), updateRequest.getSortOrder());
        return ResultUtils.success(true);
    }

    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> delete(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        pictureCategoryService.deleteCategory(deleteRequest.getId());
        return ResultUtils.success(true);
    }
}
