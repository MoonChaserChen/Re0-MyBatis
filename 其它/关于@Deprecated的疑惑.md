# 关于@Deprecated的疑惑
mybatis有一个比较顶级的`RuntimeException`： `org.apache.ibatis.exceptions.IbatisException`
```java
@Deprecated
public class IbatisException extends RuntimeException {
    // ...
}
```
虽然这个类被标记为 `@Deprecated`，但其子类却依然在正常使用。
```java
@SuppressWarnings("deprecation")
public class PersistenceException extends IbatisException {
    // ...
}
```
而这个 `PersistenceException` 下面有更多的 `Exception`。

![IbatisException](https://blog.mybatis.akira.ink/images/IbatisException.png)

难道是因为`IbatisException`名字太大了，只用于`RuntimeException`不合适？