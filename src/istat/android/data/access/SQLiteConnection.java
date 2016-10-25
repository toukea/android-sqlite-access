package istat.android.data.access;

import istat.android.data.access.interfaces.Queryable;
import istat.android.data.access.util.SQLiteParser;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/*
 * Copyright (C) 2014 Istat Dev.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 * @author Toukea Tatsi (Istat)
 * 
 */
public abstract class SQLiteConnection implements Closeable {

	/*
	 * protected static final int BASE_VERSION = 1; protected static final
	 * String BASE_NOM = "istatLib.db";
	 */
	// L�instance de la base qui sera manipul�e au travers de cette classe.
	protected SQLiteDatabase db;
	private DbOpenHelper dbOpenHelper;
	public Context context;
	protected static String SHARED_PREF_FILE = "db_file",
			DB_CREATION_TIME = "creation_time",
			DB_UPDATE_TIME = "creation_time";

	protected SQLiteConnection(Context ctx, String dbName, int dbVersion) {
		dbOpenHelper = new DbOpenHelper(ctx, dbName, null, dbVersion);
		context = ctx;
	}

	public Context getContext() {
		return context;
	}

	/**
	 * Ouvre la base de donn�es en �criture.
	 */
	public SQLiteDatabase open() {
		db = dbOpenHelper.getWritableDatabase();
		Log.i("openhelper", "BDD open");
		return db;
	}

	/**
	 * Ferme la base de donn�es.
	 */
	public void close() {
		if (db != null)
			if (db.isOpen()) {
				try {
					db.close();
					Log.i("openhelper", "BDD close");
				} catch (Exception e) {
					Log.i("openhelper",
							"BDD can't be close because it is not already Open");
				}

			} else
				Log.i("openhelper",
						"BDD can't be close because it is not already Open");

	}

	public void beginTransaction() {
		db.beginTransaction();
	}

	public void endTransaction() {
		db.endTransaction();
	}

	public void setTransactionSuccessful() {
		db.setTransactionSuccessful();
	}

	public void commit() {
		setTransactionSuccessful();
		endTransaction();
	}

	public int delete(Queryable entity) {
		return entity.delete(db);
	}

	public <T extends DOEntity> List<T> findAll(Class<T> clazz) {
		return SELECT(clazz).execute(getDataBase());
	}

	public <T extends DOEntity> List<T> find(Class<T> clazz,
			HashMap<String, String> filter) {
		return SELECT(clazz).WHERE(filter).execute(getDataBase());
	}

	public <T extends DOEntity> T findById(Class<T> clazz, String id) {
		T instance = null;
		try {
			instance = clazz.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(
					"not default constructor found for class::" + clazz);
		}
		List<T> list = SELECT(clazz).where(instance.getPrimaryKey()).equal(id)
				.execute(getDataBase());
		return list != null && list.size() > 0 ? list.get(0) : null;
	}

	public long persist(Queryable entity) {
		return entity.persist(db);
	}

	public void persists(List<Queryable> entitys) {
		for (Queryable entity : entitys) {
			entity.persist(db);
		}
	}

	public void update(Queryable entity) {
		entity.update(db);
	}

	public void inserts(List<Queryable> entitys) {
		for (Queryable entity : entitys) {
			entity.insert(db);
		}
	}

	public void insert(Queryable qAble) {
		qAble.insert(db);
	}

	public int truncateTable(String table) {
		return db.delete(table, null, null);
	}

	public <T extends DOEntity> int truncateTable(Class<T> clazz) {
		T instance = null;
		String table = "";
		try {
			instance = clazz.newInstance();
			table = instance.getEntityName();
		} catch (Exception e) {
			throw new RuntimeException(
					"not default constructor found for class::" + clazz);
		}
		return db.delete(table, null, null);
	}

	public Cursor select(DbSelection clause) {
		return (Cursor) clause.onExecute(db);
	}

	/**
	 * get the current writable Db
	 */
	public SQLiteDatabase getDataBase() {
		return db;
	}

	public DbOpenHelper getDbOpenHelper() {
		return dbOpenHelper;
	}

	public boolean doesTableExist(SQLiteDatabase db, String tableName) {
		Cursor cursor = db.rawQuery(
				"select DISTINCT tbl_name from sqlite_master where tbl_name = '"
						+ tableName + "'", null);

		if (cursor != null) {
			if (cursor.getCount() > 0) {
				cursor.close();
				return true;
			}
			cursor.close();
		}
		return false;
	}

