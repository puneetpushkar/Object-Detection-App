package com.google.mlkit.vision.demo.java.labeldetector;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.java.VisionProcessorBase;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabelerOptionsBase;
import com.google.mlkit.vision.label.ImageLabeling;
import java.io.IOException;
import java.util.List;

public class LabelDetectorProcessor extends VisionProcessorBase<List<ImageLabel>> {

  private static final String TAG = "LabelDetectorProcessor";

  private final ImageLabeler imageLabeler;

  public LabelDetectorProcessor(Context context, ImageLabelerOptionsBase options) {
    super(context);
    imageLabeler = ImageLabeling.getClient(options);
  }

  @Override
  public void stop() {
    super.stop();
    try {
      imageLabeler.close();
    } catch (IOException e) {
      Log.e(TAG, "Exception thrown while trying to close ImageLabelerClient: " + e);
    }
  }

  @Override
  protected Task<List<ImageLabel>> detectInImage(InputImage image) {
    return imageLabeler.process(image);
  }

  @Override
  protected void onSuccess(
      @NonNull List<ImageLabel> labels, @NonNull GraphicOverlay graphicOverlay) {
    graphicOverlay.add(new LabelGraphic(graphicOverlay, labels));
    logExtrasForTesting(labels);
  }

  private static void logExtrasForTesting(List<ImageLabel> labels) {
    if (labels == null) {
      Log.v(MANUAL_TESTING_LOG, "No labels detected");
    } else {
      for (ImageLabel label : labels) {
        Log.v(
            MANUAL_TESTING_LOG,
            String.format("Label %s, confidence %f", label.getText(), label.getConfidence()));
      }
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.w(TAG, "Label detection failed." + e);
  }
}

