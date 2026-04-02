package com.guo.guopicturebackend.elasticsearch;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guo.guopicturebackend.config.ElasticsearchProperties;
import com.guo.guopicturebackend.model.entity.Picture;
import com.guo.guopicturebackend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MySQL 图片变更时同步到 ES（启用 elasticsearch.enabled 时生效）
 */
@Slf4j
@Service
public class PictureEsSyncService {

    @Resource
    private ElasticsearchProperties elasticsearchProperties;

    @Autowired(required = false)
    private PictureEsRepository pictureEsRepository;

    @Lazy
    @Resource
    private PictureService pictureService;

    public boolean isActive() {
        return elasticsearchProperties.isEnabled() && pictureEsRepository != null;
    }

    public void saveOrUpdate(Picture picture) {
        if (!isActive() || picture == null || picture.getId() == null) {
            return;
        }
        try {
            pictureEsRepository.save(toDocument(picture));
        } catch (Exception e) {
            log.warn("同步图片到 ES 失败 id={}", picture.getId(), e);
        }
    }

    public void deleteByPictureId(Long pictureId) {
        if (!isActive() || pictureId == null) {
            return;
        }
        try {
            pictureEsRepository.deleteById(String.valueOf(pictureId));
        } catch (Exception e) {
            log.warn("从 ES 删除图片失败 id={}", pictureId, e);
        }
    }

    /**
     * 全量重建索引（管理员）
     */
    public long reindexAll() {
        if (!isActive()) {
            return 0;
        }
        long total = 0;
        long current = 1;
        final long size = 200;
        while (true) {
            Page<Picture> page = pictureService.page(new Page<>(current, size));
            List<Picture> records = page.getRecords();
            if (records.isEmpty()) {
                break;
            }
            pictureEsRepository.saveAll(records.stream().map(this::toDocument).collect(Collectors.toList()));
            total += records.size();
            if (!page.hasNext()) {
                break;
            }
            current++;
        }
        log.info("ES 全量索引完成，共 {} 条", total);
        return total;
    }

    private PictureEsDocument toDocument(Picture p) {
        long sid = p.getSpaceId() == null ? 0L : p.getSpaceId();
        String cat = StrUtil.blankToDefault(p.getCategory(), "");
        return PictureEsDocument.builder()
                .id(String.valueOf(p.getId()))
                .spaceId(sid)
                .name(StrUtil.nullToEmpty(p.getName()))
                .introduction(StrUtil.nullToEmpty(p.getIntroduction()))
                .reviewStatus(p.getReviewStatus())
                .category(cat)
                .build();
    }
}
