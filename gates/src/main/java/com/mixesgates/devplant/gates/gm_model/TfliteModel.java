package com.mixesgates.devplant.gates.gm_model;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Image classifier run with LiteRT (TensorFlow Lite) from {@code assets/<name>.tflite}, with one
 * label per line in {@code assets/<name>.labels.txt}. Input is [1, H, W, 3] float32 scaled to 0..1
 * or uint8; output is [1, labels] probabilities, float32 or quantised uint8.
 */
public final class TfliteModel implements LeafModel {

    private static final String TAG = "TfliteModel";
    private static final int TOP = 5;

    /**
     * Opens a bundled model. When the asset is missing (no trained model shipped yet) it returns
     * {@link ColourHeuristicModel} so the app still works end to end.
     */
    public static LeafModel open(Context context, String name) {
        try {
            MappedByteBuffer buffer = map(context, name + ".tflite");
            List<String> labels = readLabels(context, name + ".labels.txt");
            return new TfliteModel(name, buffer, labels);
        } catch (IOException | RuntimeException e) {
            Log.w(TAG, "No usable model '" + name + "', using colour heuristic: " + e);
            return new ColourHeuristicModel();
        }
    }

    private final String name;
    private final Interpreter interpreter;
    private final List<String> labels;
    private final int width;
    private final int height;
    private final DataType inputType;
    private final Tensor output;

    private TfliteModel(String name, MappedByteBuffer buffer, List<String> labels) {
        this.name = name;
        this.labels = labels;
        interpreter = new Interpreter(buffer, new Interpreter.Options().setNumThreads(2));
        Tensor input = interpreter.getInputTensor(0);
        int[] shape = input.shape();
        if (shape.length != 4 || shape[3] != 3) {
            interpreter.close();
            throw new IllegalArgumentException("Expected [1,H,W,3] input, got " + shapeText(shape));
        }
        height = shape[1];
        width = shape[2];
        inputType = input.dataType();
        output = interpreter.getOutputTensor(0);
        int classes = output.shape()[output.shape().length - 1];
        if (classes != labels.size()) {
            interpreter.close();
            throw new IllegalArgumentException(classes + " outputs but " + labels.size() + " labels");
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isTrained() {
        return true;
    }

    @Override
    public int inputSize() {
        return width;
    }

    @Override
    public synchronized List<Label> classify(Bitmap square) {
        Bitmap bitmap = square.getWidth() == width && square.getHeight() == height
                ? square : Bitmap.createScaledBitmap(square, width, height, true);
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        ByteBuffer in;
        if (inputType == DataType.FLOAT32) {
            in = ByteBuffer.allocateDirect(4 * 3 * pixels.length).order(ByteOrder.nativeOrder());
            for (int p : pixels) {
                in.putFloat(((p >> 16) & 0xFF) / 255f);
                in.putFloat(((p >> 8) & 0xFF) / 255f);
                in.putFloat((p & 0xFF) / 255f);
            }
        } else if (inputType == DataType.UINT8) {
            in = ByteBuffer.allocateDirect(3 * pixels.length).order(ByteOrder.nativeOrder());
            for (int p : pixels) {
                in.put((byte) ((p >> 16) & 0xFF));
                in.put((byte) ((p >> 8) & 0xFF));
                in.put((byte) (p & 0xFF));
            }
        } else {
            throw new IllegalStateException("Unsupported input type " + inputType);
        }
        in.rewind();

        float[] scores = new float[labels.size()];
        if (output.dataType() == DataType.FLOAT32) {
            float[][] out = new float[1][labels.size()];
            interpreter.run(in, out);
            scores = out[0];
        } else if (output.dataType() == DataType.UINT8) {
            byte[][] out = new byte[1][labels.size()];
            interpreter.run(in, out);
            Tensor.QuantizationParams q = output.quantizationParams();
            float scale = q.getScale() == 0f ? 1f / 255f : q.getScale();
            for (int i = 0; i < scores.length; i++) {
                scores[i] = ((out[0][i] & 0xFF) - q.getZeroPoint()) * scale;
            }
        } else {
            throw new IllegalStateException("Unsupported output type " + output.dataType());
        }
        return top(labels, scores, TOP);
    }

    @Override
    public synchronized void close() {
        interpreter.close();
    }

    static List<Label> top(List<String> labels, float[] scores, int count) {
        List<Label> all = new ArrayList<>(scores.length);
        for (int i = 0; i < scores.length; i++) all.add(new Label(labels.get(i), scores[i]));
        all.sort((a, b) -> Float.compare(b.confidence, a.confidence));
        return new ArrayList<>(all.subList(0, Math.min(count, all.size())));
    }

    private static MappedByteBuffer map(Context context, String asset) throws IOException {
        try (AssetFileDescriptor fd = context.getAssets().openFd(asset);
             FileInputStream in = new FileInputStream(fd.getFileDescriptor());
             FileChannel channel = in.getChannel()) {
            return channel.map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
        }
    }

    private static List<String> readLabels(Context context, String asset) throws IOException {
        List<String> labels = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(context.getAssets().open(asset), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) labels.add(line);
            }
        }
        return labels;
    }

    private static String shapeText(int[] shape) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < shape.length; i++) sb.append(i == 0 ? "" : ",").append(shape[i]);
        return sb.append("]").toString();
    }
}
