package com.allyants.audiorecorderexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.allyants.audiorecorder.AudioDialog;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button)findViewById(R.id.btnDialog);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AudioDialog dialog = new AudioDialog().build(MainActivity.this).setOnDoneRecordingListener(new AudioDialog.OnDoneRecording() {
                    @Override
                    public void onSuccess(File file) {
                        Log.e("file",file.getPath());
                    }

                    @Override
                    public void onFailure(String msg) {

                    }
                }).show();
            }
        });
    }
}
