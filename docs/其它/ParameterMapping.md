# ParameterMapping
个人理解为入参及返回值的类型映射，包含以下属性：
```
private Configuration configuration;

private String property;
private ParameterMode mode;
private Class<?> javaType = Object.class;
private JdbcType jdbcType;
private Integer numericScale;
private TypeHandler<?> typeHandler;
private String resultMapId;
private String jdbcTypeName;
private String expression;
```

## 测试生成
首先，生成一个非常简单的configuration，详见：[创建简单的Configuration](https://blog.mybatis.akira.ink/其它/Configuration.md#创建简单的Configuration)

测试生成：
```
String insertSql = "insert into student value (#{id}, #{age}, #{userName}, #{birth})";
StaticTextSqlNode sqlNode = new StaticTextSqlNode(insertSql);
RawSqlSource sqlSource = new RawSqlSource(configuration, sqlNode, Student.class);
```
虽说ParameterMapping存在于 `BoundSql`中，但是由于 `SqlSource` 的其中一个实现 `StaticSqlSource` 与 `BoundSql` 非常类似。

而上面生成的 `RawSqlSource` 本质上是 `StaticSqlSource`，因此在上面的sqlSource就能看到parameterMapping到底是什么样子：
> 参见[SqlSource与BoundSql](https://blog.mybatis.akira.ink/其它/SqlSource与BoundSql.md#SqlSource与BoundSql)

![ParameterMapping.png](https://blog.mybatis.akira.ink/images/ParameterMapping.png)

| property | mode | javaType | jdbcType | numericScale | resultMapId | jdbcTypeName | expression |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| id | IN | long | null | null | null | null | null |
| age | IN | int | null | null | null | null | null |
| userName | IN | java.lang.String | null | null | null | null | null |
| birth | IN | java.util.Date | null | null | null | null | null |

可以看出/猜测：
### property
property 即是sql中#{}之间的参数，
### mode
mode即为映射为入参还是返回值：IN、OUT、INOUT（当使用useGeneratedKey后，这里是不是就变成INOUT了呢？）
### javaType
javaType则是从 `Student.class` 找到的同名属性的类型： `SqlSourceBuilder#parse`
```
public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
    ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
    GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
    String sql = parser.parse(originalSql);
    return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
}
```
以及：`org.apache.ibatis.builder.SqlSourceBuilder.ParameterMappingTokenHandler#handleToken(String)`
```
@Override
public String handleToken(String content) {
    parameterMappings.add(buildParameterMapping(content));
    return "?";
}
```
> 可以看出这里每处理一个token（#{}），则添加一个parameterMapping并返回?