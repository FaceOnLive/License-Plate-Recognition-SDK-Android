package org.buyun.alpr.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;

import org.buyun.alpr.sdk.AlprResult;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AlprPlateView extends View {

    static final String TAG = AlprPlateView.class.getCanonicalName();

    static final float LPCI_MIN_CONFIDENCE = 80.f;
    static final float VCR_MIN_CONFIDENCE = 80.f;
    static final float VMMR_MIN_CONFIDENCE = 60.f;
    static final float VBSR_MIN_CONFIDENCE = 70.f;
    static final float VMMR_FUSE_DEFUSE_MIN_CONFIDENCE = 40.f;
    static final int VMMR_FUSE_DEFUSE_MIN_OCCURRENCES = 3;

    static final float TEXT_NUMBER_SIZE_DIP = 20;
    static final float TEXT_LPCI_SIZE_DIP = 15;
    static final float TEXT_CAR_SIZE_DIP = 15;
    static final float TEXT_INFERENCE_TIME_SIZE_DIP = 20;
    static final int STROKE_WIDTH = 10;

    private final Paint mPaintTextNumber;
    private final Paint mPaintTextNumberBackground;
    private final Paint mPaintTextLPCI;
    private final Paint mPaintTextLPCIBackground;
    private final Paint mPaintTextCar;
    private final Paint mPaintTextCarBackground;
    private final Paint mPaintBorder;
    private final Paint mPaintTextDurationTime;
    private final Paint mPaintTextDurationTimeBackground;
    private final Paint mPaintDetectROI;

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    private int mOrientation = 0;

    private long mDurationTimeMillis;

    private Size mImageSize;
    private List<AlprUtils.Plate> mPlates = null;
    private RectF mDetectROI;

    /**
     *
     * @param context
     * @param attrs
     */
    public AlprPlateView(final Context context, final AttributeSet attrs) {
        super(context, attrs);

//        final Typeface fontALPR = Typeface.createFromAsset(context.getAssets(), "GlNummernschildEng-XgWd.ttf");

        mPaintTextNumber = new Paint();
        mPaintTextNumber.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_NUMBER_SIZE_DIP, getResources().getDisplayMetrics()));
        mPaintTextNumber.setColor(Color.BLACK);
        mPaintTextNumber.setStyle(Paint.Style.FILL_AND_STROKE);
//        mPaintTextNumber.setTypeface(Typeface.create(fontALPR, Typeface.BOLD));

        mPaintTextNumberBackground = new Paint();
        mPaintTextNumberBackground.setColor(Color.YELLOW);
        mPaintTextNumberBackground.setStrokeWidth(STROKE_WIDTH);
        mPaintTextNumberBackground.setStyle(Paint.Style.FILL_AND_STROKE);

        mPaintTextLPCI = new Paint();
        mPaintTextLPCI.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_LPCI_SIZE_DIP, getResources().getDisplayMetrics()));
        mPaintTextLPCI.setColor(Color.WHITE);
        mPaintTextLPCI.setStyle(Paint.Style.FILL_AND_STROKE);
//        mPaintTextLPCI.setTypeface(Typeface.create(fontALPR, Typeface.BOLD));

        mPaintTextLPCIBackground = new Paint();
        mPaintTextLPCIBackground.setColor(Color.BLUE);
        mPaintTextLPCIBackground.setStrokeWidth(STROKE_WIDTH);
        mPaintTextLPCIBackground.setStyle(Paint.Style.FILL_AND_STROKE);

        mPaintTextCar = new Paint();
        mPaintTextCar.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_CAR_SIZE_DIP, getResources().getDisplayMetrics()));
        mPaintTextCar.setColor(Color.BLACK);
        mPaintTextCar.setStyle(Paint.Style.FILL_AND_STROKE);
//        mPaintTextCar.setTypeface(Typeface.create(fontALPR, Typeface.BOLD));

        mPaintTextCarBackground = new Paint();
        mPaintTextCarBackground.setColor(Color.RED);
        mPaintTextCarBackground.setStrokeWidth(STROKE_WIDTH);
        mPaintTextCarBackground.setStyle(Paint.Style.FILL_AND_STROKE);

        mPaintBorder = new Paint();
        mPaintBorder.setStrokeWidth(STROKE_WIDTH);
        mPaintBorder.setPathEffect(null);
        mPaintBorder.setColor(Color.YELLOW);
        mPaintBorder.setStyle(Paint.Style.STROKE);

        mPaintTextDurationTime = new Paint();
        mPaintTextDurationTime.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, TEXT_INFERENCE_TIME_SIZE_DIP, getResources().getDisplayMetrics()));
        mPaintTextDurationTime.setColor(Color.WHITE);
        mPaintTextDurationTime.setStyle(Paint.Style.FILL_AND_STROKE);
