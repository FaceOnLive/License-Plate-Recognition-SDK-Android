package org.buyun.alpr.common;

import android.graphics.RectF;
import android.media.ExifInterface;
import android.media.Image;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.buyun.alpr.sdk.SDK_IMAGE_TYPE;
import org.buyun.alpr.sdk.AlprSdk;
import org.buyun.alpr.sdk.AlprCallback;
import org.buyun.alpr.sdk.AlprResult;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.List;

/**
 * Base activity to subclass to make our life easier
 */
public abstract class AlprActivity extends AppCompatActivity implements AlprCameraFragment.AlprCameraFragmentSink {

    static final String TAG = AlprActivity.class.getCanonicalName();

    private String mDebugInternalDataPath = null;

    private boolean mIsProcessing = false;
    private boolean mIsPaused = true;

    /**
     * Parallel callback delivery function used by the engine to notify for new deferred results
     */
    static class MyUltAlprSdkParallelDeliveryCallback extends AlprCallback {
        static final String TAG = MyUltAlprSdkParallelDeliveryCallback.class.getCanonicalName();

        AlprPlateView mAlprPlateView;
        Size mImageSize;
        long mTotalDuration = 0;
        int mOrientation = 0;

        void setAlprPlateView(@NonNull final AlprPlateView view) {
            mAlprPlateView = view;
        }

        void setImageSize(@NonNull final Size imageSize, @NonNull final int orientation) {
            mImageSize = imageSize;
            mOrientation = orientation;
        }

        void setDurationTime(final long totalDuration) {
            mTotalDuration = totalDuration;
        }

        @Override
        public void onNewResult(AlprResult result) {
            Log.d(TAG, AlprUtils.resultToString(result));
            if (mAlprPlateView!= null) {
                mAlprPlateView.setResult(result, mImageSize, mTotalDuration, mOrientation);
            }
        }
        static MyUltAlprSdkParallelDeliveryCallback newInstance() {
            return new MyUltAlprSdkParallelDeliveryCallback();
        }
    }

    /**
     * The parallel delivery callback. Set to null to disable parallel mode
     * and enforce sequential mode.
     */
    private MyUltAlprSdkParallelDeliveryCallback mParallelDeliveryCallback;

    private AlprPlateView mAlprPlateView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        Log.i(TAG, "onCreate " + this);
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(getLayoutResId());

        // Create folder to dump input images for debugging
        File dummyFile = new File(getExternalFilesDir(null), "dummyFile");
        if (!dummyFile.getParentFile().exists() && !dummyFile.getParentFile().mkdirs()) {
            Log.e(TAG,"mkdir failed: " + dummyFile.getParentFile().getAbsolutePath());
        }
        mDebugInternalDataPath = dummyFile.getParentFile().exists() ? dummyFile.getParent() : Environment.getExternalStorageDirectory().getAbsolutePath();
        dummyFile.delete();

        // Create parallel delivery callback is enabled
        mParallelDeliveryCallback = isParallelDeliveryEnabled() ? MyUltAlprSdkParallelDeliveryCallback.newInstance() : null;

        // Init the engine
        final JSONObject config = getJsonConfig();
        // Retrieve previously stored key from internal storage
        final AlprResult alprResult = AlprUtils.assertIsOk(AlprSdk.init(
                getAssets(),
                config.toString(),
                mParallelDeliveryCallback
        ));
        Log.i(TAG,"ALPR engine initialized: " + AlprUtils.resultToString(alprResult));
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy " + this);
        final AlprResult result = AlprUtils.assertIsOk(AlprSdk.deInit());
        Log.i(TAG,"ALPR engine deInitialized: " + AlprUtils.resultToString(result));

        super.onDestroy();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();

