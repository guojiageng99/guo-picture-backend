package com.guo.guopicturebackend.model.dto.message;

import com.guo.guopicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserMessageQueryRequest extends PageRequest {

    /** 仅看未读 */
    private Boolean unreadOnly;
}
