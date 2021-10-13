package com.yupfeg.remote.tools.url.cache

import kotlin.math.roundToLong

/**
 * LRU 即 Least Recently Used, 最近最少使用。
 * 当缓存满了, 会优先淘汰那些最近最不常访问的数据
 * * 修改自 Glide的LruCache
 * @author yuPFeG
 * @date 2021/03/10
 */
@Suppress("unused")
open class LruCache<K,V> private constructor(
    /**初始最大可承载缓存量*/
    private val initialMaxSize: Int,
    /**初始缓存容量*/
    initialCapacity: Int
){

    companion object{
        /**默认最大承载容量*/
        private const val DEF_MAX_SIZE = 30
        /**默认初始容量*/
        private const val DEF_INIT_CAPACITY = 30

        /**
         * 创建LRU缓存
         * @param maxSize 最大缓存容量，超出容量，会清理最早访问的数据
         * @param initialCapacity 初始容量，初始容量最好大于承载容量，避免频繁扩容
         * */
        fun <K,V>create(maxSize: Int = DEF_MAX_SIZE,
                        initialCapacity : Int = DEF_INIT_CAPACITY
        ) : LruCache<K, V> {
            val size = if (maxSize <= 0) DEF_MAX_SIZE else maxSize
            return LruCache(size,initialCapacity)
        }
    }

    private val cache : LinkedHashMap<K,V>
        = LinkedHashMap(initialCapacity,0.75f,true)

    /**当前已占用缓存大小*/
    private var currentSize : Long = 0
    /**最大缓存可承载容量*/
    private var maxSize : Long = 0


    /**
     * 返回每个 缓存item 所占用的 size,默认为1
     * */
    open fun getItemSize(item : V?) : Int = 1

    /**
     * 获取当前缓存已占用的大小
     */
    @Synchronized
    fun getUsedSize() : Long = currentSize

    /**
     * 获取缓存最大可承载容积
     * */
    @Suppress("unused")
    @Synchronized
    fun getMaxSize() : Long = maxSize

    /**
     * 校验缓存中是否包含[key]，如果缓存中有对应的 `value` 并且不为 `null`,则返回 true
     *
     * @param key
     * @return `true` 为在容器中含有这个 `key`, 否则为 `false`
     */
    @Suppress("unused")
    @Synchronized
    open fun containsKey(key: K): Boolean {
        return cache.containsKey(key)
    }

    /**
     * 获取当前缓存中含有的所有映射key
     */
    @Suppress("unused")
    @Synchronized
    open fun keySet(): Set<K>? {
        return cache.keys
    }

    /**
     * 返回这个 `key` 在缓存中对应的 `value`, 如果返回 `null` 说明这个 `key` 没有对应的 `value`
     *
     * @param key 用来映射的 `key`
     */
    @Synchronized
    open operator fun get(key: K): V? {
        return cache[key]
    }

    /**
     * 将[key]与[value]对以键值对的形式加入缓存。
     *
     * 如果 [getItemSize] 返回的 size 大于或等于缓存所能允许的最大 size, 则不能向缓存中添加此条目
     * 此时会回调 [onItemEvicted] 通知此方法当前被驱逐的条目
     *
     * @param key   映射key
     * @param value 缓存数据,不可为空
     * @return 如果[key]在缓存中已经有值, 则返回之前的 `value` ,否则返回 `null`
     */
    @Synchronized
    open fun put(key: K, value: V): V? {
        val itemSize = getItemSize(value)
        if (itemSize >= maxSize) {
            onItemEvicted(key, value)
            return null
        }
        val result = cache.put(key, value)
        currentSize += getItemSize(value)
        result?.run { currentSize -= getItemSize(result) }
        evict()
        return result
    }

    /**
     * 移除缓存中[key]所映射的item,并返回所移除缓存的 `value`
     *
     * @param key
     * @return 如果这个 [key] 在容器中已经储存有 `value`，
     * 并且删除成功则返回删除的 `value`, 否则返回 `null`
     */
    @Synchronized
    open fun remove(key: K): V? {
        val value = cache.remove(key)
        if (value != null) {
            currentSize -= getItemSize(value)
        }
        return value
    }

    //<editor-fold desc="清理缓存">


    /**
     * 当缓存中有被驱逐的条目时,会回调此方法,默认空实现,子类可以重写这个方法
     * @param key   被驱逐缓存item的映射key
     * @param value 被驱逐缓存item的value
     */
    protected open fun onItemEvicted(key: K, value: V?) {
        // optional override
    }

    /**
     * 设置指定要保留的缓存容量系数，立即调用 [evict] 清除超出该条件的缓存
     *
     * @param multiplier 系数
     */
    @Synchronized
    open fun setSizeMultiplier(multiplier: Float) {
        require(multiplier >= 0) { "Multiplier must be >= 0" }
        maxSize = (initialMaxSize * multiplier).roundToLong()
        evict()
    }

    /**清空所有缓存*/
    fun clearMemory() {
        trimToSize(0)
    }

    /**
     * 移除最早访问过的缓存项，直到小于指定的数量
     * @param size The size the cache should be less than.
     */
    @Synchronized
    protected fun trimToSize(size: Long) {
        var entry: Map.Entry<K, V>
        var cacheIterator: MutableIterator<Map.Entry<K, V>>
        while (currentSize > size) {
            cacheIterator = cache.entries.iterator()
            entry = cacheIterator.next()
            val toRemove: V = entry.value
            currentSize -= getItemSize(toRemove)
            val key: K = entry.key
            cacheIterator.remove()
            onItemEvicted(key, toRemove)
        }
    }

    /**移除最早访问数据*/
    private fun evict() {
        trimToSize(maxSize)
    }

    //</editor-fold desc="清理缓存">

}