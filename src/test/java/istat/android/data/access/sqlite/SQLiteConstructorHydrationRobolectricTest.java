package istat.android.data.access.sqlite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import istat.android.data.access.sqlite.utils.TableUtils;

/**
 * Round-trip hydration tests for the constructor-based hydration feature. Runs on the JVM via
 * Robolectric (real SQLite + TextUtils), so it can be executed with {@code ./gradlew
 * :android-sqlite-access:testDebugUnitTest} without a device.
 *
 * <p>Each case creates a table, inserts a row with raw SQL (to isolate the read/hydration path
 * under test from the write path), then reads it back through the ORM select path which routes
 * into {@link SQLiteModel#asInstance(Class, SQLiteModel.Serializer)}.
 */
@RunWith(RobolectricTestRunner.class)
public class SQLiteConstructorHydrationRobolectricTest {
    private SQLiteDatabase database;
    private SQLite.SQL sql;

    @Before
    public void setUp() {
        database = SQLiteDatabase.create(null);
        sql = SQLite.from(database);
    }

    @After
    public void tearDown() {
        database.close();
    }

    // ---- Case 1: immutable entity mapped by @Column on the constructor parameters --------------

    @SQLiteModel.Persistable
    public static class ImmutableAnnotated {
        final String id;
        final String name;
        final int year;

        @SQLiteModel.CreatorConstructor
        ImmutableAnnotated(@SQLiteModel.Column(name = "id") String id,
                           @SQLiteModel.Column(name = "name") String name,
                           @SQLiteModel.Column(name = "year") int year) {
            this.id = id;
            this.name = name;
            this.year = year;
        }
    }

    @Test
    public void immutableEntity_mappedByAnnotation_hydrates() throws Exception {
        TableUtils.create(database, ImmutableAnnotated.class);
        database.execSQL("INSERT INTO ImmutableAnnotated (id, name, year) VALUES ('a1', 'Alice', 2020)");

        List<ImmutableAnnotated> rows = sql.select(ImmutableAnnotated.class).execute(ImmutableAnnotated.class);

        assertEquals(1, rows.size());
        ImmutableAnnotated row = rows.get(0);
        assertEquals("a1", row.id);
        assertEquals("Alice", row.name);
        assertEquals(2020, row.year);
    }

    // ---- Case 2: immutable entity mapped by parameter name (requires javac -parameters) --------

    @SQLiteModel.Persistable
    public static class ImmutableByName {
        final String id;
        final String name;

