package com.customcrop;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.MediaStore;
import android.util.Base64;
import android.media.ThumbnailUtils;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgcodecs.*;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import org.opencv.calib3d.Calib3d;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.util.UUID;
import java.lang.Math;

public class Cropper {
  private final ReactApplicationContext reactContext;

  public Cropper(ReactApplicationContext reactContext) {
    this.reactContext = reactContext;
  }

  public void crop(ReadableMap points, String imageUri, int maxWidth, Callback callback) {

    Point tl = new Point(
      points.getMap("topLeft").getDouble("x"), 
      points.getMap("topLeft").getDouble("y")
    );

    Point tr = new Point(
      points.getMap("topRight").getDouble("x"), 
      points.getMap("topRight").getDouble("y")
    );

    Point bl = new Point(
      points.getMap("bottomLeft").getDouble("x"), 
      points.getMap("bottomLeft").getDouble("y")
    );

    Point br = new Point(
      points.getMap("bottomRight").getDouble("x"), 
      points.getMap("bottomRight").getDouble("y")
    );
    
    Mat src = Imgcodecs.imread(imageUri.replace("file://", ""), Imgproc.COLOR_BGR2RGB);
    Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2RGB);

    double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
    double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

    double dw = Math.max(widthA, widthB);
    int docMaxWidth = Double.valueOf(dw).intValue();

    double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
    double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

    double dh = Math.max(heightA, heightB);
    int docMaxHeight = Double.valueOf(dh).intValue();

    Mat doc = new Mat(docMaxHeight, docMaxWidth, CvType.CV_8UC4);

    Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
    Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);

    src_mat.put(
      0, 
      0, 
      tl.x, 
      tl.y, 
      tr.x, 
      tr.y, 
      br.x, 
      br.y, 
      bl.x,
      bl.y);

    dst_mat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh);

    Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);

    Imgproc.warpPerspective(src, doc, m, doc.size(), 0);

    Bitmap bitmap = Bitmap.createBitmap(doc.cols(), doc.rows(), Bitmap.Config.ARGB_8888);
    Utils.matToBitmap(doc, bitmap);

    bitmap = scaleDown(bitmap, maxWidth, true);

    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 99, byteArrayOutputStream);

    byte[] byteArray = byteArrayOutputStream.toByteArray();

    String fileName = this.saveToDirectory(byteArray, "cropped", false);

    WritableMap map = Arguments.createMap();
    map.putDouble("height", dh);
    map.putDouble("width", dw);
    map.putString("imagePath", "file://" + fileName);
    // map.putString("image", imageData);
    callback.invoke(null, map);

    m.release();
  }

  private String getFileName(String folderName, boolean saveAsCache) {
    String fileName;
    String folderDir = this.reactContext.getCacheDir().toString();

    if(!saveAsCache) {
      folderDir = this.reactContext.getExternalFilesDir(null).toString();
    }

    File folder = new File(folderDir + "/" + folderName);
    
    if (!folder.exists()) {
        boolean result = folder.mkdirs();
    }
    
    fileName = folderDir + "/" + folderName + "/" + UUID.randomUUID() + ".jpg";
    return fileName;
  }

  private String saveToDirectory(byte[] byteArray, String folderName, boolean saveAsCache) {
    String fileName = this.getFileName(folderName, saveAsCache);

    try (OutputStream stream = new FileOutputStream(fileName)) {
        stream.write(byteArray);
    } catch(Exception e) {

    }

    return fileName;
  }

  public void saveThumbnail(String imageUri, int size, final Promise promise) {
    // Read image
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    options.inScaled = false;
    Bitmap bitmap = BitmapFactory.decodeFile(imageUri.replace("file://", ""), options);

    // create thumbnail
    bitmap = ThumbnailUtils.extractThumbnail(bitmap, size, size);

    // Resize image
    // bitmap = scaleDown(bitmap, size, true);

    // Convert to JPG
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);

    // Write image
    byte[] byteArray = byteArrayOutputStream.toByteArray();
    // String imageData = Base64.encodeToString(byteArray, Base64.DEFAULT);

    // return imagePath
    promise.resolve(this.saveToDirectory(byteArray, "thumbnails", false));
  }

  public static Bitmap scaleDown(Bitmap realImage, float maxImageWidth, boolean filter) {
    float ratio = (float) maxImageWidth / realImage.getWidth();

    if (ratio >= 1.0){ return realImage;}

    int width = Math.round((float) ratio * realImage.getWidth());
    int height = Math.round((float) ratio * realImage.getHeight());

    Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width, height, filter);
    return newBitmap;
  }
}