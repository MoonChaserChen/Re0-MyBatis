# transactionManager与TransactionFactory
xml中配置为：
```
<transactionManager type="MANAGED">
  <property name="closeConnection" value="false"/>
</transactionManager>
```
但是其实对应着 `org.apache.ibatis.transaction.TransactionFactory`，两个实现即为：`JdbcTransactionFactory`， `ManagedTransactionFactory` 在使用 `mybatis-spring` 时还会有 `SpringManagedTransactionFactory`

在 MyBatis 中有两种类型的事务管理器（也就是 type="[JDBC|MANAGED]"）：

1. JDBC – 这个配置直接使用了 JDBC 的提交和回滚设施，它依赖从数据源获得的连接来管理事务作用域。
2. MANAGED – 这个配置几乎没做什么。它从不提交或回滚一个连接，而是让容器来管理事务的整个生命周期（比如 JEE 应用服务器的上下文）。
默认情况下它会关闭连接。然而一些容器并不希望连接被关闭，因此需要将 closeConnection 属性设置为 false 来阻止默认的关闭行为
> 默认情况下为：MANAGED，参见： `org.apache.ibatis.session.defaults.DefaultSqlSessionFactory.getTransactionFactoryFromEnvironment`

```
private TransactionFactory getTransactionFactoryFromEnvironment(Environment environment) {
    if (environment == null || environment.getTransactionFactory() == null) {
        return new ManagedTransactionFactory();
    }
    return environment.getTransactionFactory();
}
```


如果你正在使用 Spring + MyBatis，则没有必要配置事务管理器，因为 Spring 模块会使用自带的管理器来覆盖前面的配置。
> 如何做到的？