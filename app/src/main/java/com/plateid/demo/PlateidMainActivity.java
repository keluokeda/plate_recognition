package com.plateid.demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kernal.plateid.CoreSetup;
import com.kernal.plateid.RxPlateRecognition;
import com.kernal.plateid.activity.PlateidCameraActivity;
import com.kernal.plateid.controller.CommonTools;
import com.kernal.plateid.controller.ImportPicRecog;
import com.kernal.plateid.controller.SNandTFAuth;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * @author user
 */
public class PlateidMainActivity extends FragmentActivity implements View.OnClickListener {
    private LinearLayout startLinearLayout;
    private TextView plateColor, plateId;
    private ImageView plateImage;
    private RelativeLayout endRelativeLayout;
    private ImportPicRecog importPicRecog;
    private CoreSetup coreSetup = new CoreSetup();
    /**
     * 动态授权需要的权限
     */
    static final String[] PERMISSION = new String[]{Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.VIBRATE, Manifest.permission.INTERNET,
    };
    private static final int PERMISSION_REQUESTCODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_plate);
        findViews();
    }

    private void findViews() {
        startLinearLayout = findViewById(R.id.startLinearLayout);
        Button activationButton = findViewById(R.id.activationButton);
        Button takePicButton = findViewById(R.id.takePicButton);
        Button automaticRecogButton = findViewById(R.id.automaticRecogButton);
        Button selectPicButton = findViewById(R.id.selectPicButton);
        ImageView returnButton = findViewById(R.id.plate_back);

        endRelativeLayout = findViewById(R.id.endRelativieLayout);
        plateColor = findViewById(R.id.plateColor);
        plateId = findViewById(R.id.plateId);
        plateImage = findViewById(R.id.plateImage);
        Button confirm = findViewById(R.id.confirm);


        activationButton.setOnClickListener(this);
        takePicButton.setOnClickListener(this);
        automaticRecogButton.setOnClickListener(this);
        selectPicButton.setOnClickListener(this);
        confirm.setOnClickListener(this);
        returnButton.setOnClickListener(this);

        endRelativeLayout.setVisibility(View.GONE);


        findViewById(R.id.rx_plate_recognition).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RxPlateRecognition(PlateidMainActivity.this)
                        .start("QMVPAMLUZYBFCGF")
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<String>() {
                            @Override
                            public void accept(String s) throws Exception {
                                log(s);

                            }
                        }, new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                throwable.printStackTrace();
                            }
                        });

            }
        });

    }

    private void log(String message) {
        Log.e("TAG", message);
    }

    @Override
    public void onClick(View v) {
        if (R.id.activationButton == v.getId()) {
            //激活按钮
            createViewToAuthService();
        } else if (R.id.takePicButton == v.getId()) {
            //手动拍照按钮
            Intent cameraIntent = new Intent(PlateidMainActivity.this, PlateidCameraActivity.class);
            coreSetup.takePicMode = true;
            cameraIntent.putExtra("coreSetup", coreSetup);
            startActivityForResult(cameraIntent, 1);
        } else if (R.id.automaticRecogButton == v.getId()) {
            //自动识别按钮
            Intent cameraIntent = new Intent(PlateidMainActivity.this, PlateidCameraActivity.class);
            coreSetup.takePicMode = false;
            cameraIntent.putExtra("coreSetup", coreSetup);
            startActivityForResult(cameraIntent, 1);
        } else if (R.id.selectPicButton == v.getId()) {
            //选择图片识别按钮
            //动态授权
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                //没有授权
                ActivityCompat.requestPermissions(this, PERMISSION, PERMISSION_REQUESTCODE);
            } else {
                importPicRecog = new ImportPicRecog(PlateidMainActivity.this);
                Intent selectIntent = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                Intent wrapperIntent = Intent.createChooser(selectIntent, "请选择一张图片");
                startActivityForResult(wrapperIntent, 2);
            }
        } else if (R.id.confirm == v.getId()) {
            // 确定按钮
            endRelativeLayout.setVisibility(View.GONE);
            startLinearLayout.setVisibility(View.VISIBLE);
        } else if (R.id.plate_back == v.getId()) {
            //返回按钮
            endRelativeLayout.setVisibility(View.GONE);
            startLinearLayout.setVisibility(View.VISIBLE);
        }
    }


    public void createViewToAuthService() {

        final EditText editText = new EditText(getApplicationContext());
        editText.setTextColor(Color.BLACK);
        new AlertDialog.Builder(PlateidMainActivity.this)
                .setTitle(R.string.dialog_title)
                .setView(editText)
                .setPositiveButton(R.string.license_verification, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //如果输入框里有序列号，就用输入框里的，如果没有就用默认设置的序列号
                        coreSetup.Sn = editText.getText().toString().toUpperCase();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (imm.isActive()) {
                            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
                        }
                        //在此传入序列号，激活
                        new SNandTFAuth(PlateidMainActivity.this, coreSetup.Sn);

                    }
                }).show();

    }


    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == 1 && resultCode == RESULT_OK) {
//            if (data != null) {
//                //获取到的识别结果
//                String[] recogResult = data.getStringArrayExtra("RecogResult");
//                //保存图片路径
//                String savePicturePath = data.getStringExtra("savePicturePath");
//                //是竖屏还是横屏
//                int screenDirection = data.getIntExtra("screenDirection", 0);
//                startLinearLayout.setVisibility(View.GONE);
//                endRelativeLayout.setVisibility(View.VISIBLE);
//                Bitmap bitmap;
//                int left, top, w, h;//设置现在图片的区域
//
//                if (recogResult[0] != null && !"".equals(recogResult[0])) {
//                    left = Integer.valueOf(recogResult[7]);
//                    top = Integer.valueOf(recogResult[8]);
//                    w = Integer.valueOf(recogResult[9])
//                            - Integer.valueOf(recogResult[7]);
//                    h = Integer.valueOf(recogResult[10])
//                            - Integer.valueOf(recogResult[8]);
//                    plateId.setText(recogResult[0]);
//                    plateColor.setText(recogResult[1]);
//                } else {
//                    if (screenDirection == 1 || screenDirection == 3) {
//                        left = coreSetup.preHeight / 24;
//                        top = coreSetup.preWidth / 4;
//                        w = coreSetup.preHeight / 24 + coreSetup.preHeight * 11 / 12;
//                        h = coreSetup.preWidth / 3;
//                    } else {
//                        left = coreSetup.preWidth / 4;
//                        top = coreSetup.preHeight / 4;
//                        w = coreSetup.preWidth / 2;
//                        h = coreSetup.preHeight - coreSetup.preHeight / 2;
//                    }
//                    plateId.setText("null");
//                    plateColor.setText("null");
//                }
//                bitmap = BitmapFactory.decodeFile(savePicturePath);
//                if (bitmap != null) {
//                    bitmap = Bitmap.createBitmap(bitmap, left, top, w, h);
//                    plateImage.setImageBitmap(bitmap);
//                }
//            }
//        } else {
//            if (data != null) {
//                Uri uri = data.getData();
//                String picPathString = CommonTools.getPath(PlateidMainActivity.this, uri);
//                //初始化和识别接口要有一个时间段，所以将初始化放在了上面，这里要注意下
//                //传入图片识别获取结果
//                String[] recogResult = importPicRecog.recogPicResults(picPathString);
//                plateId.setText(recogResult[0]);
//                plateColor.setText(recogResult[1]);
//                startLinearLayout.setVisibility(View.GONE);
//                endRelativeLayout.setVisibility(View.VISIBLE);
//                Bitmap bitmap = BitmapFactory.decodeFile(picPathString);
//                plateImage.setImageBitmap(bitmap);
//            }
//        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUESTCODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //用户点击了同意授权
                    importPicRecog = new ImportPicRecog(PlateidMainActivity.this);
                    Intent selectIntent = new Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    Intent wrapperIntent = Intent.createChooser(selectIntent, "请选择一张图片");
                    startActivityForResult(wrapperIntent, 2);
                } else {
                    //用户拒绝了授权
                    Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            if (endRelativeLayout.getVisibility() == View.VISIBLE) {
                endRelativeLayout.setVisibility(View.GONE);
                startLinearLayout.setVisibility(View.VISIBLE);
                return true;
            } else {
                finish();
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
