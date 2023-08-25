# Sql会话-SqlSession
执行Sql可以理解为和数据库通过sql进行会话，一个会话（SqlSession）内可以执行多个Sql。

## 一、SqlSession
可以看做是sql执行的入口，提供了一系列CRUD方法。
```java
public interface SqlSession extends Closeable {
    
    <T> T selectOne(String statement);

    <E> List<E> selectList(String statement);

    int insert(String statement);

    int update(String statement, Object parameter);

    int delete(String statement);

    void commit();

    void rollback();
    
    // ------------------
    // other code ignored
    // ------------------
}
```
> 我不理解为啥 commit, rollback 也放在了 SqlSession 里，这难道不是Sql执行过程中的一个步骤吗？比如update方法里就会进行 `commit` 或 `rollback`

### DefaultSqlSession
`SqlSession` 的默认实现，其核心是从 `Configuration` 中查出对应的 `MappedStatement` 后交给  `Executor` 去执行的。
```java
public class DefaultSqlSession implements SqlSession {
    
    private <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
        try {
            MappedStatement ms = configuration.getMappedStatement(statement);
            return executor.query(ms, wrapCollection(parameter), rowBounds, handler);
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }

    @Override
    public int update(String statement, Object parameter) {
        try {
            dirty = true;
            MappedStatement ms = configuration.getMappedStatement(statement);
            return executor.update(ms, wrapCollection(parameter));
        } catch (Exception e) {
            throw ExceptionFactory.wrapException("Error updating database.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }
    // ------------------
    // other code ignored
    // ------------------
}
```
#### update
mybatis实际上还是使用update来执行delete和insert的：
```java
public class DefaultSqlSession implements SqlSession {
    
    @Override
    public int delete(String statement) {
        return update(statement, null);
    }
    
    @Override
    public int delete(String statement, Object parameter) {
        return update(statement, parameter);
    }

    @Override
    public int insert(String statement, Object parameter) {
        return update(statement, parameter);
    }
    // ------------------
    // other code ignored
    // ------------------
}
```

## 二、SqlSessionFactory
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

### DefaultSqlSessionFactory
SqlSessionFactory的默认实现，提供了一系列生成SqlSession的方法。其核心是从Configuration中拿出相关配置，生成Executor，进而生成SqlSession。例如：
```java
public class DefaultSqlSessionFactory implements SqlSessionFactory {
    private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
        Transaction tx = null;
        try {
            final Environment environment = configuration.getEnvironment();
            final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
            tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
            final Executor executor = configuration.newExecutor(tx, execType);
            return new DefaultSqlSession(configuration, executor, autoCommit);
        } catch (Exception e) {
            closeTransaction(tx); // may have fetched a connection so lets call close()
            throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
        } finally {
            ErrorContext.instance().reset();
        }
    }
    // ------------------
    // other code ignored
    // ------------------
}
```

## 三、SqlSessionManager
SqlSessionManager则是SqlSession与SqlSessionFactory的同时代理，同时也采用了ThreadLocal用来保证线程安全
> 由于SqlSession的实现DefaultSqlSession非线程安全

### 代理SqlSessionFactory与SqlSession
由于代理了SqlSessionFactory，同时SqlSession又是由SqlSessionFactory生成，因此顺便也代理了SqlSession。其中代理SqlSessionFactory使用的是代理模式，代理SqlSession则是用的JDK的动态代理。
```java
public class SqlSessionManager implements SqlSessionFactory, SqlSession {

    private final SqlSessionFactory sqlSessionFactory;
    private final SqlSession sqlSessionProxy;
    
    private SqlSessionManager(SqlSessionFactory sqlSessionFactory) {
    this.sqlSessionFactory = sqlSessionFactory;
    this.sqlSessionProxy = (SqlSession) Proxy.newProxyInstance(
        SqlSessionFactory.class.getClassLoader(),
        new Class[]{SqlSession.class},
        new SqlSessionInterceptor());
    }
    
    // ------------------
    // other code ignored
    // ------------------
}
```