//        mPaintTextDurationTime.setTypeface(Typeface.create(fontALPR, Typeface.BOLD));

        mPaintTextDurationTimeBackground = new Paint();
        mPaintTextDurationTimeBackground.setColor(Color.BLACK);
        mPaintTextDurationTimeBackground.setStrokeWidth(STROKE_WIDTH);
        mPaintTextDurationTimeBackground.setStyle(Paint.Style.FILL_AND_STROKE);

        mPaintDetectROI = new Paint();
        mPaintDetectROI.setColor(Color.RED);
        mPaintDetectROI.setStrokeWidth(STROKE_WIDTH);
        mPaintDetectROI.setStyle(Paint.Style.STROKE);
        mPaintDetectROI.setPathEffect(new DashPathEffect(new float[] {10,20}, 0));
    }

    public void setDetectROI(final RectF roi) { mDetectROI = roi; }

    /**
     *
     * @param width
     * @param height
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.i(TAG, "onMeasure");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
    }

    /**
     *
     * @param result
     * @param imageSize
     */
    public synchronized void setResult(@NonNull final AlprResult result, @NonNull final Size imageSize, @NonNull final long durationTime, @NonNull final int orientation) {
        mPlates = AlprUtils.extractPlates(result);
        mImageSize = imageSize;
        mDurationTimeMillis = durationTime;
        mOrientation = orientation;
        postInvalidate();
    }

    @Override
    public synchronized void draw(final Canvas canvas) {
        super.draw(canvas);

        if (mImageSize == null) {
            Log.i(TAG, "Not initialized yet");
            return;
        }

        final String mInferenceTimeMillisString = "Point your camera at a License Plate ";
        Rect boundsTextmInferenceTimeMillis = new Rect();
        mPaintTextDurationTime.getTextBounds(mInferenceTimeMillisString, 0, mInferenceTimeMillisString.length(), boundsTextmInferenceTimeMillis);

        int left = (canvas.getWidth() - boundsTextmInferenceTimeMillis.width()) / 2;
        int top = 20;
        canvas.drawRect(left, top, left + boundsTextmInferenceTimeMillis.width() + 5, top + boundsTextmInferenceTimeMillis.height() + 20, mPaintTextDurationTimeBackground);
        canvas.drawText(mInferenceTimeMillisString, left, 20 + boundsTextmInferenceTimeMillis.height(), mPaintTextDurationTime);

        // Transformation info
        final AlprUtils.AlprTransformationInfo tInfo = new AlprUtils.AlprTransformationInfo(mImageSize.getWidth(), mImageSize.getHeight(), getWidth(), getHeight());

        // ROI
        if (mDetectROI != null && !mDetectROI.isEmpty()) {
            canvas.drawRect(
                    new RectF(
                            tInfo.transformX(mDetectROI.left),
                            tInfo.transformY(mDetectROI.top),
                            tInfo.transformX(mDetectROI.right),
                            tInfo.transformY(mDetectROI.bottom)
                    ),
                    mPaintDetectROI
            );
        }

        // Plates
        if (mPlates != null && !mPlates.isEmpty()) {
            for (final AlprUtils.Plate plate : mPlates) {
                // Transform corners
                final float[] plateWarpedBox = plate.getWarpedBox();
                final PointF plateCornerA = new PointF(tInfo.transformX(plateWarpedBox[0]), tInfo.transformY(plateWarpedBox[1]));
                final PointF plateCornerB = new PointF(tInfo.transformX(plateWarpedBox[2]), tInfo.transformY(plateWarpedBox[3]));
                final PointF plateCornerC = new PointF(tInfo.transformX(plateWarpedBox[4]), tInfo.transformY(plateWarpedBox[5]));
                final PointF plateCornerD = new PointF(tInfo.transformX(plateWarpedBox[6]), tInfo.transformY(plateWarpedBox[7]));
                // Draw border
                final Path platePathBorder = new Path();
                platePathBorder.moveTo(plateCornerA.x, plateCornerA.y);
                platePathBorder.lineTo(plateCornerB.x, plateCornerB.y);
                platePathBorder.lineTo(plateCornerC.x, plateCornerC.y);
                platePathBorder.lineTo(plateCornerD.x, plateCornerD.y);
                platePathBorder.lineTo(plateCornerA.x, plateCornerA.y);
                platePathBorder.close();
                mPaintBorder.setColor(mPaintTextNumberBackground.getColor());
                canvas.drawPath(platePathBorder, mPaintBorder);

                // Draw text number
                final String number = plate.getNumber();
                if (number != null && !number.isEmpty()) {
                    Rect boundsTextNumber = new Rect();
                    mPaintTextNumber.getTextBounds(number, 0, number.length(), boundsTextNumber);
                    final RectF rectTextNumber = new RectF(
                            plateCornerA.x,
                            plateCornerA.y - (boundsTextNumber.height() + 10) * 2,
                            plateCornerA.x + boundsTextNumber.width(),
                            plateCornerA.y - (boundsTextNumber.height() + 10)
                    );
                    final Path pathTextNumber = new Path();
                    pathTextNumber.moveTo(plateCornerA.x, plateCornerA.y - rectTextNumber.height() - 10);
                    pathTextNumber.lineTo(Math.max(plateCornerB.x, (plateCornerA.x + rectTextNumber.width())), plateCornerB.y - rectTextNumber.height() - 10);
                    pathTextNumber.addRect(rectTextNumber, Path.Direction.CCW);
                    pathTextNumber.close();
                    canvas.drawPath(pathTextNumber, mPaintTextNumberBackground);
                    canvas.drawTextOnPath(number, pathTextNumber, 0, 0, mPaintTextNumber);
                }

                // Draw Car
                if (plate.getCar() != null) {
                    final AlprUtils.Car car = plate.getCar();
                    if (car.getConfidence() >= 80.f) {
                        String color = null;
                        if (car.getColors() != null) {
                            final AlprUtils.Car.Attribute colorObj0 = car.getColors().get(0); // sorted, most higher confidence first
                            if (colorObj0.getConfidence() >= VCR_MIN_CONFIDENCE) {
                                color = colorObj0.getName();
                            }
                            else if (car.getColors().size() >= 2) {
                                final AlprUtils.Car.Attribute colorObj1 = car.getColors().get(1);
                                final String colorMix = colorObj0.getName() + "/" + colorObj1.getName();
                                float confidence = colorObj0.getConfidence();
                                if ("white/silver,silver/white,gray/silver,silver/gray".indexOf(colorMix) != -1) {
                                    confidence += colorObj1.getConfidence();
                                }
                                if (confidence >= VCR_MIN_CONFIDENCE) {
                                    color = (colorMix.indexOf("white") == -1) ? "DarkSilver" : "LightSilver";
									confidence = Math.max(colorObj0.getConfidence(), colorObj1.getConfidence());
                                }
                            }
                        }

                        String make = null, model = null;
                        if (car.getMakesModelsYears() != null) {
                            final List<AlprUtils.Car.MakeModelYear> makesModelsYears = car.getMakesModelsYears();
                            final AlprUtils.Car.MakeModelYear makeModelYear = makesModelsYears.get(0); // sorted, most higher confidence first
                            if (makeModelYear.getConfidence() >= VMMR_MIN_CONFIDENCE) {
                                make = makeModelYear.getMake();
                                model = makeModelYear.getModel();
                            }
                            else {
                                Map<String, Float> makes =  new HashMap<>();
                                Map<String, Integer> occurrences =  new HashMap<>();
								// Fuse makes
                                for (final AlprUtils.Car.MakeModelYear mmy : makesModelsYears) {
                                    makes.put(mmy.getMake(), AlprUtils.getOrDefault(makes, mmy.getMake(), 0.f) + mmy.getConfidence()); // Map.getOrDefault requires API level 24
                                    occurrences.put(mmy.getMake(), AlprUtils.getOrDefault(occurrences, mmy.getMake(), 0) + 1); // Map.getOrDefault requires API level 24
                                }
                                // Find make with highest confidence. Stream requires Java8
                                Iterator<Map.Entry<String, Float> > itMake = makes.entrySet().iterator();
                                Map.Entry<String, Float> bestMake = itMake.next();
                                while (itMake.hasNext()) {
                                    Map.Entry<String, Float> makeE = itMake.next();
                                    if (makeE.getValue() > bestMake.getValue()) {
                                        bestMake = makeE;
                                    }
                                }
								// Model fusion
                                if (bestMake.getValue() >= VMMR_MIN_CONFIDENCE || (occurrences.get(bestMake.getKey()) >= VMMR_FUSE_DEFUSE_MIN_OCCURRENCES && bestMake.getValue() >= VMMR_FUSE_DEFUSE_MIN_CONFIDENCE)) {
                                    make = bestMake.getKey();

                                    // Fuse models
                                    Map<String, Float> models =  new HashMap<>();
                                    for (final AlprUtils.Car.MakeModelYear mmy : makesModelsYears) {
                                        if (make.equals(mmy.getMake())) {
                                            models.put(mmy.getModel(), AlprUtils.getOrDefault(models, mmy.getModel(), 0.f) + mmy.getConfidence()); // Map.getOrDefault requires API level 24
                                        }
                                    }
                                    // Find model with highest confidence. Stream requires Java8
                                    Iterator<Map.Entry<String, Float> > itModel = models.entrySet().iterator();
                                    Map.Entry<String, Float> bestModel = itModel.next();
                                    while (itModel.hasNext()) {
                                        Map.Entry<String, Float> modelE = itModel.next();
                                        if (modelE.getValue() > bestModel.getValue()) {
                                            bestModel = modelE;
                                        }
                                    }
                                    model = bestModel.getKey();
                                }
                            }
                        }

                        String bodyStyle = null;
                        if (car.getBodyStyles() != null) {
                            final AlprUtils.Car.Attribute vbsr = car.getBodyStyles().get(0); // sorted, most higher confidence first
                            if (vbsr.getConfidence() >= VBSR_MIN_CONFIDENCE) {
                                bodyStyle = vbsr.getName();
                            }
                        }

                        // Transform corners
                        final float[] carWarpedBox = car.getWarpedBox();
                        final PointF carCornerA = new PointF(tInfo.transformX(carWarpedBox[0]), tInfo.transformY(carWarpedBox[1]));
                        final PointF carCornerB = new PointF(tInfo.transformX(carWarpedBox[2]), tInfo.transformY(carWarpedBox[3]));
                        final PointF carCornerC = new PointF(tInfo.transformX(carWarpedBox[4]), tInfo.transformY(carWarpedBox[5]));
                        final PointF carCornerD = new PointF(tInfo.transformX(carWarpedBox[6]), tInfo.transformY(carWarpedBox[7]));
                        // Draw border
                        final Path carPathBorder = new Path();
                        carPathBorder.moveTo(carCornerA.x, carCornerA.y);
                        carPathBorder.lineTo(carCornerB.x, carCornerB.y);
                        carPathBorder.lineTo(carCornerC.x, carCornerC.y);
                        carPathBorder.lineTo(carCornerD.x, carCornerD.y);
                        carPathBorder.lineTo(carCornerA.x, carCornerA.y);
                        carPathBorder.close();
                        mPaintBorder.setColor(mPaintTextCarBackground.getColor());
                        canvas.drawPath(carPathBorder, mPaintBorder);

                        // Draw car information
                        final String carText = String.format(
                                "%s%s%s%s",
                                make != null ? make : "Car",
                                model != null ? ", " + model : "",
                                color != null ? ", " + color : "",
                                bodyStyle != null ? ", " + bodyStyle : ""
                        );
                        Rect boundsTextCar = new Rect();
                        mPaintTextNumber.getTextBounds(carText, 0, carText.length(), boundsTextCar);
                        final RectF rectTextNumber = new RectF(
                                plateCornerA.x,
                                plateCornerA.y - (boundsTextCar.height() + 5) * 3,
                                plateCornerA.x + boundsTextCar.width(),
                                plateCornerA.y - (boundsTextCar.height() + 5) * 2
                        );
                        final Path pathTextCar = new Path();
                        pathTextCar.moveTo(plateCornerA.x, plateCornerA.y - (rectTextNumber.height() + 5) * 2);
                        pathTextCar.lineTo(Math.max(plateCornerB.x, (plateCornerA.x + rectTextNumber.width())), plateCornerB.y - (rectTextNumber.height() + 5) * 2);
                        pathTextCar.addRect(rectTextNumber, Path.Direction.CCW);
                        pathTextCar.close();
                        canvas.drawPath(pathTextCar, mPaintTextNumberBackground);
                        canvas.drawTextOnPath(carText, pathTextCar, 0, 0, mPaintTextNumber);
                    }
                }

                if (plate.getCountries() != null) {
                    final AlprUtils.Country country = plate.getCountries().get(0); // sorted, most higher confidence first
                    if (country.getConfidence() >= LPCI_MIN_CONFIDENCE) {
                        final String countryString = country.getCode();
                        Rect boundsConfidenceLPCI = new Rect();
                        mPaintTextNumber.getTextBounds(countryString, 0, countryString.length(), boundsConfidenceLPCI);
                        final RectF rectTextLPCI = new RectF(
                                plateCornerA.x,
                                plateCornerA.y - (boundsConfidenceLPCI.height() + 10),
                                plateCornerA.x + (boundsConfidenceLPCI.width() + 10),
                                plateCornerA.y
                        );
                        final Path pathTextLPCI = new Path();
                        pathTextLPCI.moveTo(plateCornerA.x, plateCornerA.y);
                        pathTextLPCI.lineTo(Math.max(plateCornerB.x, (plateCornerA.x + boundsConfidenceLPCI.width())), plateCornerB.y);
                        pathTextLPCI.addRect(rectTextLPCI, Path.Direction.CCW);
                        pathTextLPCI.close();
                        canvas.drawPath(pathTextLPCI, mPaintTextNumberBackground);
                        canvas.drawTextOnPath(countryString, pathTextLPCI, 0, 0, mPaintTextNumber);
                    }
                }
            }
        }
    }
}