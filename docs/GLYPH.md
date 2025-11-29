Here is the comprehensive documentation for the Glyph SDK, generated from the provided developer kit files.

# Glyph SDK Documentation

## 1\. Overview

The Glyph Matrix Developer Kit provides the tools necessary to create custom Glyph Matrix experiences within your app or to build standalone "Glyph Toys" for compatible devices.

The core of the kit is the Glyph Matrix Android library, which converts designs into Glyph Matrix Data and renders them frame-by-frame.

## 2\. License Agreement

**Important:** Use of this software is governed by the Glyph SDK End User License Agreement. By installing or using the software, you agree to these terms.

### Key Terms:

  * **License Grant:** You are granted a limited, non-exclusive, non-transferable license to use the Glyph Matrix SDK solely for integrating its functionality into your applications.
  * **Restrictions:**
      * You may not modify, reverse engineer, decompile, or disassemble the Software.
      * Commercial use is strictly prohibited without prior written permission from Nothing.
      * You cannot rent, lease, or sublicense the software.
  * **Ownership:** All intellectual property rights remain with Nothing Technology Limited.
  * **Commercial Inquiries:** For commercial licensing, contact `GDKsupport@nothing.tech`.

-----

## 3\. Getting Started

### 3.1. Library Integration

1.  Create a `libs` folder under your main app module.
2.  Copy the `GlyphMatrixSDK.aar` file into this directory.
3.  Add the library as a dependency in your `build.gradle` file (e.g., implementation files('libs/GlyphMatrixSDK.aar')).

### 3.2. AndroidManifest Configuration

Locate your `AndroidManifest.xml` at `<your-project>/app/src/main/AndroidManifest.xml` and add the required permission:

```xml
<uses-permission android:name="com.nothing.ketchum.permission.ENABLE"/>
```

### 3.3. Service Registration (Glyph Toys Only)

If you are developing a "Glyph Toy" (a standalone Glyph experience), you must register it as a service so the system recognizes it.

**Example Configuration:**

```xml
<service android:name="com.nothing.demo.TestToyOne"
    android:exported="true">
    <intent-filter>
        <action android:name="com.nothing.glyph.TOY"/>
    </intent-filter>

    <meta-data
        android:name="com.nothing.glyph.toy.name"
        android:resource="@string/toy_name_one"/>

    <meta-data
        android:name="com.nothing.glyph.toy.image"
        android:resource="@drawable/img_toy_preview_one"/>

    <meta-data
        android:name="com.nothing.glyph.toy.summary"
        android:resource="@string/toy_summary" />

    <meta-data
        android:name="com.nothing.glyph.toy.longpress"
        android:value="1"/>

    <meta-data
        android:name="com.nothing.glyph.toy.aod_support"
        android:value="1"/>
</service>
```

### 3.4. Creating Preview Images

To ensure consistency, use the official Figma template for creating preview icons. It is recommended to export these as SVGs and import them using Android Studio's Vector Asset Studio.

-----

## 4\. Developing a Glyph Toy Service

### 4.1. User Interactions

Toys are controlled via the **Glyph Button** on the back of the device:

  * **Short Press:** Cycles through available toys. Starts the toy's functions.
  * **Long Press:** Sends a `"change"` event to the active toy (requires `longpress` metadata to be enabled).
  * **Touch-down/up:** Triggers `"action_down"` and `"action_up"` events.

### 4.2. Lifecycle Management

Implement `onBind()` to initialize your toy and `onUnbind()` to clean up resources.

```java
@Override
public IBinder onBind(Intent intent) {
    init(); // Start your experience
    return serviceMessenger.getBinder(); // Return binder if handling events
}

@Override
public boolean onUnbind(Intent intent) {
    mGM.unInit();
    return false;
}
```

### 4.3. Handling Events

To handle Glyph Button events (like long press), create a `Handler` and `Messenger`.

