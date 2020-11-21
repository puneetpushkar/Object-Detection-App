
package com.google.mlkit.vision.demo.java.barcodescanner;

import android.content.Context;
import android.graphics.Point;
import androidx.annotation.NonNull;
import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.java.VisionProcessorBase;
import java.util.List;
public class BarcodeScannerProcessor extends VisionProcessorBase<List<Barcode>> {

  private static final String TAG = "BarcodeProcessor";

  private final BarcodeScanner barcodeScanner;

  public BarcodeScannerProcessor(Context context) {
    super(context);
    barcodeScanner = BarcodeScanning.getClient();
  }

  @Override
  public void stop() {
    super.stop();
    barcodeScanner.close();
  }

  @Override
  protected Task<List<Barcode>> detectInImage(InputImage image) {
    return barcodeScanner.process(image);
  }

  @Override
  protected void onSuccess(
      @NonNull List<Barcode> barcodes, @NonNull GraphicOverlay graphicOverlay) {
    if (barcodes.isEmpty()) {
      Log.v(MANUAL_TESTING_LOG, "No barcode has been detected");
    }
    for (int i = 0; i < barcodes.size(); ++i) {
      Barcode barcode = barcodes.get(i);
      graphicOverlay.add(new BarcodeGraphic(graphicOverlay, barcode));
      logExtrasForTesting(barcode);
    }
  }

  private static void logExtrasForTesting(Barcode barcode) {
    if (barcode != null) {
      Log.v(
          MANUAL_TESTING_LOG,
          String.format(
              "Detected barcode's bounding box: %s", barcode.getBoundingBox().flattenToString()));
      Log.v(
          MANUAL_TESTING_LOG,
          String.format(
              "Expected corner point size is 4, get %d", barcode.getCornerPoints().length));
      for (Point point : barcode.getCornerPoints()) {
        Log.v(
            MANUAL_TESTING_LOG,
            String.format("Corner point is located at: x = %d, y = %d", point.x, point.y));
      }
      Log.v(MANUAL_TESTING_LOG, "barcode display value: " + barcode.getDisplayValue());
      Log.v(MANUAL_TESTING_LOG, "barcode raw value: " + barcode.getRawValue());
      Barcode.DriverLicense dl = barcode.getDriverLicense();
      if (dl != null) {
        Log.v(MANUAL_TESTING_LOG, "driver license city: " + dl.getAddressCity());
        Log.v(MANUAL_TESTING_LOG, "driver license state: " + dl.getAddressState());
        Log.v(MANUAL_TESTING_LOG, "driver license street: " + dl.getAddressStreet());
        Log.v(MANUAL_TESTING_LOG, "driver license zip code: " + dl.getAddressZip());
        Log.v(MANUAL_TESTING_LOG, "driver license birthday: " + dl.getBirthDate());
        Log.v(MANUAL_TESTING_LOG, "driver license document type: " + dl.getDocumentType());
        Log.v(MANUAL_TESTING_LOG, "driver license expiry date: " + dl.getExpiryDate());
        Log.v(MANUAL_TESTING_LOG, "driver license first name: " + dl.getFirstName());
        Log.v(MANUAL_TESTING_LOG, "driver license middle name: " + dl.getMiddleName());
        Log.v(MANUAL_TESTING_LOG, "driver license last name: " + dl.getLastName());
        Log.v(MANUAL_TESTING_LOG, "driver license gender: " + dl.getGender());
        Log.v(MANUAL_TESTING_LOG, "driver license issue date: " + dl.getIssueDate());
        Log.v(MANUAL_TESTING_LOG, "driver license issue country: " + dl.getIssuingCountry());
        Log.v(MANUAL_TESTING_LOG, "driver license number: " + dl.getLicenseNumber());
      }
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.e(TAG, "Barcode detection failed " + e);
  }
}
