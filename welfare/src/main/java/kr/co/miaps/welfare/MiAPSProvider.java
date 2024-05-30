package kr.co.miaps.welfare;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.minkcomm.CMinkLogMan;
import com.minkutil.Base64;
import com.minkutil.encryption.EncryptionAES128;

import java.util.List;

import kr.co.miaps.contentprovider.ProviderDB;
import kr.co.miaps.contentprovider.ProviderDatabaseHelper;


/** 
 * ContentProvider Class 
 */
public class MiAPSProvider extends ContentProvider{


	
	public static final String 	AUTHORITY   =  BuildConfig.APPLICATION_ID + ".provider";

	public static final Uri 	CONTENT_URI  =
			Uri.parse("content://" + AUTHORITY);
	
	public static final String  PATH_GET = "/AUTH_GET";
	public static final String  PATH_UPDATE = "/AUTH_UPDATE";
	
	/** CotentProvider 접근을 위한 ContentResolver 객체를 생성할 때 넣어 주는 매개변수에
	 *  URI를 사용 한다. 
	 */
	public static final Uri 	CONTENT_URI_GET  = 
			Uri.parse("content://" + AUTHORITY + PATH_GET);
	
	public static final Uri 	CONTENT_URI_UPDATE  = 
			Uri.parse("content://" + AUTHORITY + PATH_UPDATE);

	
	//private HashMap<String, Object> Data = new HashMap<String, Object>();
	
	public ProviderDatabaseHelper dbHelper;
	
	private static final int LOG_TYPE = 0;

	@Override
	public String getType(Uri uri) {
		return null;
	}
	
	/**
	 * ContentProvider 객체가 생성 되면 호출
	 */
	@Override
	public boolean onCreate() {
		dbHelper = new ProviderDatabaseHelper(getContext());
		return false;
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		if (0 < LOG_TYPE)
			Log.d("BaseProvider", "insert()");
		
		List<String> reqValue = uri.getPathSegments();

		if(reqValue.size() > 0) {
			String serviceType = reqValue.get(0);
			
			if (0 < LOG_TYPE)
				Log.d("BaseProvider", "serviceType = " + serviceType);
			
			if(serviceType.equals("AUTH_UPDATE")){
				if (0 < LOG_TYPE)
					Log.d("BaseProvider", "insert() : key = " + values.getAsString(ProviderDB.KEY_CODE) + ", value = " + values.getAsString(ProviderDB.KEY_NAME));

				long id = db.insert(ProviderDB.SQLITE_TABLE, null, values);
				getContext().getContentResolver().notifyChange(uri, null);
				return Uri.parse(CONTENT_URI + "/" + id);

			}
		}
		
		return uri;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(ProviderDB.SQLITE_TABLE);

		
		List<String> reqValue = uri.getPathSegments();

		if(reqValue.size() > 0) {
			String serviceType = reqValue.get(0);
			
			if (0 < LOG_TYPE)
				Log.d("BaseProvider", "serviceType = " + serviceType);
			
			if(serviceType.equals("AUTH_GET")){
				
				if (0 < LOG_TYPE)
					Log.d("BaseProvider", "query() : key = " + uri.getPathSegments().get(1));

				//queryBuilder.appendWhere(ProviderDB.KEY_CODE + "='" + uri.getPathSegments().get(1) + "'");
				
				selection = ProviderDB.KEY_CODE + "='" + uri.getPathSegments().get(1) + "'";
				
				Cursor cursor = queryBuilder.query(db, projection, selection,
					    selectionArgs, null, null, sortOrder);
				
				return cursor;
			}
		}
		

		
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		if (0 < LOG_TYPE)
			Log.d("BaseProvider", "update()");
		
		List<String> reqValue = uri.getPathSegments();

		if(reqValue.size() > 0) {
			String serviceType = reqValue.get(0);
			
			if (0 < LOG_TYPE)
				Log.d("BaseProvider", "serviceType = " + serviceType);
			
			if(serviceType.equals("AUTH_UPDATE")){
				String id = uri.getPathSegments().get(1);
				selection = ProviderDB.KEY_CODE + "=" + id
						   + (!TextUtils.isEmpty(selection) ? 
						     " AND (" + selection + ')' : "");
				
				//원래는 업데이트여야 하지만 delete 후 insert하는 방식으로 바꿨다.
				try {
					db.delete(ProviderDB.SQLITE_TABLE, selection, selectionArgs);
				} catch(Exception e) { }
				db.insert(ProviderDB.SQLITE_TABLE, null, values);
				getContext().getContentResolver().notifyChange(uri, null);
				return 1;
				
				/*

				int updateCount = db.update(ProviderDB.SQLITE_TABLE, values, selection, selectionArgs);
				getContext().getContentResolver().notifyChange(uri, _null);
				return updateCount;
				*/
			}
		}
		
		return 0;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		
		if (0 < LOG_TYPE)
			Log.d("BaseProvider", "delete()");
		
		List<String> reqValue = uri.getPathSegments();

		if(reqValue.size() > 0) {
			String serviceType = reqValue.get(0);
			
			if (0 < LOG_TYPE)
				Log.d("BaseProvider", "serviceType = " + serviceType);
			
			if(serviceType.equals("AUTH_UPDATE")){
                String id = "";
                try {
                    id = uri.getPathSegments().get(1);
                }catch(Exception e) {}
                if(false == id.isEmpty()) {
                    selection = ProviderDB.KEY_CODE + "='" + id + "'"
                            + (!TextUtils.isEmpty(selection) ?
                            " AND (" + selection + ')' : "");
                }
                else {
                    selection = "";
                }
				if (0 < LOG_TYPE)
					Log.d("BaseProvider", "delete() : where => " + selection);


				int deleteCount = db.delete(ProviderDB.SQLITE_TABLE, selection, selectionArgs);
				getContext().getContentResolver().notifyChange(uri, null);
				return deleteCount;
			}
		}
		
		return 0;
	}

