package com.guo.guopicturebackend.manager.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Comparable<?>> {

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Comparable<?>> preciseShardingValue) {
        // spaceId 可能为 Integer(0) 或 Long，需兼容两种类型避免 ClassCastException
        Long spaceId = toLong(preciseShardingValue.getValue());
        String logicTableName = preciseShardingValue.getLogicTableName();
        // spaceId 为 null 表示查询所有图片
        if (spaceId == null) {
            return logicTableName;
        }
        // 根据 spaceId 动态生成分表名
        String realTableName = "picture_" + spaceId;
        if (availableTargetNames.contains(realTableName)) {
            return realTableName;
        } else {
            return logicTableName;
        }
    }

    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Comparable<?>> rangeShardingValue) {
        return new ArrayList<>();
    }

    /** 将 Integer/Long 等数值类型安全转换为 Long，避免 ClassCastException */
    private Long toLong(Comparable<?> value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    @Override
    public Properties getProps() {
        return null;
    }

    @Override
    public void init(Properties properties) {

    }
}
