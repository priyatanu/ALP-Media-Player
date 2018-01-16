package de.yaacc.upnp.server.contentdirectory;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

/**
 * Created by Priyatanu on 1/1/2018.
 */

public class SrtProcessor implements Runnable {
    Context ctx;
    static File folder1 = new File(Environment.getExternalStorageDirectory()
            .getAbsolutePath().toString() + "/Movies");// + "/SmartInstaller/ToInstall/");

    static File[] listOfFiles1 = folder1.listFiles();
    static String tablenames[] =new String[(int)listOfFiles1.length];

    SrtProcessor(Context context){
        ctx  =context;
    }
    public void setcontext() {
    }

    @Override
    public void run() {
        Log.d("ZAA", "External storage1");
        DBHelper mydb = new DBHelper(ctx);

            try {
                Log.v("ZAA", "creating D");
                File folder = new File(Environment.getExternalStorageDirectory()
                        .getAbsolutePath().toString() + "/Movies");// + "/SmartInstaller/ToInstall/");
                Log.i("ZAA", "CHecking" + folder.listFiles().length);
                File[] listOfFiles = folder.listFiles();


                //Log.i("ZAA","starting" + Environment.getExternalStorageDirectory().getAbsolutePath());

                for (int i = 0; i < listOfFiles.length; i++) {
                    Log.i("BZAA", "starting" + listOfFiles[i].getAbsolutePath());
                    String filename = listOfFiles[i].getName();
                    String tablename = "";
                    if (filename.contains("srt")) {
                        String movienames[] = filename.split(Pattern.quote("."));
                        Log.i("ZAA", "filename" + filename);
                        for (int j = 0; (j < movienames.length - 1); j++) {
                            tablename = tablename + movienames[j];
                        }
                        tablenames[i]=tablename;
                        Log.i("ZAA", "tablenames[i]" + tablenames[i]);
                        SQLiteDatabase db = DBHelper.checkDB;
                        db.execSQL("DROP TABLE IF EXISTS " + tablename + " ");
                        db.execSQL("create table " + tablename + " " +
                                "(id integer primary key, value text,hour  text, min  text, sec  text, diff  text, absoultetime text)");
                        try {
                            InputStream instream = new FileInputStream(Environment.getExternalStorageDirectory() + "/Movies/" +filename);
                            Log.d("ZAA", "Hello  " + Environment.getExternalStorageDirectory() + "/Movies/"+filename);
                            // if file the available for reading
                            if (instream != null) {
                                // prepare the file for reading
                                InputStreamReader inputreader = new InputStreamReader(instream);
                                BufferedReader buffreader = new BufferedReader(inputreader);
                                String line = buffreader.readLine();
                                ;
                                int counter = 1;
                                int primarykey = 0;
                                String val = " ";
                                String templine = "";
                                String series[] = new String[5];
                                int hour = 0;
                                int min = 0;
                                int sec = 0;
                                int timeoftoast = 0;
                                String part1[] = new String[2];
                                String part2[] = new String[2];
                                String finaltime1[] = new String[3];
                                String finaltime2[] = new String[3];
                                String finaltxt = "";
                                int diff = 0;
                                int absoultetime=0;
                                while (line != null) {

                                    templine += line + " ";

                                    //Pattern p = Pattern.compile("[0-9]+");
                                    //Matcher m = p.matcher(line);
                                    if (line.isEmpty()) {
                                        // Log.d("ZAA", "templine " +templine);
                                        series = templine.split("\\s+");
                                        //Log.d("ZAA", "LULAAA " + series[0] +"Hellulah" +series[1] + "Hellulah " + series[3]);
                                        //hour =
                                        part1 = series[1].split(",");
                                        part2 = series[3].split(",");//before comma part
                                        finaltime1 = part1[0].split(":");
                                        finaltime2 = part2[0].split(":");
                                        hour = Integer.parseInt(finaltime1[0]);
                                        min = Integer.parseInt(finaltime1[1]);
                                        sec = Integer.parseInt(finaltime1[2]);
                                        diff = (((Integer.parseInt(finaltime2[0])) * 3600) + ((Integer.parseInt(finaltime2[1])) * 60) + Integer.parseInt(finaltime2[2])) - (((Integer.parseInt(finaltime1[0])) * 3600) + ((Integer.parseInt(finaltime1[1])) * 60) + (Integer.parseInt(finaltime1[2])));
                                        //timeoftoast= (Integer.parseInt(finaltime2[2]) - Integer.parseInt(finaltime1[2]));


                                        Log.d("ZAA", "Time of toast hello " + diff + hour + min + sec);
                                        templine = "";
                                        ContentValues contentValues = new ContentValues();
                                        //contentValues.put("id",    Integer.parseInt(series[0]));
                                        for (int k = 4; k < series.length; k++) {
                                            finaltxt = finaltxt + series[k] + " ";
                                        }
                                        absoultetime = (hour * 3600 )+ (min * 60) + sec;
                                        Log.d("ZAA", "finaltxt " + finaltxt);
                                        contentValues.put("value", finaltxt);
                                        contentValues.put("hour", hour);
                                        contentValues.put("min", min);
                                        contentValues.put("sec", sec);
                                        contentValues.put("diff", diff);
                                        contentValues.put("absoultetime", absoultetime);
                                        Log.d("ZAA", "Hello efore insert nice ");
                                        db.insert(tablename, null, contentValues);
                                        Log.d("ZAA", "Hello cursor nice ");
                                        finaltxt = "";
                                    }
                                    //  Log.d("ZAA", "linesss " +line);
                                    line = buffreader.readLine();
                                    // do something with the line
                                }
                                instream.close();

                            }
                        } catch (Exception e) {

                        }
                    }
                    tablename = "";

                }

                //   contentValues.put("diff", "hello");



            }catch (Exception e){
                Log.d("ZAA", "Exception " + e.toString());
            }




        }


    }//end of run

