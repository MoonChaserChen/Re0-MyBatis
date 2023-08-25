# Mybatis中的Cache
## 一、CacheKey
提到缓存就不得不说缓存的Key是什么，Mybatis里用CacheKey来表示，包含多个元素信息(`CacheKey.update()`)，只有当这多个元素信息都相同时，才认为是同一个Key。
实际使用了以下元素信息：
* MappedStatement.id
* sql
* 分页参数
* sql参数
* Environment.id

其源码如下：
```java
public abstract class BaseExecutor implements Executor {
    @Override
    public CacheKey createCacheKey(MappedStatement ms, Object parameterObject, RowBounds rowBounds, BoundSql boundSql) {
        if (closed) {
            throw new ExecutorException("Executor was closed.");
        }
        CacheKey cacheKey = new CacheKey();
        cacheKey.update(ms.getId());
        cacheKey.update(rowBounds.getOffset());
        cacheKey.update(rowBounds.getLimit());
        cacheKey.update(boundSql.getSql());
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        TypeHandlerRegistry typeHandlerRegistry = ms.getConfiguration().getTypeHandlerRegistry();
        for (ParameterMapping parameterMapping : parameterMappings) {
            if (parameterMapping.getMode() != ParameterMode.OUT) {
                Object value;
                String propertyName = parameterMapping.getProperty();
                if (boundSql.hasAdditionalParameter(propertyName)) {
                    value = boundSql.getAdditionalParameter(propertyName);
                } else if (parameterObject == null) {
                    value = null;
                } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                    value = parameterObject;
                } else {
                    MetaObject metaObject = configuration.newMetaObject(parameterObject);
                    value = metaObject.getValue(propertyName);
                }
                cacheKey.update(value);
            }
        }
        if (configuration.getEnvironment() != null) {
            // issue #176
            cacheKey.update(configuration.getEnvironment().getId());
        }
        return cacheKey;
    }
    // ------------------
    // other code ignored
    // ------------------
}
```

也就是只要两条SQL的上述五个值相同，即可以认为是相同的SQL，才会为这个相同的SQL进行缓存查询。

## 二、Cache
Mybatis里的Cache管理，主要包括保存、查询、移除缓存等操作。
```java
public interface Cache {
    // Cache 的 id
    String getId();
    // 存缓存，这时的Key一般为 org.apache.ibatis.cache.CacheKey
    void putObject(Object key, Object value);
    // 取缓存
    Object getObject(Object key);
    // 清除所有缓存
    void clear();
    // ------------------
    // other code ignored
    // ------------------
}
```

```plantuml
interface Cache

Cache <|.. SoftCache 
Cache <|.. PerpetualCache 
Cache <|.. LoggingCache 
Cache <|.. SynchronizedCache 
Cache <|.. LruCache 
Cache <|.. ScheduledCache 
Cache <|.. WeakCache 
Cache <|.. FifoCache 
Cache <|.. SerializedCache 
Cache <|.. BlockingCache 
Cache <|.. TransactionalCache 
```

除了 PerpetualCache 以外，其它的 Cache 都有个 Cache delegate 属性，可以使用代理的方式实现装饰器模式，不断为 Cache 增加功能。

| Cache                | 说明                                                                          |
|----------------------|-----------------------------------------------------------------------------|
| PerpetualCache       | 以HashMap实现的Cache，缓存的数据不会过期，所以叫PerpetualCache。一级缓存的实现。                       |
| LoggingCache         | 增加日志功能。在查询缓存的时候debug出缓存命中率。有个疑惑：请求次数用`int requests`表示，超出范围了怎么办？             |
| SynchronizedCache    | 增加同步操作。缓存操作方法用 synchronized 同步处理                                            |
| LruCache             | 增加LRU功能。到达Cache容量上限时，删除最久未使用的KV。通过 `LinkedHashMap#removeEldestEntry()` 来实现。 |
| SerializedCache      | 增加Serialize功能。在保存V时，将其Serialize为字节数组。                                       |
| TransactionalCache   | 二级缓存会用到                                                                     |


