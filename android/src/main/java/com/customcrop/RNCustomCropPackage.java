
package com.customcrop;

import java.util.Arrays;
import java.util.Collections;

import java.util.List;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.facebook.react.bridge.JavaScriptModule;
public class RNCustomCropPackage implements ReactPackage {
    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
      return Arrays.<NativeModule>asList(
          new RNCustomCropModule(reactContext)
      );
    }

    @Override
    public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {      
      return Collections.emptyList();
    }
}
