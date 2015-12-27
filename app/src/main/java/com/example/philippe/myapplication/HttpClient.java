package com.example.philippe.myapplication;

import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HttpClient {

    private ExecutorService executor = Executors.newFixedThreadPool(2);
    private String apiTargetUrl;

    public HttpClient(String apiTargetUrl) {
        this.apiTargetUrl = apiTargetUrl;
    }


    public class GetPredictionTask implements Callable<String> {
        @Override
        public String call() throws Exception {
            return lowLevelCall(apiTargetUrl);
        }
    }

    public class SendOutcomeTask implements Callable<String> {

        private long outcomeNumber;
        private TextView textView;

        public SendOutcomeTask(int outcomeNumber, TextView textView) {
            this.outcomeNumber = outcomeNumber;
            this.textView = textView;
        }

        @Override
        public String call() throws Exception {
            long startMs = System.currentTimeMillis();
            String targetUrl = apiTargetUrl + "?outcome=" + outcomeNumber;
            DisplayMessageActivity.log(textView, "Call took " + (System.currentTimeMillis() - startMs) + "ms. Target is " + targetUrl);
            return lowLevelCall(targetUrl);
        }
    }

    public String sendOutcome(Integer outcomeNumber, TextView textView) throws Exception {
        Future<String> callRet = executor.submit(new SendOutcomeTask(outcomeNumber, textView));
        return callRet.get();
    }

    public String getPrediction() throws Exception {
        Future<String> callRet = executor.submit(new GetPredictionTask());
        return callRet.get();
    }

    private String lowLevelCall(String targetUrl) throws IOException {
        String fullString = "";
        BufferedReader reader = null;
        try {
            URL url = new URL(targetUrl);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                fullString += line;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fullString;
    }

}