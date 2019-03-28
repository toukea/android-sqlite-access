package istat.android.data.access.content;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import istat.android.data.access.content.utils.SQLiteParser;

//TODO give possibility to specify if some class should be forced to be considered as a Table.
public final class ContentSQL {

    private ContentSQL() {

    }

    public static SQL fromUri(Context context, Uri uri, boolean closeDataBaseOnExecute) throws Exception {
        SQL sql = ContentSQL.fromUri(context, uri);
        sql.setAutoClose(closeDataBaseOnExecute);
        return sql;
    }

    private static SQL fromUri(Context context, Uri uri) {
        SQL sql = new SQL(context, uri);
        return sql;
    }

    public static class Config {
        HashMap<Class<?>, ContentSQLModel.Serializer> serializers = new HashMap<Class<?>, ContentSQLModel.Serializer>();
        HashMap<Class<?>, ContentSQLModel.CursorReader> cursorReaders = new HashMap<Class<?>, ContentSQLModel.CursorReader>();
        HashMap<Class<?>, ContentSQLModel.ContentValueHandler> contentValueHandlers = new HashMap<Class<?>, ContentSQLModel.ContentValueHandler>();
        ArrayList<Class<?>> serializerAssignableTo = new ArrayList<Class<?>>();
        ArrayList<Class<?>> cursorReaderAssignableTo = new ArrayList<Class<?>>();
        ArrayList<Class<?>> contentValueHandlerAssignableTo = new ArrayList<Class<?>>();

        public Config putSerializer(Class<?> cLass, ContentSQLModel.Serializer serializer, boolean assignableTo) {
            this.serializers.put(cLass, serializer);
            if (assignableTo) {
                this.serializerAssignableTo.add(cLass);
            }
            return this;
        }

        public Config putCursorReader(Class<?> cLass, ContentSQLModel.CursorReader reader, boolean assignableTo) {
            this.cursorReaders.put(cLass, reader);
            if (assignableTo) {
                this.cursorReaderAssignableTo.add(cLass);
            }
            return this;
        }

        public Config putContentValueHandler(Class<?> cLass, ContentSQLModel.ContentValueHandler contentValueHandler, boolean assignableTo) {
            this.contentValueHandlers.put(cLass, contentValueHandler);
            if (assignableTo) {
                this.contentValueHandlerAssignableTo.add(cLass);
            }
            return this;
        }

    }

    public static class SQL {
        Uri dataUri;
        Context context;
        boolean autoClose = false;
        public final static Config GLOBAL_CONFIG = new Config() {
            {
                this.serializers.put(Object.class, ContentSQLModel.DEFAULT_SERIALIZER);
                this.cursorReaders.put(Object.class, ContentSQLModel.DEFAULT_CURSOR_READER);
                this.contentValueHandlers.put(Object.class, ContentSQLModel.DEFAULT_CONTAIN_VALUE_HANDLER);
            }
        };
        Config config = new Config() {
            {
                this.serializers.putAll(GLOBAL_CONFIG.serializers);
                this.cursorReaders.putAll(GLOBAL_CONFIG.cursorReaders);
                this.contentValueHandlers.putAll(GLOBAL_CONFIG.contentValueHandlers);
                this.contentValueHandlerAssignableTo.addAll(GLOBAL_CONFIG.contentValueHandlerAssignableTo);
                this.serializerAssignableTo.addAll(GLOBAL_CONFIG.serializerAssignableTo);
                this.cursorReaderAssignableTo.addAll(GLOBAL_CONFIG.cursorReaderAssignableTo);
            }
        };

        public Config getConfig() {
            return config;
        }

        //TODO manage isAssignableTo
        ContentSQLModel.Serializer getSerializer(Class cLass) {
            ContentSQLModel.Serializer out = config.serializers.get(cLass);
            if (out != null) {
                return out;
            }
            for (Class<?> c : config.serializerAssignableTo) {
                if (c.isAssignableFrom(cLass)) {
                    return getSerializer(c);
                }
            }
            return config.serializers.get(Object.class);

        }

        ContentSQLModel.CursorReader getCursorReader(Class cLass) {
            ContentSQLModel.CursorReader out = config.cursorReaders.get(cLass);
            if (out != null) {
                return out;
            }
            for (Class<?> c : config.cursorReaderAssignableTo) {
                if (c.isAssignableFrom(cLass)) {
                    return getCursorReader(c);
                }
            }
            return config.cursorReaders.get(Object.class);

        }