        mIsPaused = false;
    }

    @Override
    public synchronized void onPause() {
        mIsPaused = true;

        super.onPause();
    }

    @Override
    public void setAlprPlateView(@NonNull final AlprPlateView view) {
        mAlprPlateView = view;
        if (mParallelDeliveryCallback != null) {
            mParallelDeliveryCallback.setAlprPlateView(view);
        }
        final List<Float> roi = getDetectROI();
        assert(roi.size() == 4);
        mAlprPlateView.setDetectROI(
                new RectF(
                        roi.get(0).floatValue(),
                        roi.get(2).floatValue(),
                        roi.get(1).floatValue(),
                        roi.get(3).floatValue()
                )
        );
    }

    @Override
    public void setImage(@NonNull final Image image, final int jpegOrientation) {

        // On sequential mode we just ignore the processing
        if (mIsProcessing || mIsPaused) {
            Log.d(TAG, "Inference function not returned yet: Processing or paused");
            image.close();
            return;
        }

        mIsProcessing = true;

        final Size imageSize = new Size(image.getWidth(), image.getHeight());

        // Orientation
        // Convert from degree to real EXIF orientation
        int exifOrientation;
        switch (jpegOrientation) {
            case 90: exifOrientation = ExifInterface.ORIENTATION_ROTATE_90; break;
            case 180: exifOrientation = ExifInterface.ORIENTATION_ROTATE_180; break;
            case 270: exifOrientation = ExifInterface.ORIENTATION_ROTATE_270; break;
            case 0: default: exifOrientation = ExifInterface.ORIENTATION_NORMAL; break;
        }

        // Update image for the async callback
        if (mParallelDeliveryCallback != null) {
            mParallelDeliveryCallback.setImageSize((jpegOrientation % 180) == 0 ? imageSize : new Size(imageSize.getHeight(), imageSize.getWidth()), jpegOrientation);
        }

        // The actual ALPR inference is done here
        // Do not worry about the time taken to perform the inference, the caller
        // (most likely the camera fragment) set the current image using a background thread.
        final Image.Plane[] planes = image.getPlanes();
        final long startTimeInMillis = SystemClock.uptimeMillis();
        final AlprResult result = /*AlprUtils.assertIsOk*/(AlprSdk.process(
                SDK_IMAGE_TYPE.ULTALPR_SDK_IMAGE_TYPE_YUV420P,
                planes[0].getBuffer(),
                planes[1].getBuffer(),
                planes[2].getBuffer(),
                imageSize.getWidth(),
                imageSize.getHeight(),
                planes[0].getRowStride(),
                planes[1].getRowStride(),
                planes[2].getRowStride(),
                planes[1].getPixelStride(),
                exifOrientation
        ));
        final long durationInMillis = SystemClock.uptimeMillis() - startTimeInMillis; // Total time: Inference + image processing (chroma conversion, rotation...)

        if (mParallelDeliveryCallback != null) {
            mParallelDeliveryCallback.setDurationTime(durationInMillis);
        }

        // Release the image and signal the inference process is finished
        image.close();

        mIsProcessing = false;

        if (result.isOK()) {
            Log.d(TAG, AlprUtils.resultToString(result));
        } else {
            Log.e(TAG, AlprUtils.resultToString(result));
        }

        // Display the result if sequential mode. Otherwise, let the parallel callback
        // display the result when provided.
        // Starting version 3.2 the callback will be called even if the result is empty
        if (mAlprPlateView != null && (mParallelDeliveryCallback == null || (result.numPlates() == 0 && result.numCars() == 0))) { // means sequential call or no plate/car to expect from the parallel delivery callback
            mAlprPlateView.setResult(result, (jpegOrientation % 180) == 0 ? imageSize : new Size(imageSize.getHeight(), imageSize.getWidth()), durationInMillis, jpegOrientation);
        }
    }

    /**
     * Gets the base folder defining a path where the application can write private
     * data.
     * @return The path
     */
    protected String getDebugInternalDataPath() {
        return mDebugInternalDataPath;
    }

    /**
     * Gets the server url used to activate the license. Please contact us to get the correct URL.
     * e.g. https://localhost:3600
     * @return The URL
     */
    protected String getActivationServerUrl() {
        return "";
    }

    protected String getActivationMasterOrSlaveKey() {
        return "";
    }

    /**
     * Returns the layout Id for the activity
     * @return
     */
    protected abstract int getLayoutResId();

    /**
     * Returns JSON config to be used to initialize the ALPR/ANPR SDK.
     * @return The JSON config
     */
    protected abstract JSONObject getJsonConfig();

    /**
     */
    protected abstract boolean isParallelDeliveryEnabled();

    protected abstract List<Float> getDetectROI();
}