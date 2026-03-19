package com.guo.guopicturebackend.controller;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.guo.guopicturebackend.annotation.AuthCheck;
import com.guo.guopicturebackend.api.aliyun.AliYunAiApi;
import com.guo.guopicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.guo.guopicturebackend.api.aliyun.model.GetOutPaintingTaskResponse;
import com.guo.guopicturebackend.api.imagesearch.my.ImageSearchApiFacade;
import com.guo.guopicturebackend.api.imagesearch.my.model.ImageSearchResult;
import com.guo.guopicturebackend.common.BaseResponse;
import com.guo.guopicturebackend.common.DeleteRequest;
import com.guo.guopicturebackend.common.ResultUtils;
import com.guo.guopicturebackend.constant.UserConstant;
import com.guo.guopicturebackend.exception.BusinessException;
import com.guo.guopicturebackend.exception.ErrorCode;
import com.guo.guopicturebackend.exception.ThrowUtils;
import com.guo.guopicturebackend.manager.auth.SpaceUserAuthManager;
import com.guo.guopicturebackend.manager.auth.StpKit;
import com.guo.guopicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.guo.guopicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.guo.guopicturebackend.model.dto.picture.*;
import com.guo.guopicturebackend.model.entity.Picture;
import com.guo.guopicturebackend.model.entity.Space;
import com.guo.guopicturebackend.model.entity.User;
import com.guo.guopicturebackend.model.enums.PictureReviewStatusEnum;
import com.guo.guopicturebackend.model.vo.PictureTagCategory;
import com.guo.guopicturebackend.model.vo.PictureVO;
import com.guo.guopicturebackend.service.PictureService;
import com.guo.guopicturebackend.service.SpaceService;
import com.guo.guopicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {
    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private SpaceService spaceService;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Autowired
    private StpKit stpKit;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    // 在 PictureController 中注入（使用 my 包 HTTP 实现，无需 Selenium）
    @Resource(name = "myImageSearchApiFacade")
    private ImageSearchApiFacade imageSearchApiFacade;


    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        Long spaceId = deleteRequest.getSpaceId();
        pictureService.deletePicture(id, spaceId, loginUser);
//        // 判断是否存在
//        Picture oldPicture = pictureService.getById(id);
//        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        // 仅本人或管理员可删除
//        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }
//        // 操作数据库
//        boolean result = pictureService.removeById(id);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在，分表时 spaceId 不能为 null，默认为 0 表示公共图库
        long id = pictureUpdateRequest.getId();
        Long spaceId = pictureUpdateRequest.getSpaceId() != null ? pictureUpdateRequest.getSpaceId() : 0L;
        Picture oldPicture = pictureService.getPictureByIdAndSpaceId(id, spaceId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 补充审核参数
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser);
        // 操作数据库，附加 spaceId 条件
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Picture> updateWrapper =
                new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        updateWrapper.eq("id", id).eq("spaceId", spaceId);
        boolean result = pictureService.update(picture, updateWrapper);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, Long spaceId, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库，分表时必须带 spaceId 条件
        Picture picture = pictureService.getPictureByIdAndSpaceId(id, spaceId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id, Long spaceId, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库，分表时必须带 spaceId 条件，null 时默认为 0（公共图库）
        Picture picture = pictureService.getPictureByIdAndSpaceId(id, spaceId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间权限校验
        Space space = null;
        Long pictureSpaceId = picture.getSpaceId();
        if (pictureSpaceId != null && pictureSpaceId > 0) {
            boolean hasPermission = stpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
//            User loginUser = userService.getLoginUser(request);
//            pictureService.checkPictureAuth(loginUser, picture);
            space = spaceService.getById(pictureSpaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        }
        // 获取权限列表
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVO pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        // 获取封装类
        return ResultUtils.success(pictureVO);
    }




    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 空间权限校验
        Long spaceId = pictureQueryRequest.getSpaceId();
        // 公开图库
        if (spaceId == null) {
            // 普通用户默认只能查看已过审的公开数据
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            // 使用 Session 鉴权：私有空间所有者、团队空间成员、管理员均可查看
            User loginUser = userService.getLoginUser(request);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_VIEW)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看该空间的图片");
            }
        }

        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
//        // 在此处将实体类和 DTO 进行转换
//        Picture picture = new Picture();
//        BeanUtils.copyProperties(pictureEditRequest, picture);
//        // 注意将 list 转为 string
//        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
//        // 设置编辑时间
//        picture.setEditTime(new Date());
//        // 数据校验
//        pictureService.validPicture(picture);
//        User loginUser = userService.getLoginUser(request);
//        // 判断是否存在
//        long id = pictureEditRequest.getId();
//        Picture oldPicture = pictureService.getById(id);
//        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        // 仅本人或管理员可编辑
//        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }
//        // 补充审核参数
//        pictureService.fillReviewParams(picture, loginUser);
//        // 操作数据库
//        boolean result = pictureService.updateById(picture);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest,
                                                 HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 通过 URL 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVO> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }


    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();


    @PostMapping("/list/page/vo/cache")
    @Deprecated
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                                      HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能查看已过审的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 构建缓存 key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
//        String redisKey = "yupicture:listPictureVOByPage:" + hashKey;
        String cacheKey = "listPictureVOByPage:" + hashKey;
        // 从本地缓存中查询
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            // 如果缓存命中，返回结果
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }
        // 2. 查询分布式缓存（Redis）
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        cachedValue = valueOps.get(cacheKey);
        if (cachedValue != null) {
            // 如果命中 Redis，存入本地缓存并返回
            LOCAL_CACHE.put(cacheKey, cachedValue);
            Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
            return ResultUtils.success(cachedPage);
        }


        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);

        // 存入本地缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        LOCAL_CACHE.put(cacheKey, cacheValue);

        // 存入 Redis 缓存
        cacheValue = JSONUtil.toJsonStr(pictureVOPage);
        // 5 - 10 分钟随机过期，防止雪崩
        int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
        valueOps.set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);

        // 返回结果
        return ResultUtils.success(pictureVOPage);
    }


    /**
     * 以图搜图
     */
    /*@PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(oldPicture.getUrl());
        return ResultUtils.success(resultList);
    }*/

    /**
     * 以图搜图核心接口
     * 接口路径：/picture/search/picture
     * 请求方式：POST
     * 请求体：SearchPictureByPictureRequest（包含pictureId）
     * 返回值：BaseResponse<List<ImageSearchResult>>（图片列表结果）
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(
            @RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        // 1. 参数非空校验
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        // 2. 图片ID合法性校验
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR, "图片ID不合法");
        // 3. 查询图片信息，分表时必须带 spaceId 条件
        Long spaceId = searchPictureByPictureRequest.getSpaceId() != null
                ? searchPictureByPictureRequest.getSpaceId() : 0L;
        Picture oldPicture = pictureService.getPictureByIdAndSpaceId(pictureId, spaceId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
        // 4. 校验图片URL是否有效
        ThrowUtils.throwIf(oldPicture.getUrl() == null || oldPicture.getUrl().isBlank(),
                ErrorCode.OPERATION_ERROR, "图片URL为空");

        try {
            // 5. 调用门面模式核心方法，执行以图搜图
            List<ImageSearchResult> resultList = imageSearchApiFacade.searchImage(oldPicture.getUrl());
            log.info("以图搜图成功，pictureId={}，返回图片数量={}", pictureId, resultList.size());
            // 6. 返回成功结果
            return ResultUtils.success(resultList);
        } catch (Exception e) {
            // 7. 异常捕获与日志记录
            log.error("以图搜图失败，pictureId={}", pictureId, e);
            // 8. 返回统一异常结果
            return ResultUtils.error(ErrorCode.OPERATION_ERROR, "以图搜图失败：" + e.getMessage());
        }
    }

    @PostMapping("/search/color")
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVO> result = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(result);
    }


    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }


    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
            HttpServletRequest request) {
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }


    

}
