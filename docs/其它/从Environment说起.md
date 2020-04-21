# 从Environment说起
Mybatis作为持久层，直接和数据库打交道，在Mybatis中与这部分关联的类为： `org.apache.ibatis.mapping.Environment`，这个类包含了数据源 `DataSource` 及事务管理 `TransactionFactory`
```
private final String id;
private final TransactionFactory transactionFactory;
private final DataSource dataSource;
```

## id
可配置多个环境，id用于区分。例如：
```
<environments default="development">
  <environment id="development">
    <transactionManager type="JDBC">
      <property name="..." value="..."/>
    </transactionManager>
    <dataSource type="POOLED">
      <property name="driver" value="${driver}"/>
      <property name="url" value="${url}"/>
      <property name="username" value="${username}"/>
      <property name="password" value="${password}"/>
    </dataSource>
  </environment>
</environments>
```
但是这里并没有使用 `EnvironmentFactory` 来对多个环境进行管理，因此虽然可以配置多个Environment，但是实际上只会用到一个，并不能切换。

## TransactionFactory
事务管理相关， 详见[transactionManager与TransactionFactory](/其它/transactionManager与TransactionFactory.md)

## DataSource
由于java连接数据库已形成规范，即JDBC，因此Mybatis还需要引入JDBC以执行数据库操作，因此这里需要 `DataSource` 对象。

Mybatis自带了两种 `DataSource`，`PooledDataSource` 与 `UnpooledDataSource`，但是有趣的是 `PooledDataSource` 却是由 `UnpooledDataSource` 来实现的。

详见[Mybatis中的DataSource](/其它/Mybatis中的DataSource.md)

## set与build
Environment类对以上属性并未提供set方法，只能在构造器中全部传入，但是却提供了build方法，看起来更加优雅。
```
new Environment.Builder("test-env")
    .transactionFactory(getTransactionFactory())
    .dataSource(getDataSource())
    .build();
```

## 参考
[官方文档](https://mybatis.org/mybatis-3/zh/configuration.html#environments)