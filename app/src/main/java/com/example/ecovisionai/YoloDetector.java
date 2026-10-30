
package com.example.ecovisionai;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class YoloDetector {

    private static final int SIZE = 640;
    private static final int CLASSES = 80;
    private static final int CANDIDATES = 8400;
    private static final float CONFIDENCE = 0.25f;
    private static final float IOU = 0.45f;

    private final Interpreter interpreter;

    private static final String[] LABELS = {
            "person", "bicycle", "car", "motorcycle", "airplane",
            "bus", "train", "truck", "boat", "traffic light",
            "fire hydrant", "stop sign", "parking meter", "bench",
            "bird", "cat", "dog", "horse", "sheep", "cow",
            "elephant", "bear", "zebra", "giraffe", "backpack",
            "umbrella", "handbag", "tie", "suitcase", "frisbee",
            "skis", "snowboard", "sports ball", "kite", "baseball bat",
            "baseball glove", "skateboard", "surfboard", "tennis racket",
            "bottle", "wine glass", "cup", "fork", "knife", "spoon",
            "bowl", "banana", "apple", "sandwich", "orange",
            "broccoli", "carrot", "hot dog", "pizza", "donut",
            "cake", "chair", "couch", "potted plant", "bed",
            "dining table", "toilet", "tv", "laptop", "mouse",
            "remote", "keyboard", "cell phone", "microwave", "oven",
            "toaster", "sink", "refrigerator", "book", "clock",
            "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
    };

    public static class Detection {
        public final RectF box;
        public final String label;
        public final float confidence;

        public Detection(RectF box, String label, float confidence) {
            this.box = box;
            this.label = label;
            this.confidence = confidence;
        }
    }

    public YoloDetector(Context context) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(4);

        interpreter = new Interpreter(loadModel(context), options);

        android.util.Log.d("EcoVisionAI",
                "Model loaded. Input: " +
                        java.util.Arrays.toString(
                                interpreter.getInputTensor(0).shape()));

        android.util.Log.d("EcoVisionAI",
                "Output: " +
                        java.util.Arrays.toString(
                                interpreter.getOutputTensor(0).shape()));
    }

    private MappedByteBuffer loadModel(Context context)
            throws IOException {

        try (AssetFileDescriptor descriptor =
                     context.getAssets().openFd("yolov8n_float32.tflite");
             FileInputStream stream =
                     new FileInputStream(descriptor.getFileDescriptor())) {

            FileChannel channel = stream.getChannel();

            return channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    descriptor.getStartOffset(),
                    descriptor.getDeclaredLength()
            );
        }
    }

    public List<Detection> detect(Bitmap original) {

        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // YOLO letterbox preprocessing
        float scale = Math.min(
                (float) SIZE / originalWidth,
                (float) SIZE / originalHeight
        );

        int resizedWidth = Math.round(originalWidth * scale);
        int resizedHeight = Math.round(originalHeight * scale);

        int padX = (SIZE - resizedWidth) / 2;
        int padY = (SIZE - resizedHeight) / 2;

        Bitmap resized = Bitmap.createScaledBitmap(
                original, resizedWidth, resizedHeight, true
        );

        Bitmap padded = Bitmap.createBitmap(
                SIZE, SIZE, Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(padded);
        canvas.drawColor(Color.rgb(114, 114, 114));
        canvas.drawBitmap(resized, padX, padY, null);

        // NCHW input: [1, 3, 640, 640]
        ByteBuffer input = ByteBuffer.allocateDirect(
                4 * 3 * SIZE * SIZE
        );
        input.order(ByteOrder.nativeOrder());

        int[] pixels = new int[SIZE * SIZE];
        padded.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE);

        for (int channel = 0; channel < 3; channel++) {
            for (int pixel : pixels) {

                float value;

                if (channel == 0) {
                    value = ((pixel >> 16) & 0xFF) / 255.0f;
                } else if (channel == 1) {
                    value = ((pixel >> 8) & 0xFF) / 255.0f;
                } else {
                    value = (pixel & 0xFF) / 255.0f;
                }

                input.putFloat(value);
            }
        }

        input.rewind();

        // Output: [1, 84, 8400]
        float[][][] output = new float[1][84][CANDIDATES];

        interpreter.run(input, output);

        List<Detection> detections = new ArrayList<>();

        // Decode predictions
        for (int i = 0; i < CANDIDATES; i++) {

            float bestScore = 0;
            int bestClass = -1;

            for (int c = 0; c < CLASSES; c++) {
                float score = output[0][c + 4][i];

                if (score > bestScore) {
                    bestScore = score;
                    bestClass = c;
                }
            }

            if (bestScore < CONFIDENCE || bestClass < 0) {
                continue;
            }

            float centerX = output[0][0][i];
            float centerY = output[0][1][i];
            float width = output[0][2][i];
            float height = output[0][3][i];

            // Convert letterbox coordinates to original image
            float left = (centerX - width / 2f - padX) / scale;
            float top = (centerY - height / 2f - padY) / scale;
            float right = (centerX + width / 2f - padX) / scale;
            float bottom = (centerY + height / 2f - padY) / scale;

            left = Math.max(0, Math.min(originalWidth, left));
            top = Math.max(0, Math.min(originalHeight, top));
            right = Math.max(0, Math.min(originalWidth, right));
            bottom = Math.max(0, Math.min(originalHeight, bottom));

            if (right <= left || bottom <= top) {
                continue;
            }

            detections.add(new Detection(
                    new RectF(left, top, right, bottom),
                    LABELS[bestClass],
                    bestScore
            ));
        }

        return applyNms(detections);
    }

    private List<Detection> applyNms(List<Detection> detections) {

        Collections.sort(detections,
                (a, b) -> Float.compare(b.confidence, a.confidence));

        List<Detection> result = new ArrayList<>();

        for (Detection candidate : detections) {

            boolean suppressed = false;

            for (Detection selected : result) {

                if (candidate.label.equals(selected.label)
                        && calculateIou(candidate.box, selected.box) > IOU) {
                    suppressed = true;
                    break;
                }
            }

            if (!suppressed) {
                result.add(candidate);
            }
        }

        return result;
    }

    private float calculateIou(RectF a, RectF b) {

        float left = Math.max(a.left, b.left);
        float top = Math.max(a.top, b.top);
        float right = Math.min(a.right, b.right);
        float bottom = Math.min(a.bottom, b.bottom);

        float intersection = Math.max(0, right - left)
                * Math.max(0, bottom - top);

        float union = a.width() * a.height()
                + b.width() * b.height() - intersection;

        return union <= 0 ? 0 : intersection / union;
    }

    public void close() {
        interpreter.close();
    }
}
