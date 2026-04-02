package com.guo.guopicturebackend.manager.cache;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.guo.guopicturebackend.config.PictureCacheProperties;
import com.guo.guopicturebackend.model.dto.picture.PictureQueryRequest;
import com.guo.guopicturebackend.model.entity.Picture;
import com.guo.guopicturebackend.model.vo.PictureVO;
import com.guo.guopicturebackend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;

/**
 * 多级缓存（本地 Caffeine + Redis）+ 防击穿（分布式锁重建）+ 防穿透（空值缓存）+ 防雪崩（TTL 随机抖动）；
 * 热点图片：访问计数超阈值后将实体写入延长本地缓存。
 */
@Service
@Slf4j
public class PictureMultiLevelCacheService {

    private static final String LIST_VERSION_KEY = "picture:listCacheVersion";
    private static final String LIST_EMPTY_MARK = "__LIST_EMPTY__";
    private static final String NULL_ENTITY_MARK = "1";

    @Resource
    private PictureCacheProperties props;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    @Lazy
    private PictureService pictureService;

    /** 公共列表：本地 L1 */
    private Cache<String, String> listLocalCache;

    /** 热点图片实体：本地延长缓存 */
    private Cache<String, String> hotEntityLocalCache;

    @PostConstruct
    public void init() {
        listLocalCache = Caffeine.newBuilder()
                .maximumSize(props.getListLocalMaxSize())
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build();
        hotEntityLocalCache = Caffeine.newBuilder()
                .maximumSize(props.getHotEntityLocalMaxSize())
                .expireAfterWrite(props.getHotLocalExpireMinutes(), TimeUnit.MINUTES)
                .build();
        stringRedisTemplate.opsForValue().setIfAbsent(LIST_VERSION_KEY, "1");
    }

    private String listVersion() {
        String v = stringRedisTemplate.opsForValue().get(LIST_VERSION_KEY);
        return v == null ? "1" : v;
    }

    private String listCacheRedisKey(String queryHash) {
        return "pic:list:v:" + listVersion() + ":" + queryHash;
    }

    private String listLockKey(String queryHash) {
        return "pic:lock:list:" + listVersion() + ":" + queryHash;
    }

    private String entityRedisKey(long spaceId, long pictureId) {
        return "pic:entity:" + spaceId + ":" + pictureId;
    }

    private String nullEntityRedisKey(long spaceId, long pictureId) {
        return "pic:null:" + spaceId + ":" + pictureId;
    }

    private String hotCountKey(long spaceId, long pictureId) {
        return "pic:hotcnt:" + spaceId + ":" + pictureId;
    }

    private String hotEntityLocalKey(long spaceId, long pictureId) {
        return "pic:hotlocal:" + spaceId + ":" + pictureId;
    }

    /**
     * 公共图库分页（已过审、nullSpaceId），多级缓存 + 锁 + 空结果缓存
     */
    public Page<PictureVO> getPublicListPage(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        String queryJson = JSONUtil.toJsonStr(pictureQueryRequest);
        String hash = DigestUtils.md5DigestAsHex(queryJson.getBytes());
        String redisKey = listCacheRedisKey(hash);
        String lockKey = listLockKey(hash);
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();

        String cached = listLocalCache.getIfPresent(redisKey);
        if (cached != null) {
            if (LIST_EMPTY_MARK.equals(cached)) {
                return emptyPage(pictureQueryRequest);
            }
            return parseListPageVo(cached, pictureQueryRequest);
        }
        cached = ops.get(redisKey);
        if (cached != null) {
            if (LIST_EMPTY_MARK.equals(cached)) {
                listLocalCache.put(redisKey, LIST_EMPTY_MARK);
                return emptyPage(pictureQueryRequest);
            }
            listLocalCache.put(redisKey, cached);
            return parseListPageVo(cached, pictureQueryRequest);
        }

        for (int attempt = 0; attempt < props.getListLockSpinMaxAttempts(); attempt++) {
            Boolean locked = ops.setIfAbsent(lockKey, "1", props.getListRebuildLockSeconds(), TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(locked)) {
                try {
                    cached = ops.get(redisKey);
                    if (cached != null) {
                        if (LIST_EMPTY_MARK.equals(cached)) {
                            listLocalCache.put(redisKey, LIST_EMPTY_MARK);
                            return emptyPage(pictureQueryRequest);
                        }
                        listLocalCache.put(redisKey, cached);
                        return parseListPageVo(cached, pictureQueryRequest);
                    }
                    long current = pictureQueryRequest.getCurrent();
                    long size = pictureQueryRequest.getPageSize();
                    Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                            pictureService.getQueryWrapper(pictureQueryRequest));
                    Page<PictureVO> voPage = pictureService.getPictureVOPage(picturePage, request);
                    boolean empty = voPage.getRecords() == null || voPage.getRecords().isEmpty();
                    if (empty) {
                        int ttl = props.getListEmptyTtlBaseSeconds()
                                + RandomUtil.randomInt(0, Math.max(1, props.getListEmptyTtlJitterSeconds()));
                        ops.set(redisKey, LIST_EMPTY_MARK, ttl, TimeUnit.SECONDS);
                        listLocalCache.put(redisKey, LIST_EMPTY_MARK);
                        return voPage;
                    }
                    String json = JSONUtil.toJsonStr(voPage);
                    int ttl = props.getListRedisTtlBaseSeconds()
                            + RandomUtil.randomInt(0, Math.max(1, props.getListRedisTtlJitterSeconds()));
                    ops.set(redisKey, json, ttl, TimeUnit.SECONDS);
                    listLocalCache.put(redisKey, json);
                    return voPage;
                } finally {
                    stringRedisTemplate.delete(lockKey);
                }
            }
            try {
                Thread.sleep(props.getListLockSpinSleepMs() + RandomUtil.randomInt(0, 25));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            cached = ops.get(redisKey);
            if (cached != null) {
                if (LIST_EMPTY_MARK.equals(cached)) {
                    listLocalCache.put(redisKey, LIST_EMPTY_MARK);
                    return emptyPage(pictureQueryRequest);
                }
                listLocalCache.put(redisKey, cached);
                return parseListPageVo(cached, pictureQueryRequest);
            }
        }

        log.warn("公共列表缓存锁等待超时，直查数据库 hash={}", hash);
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return pictureService.getPictureVOPage(picturePage, request);
    }

