package com.riemann.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.riemann.camera.android.CameraGLSurfaceView;
import com.riemann.camera.android.Constant;
import com.riemann.camera.data.CameraEffectData;
import com.riemann.camera.data.HorizontalData;
import com.riemann.camera.ui.PreviewFrameLayout;
import com.riemann.camera.ui.Rotatable;
import com.riemann.camera.ui.RotateImageView;
import com.riemann.camera.ui.ShutterButton;
import com.riemann.camera.util.Util;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CameraActivity extends Activity {

    private static final String TAG = "CameraActivity";

    @BindView(R.id.btn_go_home)
    RotateImageView btnGoHome;
    @BindView(R.id.btn_flashlight)
    RotateImageView btnFlashlight;
    @BindView(R.id.btn_camera_switch)
    RotateImageView btnCameraSwitch;
    @BindView(R.id.btn_line)
    RotateImageView btnLine;
    @BindView(R.id.camera_top_layout)
    RelativeLayout cameraTopLayout;
    @BindView(R.id.gl_surface_view)
    CameraGLSurfaceView glSurfaceView;
    @BindView(R.id.preview_frame)
    PreviewFrameLayout previewFrame;
    @BindView(R.id.btn_thumbal)
    RotateImageView btnThumbal;
    @BindView(R.id.pb_take_photo)
    ProgressBar pbTakePhoto;
    @BindView(R.id.btn_takephoto)
    ShutterButton btnTakephoto;
    @BindView(R.id.cb_square_camera)
    CheckBox cbSquareCamera;
    @BindView(R.id.tv_camera_ratio)
    TextView tvCameraRatio;
    @BindView(R.id.camera_bottom)
    RelativeLayout cameraBottom;
    @BindView(R.id.filter_view)
    RecyclerView horizontalRecycleView;
    @BindView(R.id.image_view1)
    ImageView imageView1;

    private FilterRecyclerAdapter mFilterRecyclerAdapter;

    private MyOrientationEventListener mOrientationListener;
    private int mOrientation = OrientationEventListener.ORIENTATION_UNKNOWN;
    private int mOrientationCompensation = 0;

    private ArrayList<CameraEffectData> effectList = new ArrayList<CameraEffectData>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.camera_main);

        mOrientationListener = new MyOrientationEventListener(this);
        mOrientationListener.enable();

        ButterKnife.bind(this);
        glSurfaceView.enableView();

        readCameraEffect();
        loadCameraFilter();

        glSurfaceView.setThumbImageView(btnThumbal, pbTakePhoto);
        glSurfaceView.setActivity(CameraActivity.this);
    }

    private void loadCameraFilter() {
        ArrayList<HorizontalData> list = new ArrayList<HorizontalData>();
        for (int i = 0; i < effectList.size(); i++) {
            loadEffectList(list, i);
        }
        mFilterRecyclerAdapter = new FilterRecyclerAdapter(list);
        horizontalRecycleView.setAdapter(mFilterRecyclerAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        horizontalRecycleView.setLayoutManager(layoutManager);
        horizontalRecycleView.setItemAnimator(new DefaultItemAnimator());
        mFilterRecyclerAdapter.setOnItemClickLitener(new FilterRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                glSurfaceView.setFilterIndex(position);
            }
        });
    }

    private void loadEffectList(ArrayList<HorizontalData> list, int i) {
        Drawable drawable = null;
        try {
            InputStream is = getResources().getAssets().open(Constant.CAMERA_FILTER + File.separator + effectList.get(i).id);
            drawable = new BitmapDrawable(getResources(), is);
        } catch (Exception e) {
            Log.e(TAG, " exception " + e.getMessage());
            e.printStackTrace();
        }

        HorizontalData data = new HorizontalData(effectList.get(i).name, drawable, effectList.get(i).color, null, Integer.parseInt(effectList.get(i).id));
        list.add(data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mOrientationListener.enable();
        glSurfaceView.onResume();
    }

    @OnClick(R.id.btn_camera_switch)
    void switchCamera(){
        int index = glSurfaceView.getCameraIndex();
        if (index == Constant.CAMERA_ID_ANY || index == Constant.CAMERA_ID_BACK) {
            glSurfaceView.setCameraIndex(Constant.CAMERA_ID_FRONT);
        } else {
            glSurfaceView.setCameraIndex(Constant.CAMERA_ID_BACK);
        }
    }

    @OnClick(R.id.btn_thumbal)
    public void goToGallery(){
        glSurfaceView.goToCameraGallery();
    }

    @OnClick(R.id.btn_takephoto)
    public void takePhoto(){
        glSurfaceView.takePhoto();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOrientationListener.disable();
        if (glSurfaceView != null) {
            glSurfaceView.disableView();
            glSurfaceView.onDestory();
        }
    }


    private class MyOrientationEventListener extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == ORIENTATION_UNKNOWN)
                return;
            mOrientation = Util.roundOrientation(orientation, mOrientation);
            int orientationCompensation = mOrientation + Util.getDisplayRotation(CameraActivity.this);
            if (mOrientationCompensation != orientationCompensation) {
                mOrientationCompensation = orientationCompensation;
                setOrientationIndicator(mOrientationCompensation);
            }
            // Log.e(TAG, " mOrientation " + mOrientation +
            // " mOrientationCompensation " + mOrientationCompensation);

            glSurfaceView.setOrientation(mOrientation);
        }
    }

    private void setOrientationIndicator(int orientation) {
        Rotatable[] indicators = { btnFlashlight, btnCameraSwitch, btnLine, btnGoHome, btnThumbal };
        for (Rotatable indicator : indicators) {
            if (indicator != null) {
                indicator.setOrientation(orientation);
            }
        }
    }

//    @Override
//    public void onCameraViewStarted(int width, int height) {
//    }
//
//    @Override
//    public void onCameraViewStopped() {
//    }
//
//    @Override
//    public boolean onCameraTexture(int texIn, int texOut, int width, int height) {
//        return false;
//    }

    private void readCameraEffect() {
        try {
            InputStream is = getResources().getAssets().open(Constant.CAMERA_FILTER_XML);
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(is, "UTF-8");
            int evtType = xpp.getEventType();
            CameraEffectData cameraEffect = null;
            while (evtType != XmlPullParser.END_DOCUMENT) {
                switch (evtType) {
                    case XmlPullParser.START_TAG:
                        String tag = xpp.getName();
                        if ("filter".equals(tag)) {
                            cameraEffect = new CameraEffectData();
                            if ("id".equals(xpp.getAttributeName(0))) {
                                String idValue = xpp.getAttributeValue(0);
                                cameraEffect.id = idValue;
                            }
                            if ("name".equals(xpp.getAttributeName(1))) {
                                String nameValue = xpp.getAttributeValue(1);
                                cameraEffect.name = nameValue;
                            }
                            if ("color".equals(xpp.getAttributeName(2))) {
                                String colorValue = xpp.getAttributeValue(2);
                                cameraEffect.color = colorValue;
                            }
                            effectList.add(cameraEffect);
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    case XmlPullParser.END_DOCUMENT:
                        break;
                    default:
                        break;
                }
                evtType = xpp.next();
            }
        } catch (Exception e) {
            Log.e(TAG, " load assert exception " + e.getMessage());
        }
    }

}
