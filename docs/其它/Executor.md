# Executor
## 实现类
[Executor.png](https://blog.mybatis.akira.ink/images/Executor.png)

这里可以看到四个实现类：
1. BatchExecutor
2. SimpleExecutor
3. ReuseExecutor
4. CachingExecutor

其中 `CachingExecutor` 比较特殊，只是在其它三个的基础上增加了个缓存（代理模式）；在 `org.apache.ibatis.session.ExecutorType` 也可看出来：
```java
public enum ExecutorType {
  SIMPLE, REUSE, BATCH
}
```

同时 `CachingExecutor` 也是默认的 `Executor`（但是在 `CachingExecutor` 内部的 `Executor` 默认为： `SimpleExecutor` ）
> 参见： `org.apache.ibatis.session.Configuration.newExecutor(org.apache.ibatis.transaction.Transaction, org.apache.ibatis.session.ExecutorType)`
```
public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
    executorType = executorType == null ? defaultExecutorType : executorType;
    executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
    Executor executor;
    if (ExecutorType.BATCH == executorType) {
        executor = new BatchExecutor(this, transaction);
    } else if (ExecutorType.REUSE == executorType) {
        executor = new ReuseExecutor(this, transaction);
    } else {
        executor = new SimpleExecutor(this, transaction);
    }
    if (cacheEnabled) {
        executor = new CachingExecutor(executor);
    }
    executor = (Executor) interceptorChain.pluginAll(executor);
    return executor;
}
```
其中 `cacheEnabled` 默认为 `true` ， `defaultExecutorType` 默认为 `ExecutorType.SIMPLE`
```
public class Configuration {
    private final Executor delegate;
    // ...
    protected boolean cacheEnabled = true;
    // ...
    protected ExecutorType defaultExecutorType = ExecutorType.SIMPLE;
    // ...
}  
```