```java
private final Handler serviceHandler = new Handler(Looper.getMainLooper()) {
    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case GlyphToy.MSG_GLYPH_TOY: {
                Bundle bundle = msg.getData();
                String event = bundle.getString(GlyphToy.MSG_GLYPH_TOY_DATA);
                if (GlyphToy.EVENT_CHANGE.equals(event)) {
                    // Handle long press
                } else if (GlyphToy.EVENT_AOD.equals(event)) {
                    // Handle AOD update (triggers every minute)
                }
                break;
            }
            default:
                super.handleMessage(msg);
        }
    }
};
private final Messenger serviceMessenger = new Messenger(serviceHandler);
```

### 4.4. Best Practice: Manager Intent

To help users find and activate your toy, use the following intent to open the "Manage Glyph Toys" screen after they install or configure your app.

```java
Intent intent = new Intent();
intent.setComponent(new ComponentName("com.nothing.thirdparty", "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"));
startActivity(intent);
```

-----

## 5\. API Reference

### 5.1. GlyphMatrixManager

Manages the connection to the service and updates the display.

  * `init(Callback callback)`: Binds the service.
  * `register(String target)`: Registers the app (Target for Phone 3: `Glyph.DEVICE_23112`).
  * `setMatrixFrame(GlyphMatrixFrame frame)`: Updates the display using a frame object.
  * `setAppMatrixFrame(GlyphMatrixFrame frame)`: Use this instead of `setMatrixFrame` if controlling the Glyph Matrix from a standard app (not a toy service).
  * `closeAppMatrix()`: Closes the app matrix display.

### 5.2. GlyphMatrixFrame

Defines the LED data (default 25x25). Constructed using `GlyphMatrixFrame.Builder`.

**Builder Methods:**

  * `addTop(GlyphMatrixObject object)`
  * `addMid(GlyphMatrixObject object)`
  * `addLow(GlyphMatrixObject object)`
  * `build(Context context)`
  * `render()`: Returns the rendered data.

### 5.3. GlyphMatrixObject

Encapsulates a single image or element with properties like position, rotation, and brightness.

**Builder Methods:**

  * `setImageSource(Object imagesource)`: Sets the bitmap (must be 1:1).
  * `setPosition(int x, int y)`: Top-left coordinate.
  * `setOrientation(int degrees)`: Clockwise rotation.
  * `setScale(int scale)`: 0-200 (100 is original size).
  * `setBrightness(int brightness)`: 0-255.

-----

## 6\. Full Code Example

Here is a complete example of a Glyph Toy Service that displays a "butterfly" image.

```java
public class MyToyService extends Service {
    private GlyphMatrixManager mGM;
    private GlyphMatrixManager.Callback mCallback;

    @Override
    public IBinder onBind(Intent intent) {
        init();
        return null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (mGM != null) {
            mGM.turnOff();
            mGM.unInit();
            mGM = null;
        }
        return false;
    }

    private void init() {
        mGM = GlyphMatrixManager.getInstance(getApplicationContext());
        mCallback = new GlyphMatrixManager.Callback() {
            @Override
            public void onServiceConnected(ComponentName componentName) {
                mGM.register(Glyph.DEVICE_23112);
                displayButterfly();
            }
            @Override
            public void onServiceDisconnected(ComponentName componentName) { }
        };
        mGM.init(mCallback);
    }

    private void displayButterfly() {
        // Create the Object
        GlyphMatrixObject butterfly = new GlyphMatrixObject.Builder()
            .setImageSource(GlyphMatrixUtils.drawableToBitmap(getResources().getDrawable(R.drawable.butterfly)))
            .setScale(100)
            .setOrientation(0)
            .setPosition(0, 0)
            .build();
        
        // Add to Frame and Render
        GlyphMatrixFrame frame = new GlyphMatrixFrame.Builder()
            .addTop(butterfly)
            .build();
        
        mGM.setMatrixFrame(frame.render());
    }
}
```

## 7\. Support

For issues or inquiries:

  * **Email:** `GDKsupport@nothing.tech`.
  * **Community:** [Nothing Community - Glyph SDK](https://nothing.community/t/glyph-sdk).