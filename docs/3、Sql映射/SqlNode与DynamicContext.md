# SqlNode与DynamicContext
## SqlNode
其实现有：ChooseSqlNode, ForEachSqlNode, IfSqlNode, MixedSqlNode, SetSqlNode, StaticTextSqlNode, TextSqlNode, TrimSqlNode, VarDeclSqlNode, WhereSqlNode
### MixedSqlNode与组合模式
```
public class MixedSqlNode implements SqlNode {
    private final List<SqlNode> contents;

    public MixedSqlNode(List<SqlNode> contents) {
        this.contents = contents;
    }

    @Override
    public boolean apply(DynamicContext context) {
        contents.forEach(node -> node.apply(context));
        return true;
    }
}
```
