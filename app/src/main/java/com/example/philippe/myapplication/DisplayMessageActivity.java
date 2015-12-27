package com.example.philippe.myapplication;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.os.Vibrator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class DisplayMessageActivity extends AppCompatActivity {

    HttpClient httpClient;
    static List<String> logs = new LinkedList<>(); //Efficient to pop/push elements to HEAD
    Thread backgroundRefreshUIPredictionThread = null;
    boolean backgroundRefreshUIPredictionThread_Boolean = true;
    static final int REFRESH_UI_THREAD = 1000;
    static int logId = 0;

    private void clearLogs() {
        logs = new ArrayList<>();
        TextView layout = (TextView) findViewById(R.id.textView_logs);
        layout.setText("");
        logId = 0;
    }

    static void log(TextView textView, String newLog) {
        logs.add("[" + (logId++) + "] " + newLog);
        if(logs.size() > 10) {
            logs.remove(0);
        }
        StringBuilder sb = new StringBuilder();
        for(String log : logs) {
            sb.append(log + "\n");
        }
        textView.setText(sb.toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearLogs();
                Snackbar.make(view, getResources().getString(R.string.logs_cleared), Snackbar.LENGTH_LONG)
                        .setAction("Action", null)
                        .show();
            }
        });

        ActionBar ab = getSupportActionBar();
        if(ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        TextView textView = (TextView) findViewById(R.id.textView_logs);
        log(textView,  getResources().getString(R.string.application_started));
        textView.setMovementMethod(new ScrollingMovementMethod());
        //RelativeLayout layout = (RelativeLayout) findViewById(R.id.content);
        //layout.addView(textView);
        setTitle("Device : Visual");

        String target = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        if(target == null || target.isEmpty()) {
            target = getResources().getString(R.string.edit_message);
        }

        log(textView, "Initiated HTTP Client with target: " + target);

        httpClient = new HttpClient(target);

        startUIPredictionThread();
    }

    public void sendNumber(View view) {
        EditText editText = (EditText) findViewById(R.id.enter_number);
        TextView textView = (TextView) findViewById(R.id.textView_logs);
        if(editText.getText() == null || editText.getText().toString().isEmpty()) {
            log(textView, "Number is empty. Please try again.");
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(400); //400ms vibration
        }

        Integer number = Integer.parseInt(editText.getText().toString());
        log(textView, "Outcome is : " + number);
        try {
            httpClient.sendOutcome(number, textView);
        } catch (Exception e) {
            log(textView, "Unable to perform request.");
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(400); //400ms vibration
        }
    }

    public void startUIPredictionThread() {
        backgroundRefreshUIPredictionThread_Boolean = true;

        backgroundRefreshUIPredictionThread = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted() && backgroundRefreshUIPredictionThread_Boolean) {
                        Thread.sleep(REFRESH_UI_THREAD);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                TextView tv = (TextView) findViewById(R.id.resultsView);
                                try {
                                    String newPrediction = httpClient.getPrediction();
                                    if(newPrediction.contains("SESSION_NOT_READY_WHEEL_COUNT_ACTUAL")) {
                                        newPrediction = "Not Ready yet. Wheel count is " + newPrediction.split(",")[1];
                                    }
                                    tv.setText(newPrediction);
                                } catch (Exception e) {
                                    TextView textView = (TextView) findViewById(R.id.textView_logs);
                                    log(textView, e.getMessage());
                                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                    v.vibrate(400); //400ms vibration
                                }

                                TextView textView = (TextView) findViewById(R.id.textView_logs);
                                log(textView, "Prediction updated");
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        backgroundRefreshUIPredictionThread.start();
    }

    public void stopUIPredictionThread() {
        backgroundRefreshUIPredictionThread_Boolean = false;
    }

    @Override
    public void onStop() {
        super.onStop();
        stopUIPredictionThread();
    }

    public void onDestroy() {
        super.onDestroy();
        stopUIPredictionThread();
    }
}
