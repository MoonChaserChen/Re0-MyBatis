package ink.akira.mybatis;

import ink.akira.mybatis.domain.Student;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.scripting.xmltags.StaticTextSqlNode;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.Date;

public class EnvironmentTest {
    private Configuration configuration;
    private String insertSqlId = "ink.akira.mybatis.student.insert";

    @Before
    public void before() {
        createConfiguration();
        addInsertSql();
    }

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

    public void addInsertSql(){
        String insertSql = "insert into student value (#{id}, #{age}, #{userName}, #{birth})";
        StaticTextSqlNode sqlNode = new StaticTextSqlNode(insertSql);
        RawSqlSource sqlSource = new RawSqlSource(configuration, sqlNode, Student.class);
        MappedStatement ms = new MappedStatement.Builder(configuration, insertSqlId, sqlSource, SqlCommandType.INSERT).build();
        configuration.addMappedStatement(ms);
    }


    @Test
    public void testInsert(){
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        try(SqlSession sqlSession = sqlSessionFactory.openSession(true)) {
            Student student = new Student(1004, 18, "akira", new Date());
            sqlSession.insert(insertSqlId, student);
        }
    }

    @Test
    public void init(){
        String resource = "mybatis-conf.xml";
        InputStream is = EnvironmentTest.class.getClassLoader().getResourceAsStream(resource);
        SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(is);
        SqlSession sqlSession = sessionFactory.openSession(true);
        MappedStatement ms = sqlSession.getConfiguration().getMappedStatement("hello.mapper.insert");
        Student student = new Student(1002, 18, "akira", new Date());
        BoundSql boundSql = ms.getBoundSql(student);
        SqlSource sqlSource = ms.getSqlSource();
        BoundSql boundSql1 = sqlSource.getBoundSql(student);
    }

    @Test
    public void tmp(){
        String insertSql = "insert into student value (#{id}, #{age}, #{userName}, #{birth})";
        StaticTextSqlNode sqlNode = new StaticTextSqlNode(insertSql);
        RawSqlSource sqlSource = new RawSqlSource(configuration, sqlNode, Student.class);
    }
}