    private static Page<PictureVO> emptyPage(PictureQueryRequest q) {
        Page<PictureVO> p = new Page<>(q.getCurrent(), q.getPageSize(), 0);
        p.setRecords(java.util.Collections.emptyList());
        return p;
    }

    @SuppressWarnings("unchecked")
    private Page<PictureVO> parseListPageVo(String json, PictureQueryRequest q) {
        if (LIST_EMPTY_MARK.equals(json)) {
            return emptyPage(q);
        }
        return JSONUtil.toBean(json, Page.class);
    }

    /**
     * 详情：先防穿透空标记，再热点本地实体，再 Redis 实体，再 DB；命中后写回并统计热点。
     */
    public Picture getPictureEntityForDetail(long pictureId, long spaceId) {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String nullKey = nullEntityRedisKey(spaceId, pictureId);
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(nullKey))) {
            return null;
        }

        String hotLocal = hotEntityLocalCache.getIfPresent(hotEntityLocalKey(spaceId, pictureId));
        if (hotLocal != null) {
            return JSONUtil.toBean(hotLocal, Picture.class);
        }

        String entityKey = entityRedisKey(spaceId, pictureId);
        String json = ops.get(entityKey);
        if (json != null) {
            return JSONUtil.toBean(json, Picture.class);
        }

        Picture picture = pictureService.getPictureByIdAndSpaceId(pictureId, spaceId);
        if (picture == null) {
            int ttl = props.getDetailNullTtlBaseSeconds()
                    + RandomUtil.randomInt(0, Math.max(1, props.getDetailNullTtlJitterSeconds()));
            ops.set(nullKey, NULL_ENTITY_MARK, ttl, TimeUnit.SECONDS);
            return null;
        }
        int ttl = props.getDetailEntityTtlBaseSeconds()
                + RandomUtil.randomInt(0, Math.max(1, props.getDetailEntityTtlJitterSeconds()));
        ops.set(entityKey, JSONUtil.toJsonStr(picture), ttl, TimeUnit.SECONDS);
        return picture;
    }

    /**
     * 记录访问次数；超阈值则将实体写入延长本地缓存（热点自动识别）
     */
    public void recordPictureHotAccess(long pictureId, long spaceId, Picture picture) {
        if (picture == null) {
            return;
        }
        String cntKey = hotCountKey(spaceId, pictureId);
        Long c = stringRedisTemplate.opsForValue().increment(cntKey);
        if (c != null && c == 1L) {
            stringRedisTemplate.expire(cntKey, props.getHotWindowSeconds(), TimeUnit.SECONDS);
        }
        if (c != null && c >= props.getHotAccessThreshold()) {
            String lk = hotEntityLocalKey(spaceId, pictureId);
            hotEntityLocalCache.put(lk, JSONUtil.toJsonStr(picture));
            log.debug("热点图片已加入本地延长缓存 pictureId={} spaceId={} accessInWindow={}", pictureId, spaceId, c);
        }
    }

    /** 公共列表缓存失效：版本号递增，旧 key 自然过期 */
    public void bumpPublicListCacheVersion() {
        try {
            stringRedisTemplate.opsForValue().increment(LIST_VERSION_KEY);
            listLocalCache.invalidateAll();
        } catch (Exception e) {
            log.warn("bumpPublicListCacheVersion 失败", e);
        }
    }

    /** 单图详情缓存失效（编辑/删除/审核后） */
    public void invalidatePictureDetail(Long spaceId, long pictureId) {
        long sid = spaceId != null ? spaceId : 0L;
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        try {
            stringRedisTemplate.delete(entityRedisKey(sid, pictureId));
            stringRedisTemplate.delete(nullEntityRedisKey(sid, pictureId));
            stringRedisTemplate.delete(hotCountKey(sid, pictureId));
            hotEntityLocalCache.invalidate(hotEntityLocalKey(sid, pictureId));
            java.util.Set<String> keys = stringRedisTemplate.keys("pic:vo:" + sid + ":" + pictureId + ":*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("invalidatePictureDetail 失败 pictureId={} spaceId={}", pictureId, sid, e);
        }
    }
}
