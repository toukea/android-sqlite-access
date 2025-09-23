package istat.android.data.access.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class SQLiteInsertSelectionTest {
    private SQLiteDatabase database;
    private SQLite.SQL sql;

    @Before
    public void setUp() {
        database = SQLiteDatabase.create(null);
        sql = SQLite.from(database);
        database.execSQL("CREATE TABLE source_items (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");
        database.execSQL("CREATE TABLE target_items (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, uuid TEXT)");
        database.execSQL("INSERT INTO source_items (name) VALUES ('alpha'), ('beta')");
    }

    @After
    public void tearDown() {
        database.close();
    }

    @SQLiteModel.Table(name = "source_items")
    public static class SourceItem {
        @SQLiteModel.PrimaryKey
        long id;
        String name;
    }

    @SQLiteModel.Table(name = "target_items")
    public static class TargetItem {
        @SQLiteModel.PrimaryKey
        long id;
        String name;
        String uuid;
    }

    @Test
    public void insertSelectionCopiesRowWithGeneratedUuid() {
        SQLiteSelect selection = sql.select(new String[]{"name"}, SourceItem.class)
                .where("name")
                .equalTo("alpha");

        SQLiteInsertSelection insertSelection = sql.insertSelection(TargetItem.class, selection)
                .setExpression("uuid", "hex(randomblob(16))");

        int inserted = insertSelection.execute();
        assertEquals(1, inserted);

        Cursor cursor = database.rawQuery("SELECT name, uuid FROM target_items", null);
        assertEquals(1, cursor.getCount());
        cursor.moveToFirst();
        assertEquals("alpha", cursor.getString(0));
        String uuid = cursor.getString(1);
        assertNotNull(uuid);
        assertEquals(32, uuid.length());
        cursor.close();
    }
}
