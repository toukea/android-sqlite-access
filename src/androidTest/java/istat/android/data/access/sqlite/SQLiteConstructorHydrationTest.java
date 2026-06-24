package istat.android.data.access.sqlite;

import static org.junit.Assert.assertEquals;

import android.database.sqlite.SQLiteDatabase;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import istat.android.data.access.sqlite.utils.TableUtils;

/**
 * Instrumented smoke test for constructor-based hydration, honoring the module's existing
 * androidTest convention (in-memory {@link SQLiteDatabase}). It does a full persist + select
 * round-trip of an immutable entity. Requires a connected device/emulator. The exhaustive
 * coverage (legacy / hybrid / failure / renamed-column) lives in the Robolectric suite.
 */
@RunWith(AndroidJUnit4.class)
public class SQLiteConstructorHydrationTest {
    private SQLiteDatabase database;
    private SQLite.SQL sql;

    @Before
    public void setUp() throws Exception {
        database = SQLiteDatabase.create(null);
        sql = SQLite.from(database);
        TableUtils.create(database, ImmutableUser.class);
    }

    @After
    public void tearDown() {
        database.close();
    }

    @SQLiteModel.Persistable
    public static class ImmutableUser {
        final String id;
        final String name;
        final int year;

        @SQLiteModel.CreatorConstructor
        ImmutableUser(@SQLiteModel.Column(name = "id") String id,
                      @SQLiteModel.Column(name = "name") String name,
                      @SQLiteModel.Column(name = "year") int year) {
            this.id = id;
            this.name = name;
            this.year = year;
        }
    }

    @Test
    public void immutableEntity_persistsAndHydratesRoundTrip() {
        sql.persist(new ImmutableUser("u1", "Alice", 2020)).execute();

        List<ImmutableUser> rows = sql.select(ImmutableUser.class)
                .where("id").equalTo("u1")
                .execute(ImmutableUser.class);

        assertEquals(1, rows.size());
        ImmutableUser row = rows.get(0);
        assertEquals("u1", row.id);
        assertEquals("Alice", row.name);
        assertEquals(2020, row.year);
    }
}
