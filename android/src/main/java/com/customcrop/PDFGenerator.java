package com.customcrop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfDocument.Page;
import android.graphics.pdf.PdfDocument.PageInfo;
import android.graphics.pdf.PdfDocument.PageInfo.Builder;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import static java.lang.String.format;


public class PDFGenerator {
  private final ReactApplicationContext reactContext;

  public PDFGenerator(ReactApplicationContext reactContext) {
    this.reactContext = reactContext;
  }

  public void createPDFbyImages(ReadableMap options, final Promise promise) {
    ReadableArray images = options.getArray("imagePaths");
    String documentName = options.getString("name");

    ReadableMap maxSize = options.getMap("maxSize");
    int maxHeight = maxSize.getInt("height");
    int maxWidth = maxSize.getInt("width");

    int quality = (int)Math.round(100 * options.getDouble("quality"));

    PdfDocument document = new PdfDocument();
    try {
      for (int idx = 0; idx < images.size(); idx++) {
        // get image
        Bitmap bmp = getImageFromFile(images.getString(idx));

        // calculate quality
        // int autoQuality = this.getQuality(bmp, maxWidth, maxHeight);

        // resize
        bmp = resize(bmp, maxWidth, maxHeight);

        // compress
        bmp = compress(bmp, quality);

        PageInfo pageInfo = new Builder(maxWidth, maxHeight, 1).create();

        // start a page
        Page page = document.startPage(pageInfo);

        // add image to page
        Canvas canvas = page.getCanvas();
        canvas.drawBitmap(bmp, 0, 0, null);

        document.finishPage(page);
      }

      // write the document content
      File targetPath = reactContext.getExternalFilesDir(null);
      File filePath = new File(targetPath, documentName);

      document.writeTo(new FileOutputStream(filePath));

      WritableMap resultMap = Arguments.createMap();
      resultMap.putString("filePath", filePath.getAbsolutePath());
      promise.resolve(resultMap);
    } catch (Exception e) {
      promise.reject("failed", e);
    }

    // close the document
    document.close();
  }

  private Bitmap getImageFromFile(String path) throws IOException {
    if (path.startsWith("content://")) {
      return getImageFromContentResolver(path);
    }

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inScaled = false;
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    return BitmapFactory.decodeFile(path, options);
  }

  private Bitmap getImageFromContentResolver(String path) throws IOException {
    ParcelFileDescriptor parcelFileDescriptor = reactContext.getContentResolver().openFileDescriptor(Uri.parse(path), "r");
    FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
    Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
    parcelFileDescriptor.close();
    return image;
  }

  private int getQuality(Bitmap bitmap, int maxWidth, int maxHeight) {
    if (maxWidth == 0 || maxHeight == 0) return 100;
    if (bitmap.getWidth() <= maxWidth && bitmap.getHeight() <= maxHeight) return 100;

    double ratio = maxWidth / bitmap.getWidth();
    int quality = (int) Math.round(100 / ratio);
    
    return quality > 99? 99: quality;
  }

  private Bitmap resize(Bitmap bitmap, int maxWidth, int maxHeight) {
    if (maxWidth == 0 || maxHeight == 0) return bitmap;
    if (bitmap.getWidth() <= maxWidth && bitmap.getHeight() <= maxHeight) return bitmap;

    double aspectRatio = (double) bitmap.getHeight() / bitmap.getWidth();
    int height = Math.round(maxWidth * aspectRatio) < maxHeight ? (int) Math.round(maxWidth * aspectRatio) : maxHeight;
    int width = (int) Math.round(height / aspectRatio);

    return Bitmap.createScaledBitmap(bitmap, width, height, true);
  }

  private Bitmap compress(Bitmap bmp, int quality) throws IOException {
    if (quality <= 0 || quality >= 100) return bmp;

    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream);
    byte[] byteArray = stream.toByteArray();
    stream.close();
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
  }
}