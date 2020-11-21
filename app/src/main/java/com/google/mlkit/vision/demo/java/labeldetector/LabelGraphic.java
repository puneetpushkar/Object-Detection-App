package com.google.mlkit.vision.demo.java.labeldetector;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.google.common.primitives.Floats;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.label.ImageLabel;
import java.util.List;
import java.util.Locale;
public class LabelGraphic extends GraphicOverlay.Graphic {

  private static final float TEXT_SIZE = 70.0f;
  private static final String LABEL_FORMAT = "%.2f%% confidence (index: %d)";

  private final Paint textPaint;
  private final Paint labelPaint;
  private final GraphicOverlay overlay;

  private final List<ImageLabel> labels;

  public LabelGraphic(GraphicOverlay overlay, List<ImageLabel> labels) {
    super(overlay);
    this.overlay = overlay;
    this.labels = labels;
    textPaint = new Paint();
    textPaint.setColor(Color.WHITE);
    textPaint.setTextSize(TEXT_SIZE);

    labelPaint = new Paint();
    labelPaint.setColor(Color.BLACK);
    labelPaint.setStyle(Paint.Style.FILL);
    labelPaint.setAlpha(200);
  }

  @Override
  public synchronized void draw(Canvas canvas) {
    // First try to find maxWidth and totalHeight in order to draw to the center of the screen.
    float maxWidth = 0;
    float totalHeight = labels.size() * 2 * TEXT_SIZE;
    for (ImageLabel label : labels) {
      float line1Width = textPaint.measureText(label.getText());
      float line2Width =
          textPaint.measureText(
              String.format(
                  Locale.US, LABEL_FORMAT, label.getConfidence() * 100, label.getIndex()));
      maxWidth = Floats.max(maxWidth, line1Width, line2Width);
    }
    float x = Math.max(0, overlay.getWidth() / 2.0f - maxWidth / 2.0f);
    float y = Math.max(200, overlay.getHeight() / 2.0f - totalHeight / 2.0f);

    if (!labels.isEmpty()) {
      float padding = 20;
      canvas.drawRect(
          x - padding, y - padding, x + maxWidth + padding, y + totalHeight + padding, labelPaint);
    }

    for (ImageLabel label : labels) {
      if (y + TEXT_SIZE * 2 > overlay.getHeight()) {
        break;
      }
      canvas.drawText(label.getText(), x, y + TEXT_SIZE, textPaint);
      y += TEXT_SIZE;
      canvas.drawText(
          String.format(Locale.US, LABEL_FORMAT, label.getConfidence() * 100, label.getIndex()),
          x,
          y + TEXT_SIZE,
          textPaint);
      y += TEXT_SIZE;
    }
  }
}
