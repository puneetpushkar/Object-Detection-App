
package com.google.mlkit.vision.demo.java;

import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build.VERSION_CODES;
import android.os.SystemClock;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageProxy;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskExecutors;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.demo.BitmapUtils;
import com.google.mlkit.vision.demo.CameraImageGraphic;
import com.google.mlkit.vision.demo.FrameMetadata;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.InferenceInfoGraphic;
import com.google.mlkit.vision.demo.ScopedExecutor;
import com.google.mlkit.vision.demo.VisionImageProcessor;
import com.google.mlkit.vision.demo.preference.PreferenceUtils;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;
public abstract class VisionProcessorBase<T> implements VisionImageProcessor {

  protected static final String MANUAL_TESTING_LOG = "LogTagForTest";
  private static final String TAG = "VisionProcessorBase";

  private final ActivityManager activityManager;
  private final Timer fpsTimer = new Timer();
  private final ScopedExecutor executor;

  private boolean isShutdown;
  private int numRuns = 0;
  private long totalRunMs = 0;
  private long maxRunMs = 0;
  private long minRunMs = Long.MAX_VALUE;
  private int frameProcessedInOneSecondInterval = 0;
  private int framesPerSecond = 0;

  // To keep the latest images and its metadata.
  @GuardedBy("this")
  private ByteBuffer latestImage;

  @GuardedBy("this")
  private FrameMetadata latestImageMetaData;
  // To keep the images and metadata in process.
  @GuardedBy("this")
  private ByteBuffer processingImage;

  @GuardedBy("this")
  private FrameMetadata processingMetaData;

  protected VisionProcessorBase(Context context) {
    activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    executor = new ScopedExecutor(TaskExecutors.MAIN_THREAD);
    fpsTimer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            framesPerSecond = frameProcessedInOneSecondInterval;
            frameProcessedInOneSecondInterval = 0;
          }
        },
        /* delay= */ 0,
        /* period= */ 1000);
  }

  // -----------------Code for processing single still image----------------------------------------
  @Override
  public void processBitmap(Bitmap bitmap, final GraphicOverlay graphicOverlay) {
    requestDetectInImage(
        InputImage.fromBitmap(bitmap, 0),
        graphicOverlay,
        /* originalCameraImage= */ null,
        /* shouldShowFps= */ false);
  }
  @Override
  public synchronized void processByteBuffer(
      ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay graphicOverlay) {
    latestImage = data;
    latestImageMetaData = frameMetadata;
    if (processingImage == null && processingMetaData == null) {
      processLatestImage(graphicOverlay);
    }
  }

  private synchronized void processLatestImage(final GraphicOverlay graphicOverlay) {
    processingImage = latestImage;
    processingMetaData = latestImageMetaData;
    latestImage = null;
    latestImageMetaData = null;
    if (processingImage != null && processingMetaData != null && !isShutdown) {
      processImage(processingImage, processingMetaData, graphicOverlay);
    }
  }

  private void processImage(
      ByteBuffer data, final FrameMetadata frameMetadata, final GraphicOverlay graphicOverlay) {
    Bitmap bitmap =
        PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.getContext())
            ? null
            : BitmapUtils.getBitmap(data, frameMetadata);

    requestDetectInImage(
            InputImage.fromByteBuffer(
                data,
                frameMetadata.getWidth(),
                frameMetadata.getHeight(),
                frameMetadata.getRotation(),
                InputImage.IMAGE_FORMAT_NV21),
            graphicOverlay,
            bitmap,
            /* shouldShowFps= */ true)
        .addOnSuccessListener(executor, results -> processLatestImage(graphicOverlay));
  }
  @Override
  @RequiresApi(VERSION_CODES.KITKAT)
  @ExperimentalGetImage
  public void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay) {
    if (isShutdown) {
      image.close();
      return;
    }

    Bitmap bitmap = null;
    if (!PreferenceUtils.isCameraLiveViewportEnabled(graphicOverlay.getContext())) {
      bitmap = BitmapUtils.getBitmap(image);
    }

    requestDetectInImage(
            InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees()),
            graphicOverlay,
            /* originalCameraImage= */ bitmap,
            /* shouldShowFps= */ true)
        .addOnCompleteListener(results -> image.close());
  }
  private Task<T> requestDetectInImage(
      final InputImage image,
      final GraphicOverlay graphicOverlay,
      @Nullable final Bitmap originalCameraImage,
      boolean shouldShowFps) {
    final long startMs = SystemClock.elapsedRealtime();
    return detectInImage(image)
        .addOnSuccessListener(
            executor,
            results -> {
              long currentLatencyMs = SystemClock.elapsedRealtime() - startMs;
              numRuns++;
              frameProcessedInOneSecondInterval++;
              totalRunMs += currentLatencyMs;
              maxRunMs = Math.max(currentLatencyMs, maxRunMs);
              minRunMs = Math.min(currentLatencyMs, minRunMs);
              if (frameProcessedInOneSecondInterval == 1) {
                Log.d(TAG, "Max latency is: " + maxRunMs);
                Log.d(TAG, "Min latency is: " + minRunMs);
                Log.d(TAG, "Num of Runs: " + numRuns + ", Avg latency is: " + totalRunMs / numRuns);
                MemoryInfo mi = new MemoryInfo();
                activityManager.getMemoryInfo(mi);
                long availableMegs = mi.availMem / 0x100000L;
                Log.d(TAG, "Memory available in system: " + availableMegs + " MB");
              }

              graphicOverlay.clear();
              if (originalCameraImage != null) {
                graphicOverlay.add(new CameraImageGraphic(graphicOverlay, originalCameraImage));
              }
              graphicOverlay.add(
                  new InferenceInfoGraphic(
                      graphicOverlay, currentLatencyMs, shouldShowFps ? framesPerSecond : null));
              VisionProcessorBase.this.onSuccess(results, graphicOverlay);
              graphicOverlay.postInvalidate();
            })
        .addOnFailureListener(
            executor,
            e -> {
              graphicOverlay.clear();
              graphicOverlay.postInvalidate();
              String error = "Failed to process. Error: " + e.getLocalizedMessage();
              Toast.makeText(
                      graphicOverlay.getContext(),
                      error + "\nCause: " + e.getCause(),
                      Toast.LENGTH_SHORT)
                  .show();
              Log.d(TAG, error);
              e.printStackTrace();
              VisionProcessorBase.this.onFailure(e);
            });
  }

  @Override
  public void stop() {
    executor.shutdown();
    isShutdown = true;
    numRuns = 0;
    totalRunMs = 0;
    fpsTimer.cancel();
  }

  protected abstract Task<T> detectInImage(InputImage image);

  protected abstract void onSuccess(@NonNull T results, @NonNull GraphicOverlay graphicOverlay);

  protected abstract void onFailure(@NonNull Exception e);
}
