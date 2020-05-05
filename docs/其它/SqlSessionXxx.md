# SqlSessionXxx
这里的SqlSessionXxx包括：SqlSession、SqlSessionFactory、SqlSessionManager

## SqlSession
SqlSession可以看做是sql执行的入口，SqlSession提供了一系列CRUD方法，而注释较少的Mybatis也给予了以下提示：
```
The primary Java interface for working with MyBatis.
Through this interface you can execute commands, get mappers and manage transactions.
```

### DefaultSqlSession
SqlSession的默认实现。虽然SqlSession可以看做是sql执行的入口，但是其内部却是由Executor去执行这些sql的。如：
```
@Override
public <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds) {
    try {
        MappedStatement ms = configuration.getMappedStatement(statement);
        return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
    } catch (Exception e) {
        throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
    } finally {
        ErrorContext.instance().reset();
    }
}
```
同时DefaultSqlSession也需要引用Configuration以获取到MappedStatement
> 参数中的statement实际上是MappedStatement的id，或者称为管理所有MappedStatement的Map的Key，也是sql的xml中的 namespace + id

DefaultSqlSession其中两个属性为：dirty, autoCommit（dirty是什么？）。autoCommit为是否自动提交，在 `org.apache.ibatis.session.SqlSessionFactory.openSession(boolean)` 时指定。
```java
public interface SqlSessionFactory {

    // ... 
    SqlSession openSession(boolean autoCommit);
    // ...
}
```

### delete与update
mybatis实际上还是使用update来执行delete的：
```
public class DefaultSqlSession implements SqlSession {
    // ...
    
    @Override
    public int delete(String statement) {
        return update(statement, null);
    }
    
    @Override
    public int delete(String statement, Object parameter) {
        return update(statement, parameter);
    }
    
    // ...
}
```

## SqlSessionFactory
而SqlSessionFactory则提供了一系列方法以创建SqlSession
```java
public interface SqlSessionFactory {
    SqlSession openSession();
    SqlSession openSession(boolean autoCommit);
    SqlSession openSession(Connection connection);
    SqlSession openSession(TransactionIsolationLevel level);
    SqlSession openSession(ExecutorType execType);
    SqlSession openSession(ExecutorType execType, boolean autoCommit);
    SqlSession openSession(ExecutorType execType, TransactionIsolationLevel level);
    SqlSession openSession(ExecutorType execType, Connection connection);
    Configuration getConfiguration();
}
```

### 通过Configuration创建SqlSessionFactory
```
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
```
>1. configuration参见[创建简单的Configuration](/其它/Configuration.md#创建简单的Configuration)
>2. 这里返回的SqlSessionFactory实际上是 DefaultSqlSessionFactory

### DefaultSqlSessionFactory
SqlSessionFactory的默认实现，提供了一系列生成SqlSession的方法，但其核心方法有两个： `openSessionFromDataSource` 与 `openSessionFromConnection` （即从数据源DataSource或者链接Connection获取SqlSession）

其区别是： 

| | 含义 | FromDataSource | FromConnection |
| ---- | ---- | ---- | ---- |
| autoCommit | 是否自动提交 | 参数指定 | 从connection.getAutoCommit()获取 |
| TransactionIsolationLevel | 事务隔离级别 | 参数指定 | 未指定 |


## SqlSessionManager
SqlSessionManager则是SqlSession与SqlSessionFactory的同时代理，同时也采用了ThreadLocal用来保证线程安全
> 由于SqlSession的实现DefaultSqlSession非线程安全

### 代理SqlSessionFactory与SqlSession
由于代理了SqlSessionFactory，同时SqlSession又是由SqlSessionFactory生成，因此顺便也代理了SqlSession。其中代理SqlSessionFactory使用的是代理模式，代理SqlSession则是用的JDK的动态代理。
```
public class SqlSessionManager implements SqlSessionFactory, SqlSession {

    private final SqlSessionFactory sqlSessionFactory;
    private final SqlSession sqlSessionProxy;
    
    // ...
    
    private SqlSessionManager(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(
        SqlSessionFactory.class.getClassLoader(),
        new Class[]{SqlSession.class},
        new SqlSessionInterceptor());
    }
    
    // ...
}
```