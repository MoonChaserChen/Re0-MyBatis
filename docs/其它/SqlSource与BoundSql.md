# SqlSource与BoundSql

## SqlSource
根据参数创建可提交给数据库的sql
```
Represents the content of a mapped statement read from an XML file or an annotation. 
It creates the SQL that will be passed to the database out of the input parameter received from the user.
```

### 提供方法
```
BoundSql getBoundSql(Object parameterObject);
```

### 实现
```
ProviderSqlSource
StaticSqlSource
DynamicSqlSource
RawSqlSource
```

### XMLScriptBuilder
org.apache.ibatis.scripting.xmltags.XMLScriptBuilder#parseScriptNode()
```
public SqlSource parseScriptNode() {
    MixedSqlNode rootSqlNode = parseDynamicTags(context);
    SqlSource sqlSource;
    if (isDynamic) {
        sqlSource = new DynamicSqlSource(configuration, rootSqlNode);
    } else {
        sqlSource = new RawSqlSource(configuration, rootSqlNode, parameterType);
    }
    return sqlSource;
}
```
在解析xml生成sqlSource时，若xml中sql包含动态标签(if、choose (when, otherwise)、trim (where, set)、foreach)，
则生成 `DynamicSqlSource`， 否则生成 `RawSqlSource`

## BoundSql
```
An actual SQL String got from an {@link SqlSource} after having processed any dynamic content.
The SQL may have SQL placeholders "?" and an list (ordered) of an parameter mappings with the additional information
for each parameter (at least the property name of the input object to read the value from).

Can also have additional parameters that are created by the dynamic language (for loops, bind...).
```


| 参数 | 说明 |
| ---- | ---- |
| private final String sql; | 带?占位符的sql，与 `PreparedStatement` 类似 |
| private final List<ParameterMapping> parameterMappings;   | 参数 |
| private final Object parameterObject; | 参数 |
| private final Map<String, Object> additionalParameters;   | 额外参数，比如在for、bind标签中产生的 |
| private final MetaObject metaParameters;  | 这个是什么参数？ |

## SqlSourceBuilder
### 从#{}到?
在xml中写sql参数标记为#{}，而转换到BoundSql中则为?，是在什么地方进行转化的呢？
在 `org.apache.ibatis.builder.SqlSourceBuilder#parse` 方法
```
public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
    ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
    GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
    String sql = parser.parse(originalSql);
    return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
}
```

### SqlSourceBuilder与SqlSource
在[XMLScriptBuilder](#XMLScriptBuilder)可知解析xml实际上选择了 `DynamicSqlSource` 或 `RawSqlSource`，
但是进一步看这两个类就会发现这两个类实际上还是用到了 `SqlSourceBuilder` 进而生成了 `StaticSqlSource`。

**但是难道是因为这两个类是不同的人写的，所以风格是不同的？**

```java
public class RawSqlSource implements SqlSource {

  private final SqlSource sqlSource;

  public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType) {
    this(configuration, getSql(configuration, rootSqlNode), parameterType);
  }

  public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType) {
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    Class<?> clazz = parameterType == null ? Object.class : parameterType;
    sqlSource = sqlSourceParser.parse(sql, clazz, new HashMap<>());
  }

  private static String getSql(Configuration configuration, SqlNode rootSqlNode) {
    DynamicContext context = new DynamicContext(configuration, null);
    rootSqlNode.apply(context);
    return context.getSql();
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    return sqlSource.getBoundSql(parameterObject);
  }

}
```

```java
public class DynamicSqlSource implements SqlSource {

  private final Configuration configuration;
  private final SqlNode rootSqlNode;

  public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
    this.configuration = configuration;
    this.rootSqlNode = rootSqlNode;
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    DynamicContext context = new DynamicContext(configuration, parameterObject);
    rootSqlNode.apply(context);
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
    SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
    context.getBindings().forEach(boundSql::setAdditionalParameter);
    return boundSql;
  }

}
```

## SqlSource与BoundSql
上面提到 `DynamicSqlSource` 、 `RawSqlSource` 实际上用到的还是 `StaticSqlSource`，而 `SqlSource` 接口提供了 `BoundSql getBoundSql(Object parameterObject);` 方法。

那么 `StaticSqlSource` 与 `BoundSql` 更进一步的关系呢？ **只不过是多了个parameterObject而已！！**
```java
public class StaticSqlSource implements SqlSource {

  private final String sql;
  private final List<ParameterMapping> parameterMappings;
  private final Configuration configuration;

  public StaticSqlSource(Configuration configuration, String sql) {
    this(configuration, sql, null);
  }

  public StaticSqlSource(Configuration configuration, String sql, List<ParameterMapping> parameterMappings) {
    this.sql = sql;
    this.parameterMappings = parameterMappings;
    this.configuration = configuration;
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    return new BoundSql(configuration, sql, parameterMappings, parameterObject);
  }

}
```
所以感觉上： **`SqlSource` 类似于 `PreparedStatement` ， `BoundSql` 类似于 `PreparedStatement` + 参数**