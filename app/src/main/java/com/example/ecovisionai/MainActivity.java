
package com.example.ecovisionai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private Button btnGallery;
    private Button btnCamera;
    private TextView txtStatus;

    private PreviewView cameraPreview;
    private DetectionOverlay detectionOverlay;

    private YoloDetector detector;

    private ExecutorService inferenceExecutor;

    private boolean cameraRunning = false;

    private ActivityResultLauncher<String> galleryLauncher;
    private ActivityResultLauncher<String> cameraPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnGallery = findViewById(R.id.btnGallery);
        btnCamera = findViewById(R.id.btnCamera);
        txtStatus = findViewById(R.id.txtStatus);

        cameraPreview = findViewById(R.id.cameraPreview);
        detectionOverlay = findViewById(R.id.detectionOverlay);

        cameraPreview.setScaleType(
                PreviewView.ScaleType.FIT_CENTER
        );

        inferenceExecutor = Executors.newSingleThreadExecutor();

        try {
            detector = new YoloDetector(this);
            txtStatus.setText("AI model loaded successfully.");
        } catch (Exception e) {
            txtStatus.setText("Model loading failed: " + e.getMessage());
            btnGallery.setEnabled(false);
            btnCamera.setEnabled(false);
            return;
        }

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        loadGalleryImage(uri);
                    }
                }
        );

        cameraPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        startCamera();
                    } else {
                        txtStatus.setText("Camera permission denied.");
                    }
                }
        );

        btnGallery.setOnClickListener(v -> {
            stopCamera();
            galleryLauncher.launch("image/*");
        });

        btnCamera.setOnClickListener(v -> {
            if (cameraRunning) {
                stopCamera();
            } else {
                checkCameraPermission();
            }
        });
    }

    // ---------------- GALLERY ----------------

    private void loadGalleryImage(android.net.Uri uri) {

        txtStatus.setText("Loading image...");
        detectionOverlay.clear();

        inferenceExecutor.execute(() -> {

            try {
                Bitmap bitmap;

                try (InputStream stream =
                             getContentResolver().openInputStream(uri)) {

                    bitmap = BitmapFactory.decodeStream(stream);
                }

                if (bitmap == null) {
                    runOnUiThread(() ->
                            txtStatus.setText("Unable to read image.")
                    );
                    return;
                }

                List<YoloDetector.Detection> results =
                        detector.detect(bitmap);

                runOnUiThread(() -> {

                    cameraPreview.setVisibility(View.GONE);
                    detectionOverlay.setVisibility(View.VISIBLE);

                    detectionOverlay.showImage(bitmap, results);

                    txtStatus.setText(
                            "Detection completed. Objects found: "
                                    + results.size()
                    );
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        txtStatus.setText(
                                "Detection error: " + e.getMessage()
                        )
                );
            }
        });
    }

    // ---------------- CAMERA PERMISSION ----------------

    private void checkCameraPermission() {

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {

            startCamera();

        } else {

            cameraPermissionLauncher.launch(
                    Manifest.permission.CAMERA
            );
        }
    }

    // ---------------- START CAMERA ----------------

    private void startCamera() {

        txtStatus.setText("Starting camera...");

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {

            try {

                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();

                preview.setSurfaceProvider(
                        cameraPreview.getSurfaceProvider()
                );

                ImageAnalysis analysis =
                        new ImageAnalysis.Builder()
                                .setTargetResolution(new Size(640, 640))
                                .setBackpressureStrategy(
                                        ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                                )
                                .setOutputImageFormat(
                                        ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888
                                )
                                .build();

                analysis.setAnalyzer(
                        inferenceExecutor,
                        this::analyzeCameraFrame
                );

                provider.unbindAll();

                provider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                );

                cameraPreview.setVisibility(View.VISIBLE);
                detectionOverlay.setVisibility(View.VISIBLE);

                cameraRunning = true;
                btnCamera.setText("Stop Live Camera");
                txtStatus.setText("Live object detection is running.");

            } catch (Exception e) {

                txtStatus.setText(
                        "Camera error: " + e.getMessage()
                );
            }

        }, ContextCompat.getMainExecutor(this));
    }

    // ---------------- CAMERA IMAGE ANALYSIS ----------------

    private void analyzeCameraFrame(@NonNull ImageProxy image) {

        try {

            Bitmap bitmap = convertImageProxy(image);

            if (bitmap == null) {
                return;
            }

            List<YoloDetector.Detection> results =
                    detector.detect(bitmap);

            runOnUiThread(() -> {

                if (!cameraRunning) {
                    return;
                }

                detectionOverlay.showCameraDetections(
                        bitmap.getWidth(),
                        bitmap.getHeight(),
                        results
                );

                txtStatus.setText(
                        "Live detection | Objects: " + results.size()
                );
            });

        } catch (Exception e) {

            android.util.Log.e(
                    "EcoVisionAI",
                    "Camera analysis error",
                    e
            );

        } finally {
            image.close();
        }
    }

    // Convert YUV_420_888 camera frame into RGB Bitmap
    private Bitmap convertImageProxy(ImageProxy image) {

        int width = image.getWidth();
        int height = image.getHeight();

        ImageProxy.PlaneProxy[] planes = image.getPlanes();

        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int yRowStride = planes[0].getRowStride();
        int uvRowStride = planes[1].getRowStride();
        int uvPixelStride = planes[1].getPixelStride();

        int[] pixels = new int[width * height];

        for (int y = 0; y < height; y++) {

            for (int x = 0; x < width; x++) {

                int yIndex = y * yRowStride + x;

                int uvIndex = (y / 2) * uvRowStride
                        + (x / 2) * uvPixelStride;

                int yValue = yBuffer.get(yIndex) & 0xFF;
                int uValue = uBuffer.get(uvIndex) & 0xFF;
                int vValue = vBuffer.get(uvIndex) & 0xFF;

                int y1192 = 1192 * Math.max(0, yValue - 16);
                int uOffset = uValue - 128;
                int vOffset = vValue - 128;

                int r = (y1192 + 1634 * vOffset) >> 10;
                int g = (y1192 - 833 * vOffset
                        - 400 * uOffset) >> 10;
                int b = (y1192 + 2066 * uOffset) >> 10;

                r = Math.max(0, Math.min(255, r));
                g = Math.max(0, Math.min(255, g));
                b = Math.max(0, Math.min(255, b));

                pixels[y * width + x] =
                        0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(
                pixels, width, height, Bitmap.Config.ARGB_8888
        );

        int rotation = image.getImageInfo().getRotationDegrees();

        if (rotation != 0) {

            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);

            Bitmap rotated = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true
            );

            bitmap.recycle();
            bitmap = rotated;
        }

        return bitmap;
    }

    // ---------------- STOP CAMERA ----------------

    private void stopCamera() {

        cameraRunning = false;

        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {

            try {
                future.get().unbindAll();
            } catch (Exception e) {
                android.util.Log.e(
                        "EcoVisionAI",
                        "Camera stop error",
                        e
                );
            }

        }, ContextCompat.getMainExecutor(this));

        cameraPreview.setVisibility(View.GONE);
        detectionOverlay.clear();
        btnCamera.setText("Start Live Camera");

        txtStatus.setText("Camera stopped.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (inferenceExecutor != null) {
            inferenceExecutor.shutdown();
        }

        if (detector != null) {
            detector.close();
        }
    }
}
