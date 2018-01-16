package de.yaacc.upnp.server.contentdirectory;

/**
 * Created by Priyatanu on 1/1/2018.
 */

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DBHelper extends SQLiteOpenHelper{
    public static final String DATABASE_NAME = "SRT.db";
    public String DB_PATH = "/data/data/de.yaacc.upnp.server.contentdirectory/databases/";
    public static  SQLiteDatabase qd;
    public static SQLiteDatabase checkDB = null;
    private  Context _context;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME , null, 1);
        _context = context;

        Log.v("ZAA", "DBHelper");
    }

    public DBHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        Log.v("ZAA", "sqLiteDatabase onCreate"+sqLiteDatabase.getPath());
    }

    public void createDataBase() throws IOException
    {
        Log.v("ZAA", "createDataBase");
        boolean dbExist = checkDataBase();

        if(dbExist)
        {
            Log.v("ZAA", "dbExist");
            //do nothing - database already exist
        }else{

            this.getReadableDatabase();

            try {

                copyDataBase();

            } catch (IOException e) {

                // throw new Error("Error copying database");

            }
        }

    }

    private boolean checkDataBase()
    {


        try
        {
            //String myPath = DB_PATH + DATABASE_NAME;
            checkDB = _context.openOrCreateDatabase(DATABASE_NAME,Context.MODE_PRIVATE, null);
            Log.v("ZAA", "openDatabase"+checkDB.getPath());
        }catch(SQLiteException e)
        {
            //database does't exist yet.
            Log.v("ZAA", "exception openDatabase"+e.toString());
        }
        if(checkDB != null)
        {
            //checkDB.close();
        }

        return checkDB != null ? true : false;
    }
    private void copyDataBase() throws IOException
    {
        //Open your local db as the input stream
        InputStream myInput = _context.getAssets().open(DATABASE_NAME);

        // Path to the just created empty db
        String outFileName = DB_PATH + DATABASE_NAME;

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer))>0)
        {
            myOutput.write(buffer, 0, length);
        }
        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        Log.v("ZAA", "sqLiteDatabase Hi"+sqLiteDatabase.getPath());
        onCreate(sqLiteDatabase);
    }
}
