/*
 * encdroid - EncFS client application for Android
 * Copyright (C) 2012  Mark R. Pariente
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mrpdaemon.android.encdroidmc;

import java.util.ArrayList;
import java.util.List;

import org.mrpdaemon.android.encdroidmc.tools.KeyValueBean;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Base64;
import android.util.Log;
import fr.starn.FileSynchronizerRule;

public class EDDBHelper extends SQLiteOpenHelper {

	// Logger tag
	private final String TAG = "EDDBHelper";

	// Database name
	public static final String DB_NAME = "volume.db";

	// Database version
	public static final int DB_VERSION = 3;

	// Volume table name
	public static final String DB_TABLE = "volumes";

	// Column names
	public static final String DB_COL_ID = BaseColumns._ID;
	public static final String DB_COL_NAME = "name";
	public static final String DB_COL_PATH = "path";
	public static final String DB_COL_PROVIDER_TYPE = "provider_type";
	public static final String DB_COL_PROVIDER_PARAMS = "provider_params";
	public static final String DB_COL_KEY = "key";

	private static final String[] NO_ARGS = {};

	public EDDBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	
	public void onCreate(SQLiteDatabase db) {
		String sqlCmd = "CREATE TABLE " + DB_TABLE + " (" + DB_COL_ID
				+ " int primary key, " + DB_COL_NAME + " text, " + DB_COL_PATH
				+ " text, " + DB_COL_KEY + " text, " + DB_COL_PROVIDER_TYPE + " int, " + DB_COL_PROVIDER_PARAMS + " text)";
		Log.d(TAG, "onCreate() executing SQL: " + sqlCmd);
		db.execSQL(sqlCmd);
		
		createSyncRulesTable(db);
		
	}
	
	private void createSyncRulesTable(SQLiteDatabase db){
		//SQLiteDatabase db = getWritableDatabase();
		String sqlCmd2 = "CREATE TABLE SYNC_RULES ( id int primary key, volumeNameToSync text, volumePassword text, volumePathToSync text, deleteSrcFileAfterSync text, syncOnlyOnWifi text, localPathToSync text)";
		Log.d(TAG, "onCreate() executing SQL: " + sqlCmd2);
		db.execSQL(sqlCmd2);
		
	}

	private void createKeyValueTable(){
		SQLiteDatabase db = getWritableDatabase();
		String sqlCmd2 = "CREATE TABLE keyvalue ( key text primary key, value text)";
		db.execSQL(sqlCmd2);
		
	}
	
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);
		Log.d(TAG, "onUpgrade() recreating DB");
		
		db.execSQL("DROP TABLE IF EXISTS SYNC_RULES" );
		Log.d(TAG, "onUpgrade() recreating DB");
		onCreate(db);
		
	}

	public void insertVolume(EDVolume volume) {
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();

		values.clear();
		values.put(DB_COL_NAME, volume.getName());
		values.put(DB_COL_PATH, volume.getPath());
		values.put(DB_COL_PROVIDER_TYPE, volume.getFileProviderId());
		values.put(DB_COL_PROVIDER_PARAMS, volume.getSerializedFileProviderParams());

		Log.d(TAG, "insertVolume() name: '" + volume.getName() + "' path: '"
				+ volume.getPath() + "'");

		// TODO: Make sure path is unique
		db.insertOrThrow(DB_TABLE, null, values);
		
	}

	public void deleteVolume(EDVolume volume) {
		SQLiteDatabase db = getWritableDatabase();

		Log.d(TAG, "deleteVolume() " + volume.getName());

		db.delete(DB_TABLE, DB_COL_NAME + "=? AND " + DB_COL_PATH + "=?",
				new String[] { volume.getName(), volume.getPath() });
		
	}

	public void renameVolume(EDVolume volume, String newName) {
		SQLiteDatabase db = getWritableDatabase();

		Log.d(TAG, "renameVolume() " + volume.getName() + " to " + newName);

		ContentValues values = new ContentValues();
		values.put(DB_COL_NAME, newName);
		db.update(DB_TABLE, values, DB_COL_NAME + "=? AND " + DB_COL_PATH
				+ "=?", new String[] { volume.getName(), volume.getPath() });
		
	}

	public void cacheKey(EDVolume volume, byte[] key) {
		SQLiteDatabase db = getWritableDatabase();

		Log.d(TAG, "cacheKey() for volume" + volume.getName());

		ContentValues values = new ContentValues();
		values.put(DB_COL_KEY, Base64.encodeToString(key, Base64.DEFAULT));
		db.update(DB_TABLE, values, DB_COL_NAME + "=? AND " + DB_COL_PATH
				+ "=?", new String[] { volume.getName(), volume.getPath() });
		
	}

	public void clearKey(EDVolume volume) {
		SQLiteDatabase db = getWritableDatabase();

		Log.d(TAG, "clearKey() for volume" + volume.getName());

		ContentValues values = new ContentValues();
		values.putNull(DB_COL_KEY);
		db.update(DB_TABLE, values, DB_COL_NAME + "=? AND " + DB_COL_PATH
				+ "=?", new String[] { volume.getName(), volume.getPath() });
		
	}

	public void clearAllKeys() {
		SQLiteDatabase db = getWritableDatabase();

		Log.d(TAG, "clearAllKeys()");

		db.execSQL("UPDATE " + DB_TABLE + " SET " + DB_COL_KEY + " = NULL");
		
	}

	public byte[] getCachedKey(EDVolume volume) {
		SQLiteDatabase db = getReadableDatabase();

		Cursor cursor = db.query(DB_TABLE, NO_ARGS, DB_COL_NAME + "=? AND "
				+ DB_COL_PATH + "=?",
				new String[] { volume.getName(), volume.getPath() }, null,
				null, null);

		if (cursor.moveToFirst()) {
			String keyStr = cursor.getString(cursor.getColumnIndex(DB_COL_KEY));
			if (keyStr != null) {
				return Base64.decode(keyStr, Base64.DEFAULT);
			}
		}
		
		return null;
	}

	
	public List<FileSynchronizerRule> getSyncRules(){
		SQLiteDatabase db = null;
		try {
			ArrayList<FileSynchronizerRule> rules = new ArrayList<FileSynchronizerRule>();
			db = getReadableDatabase();
			Cursor cursor = db.rawQuery("SELECT * FROM SYNC_RULES", NO_ARGS);
			if (cursor.moveToFirst()) {
				do {
					int v1 = cursor.getInt(0);
					String v2 = cursor.getString(1);
					String v3 = cursor.getString(2);
					String v4 = cursor.getString(3);
					String v5 = cursor.getString(4);
					String v6 = cursor.getString(5);
					String v7 = cursor.getString(6);
	
					Log.d(TAG, "getSyncRules()");
	
					FileSynchronizerRule rule = new FileSynchronizerRule(v1,v2,v3,v4,v5,v6,v7);
	
					rules.add(rule);
				} while (cursor.moveToNext());
				
			}
			return rules;
		} catch (SQLiteException e){
			createSyncRulesTable(db);
			return new ArrayList<FileSynchronizerRule>();
		} finally {
			//do nothing (i should not close db on android ??)
		}
	}
	
	public void insertKeyValue(String key, String value){
		insertKeyValue(new KeyValueBean(key,value));
	}
	
	public void insertKeyValue(KeyValueBean kv){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();

		values.clear();
		values.put("key", kv.getKey());
		values.put("value", kv.getValue());
		db.insertOrThrow("keyvalue", null, values);
		
	}
	
	public void removeKeyValue(String key){
		SQLiteDatabase db = getWritableDatabase();
		db.delete("keyvalue", "key" + "=?", new String[] { key});
		
	}
	

	
	
	
	public void addRule(FileSynchronizerRule rule){
		SQLiteDatabase db = getWritableDatabase();
		ContentValues values = new ContentValues();

		values.clear();
		//values.put("id", rule.getId());
		values.put("id", rule.getId());
		values.put("volumeNameToSync", rule.getVolumeNameToSync());
		values.put("volumePassword", rule.getVolumePassword());
		values.put("volumePathToSync", rule.getVolumePathToSync());
		values.put("deleteSrcFileAfterSync", rule.getDeleteSrcFileAfterSync());
		values.put("syncOnlyOnWifi", rule.getSyncOnlyOnWifi());
		values.put("localPathToSync", rule.getLocalPathToSync());

		Log.d(TAG, "addRule()");

		db.insertOrThrow("SYNC_RULES", null, values);
		
	}
	
	public void removeRule(int id){
		SQLiteDatabase db = getWritableDatabase();

		Log.d(TAG, "removeRule() ");

		db.delete("SYNC_RULES", "id" + "=?", new String[] { ""+id});
		
	}
	
	public List<EDVolume> getVolumes() {
		ArrayList<EDVolume> volumes = new ArrayList<EDVolume>();
		SQLiteDatabase db = getReadableDatabase();

		// SELECT *, loop over each, create EDVolume
		Cursor cursor = db.rawQuery("SELECT * FROM " + DB_TABLE, NO_ARGS);

		int nameColId = cursor.getColumnIndex(DB_COL_NAME);
		int pathColId = cursor.getColumnIndex(DB_COL_PATH);
		int providerTypeColId = cursor.getColumnIndex(DB_COL_PROVIDER_TYPE);
		int providerParamsColId = cursor.getColumnIndex(DB_COL_PROVIDER_PARAMS);

		if (cursor.moveToFirst()) {
			do {
				String volName = cursor.getString(nameColId);
				String volPath = cursor.getString(pathColId);
				int volType = cursor.getInt(providerTypeColId);
				String volProviderParams = cursor.getString(providerParamsColId);

				Log.d(TAG, "getVolume() name: '" + volName + "' path: '"
						+ volPath + "'");

				EDVolume volume = new EDVolume(volName, volPath, volType,volProviderParams);

				volumes.add(volume);
			} while (cursor.moveToNext());
		}
		
		return volumes;
	}
	
	public String getKeyValueValue(String key){
		KeyValueBean result = getKeyValue(key);
		if (result==null) return null;
		return result.getValue();
	}
	
	public KeyValueBean getKeyValue(String key){
		SQLiteDatabase db = getReadableDatabase();

		// SELECT *, loop over each, create EDVolume
		try {
			Cursor cursor = db.rawQuery("SELECT * FROM keyvalue where key = ?", new String[]{key});
	
			int keyColId = cursor.getColumnIndex("key");
			int valueColId = cursor.getColumnIndex("value");
	
			if (cursor.moveToFirst()) {
				do {
					String resultKey = cursor.getString(keyColId);
					String resultValue = cursor.getString(valueColId);
	
					KeyValueBean result = new KeyValueBean(resultKey, resultValue);
	
					return result;
				} while (cursor.moveToNext());
			}
		} catch (SQLiteException e){
			createKeyValueTable();
			return null;
		} finally {
			
		}

		return null;
	}
}