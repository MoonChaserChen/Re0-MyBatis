# Mybatis中的DataSource
## DataSource
Mybatis自带了两种 `DataSource`，`PooledDataSource` 与 `UnpooledDataSource`，但是有趣的是 `PooledDataSource` 却是由 `UnpooledDataSource` 来实现的。

### PooledDataSource
### UnpooledDataSource
## DataSourceFactory
```
<dataSource type="POOLED">
  <property name="driver" value="${driver}"/>
  <property name="url" value="${url}"/>
  <property name="username" value="${username}"/>
  <property name="password" value="${password}"/>
</dataSource>
```

type: UNPOOLED, POOLED, JNDI
## 使用第三方数据源：
```
public class C3P0DataSourceFactory extends UnpooledDataSourceFactory {

  public C3P0DataSourceFactory() {
    this.dataSource = new ComboPooledDataSource();
  }
}
```

```
<dataSource type="org.myproject.C3P0DataSourceFactory">
  <property name="driver" value="org.postgresql.Driver"/>
  <property name="url" value="jdbc:postgresql:mydb"/>
  <property name="username" value="postgres"/>
  <property name="password" value="root"/>
</dataSource>
```
