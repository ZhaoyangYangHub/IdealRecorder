package tech.oom.idealrecorderdemo;

import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.constant.PermissionConstants;
import com.blankj.utilcode.util.PermissionUtils;
import com.blankj.utilcode.util.UtilsTransActivity;

import java.io.File;
import java.util.List;

import tech.oom.idealrecorder.IdealRecorder;
import tech.oom.idealrecorder.StatusListener;
import tech.oom.idealrecorder.utils.Log;
import tech.oom.idealrecorderdemo.widget.WaveView;

public class MainActivity extends AppCompatActivity {

    private Button recordBtn;
    private WaveView waveView;
//    private WaveLineView waveLineView;
    private TextView tips;

    private IdealRecorder idealRecorder;

    private IdealRecorder.RecordConfig recordConfig;

    private StatusListener statusListener = new StatusListener() {
        @Override
        public void onStartRecording() {
//            waveLineView.startAnim();
            tips.setText("开始录音");
        }

        @Override
        public void onRecordData(short[] data, int length) {

            for (int i = 0; i < length; i += 60) {
                waveView.addData(data[i]);
            }
            Log.d("MainActivity", "current buffer size is " + length);
        }

        @Override
        public void onVoiceVolume(int volume) {
            double myVolume = (volume - 40) * 4;
//            waveLineView.setVolume((int) myVolume);
            Log.d("MainActivity", "current volume is " + volume);
        }

        @Override
        public void onRecordError(int code, String errorMsg) {
            tips.setText("录音错误" + errorMsg);
        }

        @Override
        public void onFileSaveFailed(String error) {
            Toast.makeText(MainActivity.this, "文件保存失败", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFileSaveSuccess(String fileUri) {
            Toast.makeText(MainActivity.this, "文件保存成功,路径是" + fileUri, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onStopRecording() {
            tips.setText("录音结束");
//            waveLineView.stopAnim();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recordBtn = (Button) findViewById(R.id.register_record_btn);
        waveView = (WaveView) findViewById(R.id.wave_view);
//        waveLineView = (WaveLineView) findViewById(R.id.waveLineView);
        tips = (TextView) findViewById(R.id.tips);
        idealRecorder = IdealRecorder.getInstance();
        recordBtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                readyRecord();
                return true;
            }
        });
        recordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_UP:
                        stopRecord();
                        return false;

                }
                return false;
            }
        });
        recordConfig = new IdealRecorder.RecordConfig(MediaRecorder.AudioSource.MIC, 48000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

    }

    /**
     * 准备录音 录音之前 先判断是否有相关权限
     */
    private void readyRecord() {
        if (PermissionUtils.isGranted(PermissionConstants.MICROPHONE, PermissionConstants.STORAGE)) {
            record();
        } else {
            PermissionUtils.permission(PermissionConstants.MICROPHONE, PermissionConstants.STORAGE)
                    .rationale(new PermissionUtils.OnRationaleListener() {
                        @Override
                        public void rationale(@NonNull UtilsTransActivity activity, @NonNull ShouldRequest shouldRequest) {
                            new AlertDialog.Builder(activity)
                                    .setTitle("友好提醒")
                                    .setMessage("录制声音保存录音需要录音和读取文件相关权限哦，爱给不给")
                                    .setPositiveButton("好，给你", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            shouldRequest.again(true);
                                        }
                                    }).setNegativeButton("我是拒绝的", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            shouldRequest.again(false);
                                        }
                                    }).create().show();
                        }
                    })
                    .callback(new PermissionUtils.SimpleCallback() {
                        @Override
                        public void onGranted() {
                        }

                        @Override
                        public void onDenied() {
                            Toast.makeText(MainActivity.this, "没有录音和文件读取权限，你自己看着办", Toast.LENGTH_SHORT).show();
                        }
                    }).request();
        }
    }

    /**
     * 开始录音
     */
    private void record() {
        //如果需要保存录音文件  设置好保存路径就会自动保存  也可以通过onRecordData 回调自己保存  不设置 不会保存录音
        idealRecorder.setRecordFilePath(getSaveFilePath());
//        idealRecorder.setWavFormat(false);
        //设置录音配置 最长录音时长 以及音量回调的时间间隔
        idealRecorder.setRecordConfig(recordConfig).setMaxRecordTime(20000).setVolumeInterval(200);
        //设置录音时各种状态的监听
        idealRecorder.setStatusListener(statusListener);
        idealRecorder.start(); //开始录音

    }

    /**
     * 获取文件保存路径
     *
     * @return
     */
    private String getSaveFilePath() {
        File file = new File(getExternalCacheDir(), "Audio");
        if (!file.exists()) {
            file.mkdirs();
        }
        File wavFile = new File(file, "ideal.wav");
        return wavFile.getAbsolutePath();
    }


    /**
     * 停止录音
     */
    private void stopRecord() {
        //停止录音
        idealRecorder.stop();
    }
}