package com.carassistant.ui.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Location;
import android.media.AudioManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.carassistant.R;
import com.carassistant.di.components.DaggerScreenComponent;
import com.carassistant.managers.SharedPreferencesManager;
import com.carassistant.model.bus.MessageEventBus;
import com.carassistant.model.bus.model.EventGpsDisabled;
import com.carassistant.model.bus.model.EventUpdateLocation;
import com.carassistant.model.bus.model.EventUpdateStatus;
import com.carassistant.model.entity.Data;
import com.carassistant.model.entity.GpsStatusEntity;
import com.carassistant.model.entity.SignEntity;
import com.carassistant.tflite.classification.SpeedLimitClassifier;
import com.carassistant.tflite.detection.Classifier;
import com.carassistant.tflite.detection.TFLiteObjectDetectionAPIModel;
import com.carassistant.tflite.tracking.MultiBoxTracker;
import com.carassistant.ui.adapter.SignAdapter;
import com.carassistant.utils.customview.OverlayView;
import com.carassistant.utils.env.BorderedText;
import com.carassistant.utils.env.ImageUtils;
import com.carassistant.utils.env.Logger;
import com.carassistant.utils.player.MediaPlayerHolder;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

import static com.carassistant.tflite.classification.SpeedLimitClassifier.MODEL_FILENAME;


