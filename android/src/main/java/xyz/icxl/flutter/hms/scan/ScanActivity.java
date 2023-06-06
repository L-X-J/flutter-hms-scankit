package xyz.icxl.flutter.hms.scan;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.huawei.hms.hmsscankit.OnResultCallback;
import com.huawei.hms.hmsscankit.RemoteView;
import com.huawei.hms.hmsscankit.ScanUtil;
import com.huawei.hms.ml.scan.HmsScan;
import com.huawei.hms.ml.scan.HmsScanAnalyzerOptions;

import java.io.IOException;

import xyz.icxl.fluter.hms.scan.R;

/**
 * # 扫码
 * - author：`cxl`
 * - date: `2023/6/6`
 */
public class ScanActivity extends Activity {
    private FrameLayout frameLayout;
    private RemoteView remoteView;
    private int mScreenWidth;
    private int mScreenHeight;
    private final int REQUEST_CAMERA_PERMISSION_CODE = 1001;
    private final int SETTINGS_REQUEST_CODE = 1002;
    private final int REQUEST_CODE_PHOTO = 1003;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(1);
        setContentView(R.layout.icxl_activity_scan);
        frameLayout = findViewById(R.id.rim);
        selfInit(savedInstanceState);
        findViewById(R.id.back_img).setOnClickListener(v -> finish());
        findViewById(R.id.flush_iv).setOnClickListener(v -> {
            if(remoteView!=null){
                if(remoteView.getLightStatus()){
                    ((ImageView)findViewById(R.id.flush_iv)).setImageResource(R.mipmap.icxl_scan_flush_off);
                }else {
                    ((ImageView)findViewById(R.id.flush_iv)).setImageResource(R.mipmap.icxl_scan_flush_on);
                }
                remoteView.switchLight();
            }
        });

        findViewById(R.id.choose_pic_iv).setOnClickListener(v -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
            this.startActivityForResult(pickIntent, REQUEST_CODE_PHOTO);
        });
    }

    private void selfInit(@Nullable Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            initRemoteView(savedInstanceState);
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_CODE);
            }
        }
    }

    private void backSuccess(HmsScan result){
        setResult(RESULT_OK, new Intent().putExtra(ScanUtil.RESULT, result));
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selfInit(null);
            } else {
                new AlertDialog
                        .Builder(this)
                        .setTitle(R.string.icxl_scan_not_permission_dialog_title)
                        .setMessage(R.string.icxl_scan_not_permission_dialog_message)
                        .setNegativeButton(R.string.icxl_scan_not_permission_dialog_no, (dialog, which) -> {
                            dialog.dismiss();
                            finish();
                        })
                        .setPositiveButton(R.string.icxl_scan_not_permission_dialog_yes, (dialog, which) -> {
                           dialog.dismiss();
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivityForResult(intent,SETTINGS_REQUEST_CODE);
                        });
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SETTINGS_REQUEST_CODE){
           selfInit(null);
        }
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_PHOTO) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                HmsScan[] hmsScans = ScanUtil.decodeWithBitmap(this, bitmap, new HmsScanAnalyzerOptions.Creator().setPhotoMode(true).create());
                if (hmsScans != null && hmsScans.length > 0 && hmsScans[0] != null && !TextUtils.isEmpty(hmsScans[0].getOriginalValue())) {
                    backSuccess(hmsScans[0]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 初始化扫码的view
     */
    private void initRemoteView(@Nullable Bundle savedInstanceState) {
        // 设置扫码识别区域，您可以按照需求调整参数
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density;
        mScreenWidth = getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getResources().getDisplayMetrics().heightPixels;
        // 当前D框的宽高是300dp
        final int SCAN_FRAME_SIZE = 238;
        int scanFrameSize = (int) (SCAN_FRAME_SIZE * density);
        Rect rect = new Rect();
        rect.left = mScreenWidth / 2 - scanFrameSize / 2;
        rect.right = mScreenWidth / 2 + scanFrameSize / 2;
        rect.top = mScreenHeight / 2 - scanFrameSize / 2;
        rect.bottom = mScreenHeight / 2 + scanFrameSize / 2;
        remoteView = new RemoteView
                .Builder()
                .setContext(this)
                .setBoundingBox(rect)
                .setFormat(HmsScan.ALL_SCAN_TYPE)
                .build();

        remoteView.setOnResultCallback(hmsScans -> {
            remoteView.pauseContinuouslyScan();
            if (hmsScans != null && hmsScans.length != 0) {
                backSuccess(hmsScans[0]);
            } else {
                remoteView.pauseContinuouslyScan();
            }
        });
        // 将自定义view加载到activity的frameLayout中
        remoteView.onCreate(savedInstanceState);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        frameLayout.addView(remoteView, params);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (remoteView != null)
            remoteView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (remoteView != null)
            remoteView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (remoteView != null)
            remoteView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (remoteView != null)
            remoteView.onDestroy();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (remoteView != null)
            remoteView.resumeContinuouslyScan();
    }
}
