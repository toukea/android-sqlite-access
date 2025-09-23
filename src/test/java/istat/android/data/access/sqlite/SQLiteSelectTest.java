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

    @Test
    public void getStatementShouldIncludeDistinctWhenEnabled() {
        SQLite.SQL sql = new SQLite.SQL(null);
        SQLiteSelect select = sql.select(true, DummyEntity.class);

        String statement = select.getStatement();

        assertTrue(statement.startsWith("SELECT DISTINCT "));
        assertTrue(select.toString().startsWith("SELECT DISTINCT "));
    }
}
