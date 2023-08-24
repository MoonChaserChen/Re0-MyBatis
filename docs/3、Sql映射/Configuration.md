# Configuration
mybatis可粗略地分为以下几个部分：
1. mybatis的基础配置信息，如mybatis-conf.xml
2. mybatis所有sql的xml
3. mybatis将Interface与sql绑定（感觉这个其实是mybatis-spring做的）

而Configuration为mybatis中的一个非常重量级的类，几乎包含了所有与mybatis相关的东西（包括上面的）。同时很多类也引用了Configuration以获取到几乎所有的信息，比如：
1. DefaultSqlSessionFactory
2. DefaultSqlSession
3. BaseExecutor
4. MappedStatement
5. ResultMap
6. ...

* MapperRegistry
* InterceptorChain
* TypeHandlerRegistry
* TypeAliasRegistry
* LanguageDriverRegistry
* Map<String, MappedStatement> mappedStatements
* Map<String, Cache> caches
* Map<String, ResultMap>
* Map<String, ParameterMap>
* Map<String, KeyGenerator> keyGenerators
* ExecutorType
* ResultSetType

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
