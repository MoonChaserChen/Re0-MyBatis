package ink.akira.mybatis;

import com.google.gson.*;
import ink.akira.mybatis.domain.Pet;
import lombok.SneakyThrows;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.InputStream;
import java.lang.reflect.Type;

public class SqlParseTest {
    Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).setExclusionStrategies(new TestExclStra()).create();

    static class ClassCodec implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

        @SneakyThrows
        @Override
        public Class<?> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            String clazz = jsonElement.getAsString();
            return Class.forName(clazz);
        }

        @Override
        public JsonElement serialize(Class<?> aClass, Type type, JsonSerializationContext jsonSerializationContext) {
            return new JsonPrimitive(aClass.getName());
        }
    }

    static class TestExclStra implements ExclusionStrategy {

        public boolean shouldSkipClass(Class<?> arg0) {
            return false;
        }

        public boolean shouldSkipField(FieldAttributes f) {
            return f.getName().equals("configuration");
        }

    }

    @Test
    public void test() {
        InputStream is = getClass().getClassLoader().getResourceAsStream("mybatis-conf.xml");
        SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(is);

        MappedStatement ms = sessionFactory.getConfiguration().getMappedStatement("ink.akira.mybatis.pet.insert4");
        ParameterMap parameterMap = ms.getParameterMap();
        System.out.println(gson.toJson(parameterMap));
    }
}
