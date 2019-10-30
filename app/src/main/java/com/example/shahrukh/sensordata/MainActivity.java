package com.example.shahrukh.sensordata;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.Tag;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.hardware.Sensor;
import android.content.pm.PackageManager;
import android.Manifest;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public String tagfilename;
    public boolean stop=false,tagflag=false;
    static public final int REQUEST_LOCATION = 1,permissionCheck=2;
    public TextView latitude,longitude,lightview,pitchview;
    public EditText samplingrate;
    public Button getlocation,getlight,record,anglebutton,samplingok,taggingbutton;
    public LocationManager locationm;
    public SensorManager lightsense;
    public Sensor lsensor;
    public Double recent_lightvalue=0.0,latitud=0.0,longitud=0.0;
    public FileOutputStream fos,tagos;
//    public Handler handle=new Handler();
    public Thread newthread;

    //for angle
    public SensorManager sManager;
    public Sensor asensor,msensor;
    public Double recent_pitchvalue = 0.0;
    float I[] = null; //for magnetic rotational data
    float mGeomagnetic[] = new float[3];
    float mGravity[] = new float[3];
    float[] values = new float[3];
    float Rot[] = null;
    double pitch;
    public int delay=6;


    //upto this was for angle


    public LocationListener listener =new LocationListener(){
                @Override
                public void onLocationChanged(Location location){
                    //Toast.makeText(MainActivity.this, "new location updated", Toast.LENGTH_SHORT).show();
                    latitud=location.getLatitude();
                    longitud=location.getLongitude();
                    latitude.setText(latitud+"");
                    longitude.setText(longitud+"");
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override
                public void onProviderEnabled(String provider) {}
                @Override
                public void onProviderDisabled(String provider) {
                    Toast.makeText(getApplicationContext(),provider+" not working",Toast.LENGTH_SHORT).show();
                }
            };

    public SensorEventListener light=new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    //Toast.makeText(MainActivity.this, "light value obtained", Toast.LENGTH_SHORT).show();
                    recent_lightvalue= Double.valueOf(event.values[0]);
                    lightview.setText(recent_lightvalue+" lux");
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            };

    public  Runnable r= new Runnable() {
        @Override
        public void run() {

            //file creation each time start button is pressed
            File folder= new File(Environment.getExternalStorageDirectory(),"/Sensordata");
            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

            Log.v("date",currentDateTimeString);

            if(folder.exists())
            {

                File newfile = new File(folder, currentDateTimeString+".csv");
                try {
                    fos=new FileOutputStream(newfile);
                }
                catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            else {
                final boolean flag = folder.mkdirs();
                if (flag) {
                    try {
                        File newfile = new File(folder, currentDateTimeString+".csv");

                        newfile.createNewFile();
                        fos = new FileOutputStream(newfile);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            String heading="LIGHT,LATITUDE,LONGITUDE,TILT,TIMESTAMP\n";
            try {
                fos.write(heading.getBytes());
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            while(stop==false){
                Log.v("tag","in loop");
                if(fos!=null)
                {
                    Long timenow= Long.valueOf(0),prev_time;
                    prev_time=System.currentTimeMillis();

                    String sampling_value=samplingrate.getText().toString();
                    if(!sampling_value.equals(""))
                    {
                        int sampling_int_val=Integer.parseInt(sampling_value);
                        delay=1000/sampling_int_val;
                    }


                    //loop provides 3ms delay for writing
                    while (System.currentTimeMillis()!=prev_time+delay){}
                    timenow=System.currentTimeMillis();
                    String dd = recent_lightvalue + "," + latitud + "," + longitud +","+recent_pitchvalue+","+timenow+ "\n";
                    try {
                        fos.write(dd.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            if(stop==true) {
                Log.v("tag","stopped");
                break;
            }
        }
            //closing the file
        try {
            fos.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        }
    };


    public SensorEventListener pit = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {


            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                mGravity = event.values;

            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                mGeomagnetic = event.values;

            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);

                    pitch = orientation[1];
                    pitch = Math.toDegrees(pitch);
                    pitch = Double.parseDouble(String.format("%.0f",pitch));
                    recent_pitchvalue = -1*pitch;
                    pitch = -1*pitch;

                    pitchview.setText(recent_pitchvalue+"");
                    //Log.v("pitch ",pitch+"");

                }
            }


        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pitchview=(TextView)findViewById(R.id.pitchview);
        latitude =(TextView)findViewById(R.id.Latitude);
        longitude=(TextView)findViewById(R.id.Longitude);
        lightview=(TextView)findViewById(R.id.light);
        getlight=(Button) findViewById(R.id.getlight);
        getlocation=(Button)findViewById(R.id.gpsbutton);
        record=(Button)findViewById(R.id.record);
        lightsense=(SensorManager)getSystemService(Context.SENSOR_SERVICE);
        lsensor=lightsense.getDefaultSensor(Sensor.TYPE_LIGHT);
        samplingrate=(EditText)findViewById(R.id.samplingrate);
        locationm=(LocationManager)getSystemService(Context.LOCATION_SERVICE);
        anglebutton=(Button)findViewById(R.id.Angle);
        sManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        asensor = sManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        msensor = sManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        samplingok=(Button)findViewById(R.id.samplingok);
        taggingbutton=(Button)findViewById(R.id.taggingbutton);



        //permission for accessing external storage
        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},permissionCheck);
        }
        //permission for accessing internet
        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.INTERNET},permissionCheck);
        }





    }

    @Override
    protected void onResume() {

            super.onResume();
            Log.i("maxxxxxxxxxx", String.valueOf(lsensor.getMaximumRange()));

            getlight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (lsensor == null) {
                        Toast.makeText(MainActivity.this, "no light sensor found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    lightsense.registerListener(light, lsensor, SensorManager.SENSOR_DELAY_FASTEST);
                    getlight.setText("light on");
                }
            });

            record.setOnClickListener(new View.OnClickListener() {
                int count=0;
                @Override
                public void onClick(View v) {
                    if(count==0) {
                        newthread=new Thread(r);
                        newthread.start();
                        record.setText("STOP-RECORDING");
                        count=1;
                        stop=false;
                        record.setBackgroundColor(Color.BLACK);
                        record.setTextColor(Color.WHITE);
                    }
                    else{

                        stop = true;
//                        if (locationm != null)
//                            locationm.removeUpdates(listener);
//                        if (lightsense != null)
//                            lightsense.unregisterListener(light);
//                        Toast.makeText(MainActivity.this, "stopped", Toast.LENGTH_SHORT).show();
//                        sManager.unregisterListener(pit);
                        record.setBackgroundColor(Color.GRAY);
                        count=0;
                        record.setText("START-RECORDING");

                    }
                }
            });

            getlocation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getApplicationContext(), "gps started", Toast.LENGTH_SHORT).show();

                    if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(getApplicationContext(), "gps service not enabled", Toast.LENGTH_SHORT).show();
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);

                    } else {
                        locationm.requestLocationUpdates(locationm.GPS_PROVIDER, 0, 0, listener);
                        getlocation.setText("gps on");
                    }
                }
            });

            anglebutton.setOnClickListener(new View.OnClickListener(){

                @Override
                public void onClick(View v) {
                    sManager.registerListener(pit, asensor, SensorManager.SENSOR_DELAY_NORMAL);
                    sManager.registerListener(pit, msensor, SensorManager.SENSOR_DELAY_NORMAL);

                }
            });

            samplingok.setOnClickListener(new View.OnClickListener() {
                  @Override
                  public void onClick(View v) {
                      String sampling_value=samplingrate.getText().toString();
                      if(!sampling_value.equals(""))
                      {
                          int sampling_int_val=Integer.parseInt(sampling_value);
                          delay=1000/sampling_int_val;
                      }

                      InputMethodManager inputManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                      inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);

                  }
              });

            taggingbutton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){

                    if(!tagflag){
                        //file creation for tagging file
                        File folder= new File(Environment.getExternalStorageDirectory(),"/Sensordata");
                        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());

                        Log.v("date",currentDateTimeString);

                        if(folder.exists())
                        {

                            File newfile = new File(folder, currentDateTimeString+"gps.csv");
                            try {
                                tagos=new FileOutputStream(newfile);
                            }
                            catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            final boolean flag = folder.mkdirs();
                            if (flag) {
                                try {
                                    File newfile = new File(folder, currentDateTimeString+"gps.csv");
                                    tagfilename=currentDateTimeString+"gps.csv";
                                    newfile.createNewFile();
                                    tagos = new FileOutputStream(newfile);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        tagflag=true;
                    }

                    String loc=latitud+","+longitud+"\n";
                    try {
                        tagos.write(loc.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//                    tagflag=true;
                }

            });


        }

        @Override
        protected void onPause(){
            super.onPause();

            stop = true;
            Log.v("stopper", "stop changed to true");
            if (locationm != null)
                locationm.removeUpdates(listener);
            if (lightsense != null)
                lightsense.unregisterListener(light);
            sManager.unregisterListener(pit);

//            Toast.makeText(this, "gps and light sensor stopped ", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onDestroy(){
            try {
                fos.close();
                tagos.close();

//
                Toast.makeText(this, "data file closed", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                //e.printStackTrace();
                super.onDestroy();
            }
            super.onDestroy();
        }

    }