	public static String get(Context context, String key) {
		// ContentResolver 객체 얻어 오기
		ContentResolver cr = context.getContentResolver();
		ContentValues req = new ContentValues();
		req.put("req", key);
		// ContentProviderDataA 어플리케이션 insert() 메서드에 접근
		Uri uri = cr.insert(CONTENT_URI_GET, req);
		
		// ContentProviderDataA 어플리케이션 에서 리턴받은 Data값 셋팅 하기
		List<String> values = uri.getPathSegments();
		String serviceType = values.get(0);
		String res = values.get(1);
		
		if(null == res) res = "";

		if (0 < LOG_TYPE) {
			//Log.i("BaseProvider.get", "serviceType = " + serviceType);
			//Log.i("BaseProvider.get", "key = " + key + ", value = " + res);
		}
		
		return res;
	}
	
	public synchronized static String getEx(Context context, String key) {
		 String[] projection = { 
		    ProviderDB.KEY_ROWID,
		    ProviderDB.KEY_CODE, 
		    ProviderDB.KEY_NAME};
		  Uri uri = Uri.parse(CONTENT_URI_GET + "/" + key);
		  Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
		  if (cursor != null) {
			  cursor.moveToFirst();
			  try {
				  //String _code = cursor.getString(cursor.getColumnIndexOrThrow(ProviderDB.KEY_CODE));
				  String _name = cursor.getString(cursor.getColumnIndexOrThrow(ProviderDB.KEY_NAME));

                  try {

                      byte[] bt = EncryptionAES128.decrypt(Base64.decode(_name.getBytes()));

                      _name = new String(bt);
                  } catch (Exception e) {
                      CMinkLogMan.WriteE(600, "MiAPSProvider.getEx Error");
                      //e.printStackTrace();

                  }

				  return _name;
			  }
			  catch(Exception e){
			      return "";
			  }
		  }
		  return "";
	}
	
	public static void put(Context context, String key, String value) {
		// ContentResolver 객체 얻어 오기
		ContentResolver cr2 = context.getContentResolver();
		
		// ContentValuse를 사용한 Data 전달하기
		ContentValues cv = new ContentValues();
		cv.put(key, value);
		
		// ContentProviderDataA 어플리케이션 update() 메서드에 접근
		cr2.update(CONTENT_URI_UPDATE, cv, null, null);
	}
	
	public synchronized static String putEx(Context context, String key, String value) {

        try {
            byte[] bt = EncryptionAES128.encrypt(value.getBytes());

            value = new String(Base64.encode(bt));
        } catch (Exception e) {
            CMinkLogMan.WriteE(600, "MiAPSProvider.putEx Error");
            //e.printStackTrace();

        }

        ContentValues values = new ContentValues();
		values.put(ProviderDB.KEY_CODE, key);
		values.put(ProviderDB.KEY_NAME, value);

		   
		Uri uri = Uri.parse(CONTENT_URI_UPDATE + "/" + key);
		// 삭제 여부와 관계없이 지우자
		context.getContentResolver().delete(uri, null, null);
		context.getContentResolver().insert(uri, values);
		/*
		if (0 < context.getContentResolver().delete(uri, _null, _null)) {
			context.getContentResolver().insert(uri, values);
		}
		*/
        return value;
	}

    public synchronized static void clear(Context context) {
        Uri uri = Uri.parse(CONTENT_URI_UPDATE + "/");
        // 삭제 여부와 관계없이 지우자
        context.getContentResolver().delete(uri, null, null);
    }
}