	public String getDbUpdateTime() {
		return context.getSharedPreferences(SHARED_PREF_FILE, 0).getString(
				DB_UPDATE_TIME, simpleDateTime());
	}

	public String getDbCreationTime() {
		return context.getSharedPreferences(SHARED_PREF_FILE, 0).getString(
				DB_CREATION_TIME, simpleDateTime());
	}

	@SuppressWarnings("deprecation")
	public Date getDbUpdateTimeAsDate() {
		return new Date(context.getSharedPreferences(SHARED_PREF_FILE, 0)
				.getString(DB_UPDATE_TIME, simpleDateTime()));
	}

	@SuppressWarnings("deprecation")
	public Date getDbCreationTimeAsDate() {
		return new Date(context.getSharedPreferences(SHARED_PREF_FILE, 0)
				.getString(DB_CREATION_TIME, simpleDateTime()));
	}

	protected boolean isTableExist(String table) {
		return isTableExist(db, table);
	}

	// -----------------------------------------------------------------------------------------------------------
	protected abstract void onDbUpgrade(SQLiteDatabase db, int oldVersion,
			int newVersion);

	protected abstract void onDbCreate(SQLiteDatabase db);

	protected boolean executeRawRessource(SQLiteDatabase db, int resid) {
		try {
			Resources res = getContext().getResources();
			InputStream resStream = res.openRawResource(resid);
			executeDbScript(db, resStream);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	protected void executeRawRessources(SQLiteDatabase db, int... resids) {
		for (int index : resids) {
			executeRawRessource(db, index);
		}
	}

	protected void executeStatements(SQLiteDatabase db, int[] statements) {
		for (int ask : statements)
			db.execSQL(context.getResources().getString(ask));
	}

	protected static void executeStatements(SQLiteDatabase db,
			List<String> statements) {
		for (String ask : statements)
			db.execSQL(ask);
	}

	protected static void executeStatements(SQLiteDatabase db,
			String[] statements) {
		for (String ask : statements)
			db.execSQL(ask);
	}

	// ----------------------------------------------------------------------------
	public class DbOpenHelper extends SQLiteOpenHelper {
		Context context;

		public DbOpenHelper(Context context, String nom,
				CursorFactory cursorfactory, int version) {
			super(context, nom, cursorfactory, version);
			this.context = context;
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			onDbCreate(db);
			registerDbCreationTime();
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onDbUpgrade(db, oldVersion, newVersion);
			registerDbUpdateTime();
		}

	}

	private void registerDbCreationTime() {
		SharedPreferences p = context.getSharedPreferences(SHARED_PREF_FILE, 0);
		p.edit().putString(DB_CREATION_TIME, simpleDateTime());
	}

	private void registerDbUpdateTime() {
		SharedPreferences p = context.getSharedPreferences(SHARED_PREF_FILE, 0);
		p.edit().putString(DB_UPDATE_TIME, simpleDateTime());
	}

	// ----------------------------------------------------------------------------
	public static void executeDbScript(SQLiteDatabase db,
			InputStream sqlFileInputStream) throws IOException {
		List<String> statements = SQLiteParser.parseSqlFile(sqlFileInputStream);
		for (String statement : statements)
			db.execSQL(statement);
	}

	public static boolean isTableExist(SQLiteDatabase db, String table) {
		try {
			db.query(table, null, null, null, null, null, null);
			return true;
		} catch (Exception e) {

		}
		return false;
	}

	@SuppressLint("SimpleDateFormat")
	public static String simpleDateTime() {

		Date date = new Date();
		SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		return f.format(date);
	}

	public static DbSelection SELECT(Class<? extends Queryable> clazz) {
		return new DbSelection(clazz);
	}

	public static DbUpdate UPDATE(Class<? extends Queryable> clazz) {
		return new DbUpdate(clazz);
	}

	public static DbDelete DELETE(Class<? extends Queryable> clazz) {
		return new DbDelete(clazz);
	}

	public static DbInsert INSERT() {
		return new DbInsert();
	}

	public static long insert(ABSDOEntity entity, SQLiteDatabase db) {
		return entity.insert(db);
	}

}
