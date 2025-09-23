package istat.android.data.access.sqlite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class SQLiteJoinProjectionTest {
    private SQLiteDatabase database;
    private SQLite.SQL sql;

    @Before
    public void setUp() {
        database = SQLiteDatabase.create(null);
        sql = SQLite.from(database);
        database.execSQL("CREATE TABLE bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT, folder_id INTEGER)");
        database.execSQL("CREATE TABLE folders (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");
        database.execSQL("INSERT INTO folders (name) VALUES ('Work')");
        database.execSQL("INSERT INTO bookmarks (title, folder_id) VALUES ('Doc', 1)");
    }

    @After
    public void tearDown() {
        database.close();
    }

    @SQLiteModel.Table(name = "bookmarks")
    public static class Bookmark {
        @SQLiteModel.PrimaryKey
        long id;
        String title;
        @SQLiteModel.Column(name = "folder_id")
        long folderId;
    }

    @SQLiteModel.Table(name = "folders")
    public static class Folder {
        @SQLiteModel.PrimaryKey
        long id;
        String name;
    }

    @Test
    public void customProjectionSurvivesJoinChain() {
        String[] projection = new String[]{
                "bookmarks.title AS bookmark_title",
                "folders.name AS folder_name"
        };

        SQLiteSelect.SQLiteJoinSelect joinSelect = sql.select(projection, Bookmark.class)
                .leftJoin(Folder.class)
                .on(Bookmark.class, "folder_id")
                .equalTo(Folder.class, "id");

        String statement = joinSelect.getStatement();
        assertTrue(statement.contains("bookmarks.title AS bookmark_title,folders.name AS folder_name"));

        Cursor cursor = joinSelect.getCursor();
        try {
            assertEquals(1, cursor.getCount());
            cursor.moveToFirst();
            assertEquals("Doc", cursor.getString(cursor.getColumnIndexOrThrow("bookmark_title")));
            assertEquals("Work", cursor.getString(cursor.getColumnIndexOrThrow("folder_name")));
            assertArrayEquals(new String[]{"bookmark_title", "folder_name"}, cursor.getColumnNames());
        } finally {
            cursor.close();
        }
    }

    @Test
    public void additionalJoinPredicateStaysInOnClause() {
        SQLiteSelect.SQLiteJoinSelect joinSelect = sql.select(new String[]{"bookmarks.id"}, Bookmark.class)
                .leftJoin(Folder.class)
                .on(Bookmark.class, "folder_id")
                .equalTo(Folder.class, "id")
                .and(Folder.class, "name")
                .equalTo("Work")
                .where1()
                .where(Bookmark.class, "title")
                .equalTo("Doc");

        String statement = joinSelect.getStatement();
        assertTrue(statement.contains("ON (bookmarks.folder_id=folders.id AND folders.name = ?"));

        int whereIndex = statement.indexOf(" WHERE ");
        assertTrue(whereIndex > 0);
        String whereSegment = statement.substring(whereIndex);
        assertFalse(whereSegment.contains("folders.name"));
    }
}
