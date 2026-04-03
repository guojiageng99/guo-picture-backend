package com.guo.guopicturebackend.elasticsearch;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guo.guopicturebackend.config.ElasticsearchProperties;
import com.guo.guopicturebackend.model.dto.picture.PictureQueryRequest;
import com.guo.guopicturebackend.model.entity.Picture;
import com.guo.guopicturebackend.model.vo.PictureVO;
import com.guo.guopicturebackend.service.PictureService;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 基于 ES 的「名称 + 简介」全文检索；与 {@link PictureQueryRequest#searchText} 对齐。
 */
@Service
public class PictureEsSearchService {

    @Resource
    private ElasticsearchProperties elasticsearchProperties;

    @Autowired(required = false)
    private ElasticsearchOperations elasticsearchOperations;

    @Lazy
    @Resource
    private PictureService pictureService;

    public boolean isActive() {
        return elasticsearchProperties.isEnabled() && elasticsearchOperations != null;
    }

    /**
     * 仅处理「关键词 + 少量与 ES 文档一致的筛选」；其它条件走 MySQL。
     */
    public boolean supportsEsQuery(PictureQueryRequest q) {
        if (!isActive() || StrUtil.isBlank(q.getSearchText())) {
            return false;
        }
        if (!isEsCompatibleSortField(q.getSortField())) {
            return false;
        }
        if (ObjUtil.isNotEmpty(q.getId())) {
            return false;
        }
        if (ObjUtil.isNotEmpty(q.getUserId())) {
            return false;
        }
        if (StrUtil.isNotBlank(q.getName())) {
            return false;
        }
        if (StrUtil.isNotBlank(q.getIntroduction())) {
            return false;
        }
        if (CollUtil.isNotEmpty(q.getTags())) {
            return false;
        }
        if (ObjUtil.isNotEmpty(q.getPicSize()) || ObjUtil.isNotEmpty(q.getPicWidth())
                || ObjUtil.isNotEmpty(q.getPicHeight()) || ObjUtil.isNotEmpty(q.getPicScale())) {
            return false;
        }
        if (StrUtil.isNotBlank(q.getPicFormat())) {
            return false;
        }
        if (ObjUtil.isNotEmpty(q.getReviewerId())) {
            return false;
        }
        if (StrUtil.isNotBlank(q.getReviewMessage())) {
            return false;
        }
        if (ObjUtil.isNotEmpty(q.getReviewTime())) {
            return false;
        }
        if (ObjUtil.isNotEmpty(q.getStartEditTime()) || ObjUtil.isNotEmpty(q.getEndEditTime())) {
            return false;
        }
        return true;
    }

    /**
     * 列表默认按 createTime 排序；仅该字段与 ES 文档中的 createTimeMillis 对齐，其它排序仍走 MySQL。
     */
    private static boolean isEsCompatibleSortField(String sortField) {
        return StrUtil.isBlank(sortField) || "createTime".equals(sortField);
    }

    public Page<PictureVO> searchPictureVoPage(PictureQueryRequest q, HttpServletRequest request) {
        String text = q.getSearchText().trim();
        long current = q.getCurrent();
        long size = q.getPageSize();

        BoolQueryBuilder bool = QueryBuilders.boolQuery()
                .must(QueryBuilders.multiMatchQuery(text, "name", "introduction")
                        .type(MultiMatchQueryBuilder.Type.BEST_FIELDS));

        if (q.getSpaceId() == null || q.isNullSpaceId()) {
            bool.filter(QueryBuilders.termQuery("spaceId", 0L));
        } else {
            bool.filter(QueryBuilders.termQuery("spaceId", q.getSpaceId()));
        }

        if (q.getReviewStatus() != null) {
            bool.filter(QueryBuilders.termQuery("reviewStatus", q.getReviewStatus()));
        }
        if (StrUtil.isNotBlank(q.getCategory())) {
            bool.filter(QueryBuilders.termQuery("category", q.getCategory().trim()));
        }

        SortOrder timeOrder = "ascend".equalsIgnoreCase(StrUtil.nullToEmpty(q.getSortOrder()))
                ? SortOrder.ASC
                : SortOrder.DESC;
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(bool)
                .withPageable(PageRequest.of((int) Math.max(0, current - 1), (int) size))
                .withSort(SortBuilders.fieldSort("createTimeMillis").order(timeOrder))
                .build();

        SearchHits<PictureEsDocument> hits = elasticsearchOperations.search(searchQuery, PictureEsDocument.class);
        long total = hits.getTotalHits();

        List<Long> idOrder = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(PictureEsDocument::getId)
                .map(Long::parseLong)
                .collect(Collectors.toList());

        if (idOrder.isEmpty()) {
            Page<Picture> empty = new Page<>(current, size, 0);
            empty.setRecords(new ArrayList<>());
            return pictureService.getPictureVOPage(empty, request);
        }

        List<Picture> loaded = pictureService.listByIds(idOrder);
        Map<Long, Picture> byId = loaded.stream().collect(Collectors.toMap(Picture::getId, p -> p, (a, b) -> a));
        List<Picture> ordered = new ArrayList<>();
        for (Long id : idOrder) {
            Picture p = byId.get(id);
            if (p != null) {
                ordered.add(p);
            }
        }

        Page<Picture> picturePage = new Page<>(current, size, total);
        picturePage.setRecords(ordered);
        return pictureService.getPictureVOPage(picturePage, request);
    }
}
