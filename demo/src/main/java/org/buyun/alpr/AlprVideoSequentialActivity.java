package org.buyun.alpr;

import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import org.buyun.alpr.common.AlprActivity;
import org.buyun.alpr.common.AlprCameraFragment;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

/**
 * Main activity
 */
public class AlprVideoSequentialActivity extends AlprActivity {

    static final String TAG = AlprVideoSequentialActivity.class.getCanonicalName();
    static final Size PREFERRED_SIZE = new Size(1280, 720);
    static final String CONFIG_DEBUG_LEVEL = "info";
    static final boolean CONFIG_DEBUG_WRITE_INPUT_IMAGE = false; // must be false unless you're debugging the code
    static final int CONFIG_NUM_THREADS = -1;
    static final boolean CONFIG_GPGPU_ENABLED = true;
    static final int CONFIG_MAX_LATENCY = -1;
    static final String CONFIG_CHARSET = "latin";
    static final boolean CONFIG_IENV_ENABLED = false;
    static final boolean CONFIG_OPENVINO_ENABLED = true;
    static final String CONFIG_OPENVINO_DEVICE = "CPU";
    static final double CONFIG_DETECT_MINSCORE = 0.1; // 10%
    static final boolean CONFIG_CAR_NOPLATE_DETECT_ENABLED = false;
    static final double CONFIG_CAR_NOPLATE_DETECT_MINSCORE = 0.8; // 80%
    static final List<Float> CONFIG_DETECT_ROI = Arrays.asList(0.f, 0.f, 0.f, 0.f);
    static final boolean CONFIG_PYRAMIDAL_SEARCH_ENABLED = true;
    static final double CONFIG_PYRAMIDAL_SEARCH_SENSITIVITY= 0.28; // 28%
    static final double CONFIG_PYRAMIDAL_SEARCH_MINSCORE = 0.5; // 50%
    static final int CONFIG_PYRAMIDAL_SEARCH_MIN_IMAGE_SIZE_INPIXELS = 800; // pixels
    static final boolean CONFIG_KLASS_LPCI_ENABLED = true;
    static final boolean CONFIG_KLASS_VCR_ENABLED = true;
    static final boolean CONFIG_KLASS_VMMR_ENABLED = true;
    static final boolean CONFIG_KLASS_VBSR_ENABLED = false;
    static final double CONFIG_KLASS_VCR_GAMMA = 1.5;
    static final double CONFIG_RECOGN_MINSCORE = 0.4; // 40%
    static final String CONFIG_RECOGN_SCORE_TYPE = "min";
    static final boolean CONFIG_RECOGN_RECTIFY_ENABLED = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate " + this);
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, AlprCameraFragment.newInstance(PREFERRED_SIZE, this))
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy " + this);
        super.onDestroy();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_main;
    }

    @Override
    protected JSONObject getJsonConfig() {

        JSONObject config = new JSONObject();
        try {
            config.put("debug_level", CONFIG_DEBUG_LEVEL);
            config.put("debug_write_input_image_enabled", CONFIG_DEBUG_WRITE_INPUT_IMAGE);
            config.put("debug_internal_data_path", getDebugInternalDataPath());

            config.put("num_threads", CONFIG_NUM_THREADS);
            config.put("gpgpu_enabled", CONFIG_GPGPU_ENABLED);
            config.put("charset", CONFIG_CHARSET);
            config.put("max_latency", CONFIG_MAX_LATENCY);
            config.put("ienv_enabled", CONFIG_IENV_ENABLED);
            config.put("openvino_enabled", CONFIG_OPENVINO_ENABLED);
            config.put("openvino_device", CONFIG_OPENVINO_DEVICE);

            config.put("detect_minscore", CONFIG_DETECT_MINSCORE);
            config.put("detect_roi", new JSONArray(getDetectROI()));

            config.put("car_noplate_detect_enabled", CONFIG_CAR_NOPLATE_DETECT_ENABLED);
            config.put("car_noplate_detect_min_score", CONFIG_CAR_NOPLATE_DETECT_MINSCORE);

            config.put("pyramidal_search_enabled", CONFIG_PYRAMIDAL_SEARCH_ENABLED);
            config.put("pyramidal_search_sensitivity", CONFIG_PYRAMIDAL_SEARCH_SENSITIVITY);
            config.put("pyramidal_search_minscore", CONFIG_PYRAMIDAL_SEARCH_MINSCORE);
            config.put("pyramidal_search_min_image_size_inpixels", CONFIG_PYRAMIDAL_SEARCH_MIN_IMAGE_SIZE_INPIXELS);

            config.put("klass_lpci_enabled", CONFIG_KLASS_LPCI_ENABLED);
            config.put("klass_vcr_enabled", CONFIG_KLASS_VCR_ENABLED);
            config.put("klass_vmmr_enabled", CONFIG_KLASS_VMMR_ENABLED);
            config.put("klass_vbsr_enabled", CONFIG_KLASS_VBSR_ENABLED);
            config.put("klass_vcr_gamma", CONFIG_KLASS_VCR_GAMMA);

            config.put("recogn_minscore", CONFIG_RECOGN_MINSCORE);
            config.put("recogn_score_type", CONFIG_RECOGN_SCORE_TYPE);
            config.put("recogn_rectify_enabled", CONFIG_RECOGN_RECTIFY_ENABLED);
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return config;
    }


    @Override
    protected boolean isParallelDeliveryEnabled() { return false; /* we want to deactivated parallel and use sequential delivery*/ }

    @Override
    protected List<Float> getDetectROI() { return CONFIG_DETECT_ROI; }
}
