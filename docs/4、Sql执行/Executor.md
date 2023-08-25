# Executor
## UML
```plantuml
interface Executor
abstract BaseExecutor

Executor <|.. CachingExecutor
Executor <|.. BaseExecutor
BaseExecutor <|-- SimpleExecutor
BaseExecutor <|-- ReuseExecutor
BaseExecutor <|-- BatchExecutor
```

主要用到了以下三种Executor：
```java
public enum ExecutorType {
  SIMPLE, REUSE, BATCH
}
```
`CachingExecutor` 比较特殊，只是在其它三个的基础上增加了个缓存（也就是Mybatis中的一级缓存）。 默认的 `Executor` 为 `CachingExecutor`(`SimpleExecutor`)
```java
public class Configuration {
    protected boolean cacheEnabled = true;
    protected ExecutorType defaultExecutorType = ExecutorType.SIMPLE;
    
    public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
        executorType = executorType == null ? defaultExecutorType : executorType;
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
    // ------------------
    // other code ignored
    // ------------------
}
```

Executor 将 CRUD 方法进行了抽象汇总。
仅剩下了 query 和 update(包括insert, delete)
```java
public interface Executor {
    
    int update(MappedStatement ms, Object parameter) throws SQLException;

    <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey cacheKey, BoundSql boundSql) throws SQLException;

    <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler) throws SQLException;
    // ------------------
    // other code ignored
    // ------------------
}
```

* BaseExecutor：基础执行器，封装了子类的公共方法，包括一级缓存、延迟加载、回滚、关闭等功能；
* SimpleExecutor：简单执行器，每执行一条 sql，都会打开一个 Statement，执行完成后关闭；
* ReuseExecutor：重用执行器，相较于 SimpleExecutor 多了 Statement 的缓存功能，其内部维护一个 Map<String, Statement>，每次编译完成的 Statement 都会进行缓存，不会关闭；
* BatchExecutor：批量执行器，基于 JDBC 的 addBatch、executeBatch 功能，并且在当前 sql 和上一条 sql 完全一样的时候，重用 Statement，在调用 doFlushStatements 的时候，将数据刷新到数据库；
* CachingExecutor：缓存执行器，装饰器模式，在开启二级缓存的时候。会在上面三种执行器的外面包上 CachingExecutor；

SimpleExecutor 非常的简单，每次打开一个 Statement，使用完成以后关闭；
ReuseExecutor 比 SimpleExecutor 多了一个 Statement 的缓存功能
BatchExecutor 是基于 JDBC 的 addBatch、executeBatch 功能的执行器，所以 BachExecutor 只能用于更新（insert|delete|update），不能用于查询（select）


## Executor 的生命周期
Executor 的生命周期和 SqlSession 是一样的
