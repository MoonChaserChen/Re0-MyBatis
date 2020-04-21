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

## 从#{}到?
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

## SqlSourceBuilder
在[XMLScriptBuilder](#XMLScriptBuilder)可知解析xml实际上选择了 `DynamicSqlSource` 或 `RawSqlSource`，
但是进一步看这两个类就会发现这两个类实际上还是用到了 `SqlSourceBuilder` 进而生成了 `StaticSqlSource`。
