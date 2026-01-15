package com.guo.guopicturebackend.service;

import com.guo.guopicturebackend.model.dto.picture.PictureUploadRequest;
import com.guo.guopicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.guo.guopicturebackend.model.entity.User;
import com.guo.guopicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

/**
* @author 44884
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-01-15 09:22:15
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);



}
