package istat.android.data.access.sqlite;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
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
        assertTrue(leftStatement.contains("LEFT"));
        assertTrue(leftStatement.contains("JOIN"));
        assertTrue(leftStatement.contains("child_table"));
        assertTrue(leftStatement.contains("parent_table"));

        SQLiteSelect innerJoin = sql.select(ChildEntity.class);
        innerJoin.innerJoin(ParentEntity.class);
        String innerStatement = innerJoin.getStatement();
        assertTrue(innerStatement.contains("INNER"));
        assertTrue(innerStatement.contains("JOIN"));
        assertTrue(innerStatement.contains("child_table"));
        assertTrue(innerStatement.contains("parent_table"));
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
