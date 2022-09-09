package com.example.mediaprojection;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.display.DisplayManager;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String videoFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/MediaProjection.mp4";

    private static final int REQUEST_CODE_PERMISSIONS = 100;
    private static final int REQUEST_CODE_MediaProjection = 101;

    private MediaProjection mediaProjection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 퍼미션 확인
        checkSelfPermission();
    }
    @Override
    protected void onDestroy() {
        // 녹화중이면 종료하기
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        super.onDestroy();
    }
    //사용자가 권한을 허용하였는지에 따라 처리
    @Override
    protected void onActivityResult(int requestCode, final int resultCode, @Nullable final Intent data) {
        // 미디어 프로젝션 응답
        if (requestCode == REQUEST_CODE_MediaProjection && resultCode == RESULT_OK) {
            screenRecorder(resultCode, data);
            return;
        }
        //재요청
        super.onActivityResult(requestCode, resultCode, data);
    }
    /**
     * 음성녹음, 저장소 퍼미션
     * (접근권한)
     * @return
     * 사용자가 이미 앱에 권한을 부여했는지 확인 : PERMISSION_GRATED OR PERMISSION_DENIED
     */
    public boolean checkSelfPermission() {
        String temp = "";
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.RECORD_AUDIO + " ";
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            temp += Manifest.permission.WRITE_EXTERNAL_STORAGE + " ";
        }

        if (TextUtils.isEmpty(temp) == false) {
            ActivityCompat.requestPermissions(this, temp.trim().split(" "), REQUEST_CODE_PERMISSIONS);
            return false;
        } else {
            initView();
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_PERMISSIONS: {
                int length = permissions.length;
                for (int i = 0; i < length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        // 퍼미션 동의가 1개라도 부족하면 화면을 초기화 하지 않음
                        return;
                    }
                    initView();
                }
                return;
            }
            default:
                return;
        }
    }

    /**
     * 뷰 초기화
     */
    private void initView() {
        findViewById(R.id.actionRec).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 미디어 프로젝션 요청
                startMediaProjection();
            }
        });
    }

    /**
     * 화면녹화
     *
     * @param resultCode
     * @param data
     */

    private void screenRecorder(int resultCode, @Nullable Intent data) {
        //MediaRecorder screenRecorder = createRecorder();
        createRecorder();
        MediaRecorder screenRecorder=mediaRecorder;
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        MediaProjection.Callback callback = new MediaProjection.Callback() {
            @Override
            public void onStop() {
                super.onStop();
                if (screenRecorder != null) {
                    //screenRecorder.stop();
                    screenRecorder.reset();
                    screenRecorder.release();
                }
                mediaProjection.unregisterCallback(this);
                mediaProjection = null;
            }
        };
        mediaProjection.registerCallback(callback, null);
        
        //화면 크기 구하기
        //DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();

        //Log.i("displayMetrics","화면 크기 : " +screenRecorder.getSurface());
        mediaProjection.createVirtualDisplay(
                "sample",
                displayMetrics.widthPixels, displayMetrics.heightPixels, displayMetrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                screenRecorder.getSurface(), null, null);

        final Button actionRec = findViewById(R.id.actionRec);
        actionRec.setText("STOP REC");
        screenRecorder.start();
        actionRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionRec.setText("START REC");
                if (mediaProjection != null) {
                    mediaProjection.stop();

                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse(videoFile), "video/mp4");
                    startActivity(intent);
                }
            }
        });
    }

    /**
     * 미디어 프로젝션 요청
     */
    private void startMediaProjection() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE_MediaProjection);
        }
    }

    /**
     * 미디어 레코더
     *
     * @return
     */

    MediaRecorder mediaRecorder = new MediaRecorder();
    DisplayMetrics displayMetrics = Resources.getSystem().getDisplayMetrics();
    private MediaRecorder createRecorder() {
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setOutputFile(videoFile);
        mediaRecorder.setVideoSize(displayMetrics.widthPixels, displayMetrics.heightPixels);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoEncodingBitRate(512 * 1000);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}