public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    private final String TAG = DetectorActivity.class.getSimpleName();

    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/detect_labelmap.txt";
    private static float MINIMUM_CONFIDENCE_TF_OD_API = 0.7f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;

    private Classifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private Data data;
    private boolean firstfix;

    private TextView currentSpeed, distance, satellite, status, accuracy, totalDistance;
    private TextView signCountText, maxSpeedText, tripTimerText;
    private TextView speedLimitValueTxt, speedLimitStatusTxt;
    private CardView speedLimitCard;
    private View emptyStateText;
    private SignAdapter adapter;

    private SwitchCompat notification, hapticSwitch;
    private double distanceValue = 0;
    private double maxSpeedRecorded = 0;
    private CompositeDisposable compositeDisposable;
    private MediaPlayerHolder mediaPlayerHolder;

    private String lastSpeedLimitTitle = null;
    private int lastSpeedLimitValue = 0;
    private boolean isExceedingSpeedLimit = false;

    SpeedLimitClassifier speedLimitClassifier;

    @Inject
    SharedPreferencesManager sharedPreferencesManager;

    private final String SIGN_LIST = "sign_list";
    private final String DISTANCE = "distance";
    private final String MAX_SPEED = "max_speed";
    private final String SIGN_COUNT = "sign_count";
    private final String PREF_HAPTIC = "haptic_enabled";
    private final String PREF_VOLUME = "notification_volume";
    private final String PREF_THEME = "theme_mode";
    private Boolean notificationSpeed = true;
    private int totalSignCounter = 0;

    private long sessionStartTime = 0;
    private android.os.Handler timerHandler;
    private Runnable timerRunnable;

    private Vibrator vibrator;
    private AudioManager audioManager;
    private int notificationVolume = 100;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        mediaPlayerHolder = new MediaPlayerHolder(this);

        Observable.interval(30L, TimeUnit.SECONDS)
                .timeInterval()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(v -> {
                    notificationSpeed = true;
                });

        inject();
        setupLocation();
        setupRecycler();
        setupViews();
        setCallBack();
        setupClassifier();
        setupTimer();
        setupVibrator();
        setupAudio();

        sessionStartTime = System.currentTimeMillis();
    }

    private void applyTheme() {
        int themeMode = sharedPreferencesManager.getInt(PREF_THEME, 2);
        switch (themeMode) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    private void setupTimer() {
        timerHandler = new android.os.Handler();
        timerRunnable = () -> {
            if (sessionStartTime > 0) {
                long elapsed = System.currentTimeMillis() - sessionStartTime;
                int seconds = (int) (elapsed / 1000);
                int minutes = seconds / 60;
                int hours = minutes / 60;
                seconds = seconds % 60;
                minutes = minutes % 60;
                if (hours > 0) {
                    tripTimerText.setText(String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds));
                } else {
                    tripTimerText.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
                }
            }
            timerHandler.postDelayed(timerRunnable, 1000);
        };
        timerHandler.post(timerRunnable);
    }

    private void setupVibrator() {
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
    }

    private void setupAudio() {
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        notificationVolume = sharedPreferencesManager.getInt(PREF_VOLUME, 100);
        if (audioManager != null) {
            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int vol = (int) (maxVol * (notificationVolume / 100.0f));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Math.max(1, vol), 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        } else if (id == R.id.action_statistics) {
            showStatisticsDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showStatisticsDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Session Statistics")
                .setMessage("Total Signs Detected: " + totalSignCounter + "\n" +
                        "Max Speed: " + String.format("%.0f", maxSpeedRecorded) + " km/h\n" +
                        "Signs in History: " + adapter.getItemCount() + "\n" +
                        "Last Speed Limit: " + (lastSpeedLimitTitle != null ? lastSpeedLimitTitle : "None"))
                .setPositiveButton("OK", null)
                .show();
    }

    private void setupClassifier() {
        try {
            speedLimitClassifier = SpeedLimitClassifier.classifier(getAssets(), MODEL_FILENAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setCallBack() {
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(MessageEventBus.INSTANCE
                .toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(eventModel -> {
                    if (eventModel instanceof EventUpdateLocation) {
                        refresh(((EventUpdateLocation) eventModel).getData());
                    }
                    if (eventModel instanceof EventUpdateStatus) {
                        onGpsStatusChanged(((EventUpdateStatus) eventModel).getStatus());
                    }
                    if (eventModel instanceof EventGpsDisabled) {
                        showGpsDisabledDialog();
                    }
                }));
    }

    @SuppressLint({"ResourceType", "DefaultLocale"})
    private void refresh(Data data) {
        this.data = data;

        double distanceTemp = distanceValue + data.getDistance();

        String distanceUnits;
        if (distanceTemp <= 1000.0) {
            distanceUnits = "m";
        } else {
            distanceTemp /= 1000.0;
            distanceUnits = "km";
        }

        distance.setText(String.format("%.1f %s", distanceTemp, distanceUnits).replace(',', '.'));

        if (distanceValue != data.getDistance()) {
            double distance = sharedPreferencesManager.getDistance();
            distance += (distanceTemp - distanceValue);
            data.setSessionDistanceM(distanceTemp);
            sharedPreferencesManager.setDistance((float) distance);
            distanceValue = distanceTemp;
            showTotalDistance();
        }

        if (data.getLocation().hasAccuracy()) {
            double acc = data.getLocation().getAccuracy();
            String units = "m";
            SpannableString s = new SpannableString(String.format("%.0f %s", acc, units));
            s.setSpan(new RelativeSizeSpan(0.75f), s.length() - units.length() - 1, s.length(), 0);
            accuracy.setText(s);
            if (firstfix) {
                status.setText("");
                firstfix = false;
            }
        } else {
            firstfix = true;
        }

        if (data.getLocation().hasSpeed()) {
            double speed = data.getLocation().getSpeed() * 3.6;
            if (speed > maxSpeedRecorded) {
                maxSpeedRecorded = speed;
                maxSpeedText.setText(String.format("%.0f", maxSpeedRecorded));
            }
            if (speed > 50 && notification.isChecked() && notificationSpeed) {
                notificationSpeed = false;
                mediaPlayerHolder.loadMedia(R.raw.speed_limit_was_exceeded);
            }
            currentSpeed.setText(String.format("%.0f", speed));
            updateSpeedLimitComparison(speed);
        }
    }

    private void updateSpeedLimitComparison(double currentSpeedKmh) {
        if (lastSpeedLimitValue > 0) {
            if (currentSpeedKmh > lastSpeedLimitValue) {
                isExceedingSpeedLimit = true;
                speedLimitStatusTxt.setText(R.string.exceeding_limit);
                speedLimitStatusTxt.setTextColor(ContextCompat.getColor(this, R.color.danger_red));
                speedLimitCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.limit_exceed_bg));
            } else {
                isExceedingSpeedLimit = false;
                speedLimitStatusTxt.setText(R.string.under_limit);
                speedLimitStatusTxt.setTextColor(ContextCompat.getColor(this, R.color.safe_green));
                speedLimitCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.limit_badge_bg));
            }
        }
    }

    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SIGN_LIST, new Gson().toJson(adapter.getSigns()));
        outState.putDouble(DISTANCE, distanceValue);
        outState.putDouble(MAX_SPEED, maxSpeedRecorded);
        outState.putInt(SIGN_COUNT, totalSignCounter);
    }

    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String json = savedInstanceState.getString(SIGN_LIST);
        ArrayList<SignEntity> items = null;
        try {
            items = (new Gson()).fromJson(json, new TypeToken<ArrayList<SignEntity>>() {
            }.getType());
        } catch (Exception ignored) {
            items = new ArrayList<>();
        }
        adapter.setSigns(items);
        distanceValue = savedInstanceState.getDouble(DISTANCE);
        maxSpeedRecorded = savedInstanceState.getDouble(MAX_SPEED);
        totalSignCounter = savedInstanceState.getInt(SIGN_COUNT);
        updateStatsUI();
    }

    private void updateStatsUI() {
        if (signCountText != null) {
            signCountText.setText(String.valueOf(totalSignCounter));
        }
        if (maxSpeedText != null) {
            maxSpeedText.setText(String.format("%.0f", maxSpeedRecorded));
        }
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (emptyStateText != null) {
            emptyStateText.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    @SuppressLint("DefaultLocale")
    private void setupViews() {
        TextView confidence = findViewById(R.id.confidence_value);
        confidence.setText(String.format("%.2f", MINIMUM_CONFIDENCE_TF_OD_API));

        SwitchCompat camera = findViewById(R.id.camera_switch);
        camera.setOnCheckedChangeListener((buttonView, isChecked) ->
                findViewById(R.id.container).setAlpha(isChecked ? 1f : 0f)
        );

        notification = findViewById(R.id.notification_switch);
        notification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) mediaPlayerHolder.reset();
        });

        hapticSwitch = findViewById(R.id.haptic_switch);
        boolean hapticEnabled = sharedPreferencesManager.getBoolean(PREF_HAPTIC, true);
        hapticSwitch.setChecked(hapticEnabled);
        hapticSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                sharedPreferencesManager.setBoolean(PREF_HAPTIC, isChecked)
        );

        SeekBar confidenceSeekBar = findViewById(R.id.confidence_seek);
        confidenceSeekBar.setMax(100);
        confidenceSeekBar.setProgress((int) (MINIMUM_CONFIDENCE_TF_OD_API * 100));
        confidenceSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                MINIMUM_CONFIDENCE_TF_OD_API = progress / 100.0F;
                confidence.setText(String.format("%.2f", MINIMUM_CONFIDENCE_TF_OD_API));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Volume control
        SeekBar volumeSeek = findViewById(R.id.volume_seek);
        TextView volumeValue = findViewById(R.id.volume_value);
        notificationVolume = sharedPreferencesManager.getInt(PREF_VOLUME, 100);
        volumeSeek.setProgress(notificationVolume);
        volumeValue.setText(notificationVolume + "%");
        volumeSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                notificationVolume = progress;
                volumeValue.setText(progress + "%");
                if (audioManager != null) {
                    int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    int vol = (int) (maxVol * (progress / 100.0f));
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Math.max(1, vol), 0);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                sharedPreferencesManager.setInt(PREF_VOLUME, notificationVolume);
            }
        });

        // Theme toggle
        MaterialButtonToggleGroup themeToggle = findViewById(R.id.themeToggle);
        int themeMode = sharedPreferencesManager.getInt(PREF_THEME, 2);
        int checkedId = R.id.theme_system;
        if (themeMode == 0) checkedId = R.id.theme_light;
        else if (themeMode == 1) checkedId = R.id.theme_dark;
        themeToggle.check(checkedId);
        themeToggle.addOnButtonCheckedListener((group, checkedId1, isChecked) -> {
            if (!isChecked) return;
            int mode;
            if (checkedId1 == R.id.theme_light) mode = 0;
            else if (checkedId1 == R.id.theme_dark) mode = 1;
            else mode = 2;
            sharedPreferencesManager.setInt(PREF_THEME, mode);
            if (mode == 0) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            else if (mode == 1) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        });

        showTotalDistance();

        signCountText = findViewById(R.id.signCountText);
        maxSpeedText = findViewById(R.id.maxSpeedText);
        tripTimerText = findViewById(R.id.tripTimerTxt);
        speedLimitValueTxt = findViewById(R.id.speedLimitValueTxt);
        speedLimitStatusTxt = findViewById(R.id.speedLimitStatusTxt);
        speedLimitCard = findViewById(R.id.speedLimitCard);
        emptyStateText = findViewById(R.id.emptyStateText);

        updateStatsUI();

        ImageButton clearHistoryBtn = findViewById(R.id.clearHistoryBtn);
        clearHistoryBtn.setOnClickListener(v -> clearSignHistory());
    }

    private void clearSignHistory() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.clear_history)
                .setMessage(R.string.clear_history_confirm)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    adapter.clearSigns();
                    totalSignCounter = 0;
                    updateStatsUI();
                    lastSpeedLimitTitle = null;
                    lastSpeedLimitValue = 0;
                    speedLimitValueTxt.setText(R.string.no_limit_detected);
                    speedLimitStatusTxt.setText("");
                    speedLimitCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.limit_badge_bg));
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        mediaPlayerHolder.reset();
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        BorderedText borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        int sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(canvas -> {
            tracker.draw(canvas);
            if (isDebug()) {
                tracker.drawDebug(canvas);
            }
        });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();

                        for (final Classifier.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                                canvas.drawRect(location, paint);
                                cropToFrameTransform.mapRect(location);
                                result.setLocation(location);
                                mappedRecognitions.add(result);

                                runOnUiThread(() -> updateSignList(result, croppedBitmap));
                            }
                        }

                        tracker.trackResults(mappedRecognitions, currTimestamp);
                        trackingOverlay.postInvalidate();

                        computingDetection = false;

                        runOnUiThread(() -> {
                            showFrameInfo(previewWidth + "x" + previewHeight);
                            showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                            showInference(lastProcessingTimeMs + "ms");
                        });
                    }
                });
    }

    private void updateSignList(Classifier.Recognition result, Bitmap bitmap) {

        SignEntity sign = getSignImage(result, bitmap);

        if (sign == null) return;

        ArrayList<SignEntity> list = new ArrayList<>(adapter.getSigns());

        if (list.isEmpty()) {
            addSignToAdapter(sign);
            return;
        }
        if (list.contains(sign)) {
            if (isRemoveValid(sign, list.get(list.indexOf(sign)))) {
                adapter.getSigns().remove(sign);
                addSignToAdapter(sign);
            }
        } else {
            addSignToAdapter(sign);
        }

    }

    private void addSignToAdapter(SignEntity sign) {
        adapter.setSign(sign);
        totalSignCounter++;
        updateStatsUI();
        triggerHaptic();

        if (notification.isChecked()) {
            mediaPlayerHolder.loadMedia(sign.getSoundNotification());
        }

        updateSpeedLimitDisplay(sign);
    }

    private void updateSpeedLimitDisplay(SignEntity sign) {
        String name = sign.getName();
        if (name != null && name.contains("speed limit")) {
            try {
                String numPart = name.replaceAll("[^0-9]", "");
                if (!numPart.isEmpty()) {
                    int limit = Integer.parseInt(numPart);
                    lastSpeedLimitValue = limit;
                    lastSpeedLimitTitle = name;
                    speedLimitValueTxt.setText(String.valueOf(limit));

                    if (data != null && data.getLocation() != null && data.getLocation().hasSpeed()) {
                        double currentSpeedKmh = data.getLocation().getSpeed() * 3.6;
                        updateSpeedLimitComparison(currentSpeedKmh);
                    } else {
                        speedLimitStatusTxt.setText(R.string.speed_limit_detected);
                        speedLimitStatusTxt.setTextColor(ContextCompat.getColor(this, R.color.safe_green));
                        speedLimitCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.limit_badge_bg));
                    }
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private void triggerHaptic() {
        if (hapticSwitch != null && hapticSwitch.isChecked() && vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    private boolean isRemoveValid(SignEntity sign1, SignEntity sign2) {
        return isTimeDifferenceValid(sign1.getDate(), sign2.getDate())
                || isLocationDifferenceValid(sign1.getLocation(), sign2.getLocation());
    }

    private boolean isTimeDifferenceValid(Date date1, Date date2) {
        long milliseconds = date1.getTime() - date2.getTime();
        Log.i("sign", "isTimeDifferenceValid " + ((milliseconds / (1000)) > 30));
        return (int) (milliseconds / (1000)) > 30;
    }

    private boolean isLocationDifferenceValid(Location location1, Location location2) {
        if (location1 == null || location2 == null)
            return false;
        return location1.distanceTo(location2) > 50;
    }

    @SuppressLint("DefaultLocale")
    private void setupLocation() {
        satellite = findViewById(R.id.satellite_info);
        status = findViewById(R.id.gps_status_info);
        accuracy = findViewById(R.id.accuracy_info);
        distance = findViewById(R.id.distanceValueTxt);
        totalDistance = findViewById(R.id.totalDistanceValueTxt);
        currentSpeed = findViewById(R.id.currentSpeedTxt);
    }

    private void showTotalDistance() {
        double distance = sharedPreferencesManager.getDistance();
        String distanceUnits;
        if (distance <= 1000.0) {
            distanceUnits = "m";
        } else {
            distance /= 1000.0;
            distanceUnits = "km";
        }
        totalDistance.setText(
                String.format("%.1f %s", distance, distanceUnits)
                        .replace(',', '.')
                        .replace(".0", ""));
    }

    private void setupRecycler() {
        adapter = new SignAdapter(this);

        RecyclerView signRecycler = findViewById(R.id.signRecycler);
        signRecycler.setAdapter(adapter);
        signRecycler.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }

    public void onGpsStatusChanged(GpsStatusEntity event) {
        satellite.setText(event.getSatellite());
        status.setText(event.getStatus());
        accuracy.setText(event.getAccuracy());
    }

    private void showGpsDisabledDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.gps_disabled))
                .setMessage(getString(R.string.please_enable_gps))
                .setNegativeButton(android.R.string.cancel, (dialog, id) -> dialog.cancel())
                .setPositiveButton(android.R.string.ok, (dialog, id) ->
                        startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS")));
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.setOnShowListener(arg -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.cod_gray));
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this, R.color.cod_gray));
        });
        dialog.show();
    }

    private SignEntity getSignImage(Classifier.Recognition result, Bitmap bitmap) {
        SignEntity sign = null;
        String title = result.getTitle();
        if (title == null) return null;

        switch (title) {
            case "crosswalk": sign = new SignEntity(title, R.drawable.crosswalk, R.raw.crosswalk); break;
            case "stop": sign = new SignEntity(title, R.drawable.stop, R.raw.stop); break;
            case "main road": sign = new SignEntity(title, R.drawable.main_road, R.raw.main_road); break;
            case "give road": sign = new SignEntity(title, R.drawable.give_road, R.raw.give_road); break;
            case "children": sign = new SignEntity(title, R.drawable.children, R.raw.children); break;
            case "dont stop": sign = new SignEntity(title, R.drawable.dont_stop, R.raw.dont_stop); break;
            case "no parking": sign = new SignEntity(title, R.drawable.no_parking, R.raw.no_parking); break;
            case "dont move": sign = new SignEntity(title, R.drawable.dont_move, R.raw.dont_move); break;
            case "dont enter": sign = new SignEntity(title, R.drawable.dont_enter, R.raw.dont_enter); break;
            case "dont overtake": sign = new SignEntity(title, R.drawable.no_overtake, R.raw.dont_overtake); break;
            case "speed limit 5": sign = new SignEntity(title, R.drawable.speed_limit_5, R.raw.speed_limit_5); break;
            case "speed limit 10": sign = new SignEntity(title, R.drawable.speed_limit_10, R.raw.speed_limit_10); break;
            case "speed limit 20": sign = new SignEntity(title, R.drawable.speed_limit_20, R.raw.speed_limit_20); break;
            case "speed limit 30": sign = new SignEntity(title, R.drawable.speed_limit_30, R.raw.speed_limit_30); break;
            case "speed limit 40": sign = new SignEntity(title, R.drawable.speed_limit_40, R.raw.speed_limit_40); break;
            case "speed limit 50": sign = new SignEntity(title, R.drawable.speed_limit_50, R.raw.speed_limit_50); break;
            case "speed limit 60": sign = new SignEntity(title, R.drawable.speed_limit_60, R.raw.speed_limit_60); break;
            case "speed limit 70": sign = new SignEntity(title, R.drawable.speed_limit_70, R.raw.speed_limit_70); break;
            case "speed limit 80": sign = new SignEntity(title, R.drawable.speed_limit_80, R.raw.speed_limit_80); break;
            case "speed limit 90": sign = new SignEntity(title, R.drawable.speed_limit_90, R.raw.speed_limit_90); break;
            case "speed limit 100": sign = new SignEntity(title, R.drawable.speed_limit_100, R.raw.speed_limit_100); break;
        }

        if (sign != null) {
            sign.setConfidenceDetection(result.getConfidence());
            sign.setScreenLocation(result.getLocation());
            if (data != null) {
                sign.setLocation(data.getLocation());
            }
        }

        return sign;
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void inject() {
        DaggerScreenComponent.builder()
                .applicationComponent(getApplicationComponent())
                .build()
                .inject(this);
    }

}
