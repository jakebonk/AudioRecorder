package com.allyants.audiorecorder;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by jbonk on 7/15/2017.
 */

public class AudioDialog {

    Context context;
    private MediaRecorder mediaRecorder;
    private boolean isListening = false;
    Thread thread;
    private File outfile = null;
    long startTime;
    AlertDialog ad;

    public interface OnClick{
        boolean onClick();
    }

    private OnClick onDoneClick = new OnClick() {
        @Override
        public boolean onClick() {
            return true;
        }
    };
    private OnClick onCancelClick = new OnClick() {
        @Override
        public boolean onClick() {
            return true;
        }
    };

    private OnDoneRecording onDoneRecordingListener = new OnDoneRecording() {
        @Override
        public void onSuccess(File file) {

        }

        @Override
        public void onFailure(String msg) {

        }
    };


    public AudioDialog(){

    }

    public interface OnDoneRecording{
        void onSuccess(File file);
        void onFailure(String msg);
    }

    public AudioDialog setOnDoneRecordingListener(OnDoneRecording onDoneRecording){
        this.onDoneRecordingListener = onDoneRecording;
        return this;
    }

    public AudioDialog setOnDoneClickListener(OnClick onClick){
        this.onDoneClick = onClick;
        return this;
    }

    public AudioDialog setOnCancelClickListener(OnClick onClick){
        this.onCancelClick = onClick;
        return this;
    }

    public AudioDialog build(Context context){
        this.context = context;
        return this;
    }

    public AudioDialog show(){
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogLayout = inflater.inflate(R.layout.audio_recorder_layout,null);
        final TextView tvTime = (TextView)dialogLayout.findViewById(R.id.tvTime);
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        Button btnDone = (Button)dialogLayout.findViewById(R.id.btnDone);
        Button btnCancel = (Button)dialogLayout.findViewById(R.id.btnCancel);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(onDoneClick.onClick())
                    ad.dismiss();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(onCancelClick.onClick())
                    ad.dismiss();
            }
        });
        final SeekBar seekBar = (SeekBar)dialogLayout.findViewById(R.id.seekBar);
        seekBar.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View view, DragEvent dragEvent) {
                return false;
            }
        });
        seekBar.setMax(32767);
        builder.setView(dialogLayout);
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                Long time = SystemClock.uptimeMillis() - startTime;
                AudioDialog.this.onDestroy();
            }
        });
        try {
            resetRecorder();
            startRecording();
            ad = builder.show();
            startTime = SystemClock.uptimeMillis();
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(isListening) {
                        seekBar.setProgress(mediaRecorder.getMaxAmplitude());
                        final Long time = SystemClock.uptimeMillis() - startTime;
                        ((Activity)context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvTime.setText(longToTime(time));
                            }
                        });
                    }
                }
            });
            thread.start();

        } catch (IOException e) {
            onDoneRecordingListener.onFailure(e.getMessage());
        }
        return this;
    }

    private String longToTime(long diff){
        String temp = "";
        long hours = diff / (60*60*1000);
        if(hours >= 1){
            temp += String.valueOf(hours);
        }
        diff -= hours * 60*60*1000;
        long minutes = diff / (60*1000);
        if(minutes >= 10){
            if(!temp.equals("")){
                temp += " : ";
            }
            temp += String.valueOf(minutes)+":";
        }else if(minutes >= 1){
            if(!temp.equals("")){
                temp += " : ";
            }
            temp += "0"+String.valueOf(minutes)+":";
        }else{
            if(!temp.equals("")){
                temp += " : ";
            }
            temp += "00:";
        }
        diff -= minutes*60*1000;
        long seconds = diff / 1000;
        if(seconds >= 10){
            temp += String.valueOf(seconds);
        }else if(seconds >= 1){
            temp += "0"+String.valueOf(seconds);
        }else{
            temp += "00";
        }

        return temp;
    }

    private void resetRecorder() throws IOException{
        mediaRecorder = new MediaRecorder();
        String state = android.os.Environment.getExternalStorageState();
        if(!state.equals(android.os.Environment.MEDIA_MOUNTED))  {
            throw new IOException("SD Card is not mounted.  It is " + state + ".");
        }

        // make sure the directory we plan to store the recording in exists

        File directory = context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS);
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Path to file could not be created.");
        }
        try{
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String audioFileName = "RECORDING_" + timeStamp + "_";
            outfile=File.createTempFile(audioFileName, ".mp3",directory);
        }catch(IOException e){
            onDoneRecordingListener.onFailure(e.getMessage());
        }
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setAudioEncodingBitRate(16);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setOutputFile(outfile.getAbsolutePath());
        try {
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,android.net.Uri.parse(new java.net.URI(outfile.toURI().toString()).toString())));
            try {
                mediaRecorder.prepare();
            } catch (IllegalStateException e) {
                onDoneRecordingListener.onFailure(e.getMessage());
            } catch (IOException e) {
                onDoneRecordingListener.onFailure(e.getMessage());
            }
        } catch (URISyntaxException e) {
            onDoneRecordingListener.onFailure(e.getMessage());
        }
    }

    private void startRecording(){
        mediaRecorder.start();
        isListening = true;
    }

    private void onDestroy(){
        stop();
    }

    public void stop() {
        if(thread.isAlive()){
            thread.interrupt();
        }
        isListening = false;
        try {
            mediaRecorder.stop();
        }catch (RuntimeException ex){
            onDoneRecordingListener.onFailure(ex.toString());
        }
        mediaRecorder.release();
        mediaRecorder = null;
        onDoneRecordingListener.onSuccess(outfile);
    }

}
