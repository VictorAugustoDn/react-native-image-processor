
package com.customcrop;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;

public class RNCustomCropModule extends ReactContextBaseJavaModule {

  private final ReactApplicationContext reactContext;

  public RNCustomCropModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "CustomCropManager";
  }

  @ReactMethod
  public void createPDFbyImages(ReadableMap options, final Promise promise) {
    PDFGenerator generator = new PDFGenerator(this.reactContext);
    generator.createPDFbyImages(options, promise);
  }

  @ReactMethod
  public void crop(ReadableMap points, String imageUri, Callback callback) {
    Cropper cropper = new Cropper(this.reactContext);
    cropper.crop(points, imageUri, callback);
  }

  @ReactMethod
  public void storeThumbnail(String imageUri, int size, final Promise promise) {
    Cropper cropper = new Cropper(this.reactContext);
    cropper.saveThumbnail(imageUri, size, promise);
  }
}