package istat.android.data.access.sqlite;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SQLiteSelectTest {

    @SQLiteModel.Table(name = "dummy_table")
    private static class DummyEntity {
        @SQLiteModel.PrimaryKey
        long id;
        String name;
    }

    @SQLiteModel.Table(name = "parent_table")
    private static class ParentEntity {
        @SQLiteModel.PrimaryKey
        long id;
    }

    @SQLiteModel.Table(name = "child_table")
    private static class ChildEntity {
        @SQLiteModel.PrimaryKey
        long id;

        @SQLiteModel.ManyToOne(mappedBy = "parent_id")
        ParentEntity parent;
    }

    @Test
    public void getStatementShouldIncludeDistinctWhenEnabled() {
        SQLite.SQL sql = new SQLite.SQL(null);
        SQLiteSelect select = sql.select(true, DummyEntity.class);

        String statement = select.getStatement();

        assertTrue(statement.startsWith("SELECT DISTINCT "));
        assertTrue(select.toString().startsWith("SELECT DISTINCT "));
    }

    @Test
    public void autoJoinShouldUseColumnsFromBothTables() {
        SQLite.SQL sql = new SQLite.SQL(null);

        SQLiteSelect leftJoin = sql.select(ChildEntity.class);
        leftJoin.leftJoin(ParentEntity.class);
        String leftStatement = leftJoin.getStatement();
        assertTrue(leftStatement.contains("ON (child_table.parent_id=parent_table.id)"));

        SQLiteSelect innerJoin = sql.select(ChildEntity.class);
        innerJoin.innerJoin(ParentEntity.class);
        String innerStatement = innerJoin.getStatement();
        assertTrue(innerStatement.contains("ON (child_table.parent_id=parent_table.id)"));
    }

    @Test
    public void joinWhereNotEqualToShouldUseInequalityOperator() {
        SQLite.SQL sql = new SQLite.SQL(null);

        SQLiteSelect select = sql.select(ChildEntity.class);
        select.innerJoin(ParentEntity.class)
                .where(ParentEntity.class, "id")
                .notEqualTo(5);

        String statement = select.getStatement();

        assertTrue(statement.contains("parent_table.id != 5"));
        assertTrue("Join clause should remain intact", statement.contains("ON (child_table.parent_id=parent_table.id)"));
    }

    @Test
    public void whereEqualToShouldQuoteStringValuesInStatements() {
        SQLite.SQL sql = new SQLite.SQL(null);
        SQLiteSelect select = sql.select(DummyEntity.class);

        select.where("name").equalTo("Bob");

        String statement = select.getStatement();

        assertTrue("Statement should contain the quoted literal", statement.contains("WHERE dummy_table.name = 'Bob'"));
    }

    @Test
    public void getStatementShouldUseProjectionWithAliases() {
        SQLite.SQL sql = new SQLite.SQL(null);
        String[] projection = new String[]{"COUNT(dummy_table.id) AS size", "dummy_table.name"};
        SQLiteSelect select = sql.select(projection, DummyEntity.class);

        String statement = select.getStatement();

        assertTrue("Projection should include the COUNT alias", statement.startsWith(
                "SELECT COUNT(dummy_table.id) AS size,dummy_table.name FROM dummy_table"));
    }
}
