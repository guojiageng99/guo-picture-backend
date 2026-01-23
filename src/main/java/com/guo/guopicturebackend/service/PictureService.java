package com.guo.guopicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guo.guopicturebackend.model.dto.picture.PictureQueryRequest;
import com.guo.guopicturebackend.model.dto.picture.PictureReviewRequest;
import com.guo.guopicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.guo.guopicturebackend.model.dto.picture.PictureUploadRequest;
import com.guo.guopicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guo.guopicturebackend.model.entity.User;
import com.guo.guopicturebackend.model.vo.PictureVO;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 44884
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-01-15 09:22:15
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);



    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    public PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    public void validPicture(Picture picture);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);


    public void fillReviewParams(Picture picture, User loginUser);


    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    public void clearPictureFile(Picture oldPicture);

    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

}
