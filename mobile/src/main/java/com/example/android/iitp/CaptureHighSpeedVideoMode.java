package com.example.android.iitp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraConstrainedHighSpeedCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import wseemann.media.FFmpegMediaMetadataRetriever;

public class CaptureHighSpeedVideoMode extends Fragment
        implements View.OnClickListener, FragmentCompat.OnRequestPermissionsResultCallback {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private static final String TAG = "CaptureVideoMode";
    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private static final String FRAGMENT_DIALOG = "dialog";

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * Button to record video
     */
    private Button mRecButtonVideo;

    /**
     * A refernce to the opened {@link android.hardware.camera2.CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * A reference to the current {@link CameraCaptureSession} for
     * preview.
     */
    private CameraConstrainedHighSpeedCaptureSession mPreviewSessionHighSpeed;
    private CameraCaptureSession mPreviewSession;

    public static File videoFile;
    private String currentDateAndTime;
    private SensorEventListener mSensorEventListener;
    private float[] rotationMatrix;


    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size mPreviewSize;

    /**
     * The {@link android.util.Size} of video recording.
     */
    private Size mVideoSize;

    private Range<Integer>[] mVideoFps;

    /**
     * Camera preview.
     */
    private CaptureRequest.Builder mPreviewBuilder;


    /**
     * High Speed Camera Request-list
     */
//    private
    /**
     * MediaRecorder
     */
    private MediaRecorder mMediaRecorder;


    List<Surface> surfaces = new ArrayList<Surface>();

    /**
     * Whether the app is recording video now
     */
    private boolean mIsRecordingVideo = false;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraDevice = cameraDevice;
            startPreview();
            mCameraOpenCloseLock.release();
            if (null != mTextureView) {
                configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };

    public static CaptureHighSpeedVideoMode newInstance() {
        return new CaptureHighSpeedVideoMode();
    }

    /**
     * In this sample, we choose a video size with 3x4 aspect ratio. Also, we don't use sizes
     * larger than 1080p, since MediaRecorder cannot handle such a high-resolution video.
     *
     * @param choices The list of available sizes
     * @return The video size
     */
    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == 1280 && size.getHeight() <= 720) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
     * width and height are at least as large as the respective requested values, and whose aspect
     * ratio matches with the specified value.
     *
     * @param choices     The list of sizes that the camera supports for the intended output class
     * @param width       The minimum desired width
     * @param height      The minimum desired height
     * @param aspectRatio The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() <= width && option.getHeight() <= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.max(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.camera, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mRecButtonVideo = (Button) view.findViewById(R.id.video_record);
        mRecButtonVideo.setOnClickListener(this);
        View decorView = getActivity().getWindow().getDecorView();
        getActivity().getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

    }

    @Override
    public void onResume() {
        surfaces.clear();
        super.onResume();
        startBackgroundThread();
        View decorView = getActivity().getWindow().getDecorView();
        getActivity().getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#000000")));
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        if (mIsRecordingVideo) {
            stopRecordingVideoOnPause();
        }
        closeCamera();
        stopBackgroundThread();
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        View decorView = getActivity().getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        switch (view.getId()) {
            case R.id.video_record: {
                decorView.setSystemUiVisibility(uiOptions);
                if (mIsRecordingVideo) {
                    stopRecordingVideo();
                } else {
                    startRecordingVideo();
                }
                break;
            }
        }
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets whether you should show UI with rationale for requesting permissions.
     *
     * @param permissions The permissions your app wants to request.
     * @return Whether you can show permission rationale UI.
     */
    private boolean shouldShowRequestPermissionRationale(String[] permissions) {
        for (String permission : permissions) {
            if (FragmentCompat.shouldShowRequestPermissionRationale(this, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requests permissions needed for recording video.
     */
    private void requestVideoPermissions() {
        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            FragmentCompat.requestPermissions(this, VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            if (grantResults.length == VIDEO_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        ErrorDialog.newInstance("This sample needs camera permission.")
                                .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                        break;
                    }
                }
            } else {
                ErrorDialog.newInstance("This sample needs camera permission.")
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(getActivity(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Camcorder Profile
     */
    private Range<Integer>[] availableFpsRange;

    private void openCamera(int width, int height) {
        if (!hasPermissionsGranted(VIDEO_PERMISSIONS)) {
            requestVideoPermissions();
            return;
        }
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.d(TAG, "tryAcquire");
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            String cameraId = manager.getCameraIdList()[0];

            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            /*
      Tries to open a {@link CameraDevice}. The result is listened by `mStateCallback`.
     */
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            mVideoSize = chooseVideoSize(map.getHighSpeedVideoSizes());
            for (Size size : map.getHighSpeedVideoSizes()) {
                Log.d("RESOLUTION", size.toString());
            }
            mVideoFps = map.getHighSpeedVideoFpsRangesFor(mVideoSize);

            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, mVideoSize);

            // FPS
            availableFpsRange = map.getHighSpeedVideoFpsRangesFor(mVideoSize);
            int max = 0;
            int min;
            for (Range<Integer> r : availableFpsRange) {
                if (max < r.getUpper()) {
                    max = r.getUpper();
                }
            }
            min = max;
            for (Range<Integer> r : availableFpsRange) {
                if (min > r.getLower()) {
                    min = r.getUpper();
                }
            }
//            for(Range<Integer> r: availableFpsRange) {
//                if(min == r.getLower() && max == r.getUpper()) {
//                     mPreviewBuilder.set(CONTROL_AE_TARGET_FPS_RANGE,r);
//                    Log.d("RANGES", "[ " + r.getLower() + " , " + r.getUpper() + " ]");
//                }
//            }

            for (Range<Integer> r : availableFpsRange) {
                Log.d("RANGES", "[ " + r.getLower() + " , " + r.getUpper() + " ]");
            }
            Log.d("RANGE", "[ " + min + " , " + max + " ]");
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }
            configureTransform(width, height);
            mMediaRecorder = new MediaRecorder();
//            mMediaFormat = new MediaFormat();
            if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            manager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            ErrorDialog.newInstance("This device doesn't support Camera2 API.")
                    .show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Start the camera preview.
     */
    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {
            surfaces.clear();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            HandlerThread thread = new HandlerThread("CameraHighSpeedPreview");
            thread.start();

            if (mIsRecordingVideo) {
                setUpCaptureRequestBuilder(mPreviewBuilder);
                List<CaptureRequest> mPreviewBuilderBurst = mPreviewSessionHighSpeed.createHighSpeedRequestList(mPreviewBuilder.build());
                mPreviewSessionHighSpeed.setRepeatingBurst(mPreviewBuilderBurst, null, mBackgroundHandler);
            } else {
                mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Finds the framerate-range with the highest capturing framerate, and the lowest
     * preview framerate.
     *
     * @param fpsRanges A list contains framerate ranges.
     * @return The best option available.
     */
    private Range<Integer> getHighestFpsRange(Range<Integer>[] fpsRanges) {
        Range<Integer> fpsRange = Range.create(fpsRanges[0].getLower(), fpsRanges[0].getUpper());
        for (Range<Integer> r : fpsRanges) {
            if (r.getUpper() > fpsRange.getUpper()) {
                fpsRange.extend(0, r.getUpper());
            }
        }

        for (Range<Integer> r : fpsRanges) {
            if (r.getUpper() == fpsRange.getUpper()) {
                if (r.getLower() < fpsRange.getLower()) {
                    fpsRange.extend(r.getLower(), fpsRange.getUpper());
                }
            }
        }
        return fpsRange;
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
//        Range<Integer> fpsRange = Range.create(240, 240);
        Range<Integer> fpsRange = getHighestFpsRange(availableFpsRange);
        builder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange);

    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    //    private MediaFormat mMediaFormat;
    private void setUpMediaRecorder() throws IOException {
        final Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.WEBM);
        videoFile = getVideoFile(activity);
        mMediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mMediaRecorder.setVideoEncodingBitRate(20000000);
        mMediaRecorder.setVideoFrameRate(60);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.VP8);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int orientation = ORIENTATIONS.get(rotation);
        mMediaRecorder.setOrientationHint(orientation);
        mMediaRecorder.prepare();
    }

    /**
     * This method chooses where to save the video and what the name of the video file is
     *
     * @param context where the camera activity is
     * @return path + filename
     */
    private File getVideoFile(Context context) {
        String root = Environment.getExternalStorageDirectory().toString();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        currentDateAndTime = sdf.format(new Date());
        File myDir = new File(root + "/MSD/" + currentDateAndTime);
        myDir.mkdirs();

        return new File(myDir, "VIDEO" + ".webm");
    }

    private void startRecordingVideo() {
        try {
            // UI
            mIsRecordingVideo = true;
            surfaces.clear();
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
//            List<Surface> surfaces = new ArrayList<>();
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            Log.d("FPS", CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE.toString());

            mCameraDevice.createConstrainedHighSpeedCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    mPreviewSessionHighSpeed = (CameraConstrainedHighSpeedCaptureSession) mPreviewSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Activity activity = getActivity();
                    if (null != activity) {
                        Log.d("ERROR", "COULD NOT START CAMERA");
                        Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            }, mBackgroundHandler);
            mRecButtonVideo.setText("Stop");
            // Start recording
            mMediaRecorder.start();


//            mSensorEventListener = new SensorEventListener() {
//                float[] mGravity;
//                float[] mGeomagnetic;
//
//                @Override
//                public void onSensorChanged(SensorEvent sensorEvent) {
//                    if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
//                        mGravity = sensorEvent.values;
//                    if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
//                        mGeomagnetic = sensorEvent.values;
//                    if (mGravity != null && mGeomagnetic != null) {
//                        float R[] = new float[9];
//                        float I[] = new float[9];
//                        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
//                        if (success) {
//                        }
//                        rotationMatrix = R;
//                    }
//                }
//
//                @Override
//                public void onAccuracyChanged(Sensor sensor, int i) {
//
//                }
//            };
//
//            VideoHighFPSActivity.mSensorManager.registerListener(mSensorEventListener, VideoHighFPSActivity.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_FASTEST);
//            VideoHighFPSActivity.mSensorManager.registerListener(mSensorEventListener, VideoHighFPSActivity.mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_FASTEST);
//
//            Handler handler = new Handler();
//            handler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    VideoHighFPSActivity.mSensorManager.unregisterListener(mSensorEventListener);
//                }
//            }, 1000);



        } catch (IllegalStateException | IOException | CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void stopRecordingVideo() {
//        // UI
//        mIsRecordingVideo = false;
//        mRecButtonVideo.setText("Processing");
//        // Stop recording
//        try {
//            mPreviewSessionHighSpeed.stopRepeating();
//        } catch (CameraAccessException e) {
//            e.printStackTrace();
//        }
//
//        mMediaRecorder.stop();
//        mMediaRecorder.reset();
//        Activity activity = getActivity();
//        if (null != activity) {
//            Toast.makeText(activity, "Video saved: " + getVideoFile(activity),
//                    Toast.LENGTH_SHORT).show();
//        }
//        startPreview();




        mRecButtonVideo.setEnabled(false);
        // UI
        mIsRecordingVideo = false;
        VideoActivity.videoStopTimeInMillis = System.currentTimeMillis();
        mRecButtonVideo.setText("Processing...");
        // Stop recording
        try {
            mPreviewSessionHighSpeed.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        new TimeVideoStop(getActivity()).execute();
        new TimeServer(getActivity()).execute();

        mMediaRecorder.stop();
        mMediaRecorder.reset();
        Activity activity = getActivity();
        if (null != activity) {
            Toast.makeText(activity, "Video saved, please wait for a file to process...",
                    Toast.LENGTH_SHORT).show();
        }
        startPreview();

        File path = getContext().getExternalFilesDir(null);
        final File VideoData = new File(path, "VideoData.txt");

        final String filePath = videoFile.getPath();


        // Todo: Handle this
//        while(!(nClients == ServerConnectionActivity.mServerChatService.getIsAllTimeReceived())) {
////            // wait
////        }

        while (ServerConnectionActivity.mServerDataModel == null){
            //wait
        }

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(filePath);

        long duration = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));

        int height, width;

        FFmpegMediaMetadataRetriever mediaRetriever = new FFmpegMediaMetadataRetriever();
        mediaRetriever.setDataSource(filePath);

        Bitmap sample = mediaRetriever.getFrameAtTime(0);

        height = sample.getHeight();
        width = sample.getWidth();

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/MSD/" + currentDateAndTime);

        File jumpStats = new File(myDir, "jumpStats.txt");

        File rotationMatrixFile = new File(myDir, "rotationMatrixFileCamera.txt");

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(rotationMatrixFile, true), 1024);
            String entry = "";
            for (int i = 0; i < rotationMatrix.length; i++) {
                entry += rotationMatrix[i] + " ";
            }
            out.write(entry);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(jumpStats, true), 1024);
            String entry = "DeviceId, TargetTime, JumpStart, JumpEnd, VideoDuration, VideoEndUTC, DataStartUTC, DataDuration\n";
            out.write(entry);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long averageTime = 0;

        for (int i = 0; i < 1; i++) {

            Bitmap savedBitmap;

            long timeToSend = ServerConnectionActivity.mServerDataModel.getTargetTime();
            ArrayList<String> accData = ServerConnectionActivity.mServerDataModel.getAccData();
            ArrayList<String> horizontalAccelerationData = ServerConnectionActivity.mServerDataModel.getHorAccData();

            ArrayList<String> generalAccDataAlongX = ServerConnectionActivity.mServerDataModel.getGeneralAccDataAlongX();
            ArrayList<String> generalAccDataAlongY = ServerConnectionActivity.mServerDataModel.getGeneralAccDataAlongY();
            ArrayList<String> generalAccDataAlongZ = ServerConnectionActivity.mServerDataModel.getGeneralAccDataAlongZ();

            ArrayList<String> gravityX = ServerConnectionActivity.mServerDataModel.getGravityX();
            ArrayList<String> gravityY = ServerConnectionActivity.mServerDataModel.getGravityY();
            ArrayList<String> gravityZ = ServerConnectionActivity.mServerDataModel.getGravityZ();

            ArrayList<String> gyroscopeX = ServerConnectionActivity.mServerDataModel.getGyroscopeX();
            ArrayList<String> gyroscopeY = ServerConnectionActivity.mServerDataModel.getGyroscopeY();
            ArrayList<String> gyroscopeZ = ServerConnectionActivity.mServerDataModel.getGyroscopeZ();


//            ArrayList<String> rotationMatrixDataPhoneList = ServerConnectionActivity.mServerChatService.getConnectedThreads().get(i).getRotationMatrix();

            ArrayList<String> timeData = ServerConnectionActivity.mServerDataModel.getTimeData();
            long jumpStart = ServerConnectionActivity.mServerDataModel.getTimeJumpStart();
            long jumpEnd = ServerConnectionActivity.mServerDataModel.getTimeJumpEnd();
            long dataStartTime = ServerConnectionActivity.mServerDataModel.getDataStartTime();


            long diff = timeToSend - (VideoActivity.videoStopTimeInMillis - duration);
            averageTime += diff;
            jumpStart = jumpStart - (VideoActivity.videoStopTimeInMillis - duration);
            jumpEnd = jumpEnd - (VideoActivity.videoStopTimeInMillis - duration);

            //TODO: handle deviceID properly
            //String deviceId = deviceName + "_" + address;
            String deviceId = "SportWatch2";

            File deviceFolder = new File(myDir.getPath() + "/" + deviceId);
            deviceFolder.mkdirs();
            File dataFile = new File(deviceFolder, "VerticalAccelerationData_" + deviceId + ".txt");
            File dataHorizontalFile = new File(deviceFolder, "HorizontalAccelerationData_" + deviceId + ".txt");
            File generalAccDataFile = new File(deviceFolder, "GeneralAccData_" + deviceId + ".txt");
            File gravityDataFile = new File(deviceFolder, "GravityData_" + deviceId + ".txt");
            File gyroscopeDataFile = new File(deviceFolder, "GyroscopeData_" + deviceId + ".txt");
//            File rotationMatrixDataPhoneFile = new File(myDir, "RotationMatrixDataPhone_" + deviceId + ".txt");

            Log.d(TAG, "Time" + i + ": " + timeToSend);

            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(jumpStats, true), 1024);
                String entry = deviceId + ", " + diff + ", " + jumpStart + ", " + jumpEnd + ", " + duration + ", " + VideoActivity.videoStopTimeInMillis + ", " + dataStartTime + ", " + timeData.get(timeData.size() - 1) + "\n";
                out.write(entry);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (int j = 0; j < accData.size(); j++) {
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(dataFile, true), 1024);
                    String entry = accData.get(j) + " " + timeData.get(j) + "\n";
                    out.write(entry);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (int j = 0; j < horizontalAccelerationData.size(); j++) {
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(dataHorizontalFile, true), 1024);
                    String entry = horizontalAccelerationData.get(j) + " " + timeData.get(j) + "\n";
                    out.write(entry);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (int j = 0; j < generalAccDataAlongX.size(); j++) {
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(generalAccDataFile, true), 1024);
                    String entry = generalAccDataAlongX.get(j) + " " + generalAccDataAlongY.get(j) + " " + generalAccDataAlongZ.get(j) + " " + timeData.get(j) + "\n";
                    out.write(entry);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (int j = 0; j < gravityX.size(); j++) {
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(gravityDataFile, true), 1024);
                    String entry = gravityX.get(j) + " " + gravityY.get(j) + " " + gravityZ.get(j) + " " + timeData.get(j) + "\n";
                    out.write(entry);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (int j = 0; j < gyroscopeX.size(); j++) {
                try {
                    BufferedWriter out = new BufferedWriter(new FileWriter(gyroscopeDataFile, true), 1024);
                    String entry = gyroscopeX.get(j) + " " + gyroscopeY.get(j) + " " + gyroscopeZ.get(j) + " " + timeData.get(j) + "\n";
                    out.write(entry);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        Toast.makeText(getContext(), "All data is saved!", Toast.LENGTH_SHORT).show();
    }

    private void stopRecordingVideoOnPause() {
        mIsRecordingVideo = false;
        try {
            mPreviewSessionHighSpeed.stopRepeating();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        mMediaRecorder.stop();

        mMediaRecorder.reset();
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }

    public static class ErrorDialog extends DialogFragment {

        private static final String ARG_MESSAGE = "message";

        public static ErrorDialog newInstance(String message) {
            ErrorDialog dialog = new ErrorDialog();
            Bundle args = new Bundle();
            args.putString(ARG_MESSAGE, message);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();
            return new AlertDialog.Builder(activity)
                    .setMessage(getArguments().getString(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activity.finish();
                        }
                    })
                    .create();
        }

    }

    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage("This sample needs camera permission.")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentCompat.requestPermissions(parent, VIDEO_PERMISSIONS,
                                    REQUEST_VIDEO_PERMISSIONS);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    parent.getActivity().finish();
                                }
                            })
                    .create();
        }

    }

}