        ContentSQLModel.ContentValueHandler getContentValueHandler(Class cLass) {
            ContentSQLModel.ContentValueHandler out = config.contentValueHandlers.get(cLass);
            if (out != null) {
                return out;
            }
            for (Class<?> c : config.contentValueHandlerAssignableTo) {
                if (c.isAssignableFrom(cLass)) {
                    return getContentValueHandler(c);
                }
            }
            return config.contentValueHandlers.get(Object.class);

        }

        public SQL useConfig(Config config) {
            this.config = config;
            return this;
        }

        public SQL useSerializer(Class<?> cLass, ContentSQLModel.Serializer serializer) {
            return useSerializer(cLass, serializer, false);
        }

        public SQL useCursorReader(Class<?> cLass, ContentSQLModel.CursorReader reader) {
            return useCursorReader(cLass, reader, false);
        }

        public SQL useContentValueHandler(Class<?> cLass, ContentSQLModel.ContentValueHandler contentValueHandler) {
            return useContentValueHandler(cLass, contentValueHandler, false);
        }

        //-----------------------------------------------
        public SQL useSerializer(Class<?> cLass, ContentSQLModel.Serializer serializer, boolean assignableTo) {
            this.config.putSerializer(cLass, serializer, assignableTo);
            return this;
        }

        public SQL useCursorReader(Class<?> cLass, ContentSQLModel.CursorReader reader, boolean assignableTo) {
            this.config.putCursorReader(cLass, reader, assignableTo);
            return this;
        }

        public SQL useContentValueHandler(Class<?> cLass, ContentSQLModel.ContentValueHandler contentValueHandler, boolean assignableTo) {
            this.config.putContentValueHandler(cLass, contentValueHandler, assignableTo);

            return this;
        }

        public SQL useSerializer(ContentSQLModel.Serializer serializer) {
            return useSerializer(Object.class, serializer);
        }

        public SQL useCursorReader(ContentSQLModel.CursorReader reader) {
            return useCursorReader(Object.class, reader);
        }

        public SQL useContentValueHandler(ContentSQLModel.ContentValueHandler contentValueHandler) {
            return useContentValueHandler(Object.class, contentValueHandler);
        }

        public void setAutoClose(boolean autoClose) {
            this.autoClose = autoClose;
        }

        public boolean isAutoClose() {
            return this.autoClose;
        }

        SQL(Context context, Uri uri) {
            this.context = context;
            this.dataUri = uri;
        }

        public ContentSQLSelect select(Class<?> clazz) {
            return new ContentSQLSelect(this, clazz);
        }

        public ContentSQLSelect select(boolean distinct, Class<?> clazz) {
            ContentSQLSelect select = new ContentSQLSelect(this, clazz);
            return select.distinct(distinct);
        }

        public ContentSQLSelect select(boolean distinct, Class<?>... clazz) {
            ContentSQLSelect select = new ContentSQLSelect(this, clazz);
            return select.distinct(distinct);
        }

        public ContentSQLSelect select(Class<?>... clazz) {
            return new ContentSQLSelect(this, clazz);
        }

        //------------------------------------------
        public ContentSQLSelect select(String uniqueColumn, Class<?> clazz) {
            ContentSQLSelect select = new ContentSQLSelect(this, clazz);
            select.columns = new String[]{uniqueColumn};
            return select;
        }

        public ContentSQLSelect select(boolean distinct, String uniqueColumn, Class<?> clazz) {
            ContentSQLSelect select = new ContentSQLSelect(this, clazz);
            select.distinct(distinct);
            select.columns = new String[]{uniqueColumn};
            return select;
        }

        public ContentSQLSelect select(boolean distinct, String uniqueColumn, Class<?>... clazz) {
            ContentSQLSelect select = new ContentSQLSelect(this, clazz);
            select.distinct(distinct);
            select.columns = new String[]{uniqueColumn};
            return select;
        }

        public ContentSQLSelect select(String uniqueColumn, Class<?>... clazz) {
            ContentSQLSelect select = new ContentSQLSelect(this, clazz);
            select.columns = new String[]{uniqueColumn};
            return select;
        }

        //------------------------------------------
        public ContentSQLSelect select(String[] columns, Class<?> clazz) {
            ContentSQLSelect select = new ContentSQLSelect(this, clazz);
            select.columns = columns;
            return select;
        }

        public ContentSQLSelect select(boolean distinct, String[] columns, Class<?> clazz) {
            ContentSQLSelect select = new ContentSQLSelect(this, clazz);
            select.distinct(distinct);
            select.columns = columns;
            return select;
        }

        public ContentSQLSelect select(boolean distinct, String[] columns, Class<?>... clazz) {
            ContentSQLSelect select = new ContentSQLSelect(this, clazz);
            select.distinct(distinct);
            select.columns = columns;
            return select;
        }

