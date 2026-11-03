package com.shopledger.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 图表 JSON 序列化工具。
 */
public final class JsonUtil {

    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private JsonUtil() {
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }
}