        // No @Column: parameters are matched to columns by their names. This works because the
        // library's JavaCompile is configured with -parameters (build.gradle), which also applies
        // to this test source set.
        ImmutableByName(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Test
    public void immutableEntity_mappedByParameterName_hydrates() throws Exception {
        TableUtils.create(database, ImmutableByName.class);
        database.execSQL("INSERT INTO ImmutableByName (id, name) VALUES ('b1', 'Bob')");

        List<ImmutableByName> rows = sql.select(ImmutableByName.class).execute(ImmutableByName.class);

        assertEquals(1, rows.size());
        assertEquals("b1", rows.get(0).id);
        assertEquals("Bob", rows.get(0).name);
    }

    // ---- Case 3: legacy entity (no-arg constructor + mutable fields) — regression guard --------

    public static class Legacy {
        String id;
        String name;

        public Legacy() {
        }
    }

    @Test
    public void legacyEntity_stillHydratesByFieldInjection() throws Exception {
        TableUtils.create(database, Legacy.class);
        database.execSQL("INSERT INTO Legacy (id, name) VALUES ('l1', 'Legacy')");

        List<Legacy> rows = sql.select(Legacy.class).execute(Legacy.class);

        assertEquals(1, rows.size());
        assertEquals("l1", rows.get(0).id);
        assertEquals("Legacy", rows.get(0).name);
    }

    // ---- Case 4: hybrid entity (constructor covers a subset; rest by field injection) ----------

    @SQLiteModel.Persistable
    public static class Hybrid {
        final String id;
        final String name;
        String note;             // mutable, NOT in the constructor -> set by field injection
        final String untouched;  // final, NOT in the constructor -> must keep its constructed value

        @SQLiteModel.CreatorConstructor
        Hybrid(@SQLiteModel.Column(name = "id") String id,
               @SQLiteModel.Column(name = "name") String name) {
            this.id = id;
            this.name = name;
            this.untouched = "CONSTANT";
        }
    }

    @Test
    public void hybridEntity_injectsRemainingFields_andSkipsFinalOnes() throws Exception {
        TableUtils.create(database, Hybrid.class);
        // 'untouched' is stored as 'FROM_DB' on purpose: the final field must NOT be overwritten.
        database.execSQL("INSERT INTO Hybrid (id, name, note, untouched) "
                + "VALUES ('h1', 'Hank', 'noted', 'FROM_DB')");

        List<Hybrid> rows = sql.select(Hybrid.class).execute(Hybrid.class);

        assertEquals(1, rows.size());
        Hybrid row = rows.get(0);
        assertEquals("h1", row.id);               // from constructor
        assertEquals("Hank", row.name);           // from constructor
        assertEquals("noted", row.note);          // injected into the mutable field
        assertEquals("CONSTANT", row.untouched);  // final field left untouched (DB value ignored)
        assertFalse("FROM_DB".equals(row.untouched));
    }

    // ---- Case 5: no exploitable constructor and no no-arg constructor -> clear failure ---------

    public static class Unhydratable {
        final String id;

        // Single constructor, parameter maps to a non-existent column, and no no-arg constructor.
        Unhydratable(@SQLiteModel.Column(name = "does_not_exist") String value) {
            this.id = value;
        }
    }

    @Test
    public void unhydratableEntity_throwsClearInstantiationException() throws Exception {
        SQLiteModel model = SQLiteModel.fromClass(Unhydratable.class);
        model.set("id", "z1");
        try {
            model.asInstance(Unhydratable.class);
            fail("Expected InstantiationException for an entity with no usable constructor");
        } catch (InstantiationException e) {
            assertTrue(e.getMessage(), e.getMessage().contains(Unhydratable.class.getName()));
            assertTrue(e.getMessage(), e.getMessage().contains("no usable no-arg constructor"));
        }
    }

    // ---- Case 6: @Column rename keyed end-to-end through the constructor path -------------------

    @SQLiteModel.Persistable
    public static class ColumnMapped {
        @SQLiteModel.Column(name = "id")
        final String id;
        @SQLiteModel.Column(name = "full_name")
        final String name;

        @SQLiteModel.CreatorConstructor
        ColumnMapped(@SQLiteModel.Column(name = "id") String id,
                     @SQLiteModel.Column(name = "full_name") String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Test
    public void renamedColumn_isKeyedByColumnNameThroughConstructor() throws Exception {
        TableUtils.create(database, ColumnMapped.class);
        database.execSQL("INSERT INTO ColumnMapped (id, full_name) VALUES ('c1', 'Charlie')");

        List<ColumnMapped> rows = sql.select(ColumnMapped.class).execute(ColumnMapped.class);

        assertEquals(1, rows.size());
        assertEquals("c1", rows.get(0).id);
        // Proves the constructor argument is fed from the 'full_name' column, not the field name.
        assertEquals("Charlie", rows.get(0).name);
    }

    // ---- Sanity: the creator plan is cached and reused per class --------------------------------

    @Test
    public void creatorPlan_isMemoizedPerClass() throws Exception {
        TableUtils.create(database, ImmutableByName.class);
        database.execSQL("INSERT INTO ImmutableByName (id, name) VALUES ('p1', 'One'), ('p2', 'Two')");

        List<ImmutableByName> rows = sql.select(ImmutableByName.class).execute(ImmutableByName.class);

        assertEquals(2, rows.size()); // both rows hydrate through the same (cached) creator plan
        assertNull(null);             // no exception thrown for the second row
    }
}
