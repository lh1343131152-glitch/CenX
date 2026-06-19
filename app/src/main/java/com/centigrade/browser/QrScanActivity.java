package com.centigrade.browser;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class QrScanActivity extends AppCompatActivity {

    public static final String EXTRA_SCAN_RESULT = "scan_result";

    private DecoratedBarcodeView barcodeView;
    private ImageView scanLine;
    private ImageButton btnFlashlight;
    private boolean flashOn = false;
    private String cameraId;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startScan();
                } else {
                    Toast.makeText(this, "未授予相机权限，无法扫码", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });

    private final ActivityResultLauncher<String[]> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (Exception ignored) {
                }
                decodeQrFromImage(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏沉浸式设置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
            int flags = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            getWindow().getDecorView().setSystemUiVisibility(flags);
            // 深色背景使用白色状态栏图标（不设置LIGHT_STATUS_BAR）
        }

        setContentView(R.layout.activity_qr_scan);

        // 标题栏嵌入状态栏高度
        View topBar = findViewById(R.id.top_bar);
        int statusBarHeight = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
        topBar.setPadding(
                topBar.getPaddingStart(),
                statusBarHeight,
                topBar.getPaddingEnd(),
                topBar.getPaddingBottom()
        );

        barcodeView = findViewById(R.id.barcode_scanner);
        scanLine = findViewById(R.id.scan_line);
        ImageButton btnBack = findViewById(R.id.btn_scan_back);
        TextView btnAlbum = findViewById(R.id.btn_scan_album);
        btnFlashlight = findViewById(R.id.btn_flashlight);

        // 返回按钮
        btnBack.setOnClickListener(v -> finish());

        // 相册识别按钮
        btnAlbum.setOnClickListener(v -> {
            hapticFeedback(v);
            pickImageLauncher.launch(new String[]{"image/*"});
        });

        // 闪光灯按钮
        btnFlashlight.setOnClickListener(v -> {
            hapticFeedback(v);
            toggleFlashlight();
            v.setSelected(flashOn);
        });

        // 配置扫码格式
        barcodeView.getBarcodeView().setDecoderFactory(
                new DefaultDecoderFactory(java.util.Arrays.asList(
                        BarcodeFormat.QR_CODE,
                        BarcodeFormat.EAN_13,
                        BarcodeFormat.EAN_8,
                        BarcodeFormat.UPC_A,
                        BarcodeFormat.UPC_E,
                        BarcodeFormat.CODE_128,
                        BarcodeFormat.CODE_39,
                        BarcodeFormat.CODE_93,
                        BarcodeFormat.ITF,
                        BarcodeFormat.CODABAR,
                        BarcodeFormat.DATA_MATRIX,
                        BarcodeFormat.AZTEC,
                        BarcodeFormat.PDF_417
                ))
        );

        // 检测闪光灯支持
        checkFlashlightSupport();

        ensureCameraPermissionAndStart();
    }

    private void checkFlashlightSupport() {
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            if (cameraManager != null) {
                for (String id : cameraManager.getCameraIdList()) {
                    CameraCharacteristics chars = cameraManager.getCameraCharacteristics(id);
                    Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
                    if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        cameraId = id;
                        Boolean hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                        if (hasFlash != null && hasFlash) {
                            findViewById(R.id.btn_flashlight).setVisibility(View.VISIBLE);
                        }
                        break;
                    }
                }
            }
        } catch (CameraAccessException ignored) {
        }
    }

    private void toggleFlashlight() {
        if (cameraId == null) return;
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
            if (cameraManager != null) {
                flashOn = !flashOn;
                cameraManager.setTorchMode(cameraId, flashOn);
                btnFlashlight.setImageResource(flashOn ? R.drawable.ic_flashlight_off : R.drawable.ic_flashlight);
                btnFlashlight.setContentDescription(flashOn ? "关闭闪光灯" : "开启闪光灯");
            }
        } catch (Exception ignored) {
        }
    }

    private void hapticFeedback(View view) {
        view.performHapticFeedback(
                android.view.HapticFeedbackConstants.CONTEXT_CLICK,
                android.view.HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
        );
    }

    private void ensureCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startScan();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void startScan() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            private boolean handled = false;

            @Override
            public void barcodeResult(BarcodeResult result) {
                if (handled || result == null || result.getText() == null) return;
                handled = true;

                // 震动反馈
                try {
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE));
                        } else {
                            vibrator.vibrate(80);
                        }
                    }
                } catch (Exception ignored) {
                }

                Intent data = new Intent();
                data.putExtra(EXTRA_SCAN_RESULT, result.getText());
                setResult(RESULT_OK, data);
                finish();
            }
        });
        barcodeView.resume();

        // 启动扫描线动画
        if (scanLine != null) {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.scan_line_move);
            scanLine.startAnimation(anim);
        }
    }

    // ====== 相册识别部分 ======

    private void decodeQrFromImage(Uri uri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                    decoder.setMutableRequired(false);
                    int w = info.getSize().getWidth();
                    int h = info.getSize().getHeight();
                    int max = Math.max(w, h);
                    if (max > 2200) {
                        decoder.setTargetSampleSize(Math.max(1, max / 1400));
                    }
                });
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                if (bitmap != null) {
                    int max = Math.max(bitmap.getWidth(), bitmap.getHeight());
                    if (max > 2200) {
                        float scale = 1400f / max;
                        bitmap = Bitmap.createScaledBitmap(
                                bitmap,
                                Math.max(1, Math.round(bitmap.getWidth() * scale)),
                                Math.max(1, Math.round(bitmap.getHeight() * scale)),
                                true
                        );
                    }
                }
            }

            if (bitmap == null) {
                Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                return;
            }

            String text = tryDecodeBitmap(bitmap);

            if ((text == null || text.trim().isEmpty()) && bitmap.getWidth() > 900 && bitmap.getHeight() > 900) {
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, true);
                text = tryDecodeBitmap(scaled);
            }

            if ((text == null || text.trim().isEmpty()) && bitmap.getWidth() > 500 && bitmap.getHeight() > 500) {
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 3, bitmap.getHeight() / 3, true);
                text = tryDecodeBitmap(scaled);
            }

            if (text != null && !text.trim().isEmpty()) {
                Intent data = new Intent();
                data.putExtra(EXTRA_SCAN_RESULT, text);
                setResult(RESULT_OK, data);
                finish();
            } else {
                Toast.makeText(this, "未识别到二维码", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "相册识别失败", Toast.LENGTH_SHORT).show();
        }
    }

    private String tryDecodeBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return null;

        String result = decodeBitmapWithHints(bitmap, false, false);
        if (result != null) return result;

        result = decodeBitmapWithHints(bitmap, true, false);
        if (result != null) return result;

        result = decodeBitmapWithHints(bitmap, false, true);
        if (result != null) return result;

        result = decodeBitmapWithHints(bitmap, true, true);
        if (result != null) return result;

        Bitmap rotated90 = rotateBitmap(bitmap, 90f);
        result = decodeBitmapWithHints(rotated90, false, false);
        if (result != null) return result;
        result = decodeBitmapWithHints(rotated90, true, false);
        if (result != null) return result;

        Bitmap rotated180 = rotateBitmap(bitmap, 180f);
        result = decodeBitmapWithHints(rotated180, false, false);
        if (result != null) return result;

        Bitmap rotated270 = rotateBitmap(bitmap, 270f);
        result = decodeBitmapWithHints(rotated270, false, false);
        if (result != null) return result;

        return null;
    }

    private String decodeBitmapWithHints(Bitmap bitmap, boolean invert, boolean histogram) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            for (int i = 0; i < pixels.length; i++) {
                int c = pixels[i];
                int r = (c >> 16) & 0xff;
                int g = (c >> 8) & 0xff;
                int b = c & 0xff;
                int gray = (r * 38 + g * 75 + b * 15) >> 7;
                if (invert) gray = 255 - gray;
                pixels[i] = 0xFF000000 | (gray << 16) | (gray << 8) | gray;
            }

            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(
                    histogram ? new GlobalHistogramBinarizer(source) : new HybridBinarizer(source)
            );

            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");

            MultiFormatReader reader = new MultiFormatReader();
            reader.setHints(hints);
            Result result = reader.decode(binaryBitmap);
            if (result != null && result.getText() != null && !result.getText().trim().isEmpty()) {
                return result.getText();
            }
        } catch (Exception ignored) {
        }

        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            RGBLuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(
                    histogram ? new GlobalHistogramBinarizer(source) : new HybridBinarizer(source)
            );

            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

            Result[] results = new QRCodeMultiReader().decodeMultiple(binaryBitmap, hints);
            if (results != null && results.length > 0 && results[0] != null) {
                String text = results[0].getText();
                if (text != null && !text.trim().isEmpty()) {
                    return text;
                }
            }
        } catch (NotFoundException ignored) {
        } catch (Exception ignored) {
        }

        return null;
    }

    private Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        try {
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        } catch (Exception e) {
            return bitmap;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeView != null) {
            barcodeView.resume();
            // 恢复扫描线动画
            if (scanLine != null && scanLine.getAnimation() == null) {
                Animation anim = AnimationUtils.loadAnimation(this, R.anim.scan_line_move);
                scanLine.startAnimation(anim);
            }
        }
    }

    @Override
    protected void onPause() {
        if (barcodeView != null) {
            barcodeView.pause();
        }
        // 停止扫描线动画
        if (scanLine != null) {
            scanLine.clearAnimation();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        // 关闭闪光灯
        if (flashOn && cameraId != null) {
            try {
                CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
                if (cameraManager != null) {
                    cameraManager.setTorchMode(cameraId, false);
                }
            } catch (Exception ignored) {
            }
        }
        super.onDestroy();
    }
}