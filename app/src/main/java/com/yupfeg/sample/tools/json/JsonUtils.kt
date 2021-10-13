package com.yupfeg.sample.tools.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import java.lang.reflect.Type

/**
 * json解析的工具类
 * @author yuPFeG
 * @date 2020/01/04
 */
object JsonUtils {
    private val gson: Gson by lazy(LazyThreadSafetyMode.SYNCHRONIZED){
        GsonBuilder().setLenient().create()
    }

    @Throws(JsonSyntaxException::class)
    @JvmStatic
    fun <T> fromJson(json: String,classOfT : Class<T>) : T?{
        return gson.fromJson(json, classOfT)
    }

    /**
     * 将json字符串转换成type类型的对象
     * @param json json字符串
     * @param type 目标解析bean类
     * @param <T>
     * @return
     */
    @Throws(JsonSyntaxException::class)
    @JvmStatic
    fun <T> fromJson(json: String, type: Type): T?{
        return gson.fromJson(json, type)
    }

    /**
     * 将对象序列化成字符串对象
     * @param data
     * @return
     */
    @JvmStatic
    fun toJson(data: Any): String {
        return gson.toJson(data)
    }

}