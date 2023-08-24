package ink.akira.mybatis;

import ink.akira.mybatis.domain.Pet;
import ink.akira.mybatis.domain.Student;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.scripting.xmltags.StaticTextSqlNode;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;import java.sql.Connection;import java.sql.SQLException;

public class QuickStartTest {
    private Configuration configuration;
    private String insertSqlId = "ink.akira.mybatis.pet.insert";

    @Before
    public void before() {
        initEnvironment();
        addSql();
    }

    public void initEnvironment() {
        // 数据源
        String driver = "com.mysql.cj.jdbc.Driver";
        String url = "jdbc:mysql://localhost:3306/akira?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true";
        String username = "root";
        String password = "root@Mysql8.0";
        UnpooledDataSource dataSource = new UnpooledDataSource(driver, url, username, password);
        // 事务管理
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment.Builder("test-env")
                .transactionFactory(transactionFactory)
                .dataSource(dataSource)
                .build();
        configuration = new Configuration(environment);
    }

    public void addSql(){
        String insertSql = "insert into pet value (#{id}, #{petName}, #{age})";
        StaticTextSqlNode sqlNode = new StaticTextSqlNode(insertSql);
        RawSqlSource sqlSource = new RawSqlSource(configuration, sqlNode, Student.class);
        MappedStatement ms = new MappedStatement.Builder(configuration, insertSqlId, sqlSource, SqlCommandType.INSERT).build();
        configuration.addMappedStatement(ms);
    }

    @Test
    public void test() {
        SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        try (SqlSession sqlSession = sessionFactory.openSession(true)) {
            int result = sqlSession.insert(insertSqlId, new Pet(1L, "Tome", 18));
            Assert.assertEquals(1, result);
        }
    }

    @Test
    public void test1() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("mybatis-conf.xml");
        SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(is);
        try (SqlSession sqlSession = sessionFactory.openSession(true)) {
            int result = sqlSession.insert("ink.akira.mybatis.pet.insert", new Pet(1L, "Tome", 18));
            Assert.assertEquals(1, result);
        }
    }

    @Test
    public void test2() throws SQLException {
        Connection connection = configuration.getEnvironment().getDataSource().getConnection();
    }
}