## TransactionalCacheManager
```java
public class TransactionalCacheManager {

    private final Map<Cache, TransactionalCache> transactionalCaches = new HashMap<>();

    public void clear(Cache cache) {
        getTransactionalCache(cache).clear();
    }

    public Object getObject(Cache cache, CacheKey key) {
        return getTransactionalCache(cache).getObject(key);
    }

    public void putObject(Cache cache, CacheKey key, Object value) {
        getTransactionalCache(cache).putObject(key, value);
    }

    public void commit() {
        for (TransactionalCache txCache : transactionalCaches.values()) {
            txCache.commit();
        }
    }

    public void rollback() {
        for (TransactionalCache txCache : transactionalCaches.values()) {
            txCache.rollback();
        }
    }

    private TransactionalCache getTransactionalCache(Cache cache) {
        return MapUtil.computeIfAbsent(transactionalCaches, cache, TransactionalCache::new);
    }

}
```
这个结构好难理解啊。TransactionalCache 是二级缓存

## 三、一级缓存
Mybatis的一级缓存由BaseExecutor实现。使用到的Cache是PerpetualCache。
```java
public abstract class BaseExecutor implements Executor {
    protected PerpetualCache localCache;

    @Override
    public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
        // ------------------
        // code simplified
        // ------------------
        List<E> list;
        try {
            // 先查localCache
            list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
            if (list != null) {
                handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
            } else {
                // 未查到，查询数据库
                list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
            }
        } finally {
            queryStack--;
        }
        return list;
    }
}
```
上面可以看到在BaseExecutor查询时，先查localCache，查不到再查询数据库。

### 缓存的清除
缓存的清除逻辑直接影响缓存的生命周期。缓存的清除方法为：`BaseExecutor#clearLocalCache()`，其调用时机有：
1. 当前sqlSession执行了insert, update, delete方法（即使这些方法修改的是别的表）
2. 当前sqlSession执行了commit, rollback方法
3. sql语句(MappedStatement)指明要清除缓存 flushCacheRequired。 `<select id="selectXx" useCache="false">..</select>`
4. 一级缓存指明范围为 LocalCacheScope.STATEMENT 时，每次query后都会删除


```xml
<settings>
    <!--SESSION | STATEMENT，默认为 SESSION-->
    <setting name="localCacheScope" value="STATEMENT"/>
</settings>
```
## 四、二级缓存
Mybatis的二级缓存由CachingExecutor实现。使用到的Cache是PerpetualCache。
```java
public class CachingExecutor implements Executor {
    // 这个delegate即为除缓存外实际执行的Executor，如：SIMPLE, REUSE, BATCH。
    private final Executor delegate;
    // 这个tcm用到缓存管理
    private final TransactionalCacheManager tcm = new TransactionalCacheManager();

    @Override
    public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql)
            throws SQLException {
        // 缓存是基于 MappedStatement 的
        Cache cache = ms.getCache();
        if (cache != null) {
            flushCacheIfRequired(ms);
            if (ms.isUseCache() && resultHandler == null) {
                ensureNoOutParams(ms, boundSql);
                // 通过 TransactionalCacheManager、Cache、CacheKey 去查询缓存
                @SuppressWarnings("unchecked")
                List<E> list = (List<E>) tcm.getObject(cache, key);
                if (list == null) {
                    list = delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
                    tcm.putObject(cache, key, list); // issue #578 and #116
                }
                return list;
            }
        }
        return delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
    }
    // ------------------
    // other code ignored
    // ------------------
}
```
查询逻辑：先查缓存，如果能查到则直接返回，如果查不到则通过内部Executor再去查询，查到后设置到缓存中并返回。

数据的查询执行的流程就是 二级缓存 -> 一级缓存 -> 数据库。