        public ContentSQLSelect select(String[] columns, Class<?>... clazz) {
            ContentSQLSelect select = new ContentSQLSelect(this, clazz);
            select.columns = columns;
            return select;
        }

        //---------------------------------------
        public ContentSQLUpdate update(Class<?> clazz) {
            return new ContentSQLUpdate(clazz, this);
        }

        public ContentSQLDelete delete(Class<?> clazz) {
            return new ContentSQLDelete(clazz, this);
        }


        public int delete(Object... object) {
            int count = 0;
            for (Object obj : object) {
                if (delete(obj)) {
                    count++;
                }
            }
            return count;
        }

        public boolean delete(Object object) {
            try {
                Class<?> cLass = object.getClass();
                ContentSQLModel model = ContentSQLModel.fromClass(cLass);
                return delete(cLass).where(model.getPrimaryKeyName()).equalTo(model.getPrimaryKeyStringValue()).execute() > 0;
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        }

        public boolean deleteById(Class<?> cLass, Object id) {
            try {
                ContentSQLModel model = ContentSQLModel.fromClass(cLass);
                return delete(cLass)
                        .where(model.getPrimaryKeyName())
                        .equalTo(id)
                        .execute() > 0;
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return false;
        }

        public <T> T findById(Class<T> cLass, Object id) {
            try {
                ContentSQLModel model = ContentSQLModel.fromClass(cLass);
                if (!model.isPrimaryFieldDefined()) {
                    throw new RuntimeException("Oups, class:" + cLass + " mapped by table:" + model.getName() + " doesn't has primary field defined.");
                }
                return select(cLass)
                        .where(model.getPrimaryKeyName())
                        .equalTo(id)
                        .executeLimit1();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        }

        public <T> List<T> findAll(Class<T> classTable) {
            return findAll(classTable, false);
        }

        public <T> List<T> findAll(Class<T> classTable, boolean distinct) {
            return findAll(classTable, distinct, null, null, null, null, null, null);
        }

        public <T> List<T> findAll(Class<T> classTable, boolean distinct, String whereClause, String[] whereParams, String groupBy, String having, String orderBy, String limit) {
            ContentSQLSelect select = select(distinct, classTable);
            select.distinct = distinct;
            if (!TextUtils.isEmpty(whereClause)) {
                select.whereClause = new StringBuilder(whereClause);
                select.whereParams = Arrays.asList(whereParams);
            } else {
                select.whereParams = new ArrayList();
            }
            select.limit = limit;
            select.orderBy = orderBy;
            select.groupBy = groupBy;
            if (!TextUtils.isEmpty(having)) {
                select.having = new StringBuilder(having);
            }
            return select.execute();
        }

        public <T> List<T> findAll(Class<T> classTable, final String rawQuery, final String[] selectionArgs) {
            return findAll(classTable, rawQuery, selectionArgs, null);
        }

        public <T> List<T> findAll(Class<T> classTable, final String rawQuery, final String[] selectionArgs, final CancellationSignal signal) {
            ContentSQLSelect select = new ContentSQLSelect(this, classTable) {
                @Override
                protected Cursor onExecute(ContentResolver db) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        return db.rawQuery(rawQuery, selectionArgs, signal);
                    } else {
                        return db.rawQuery(rawQuery, selectionArgs);
                    }
                }
            };
            return select.execute();
        }

        public <T> int update(Class<T> classTable, ContentValues contentValues, String whereClause, String[] whereParams, String having, String limit) {
            ContentSQLUpdate.Updater update = update(classTable).updater;
            update.model.fillFromContentValues(contentValues);
            update.whereClause = new StringBuilder(whereClause);
            update.whereParams = whereParams != null ? Arrays.asList(whereParams) : new ArrayList<String>();
            update.limit = limit;
            update.having = new StringBuilder(having);
            return update.execute();
        }

        //------------------------------------------
        public ContentSQLInsert insert(Object entity) {
            ContentSQLInsert insert = new ContentSQLInsert(this);
            return insert.insert(entity);
        }

        public ContentSQLInsert insert(Object... entity) {
            ContentSQLInsert insert = new ContentSQLInsert(this);
            return insert.insert(entity);
        }

        public <T> ContentSQLInsert insert(boolean asClass, List<T> entity) {
            ContentSQLInsert insert = new ContentSQLInsert(this);
            return insert.insert(asClass, entity);
        }

        public <T> ContentSQLInsert insert(Collection<T> entity) {
            ContentSQLInsert insert = new ContentSQLInsert(this);
            return insert.insert(entity);
        }

        public ContentSQLPersist persist(Object entity) {
            ContentSQLPersist persist = new ContentSQLPersist(this);
            return persist.persist(entity);
        }

        public ContentSQLPersist persist(Object... entity) {
            ContentSQLPersist persist = new ContentSQLPersist(this);
            return persist.persist(entity);
        }

        public <T> ContentSQLPersist persist(List<T> entity) {
            ContentSQLPersist persist = new ContentSQLPersist(this);
            return persist.persist(entity);
        }

        public <T> void replaces(List<T> entity) {
            try {
                if (entity != null && !entity.isEmpty()) {
                    delete(entity.get(0).getClass()).execute();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            ContentSQLPersist insert = new ContentSQLPersist(this);
            insert.persist(entity).execute();
        }

        //---------------------------------------------
        public ContentSQLMerge merge(Object entity) {
            ContentSQLMerge merge = new ContentSQLMerge(this);
            return merge.merge(entity);
        }

        public ContentSQLMerge merge(Object... entity) {
            ContentSQLMerge merge = new ContentSQLMerge(this);
            return merge.merge(entity);
        }

        public <T> ContentSQLMerge merge(List<T> entity) {
            ContentSQLMerge merge = new ContentSQLMerge(this);
            return merge.merge(entity);
        }

        public Cursor query(String[] column, String selection, String[] selectionArgs, String orderBy, CancellationSignal cancellationSignal) {
            return this.getContentResolver().query(dataUri, column, selection, selectionArgs, orderBy, cancellationSignal);
        }

        public ContentResolver getContentResolver() {
            return context.getContentResolver();
        }
    }

    public static abstract class SQLiteConnection implements BootDescription {
        String dbName;
        int dbVersion = 1;
        Context context;

        public static SQLiteConnection create(Context context, File file) {
            return create(context, file, -1, null);
        }

        public static SQLiteConnection create(Context context, File file, int version, final BootDescription description) {
            if (version < 0) {
                SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(file, null);
                db.getVersion();
                db.close();
            }
            return create(context, file.getAbsolutePath(), version, description);
        }

        public static SQLiteConnection create(Context context, String dbName, int dbVersion, final BootDescription description) {
            SQLiteConnection connection = new SQLiteConnection(context, dbName, dbVersion) {
                @Override
                public void onCreateDb(SQLiteDatabase db) {
                    if (description != null) {
                        description.onCreateDb(db);
                    }
                }

                @Override
                public void onUpgradeDb(SQLiteDatabase db, int oldVersion, int newVersion) {
                    if (description != null) {
                        description.onUpgradeDb(db, oldVersion, newVersion);
                    }
                }

                @Override
                public void onConfigure(SQLiteDatabase db) {
                    if (description != null) {
                        description.onConfigure(db);
                    }
                }

                @Override
                public boolean onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                    if (description != null) {
                        return description.onDowngrade(db, oldVersion, newVersion);
                    }
                    return false;
                }

                @Override
                public void onOpen(SQLiteDatabase db) {
                    if (description != null) {
                        description.onOpen(db);
                    }
                }
            };
            return connection;
        }

        public SQLiteConnection(Context context, String dbName, int dbVersion) {
            this.dbName = dbName;
            this.dbVersion = dbVersion;
            this.context = context;
        }

        public final static void executeScripts(SQLiteDatabase db, List<String> scripts) {
            for (String script : scripts) {
                db.execSQL(script);
            }
        }

        @Override
        public void onConfigure(SQLiteDatabase db) {

        }

        @Override
        public boolean onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            return false;
        }

        @Override
        public void onOpen(SQLiteDatabase db) {

        }
    }

    public interface BootDescription {
        void onCreateDb(SQLiteDatabase db);

        void onUpgradeDb(SQLiteDatabase db, int oldVersion,
                         int newVersion);

        void onConfigure(SQLiteDatabase db);

        boolean onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion);

        void onOpen(SQLiteDatabase db);


    }

    public static void executeSQLScript(SQLiteDatabase db,
                                        InputStream sqlFileInputStream) throws IOException {
        List<String> statements = SQLiteParser.parseSqlStream(sqlFileInputStream);
        for (String statement : statements)
            db.execSQL(statement);
    }

    public static void executeStatements(SQLiteDatabase db, List<String> statements) {
        for (String ask : statements) {
            db.execSQL(ask);
        }
    }

    public static void executeStatements(SQLiteDatabase db, String... statements) {
        for (String ask : statements) {
            db.execSQL(ask);
        }
    }

    public interface PrepareHandler {
        void onSQLReady(SQL sql) throws Exception;

        void onSQLPrepareFail(Exception e);
    }

    public static abstract class SQLReadyHandler implements PrepareHandler {

        public void onSQLPrepareFail(Exception e) {
            e.printStackTrace();
        }
    }
}
