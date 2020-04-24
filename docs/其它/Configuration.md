# Configuration
mybatis可粗略地分为以下几个部分：
1. mybatis的基础配置信息，如mybatis-conf.xml
2. mybatis所有sql的xml
3. mybatis将Interface与sql绑定

而Configuration为mybatis中的一个非常重量级的类，几乎包含了所有与mybatis相关的东西（包括上面的），同时很多类也引用了Configuration以获取到几乎所有的信息。
## 从Environment说起
Mybatis作为持久层，直接和数据库打交道，在Mybatis中与这部分关联的类为： `org.apache.ibatis.mapping.Environment`，这个类包含了数据源 `DataSource` 及事务管理 `TransactionFactory`
```
private final String id;
private final TransactionFactory transactionFactory;
private final DataSource dataSource;
```
`Environment` 配置于 mybatis-conf.xml

### id
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

### TransactionFactory
事务管理相关， 详见[transactionManager与TransactionFactory](/其它/transactionManager与TransactionFactory.md)

### DataSource
由于java连接数据库已形成规范，即JDBC，因此Mybatis还需要引入JDBC以执行数据库操作，因此这里需要 `DataSource` 对象。

Mybatis自带了两种 `DataSource`，`PooledDataSource` 与 `UnpooledDataSource`，但是有趣的是 `PooledDataSource` 却是由 `UnpooledDataSource` 来实现的。

详见[Mybatis中的DataSource](/其它/Mybatis中的DataSource.md)

### set与build
Environment类对以上属性并未提供set方法，只能在构造器中全部传入，但是却提供了Builder类及build方法，看起来更加优雅。
```
new Environment.Builder("test-env")
    .transactionFactory(getTransactionFactory())
    .dataSource(getDataSource())
    .build();
```
这种XxxBuilder在Mybatis中很常见。

### 参考
[官方文档](https://mybatis.org/mybatis-3/zh/configuration.html#environments)

## 创建简单的Configuration
```
public void createConfiguration() {
    // 数据源
    String driver = "com.mysql.cj.jdbc.Driver";
    String url = "jdbc:mysql://localhost:3306/re0_mybatis?serverTimezone=GMT%2B8";
    String username = "root";
    String password = "root";
    UnpooledDataSource dataSource = new UnpooledDataSource(driver, url, username, password);
    // 事务管理
    TransactionFactory transactionFactory = new JdbcTransactionFactory();
    // 运行环境
    Environment environment = new Environment.Builder("test-env")
            .transactionFactory(transactionFactory)
            .dataSource(dataSource)
            .build();
    // 配置
    configuration = new Configuration(environment);
}
```