package com.practicegamestudio.stepsperminute;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity implements SensorEventListener{

    private TextView TvSteps;
    private Button BtnStart;
    SensorManager sensorManager;
    Sensor stepSensor;
    private boolean running = false;
    private long sensorTimeReference = 0l;
    private long myTimeReference = 0l;


    // initialise array to hold timestamps of detected steps
    private static final int RING_SIZE = 40;
    private int ringCounter = 0;
    private long[] timeRing = new long[RING_SIZE];
    private long stepWindow = 10000; // length of step history in milliseconds (10 sec)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // access built-in android step detector
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor= sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        // get handles for UI elements
        TvSteps = (TextView) findViewById(R.id.tv_steps);
        BtnStart = (Button) findViewById(R.id.btn_start);

        // react to start button presses
        BtnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (!running) {
                    // start step detector sensor
                    sensorManager.registerListener(MainActivity.this, stepSensor,SensorManager.SENSOR_DELAY_FASTEST);
                    ringCounter = 0;
                    timeRing = new long[RING_SIZE];
                    BtnStart.setText("Stop");
                    running = true;
                } else {
                    sensorManager.unregisterListener(MainActivity.this);
                    BtnStart.setText("Start");
                    TvSteps.setText("0");
                    running = false;
                }
            }
        });

        // start repeating function to continually calculate steps per minute
        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(running) {
                    int steps = 0;
                    long currentTime = System.currentTimeMillis();

                    // count how many steps have occurred during stepWindow
                    for (int i = 0; i < timeRing.length; i++) {
                        if ((currentTime - timeRing[i]) < stepWindow) {
                            steps++;
                        }
                    }
                    // convert to steps per minute
                    float multiplier = 60000 / stepWindow;
                    int stepsPerMinute = Math.round(steps * multiplier);

                    // update UI text
                    TvSteps.setText("" + stepsPerMinute);
                }
            }
        }, 0, 1000); // runs once per second

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // React to step detection events
        if (running && event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {

            // set reference times
            if(sensorTimeReference == 0l && myTimeReference == 0l) {
                sensorTimeReference = event.timestamp;
                myTimeReference = System.currentTimeMillis();
            }
            // set event timestamp to current time in milliseconds
            event.timestamp = myTimeReference + Math.round((event.timestamp - sensorTimeReference) / 1000000.0);

            // change steps-per-minute value text to green for 100ms
            TvSteps.setTextColor(Color.GREEN);
            new CountDownTimer(100, 10) {
                public void onTick(long millisUntilFinished) {
                }
                public void onFinish() {
                    TvSteps.setTextColor(Color.DKGRAY);
                }
            }.start();

            // add timestamp (milliseconds) of step to detected steps ring array
            timeRing[(ringCounter) % RING_SIZE] = event.timestamp;
            ringCounter++;
        }
    }


}