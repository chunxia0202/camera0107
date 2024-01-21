package com.example.camera0107;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {
    private final static String TAG = MainActivity.class.getSimpleName();
    TextureView mTexture;
    CameraManager cameraManagerr;
    CameraDevice mCameraDevice;
    Handler mainHandler;
    CaptureRequest.Builder captureRequestBuilder;
    ImageReader mImageReader;
    CameraCaptureSession mCameraCaptureSession;
    ImageView mImageView;
    private String[] permissions = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PermissionsUtils.getInstance().checkPermissions(permissions, permissionsResult);
        mTexture = (TextureView) findViewById(R.id.mine_texture);
        mImageView = (ImageView) findViewById(R.id.mine_picture);

        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });
        mTexture.setSurfaceTextureListener(this);
    }

    /**
     * 创建监听权限的接口对象
     */
    private PermissionsUtils.IPermissionsResult permissionsResult = new PermissionsUtils.IPermissionsResult() {
        @Override
        public void requestPermissions(String[] permissions, int requestCode) {
            ActivityCompat.requestPermissions(MainActivity.this, permissions, requestCode);
        }
        @Override
        public void passPermissons() {
            //授权后的操作
            //获取相机管理类的实例
        }
        @Override
        public void forbitPermissons() {
            //未授权，请手动授权
        }
        @Override
        public void positiveClick(Intent intent) {
            startActivity(intent);
            finish();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[]
            permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //就多一个参数this
        PermissionsUtils.getInstance().onRequestPermissionsResult(this.getPackageName(), requestCode, permissions, grantResults);
    }

    public void init() {
        cameraManagerr = (CameraManager) getSystemService(CAMERA_SERVICE);
        mainHandler = new Handler(getMainLooper());
        mImageReader = ImageReader.newInstance(mTexture.getWidth(), mTexture.getHeight(), ImageFormat.JPEG, 2);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader imageReader) {
                // 拿到拍照照片数据
                Image image = imageReader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);//由缓冲区存入字节数组
                final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    mImageView.setImageBitmap(bitmap);
                }
                image.close();
            }
        }, mainHandler);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            cameraManagerr.openCamera(String.valueOf(0), new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    mCameraDevice = cameraDevice;
                    afterOpenCamera();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    mCameraDevice.close();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    Log.e(TAG, cameraDevice.toString() + i);
                }
            }, mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        Log.i("message","start init");
        init();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

    }
    //打开摄像头之后要做的事情
    public void afterOpenCamera(){
        SurfaceTexture surfaceTexture = mTexture.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mTexture.getWidth(),mTexture.getHeight());
        Surface surface = new Surface(surfaceTexture);
        try {
            captureRequestBuilder  = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            Log.i("request","request success");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        captureRequestBuilder.addTarget(surface);
//        captureRequestBuilder.addTarget(mImageReader.getSurface());
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(surface,mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try {
                        mCameraCaptureSession = cameraCaptureSession;
                        cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                        },mainHandler);
//                        mCameraCaptureSession.capture(captureRequestBuilder.build(),null,mainHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }
    //拍照
    public void takePicture(){
        captureRequestBuilder.addTarget(mImageReader.getSurface());
        try {
            mCameraCaptureSession.capture(captureRequestBuilder.build(),null,mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
