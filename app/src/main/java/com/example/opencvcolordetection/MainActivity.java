package com.example.opencvcolordetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import com.google.common.util.concurrent.ListenableFuture;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private int REQUEST_CODE_FOR_PERMISSIONS = 1234;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};

    private PreviewView previewView;

    private Camera camera = null;
    private Preview preview = null;
    private ImageAnalysis imageAnalysis = null;
    private ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
    private Mat yuv;
    private int count = 0;
    private int total = 0;
    private int pixel;
    private int red;
    private int green;
    private int blue;
    private int totalColor;
    private int sumRed = 0;
    private int sumGreen = 0;
    private int sumBlue = 0;
    private int average = 0;

    public BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    yuv = new Mat();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        previewView = findViewById(R.id.previewView);

        if (checkPermissions()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_FOR_PERMISSIONS);
        }
    }

    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void startCamera() {
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        Context context = this;

        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    preview = new Preview.Builder().build();
                    imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(176, 144))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                    imageAnalysis.setAnalyzer(cameraExecutor, new MyImageAnalyzer());
                    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
                    cameraProvider.unbindAll();
                    camera = cameraProvider.bindToLifecycle((LifecycleOwner) context, cameraSelector, preview, imageAnalysis);
                    preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.getCameraInfo()));
                } catch (Exception e) {
                }
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private class MyImageAnalyzer implements ImageAnalysis.Analyzer {
        private Mat matPrevious = null;

        @Override
        public void analyze(@NonNull ImageProxy image) {

            Mat matOrg = getMatFromImage(image);

            Mat mat = fixMatRotation(matOrg);

            if (matPrevious == null) matPrevious = mat;
            matPrevious = mat;

            Bitmap bitmap = Bitmap.createBitmap(matPrevious.cols(), matPrevious.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(matPrevious, bitmap);

            sumRed=0;
            sumBlue=0;
            sumGreen=0;

            for (int x = 0; x <= 160; x += 10) {
                for (int y = 0; y <= 130; y += 10) {
                    pixel = bitmap.getPixel(x, y);
                    red = Color.red(pixel);
                    green = Color.green(pixel);
                    blue = Color.blue(pixel);
                    sumRed += red;
                    sumGreen += green;
                    sumBlue += blue;
                }
            }

            Log.d("here", "ここ" + sumRed + "   " + sumGreen + "   " + "   " + sumBlue);
            totalColor = sumRed + sumBlue + sumGreen;
            total += totalColor;
            count++;

            if (count == 50) {
                average = total / count;
                Log.d("average", String.valueOf(average));
                count = 0;
                total = 0;
            }

            image.close();
        }

        private Mat getMatFromImage(ImageProxy image) {
            ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
            ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
            ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();
            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();
            byte[] nv21 = new byte[ySize + uSize + vSize];
            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);
            yuv = new Mat(image.getHeight() + image.getHeight() / 2, image.getWidth(), CvType.CV_8UC1);
            yuv.put(0, 0, nv21);
            Mat mat = new Mat();
            Imgproc.cvtColor(yuv, mat, Imgproc.COLOR_YUV2RGB_NV21, 3);
            return mat;
        }

        private Mat fixMatRotation(Mat matOrg) {
            Mat mat;
            switch (previewView.getDisplay().getRotation()) {
                default:
                case Surface.ROTATION_0:
                    mat = new Mat(matOrg.cols(), matOrg.rows(), matOrg.type());
                    Core.transpose(matOrg, mat);
                    Core.flip(mat, mat, 1);
                    break;
                case Surface.ROTATION_90:
                    mat = matOrg;
                    break;
                case Surface.ROTATION_270:
                    mat = matOrg;
                    Core.flip(mat, mat, -1);
                    break;
            }
            return mat;
        }
    }

    private boolean checkPermissions() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_FOR_PERMISSIONS) {
            if (checkPermissions()) {
                startCamera();
            } else {
                this.finish();
            }
        }
    }
}