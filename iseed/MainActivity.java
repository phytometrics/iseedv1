// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package jp.phytometrics.iseed;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import jp.phytometrics.iseed.env.ImageUtils;
import jp.phytometrics.iseed.env.Logger;
import jp.phytometrics.iseed.env.Utils;

//import org.pytorch.IValue;
//import org.pytorch.Module;
//import org.pytorch.PyTorchAndroid;
//import org.pytorch.Tensor;
//import org.pytorch.torchvision.TensorImageUtils;

// important library for Google adMob
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
//import com.google.android.gms.ads.InterstitialAd;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.yalantis.ucrop.UCrop;


//import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
//import org.tensorflow.lite.support.image.ImageProcessor;
//import org.tensorflow.lite.support.image.TensorImage;
//import org.tensorflow.lite.support.image.ops.ResizeOp;

import jp.phytometrics.iseed.tflite.Classifier;
import jp.phytometrics.iseed.tflite.YoloV5Classifier;
import java.util.regex.Pattern;


//https://protocoderspoint.com/floating-action-button-animation-fab-menu-example/

public class MainActivity extends AppCompatActivity implements Runnable {


    public static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private int mImageIndex = 0;
    private String[] mTestImages;

    private ImageView mImageView;
    private ResultView mResultView;
    private Button mButtonDetect;
    private ProgressBar mProgressBar;
    private TextView mSourceView;
    private TextView resultTextView;

    private FloatingActionButton detectIcon;
    private FloatingActionButton photoIcon;
    private FloatingActionButton cameraIcon;
    private FloatingActionButton cropIcon;
    private FloatingActionButton exIcon;
    private FloatingActionButton helpIcon;
    private FloatingActionButton mailIcon;
    private FloatingActionButton settingsIcon;
    private FloatingActionButton shareIcon;
    private Boolean isAllFabsVisible;
    private Boolean detected;

    private String numberOfObject = "0";
    private String textdef = "iSeed";

    private Bitmap mBitmap = null;
    //private Module mModule = null;

    private float mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY;

    //creating Object of AdLoader
    private AdView mBannerAd;
    //private InterstitialAd mInterstitialAd;

    private final int cameraRequestCode = 12;
    private final int cropRequestCode = 6;

    private File storageDir;
    private String currentPhotoPath;
    private File preCroppedFile = null; //beforecrop file
    //private File postCroppedFile = null; //postcrop file
    //private Uri prephotoURI;
    //private Uri postphotoURI;
    private Uri currentURI;

    static String[] mClasses;

    private int analysisCount;
    private TextView countTextView;

    private int flagToast = 0;
    private long SPEED;
    private long startTime;
    private static final int REQUEST_CODE_PERMISSION = 1000;

    public List<Classifier.Recognition> results;

    //tflite
    Interpreter.Options options = new Interpreter.Options();
    CompatibilityList compatList = new CompatibilityList();

    private Classifier detector;
    public static final int TF_OD_API_INPUT_SIZE = 1280;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final Logger LOGGER = new Logger();
    private static final String TF_OD_API_MODEL_FILE = "version_20210624.model";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/models/seed.txt";
    // Minimum detection confidence to track a detection.
    private static final boolean MAINTAIN_ASPECT = true;
    private Integer sensorOrientation = 90;
    private MappedByteBuffer mTFModel;
    //    private Classifier detector;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
//    private MultiBoxTracker tracker;
//    private OverlayView trackingOverlay;

    protected int previewWidth = 1280;
    protected int previewHeight = 1280;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //initialization of configuration parameters
        Configs.main();
        detected = false;

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_CODE_PERMISSION);

        //admob
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {}
        });

        mBannerAd = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();

        //mBannerAd.setAdSize(AdSize.BANNER); deprecated
        //mBannerAd.setAdUnitId("ca-app-pub-3940256099942544/6300978111"); deprecated
        mBannerAd.loadAd(adRequest);

//        mInterstitialAd = new InterstitialAd(this);
//        mInterstitialAd.setAdUnitId("ca-app-pub-5611724983023421/8224235416");
//        mInterstitialAd.loadAd(new AdRequest.Builder().build());
//
//        mInterstitialAd.setAdListener(new AdListener(){
//           @Override
//           public void onAdLoaded() {
//           }
//            @Override
//            public void onAdClosed() {
//                AdRequest adRequest = new AdRequest.Builder().build();
//                mInterstitialAd.loadAd(adRequest);
//            }
//        });

        //admob end

        mSourceView = findViewById(R.id.sourceView);

        mImageView = findViewById(R.id.imageView);
        mImageView.setImageBitmap(mBitmap);
        mResultView = findViewById(R.id.resultView);
        mResultView.setVisibility(View.INVISIBLE);
        resultTextView = findViewById(R.id.textView);
        resultTextView.setText("iSeed");
        countTextView = findViewById(R.id.countView);

        photoIcon = findViewById((R.id.photoLibraryIcon));
        settingsIcon = findViewById(R.id.settings);
        mailIcon = findViewById(R.id.mailIcon);
        helpIcon = findViewById(R.id.helpIcon);
        shareIcon = findViewById(R.id.shareIcon);
        detectIcon = findViewById(R.id.detectIcon);
        cameraIcon = findViewById(R.id.cameraIcon);
        exIcon = findViewById(R.id.exIcon);
        cropIcon = findViewById(R.id.crop);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        mailIcon.setVisibility(View.GONE);
        helpIcon.setVisibility(View.GONE);
        shareIcon.setVisibility(View.GONE);
        isAllFabsVisible = false;

        shareIcon.setEnabled(false);
        mailIcon.setEnabled(false);
        cropIcon.setEnabled(false);

        shareIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showShareChooser();
            }
        });

        mailIcon.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                String[] addresses = {"iseedfeedback@phytometrics.jp"};
                Intent mailintent = new Intent(Intent.ACTION_SEND_MULTIPLE);
                ArrayList<Uri> imageUris = new ArrayList<Uri>();
                OutputStream fOut1;
                OutputStream fOut2;
                File file1;
                File file2;
                String body;

                body = String.format("#iSeed Feedback form.\nSent image will be used for model update." +
                        "By sending data through this form, user agrees to the copyright waiver " +
                        "and the free use of said images in this application.\n\n" +
                        "(optional) This image is a seed of:\n" +
                        "%s \n" +
                        "Speed (sec): %.3f \n",
                        TF_OD_API_MODEL_FILE, SPEED/1000.0f);
                body += Build.MANUFACTURER + "_" + Build.MODEL + "_" + Build.VERSION.RELEASE + "_" + Build.VERSION.SDK_INT;
                //mailintent.setAction();
                mailintent.setData(Uri.parse("mailto:")); // only email apps should handle this
                mailintent.setType("text/plain,image/*");
                mailintent.putExtra(Intent.EXTRA_EMAIL, addresses);
                mailintent.putExtra(Intent.EXTRA_SUBJECT, "iSeed Feedback");
                mailintent.putExtra(Intent.EXTRA_TEXT, body);

                if (mBitmap != null){

                    try {
                        file1 = createImageFile();
                        fOut1 = new FileOutputStream(file1);
                        mBitmap.compress(Bitmap.CompressFormat.JPEG,100,fOut1);
                        fOut1.flush();

                        Uri currentURI1 = FileProvider.getUriForFile(MainActivity.this,
                                BuildConfig.APPLICATION_ID+".provider",
                                file1);
                        imageUris.add(currentURI1);

                    } catch (IOException ex) {
                        System.out.println("Error occurred while creating the File");
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (detected == true && results != null){
                        try {
                            Bitmap mBitmap2 = mBitmap.copy(mBitmap.getConfig(),true);
                            mBitmap2 = Bitmap.createScaledBitmap(mBitmap2, Configs.mInputWidth, Configs.mInputHeight, true);
                            Canvas canvas = new Canvas(mBitmap2);
                            Paint mPaint = new Paint();
                            mPaint.setColor(Color.MAGENTA);
                            //draw on bitmap
                            for (Classifier.Recognition result : results) {
                                RectF location = result.getLocation();
                                if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                                    //canvas.drawRect(location, mPaintCircle);
                                    //float centerx =  (location.left + location.right) / 2;
                                    //float centery = (location.top + location.bottom) / 2;

                                    float centerx = (Math.max(0,(location.left -mStartX) / mIvScaleX / mImgScaleX) +
                                            Math.min(mBitmap2.getWidth() -1,(location.right -mStartX) / mIvScaleX / mImgScaleX))/2;
                                    float centery = (Math.max(0,(location.top -mStartY) / mIvScaleY / mImgScaleY) +
                                            Math.min(mBitmap2.getHeight() -1,(location.bottom -mStartY) / mIvScaleY / mImgScaleY))/2;

                                    canvas.drawCircle(centerx, centery, 5.3f, mPaint);
                                    //System.out.println(centery);
                                }
                            }

                            file2 = createImageFile();
                            fOut2 = new FileOutputStream(file2);
                            mBitmap2.compress(Bitmap.CompressFormat.JPEG,100,fOut2);
                            fOut2.flush();

                            Uri currentURI2 = FileProvider.getUriForFile(MainActivity.this,
                                    BuildConfig.APPLICATION_ID+".provider",
                                    file2);
                            imageUris.add(currentURI2);

                        } catch (IOException ex) {
                            System.out.println("Error occurred while creating the File");
                        }catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    for (Uri i : imageUris){
                        System.out.println(i);
                    }

                    mailintent.putExtra(Intent.EXTRA_STREAM,imageUris);

                }

                if (mailintent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mailintent);
                }
            }
        });

        settingsIcon.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                if (!isAllFabsVisible) {
                    mailIcon.show();
                    helpIcon.show();
                    shareIcon.show();
                    isAllFabsVisible = true;

                }else{
                    mailIcon.hide();
                    helpIcon.hide();
                    shareIcon.hide();
                    isAllFabsVisible = false;
                }
            }
        });

        helpIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                Intent intent = new Intent(getApplicationContext(), HelpActivity.class);
                startActivity(intent);

            }
        });

        photoIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v){
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto , 1);
            }
        });

        detectIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //mButtonDetect.setEnabled(false);
                startTime = System.currentTimeMillis();

                detectIcon.setEnabled(false);

                analysisCount++;
                countTextView.setText(String.format("Detection Trial Count: %d", analysisCount));
//                if (analysisCount % 9 == 0) {
//                    countTextView.setText("Ad will apear at next detection");
//                }else{
//                    countTextView.setText(String.format("Detection Trial Count: %d", analysisCount));
//                }
//                if (analysisCount % 10 == 0 && mInterstitialAd.isLoaded()) {
//                    mInterstitialAd.show();}else {
//                    //Log.d("TAG", "The interstitial wasn't loaded yet.");
//                }
                mProgressBar.setVisibility(ProgressBar.VISIBLE);
                //mButtonDetect.setText(getString(R.string.run_model));

                mImgScaleX = (float)mBitmap.getWidth() / Configs.mInputWidth;
                mImgScaleY = (float)mBitmap.getHeight() / Configs.mInputHeight;

                mIvScaleX = (mBitmap.getWidth() > mBitmap.getHeight() ? (float)mImageView.getWidth() / mBitmap.getWidth() : (float)mImageView.getHeight() / mBitmap.getHeight());
                mIvScaleY  = (mBitmap.getHeight() > mBitmap.getWidth() ? (float)mImageView.getHeight() / mBitmap.getHeight() : (float)mImageView.getWidth() / mBitmap.getWidth());

                mStartX = (mImageView.getWidth() - mIvScaleX * mBitmap.getWidth())/2;
                mStartY = (mImageView.getHeight() -  mIvScaleY * mBitmap.getHeight())/2;

                Thread thread = new Thread(MainActivity.this);

                thread.start();
            }
        });

        cameraIcon.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                resultTextView.setText("iSeed");
                try {
                    preCroppedFile = createImageFile();
                } catch (IOException ex) {
                    System.out.println("Error occurred while creating the File");
                    resultTextView.setText("Error at parsing createImageFile()");
                }
                Intent takePicture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

                currentURI = FileProvider.getUriForFile(MainActivity.this,
                        BuildConfig.APPLICATION_ID+".provider",
                        preCroppedFile);
                takePicture.putExtra(MediaStore.EXTRA_OUTPUT, currentURI);
                startActivityForResult(takePicture, cameraRequestCode);


            }
        });

        cropIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                UCrop.of(currentURI, Uri.fromFile(new File(getCacheDir(), "camera_image.jpg")))
                        .withAspectRatio(1, 1)
//                                    .withMaxResultSize(640, 640)
                        .start(MainActivity.this);
            }
        });

        exIcon.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                detected = false;
                shareIcon.setEnabled(false);
                mailIcon.setEnabled(false);

                detectIcon.setEnabled(true);
                cropIcon.setEnabled(false);
                resultTextView.setText("");
                mResultView.setVisibility(View.INVISIBLE);
                mImageIndex = (mImageIndex + 1) % mTestImages.length;
                try {
                    mBitmap = BitmapFactory.decodeStream(getAssets().open("test_images/" + mTestImages[mImageIndex]));
                    mImageView.setImageBitmap(mBitmap);
                    System.out.println(mTestImages[mImageIndex]);
                    mSourceView.setText(mTestImages[mImageIndex].split(Pattern.quote("."))[0].split("__")[1]);
                } catch (IOException e) {
                    Log.e("Object Detection", "Error reading assets", e);
                    finish();
                mImageView.setImageBitmap(mBitmap);




            }
        }
        });

        initBox();
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();

        previewHeight = TF_OD_API_INPUT_SIZE;
        previewWidth = TF_OD_API_INPUT_SIZE;
//        frameToCropTransform =
//                ImageUtils.getTransformationMatrix(
//                        previewWidth, previewHeight,
//                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
//                        sensorOrientation, MAINTAIN_ASPECT);
//
//        cropToFrameTransform = new Matrix();
//        frameToCropTransform.invert(cropToFrameTransform);
//
//        tracker = new MultiBoxTracker(this);
//        trackingOverlay = findViewById(R.id.tracking_overlay);
//        trackingOverlay.addCallback(
//                canvas -> tracker.draw(canvas));
//
//        tracker.setFrameConfiguration(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, sensorOrientation);
        try {
            mTestImages = getAssets().list("test_images/");


            //currentURI = Uri.fromFile(new File("//android_asset/"+mTestImages[mImageIndex]));
            //mBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), currentURI);
            mBitmap = BitmapFactory.decodeStream(getAssets().open("test_images/" + mTestImages[mImageIndex]));

            mSourceView.setText(mTestImages[mImageIndex].split(Pattern.quote("."))[0].split("__")[1]);
            mImageView.setImageBitmap(mBitmap);

            //mBitmap = BitmapFactory.decodeStream(getAssets().open("images/first_screen.png"));
        } catch (IOException e) {
            Log.e("Object Detection", "Error reading assets", e);
            finish();
        }

        try {
            detector =
                    YoloV5Classifier.create(
                            getAssets(),
                            "models/"+TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_IS_QUANTIZED,
                            TF_OD_API_INPUT_SIZE);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
    }

    private void initBox() {
        previewHeight = TF_OD_API_INPUT_SIZE;
        previewWidth = TF_OD_API_INPUT_SIZE;
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

//        tracker = new MultiBoxTracker(this);
//        trackingOverlay = findViewById(R.id.tracking_overlay);
//        trackingOverlay.addCallback(
//                canvas -> tracker.draw(canvas));
//
//        tracker.setFrameConfiguration(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, sensorOrientation);

        try {
            detector =
                    YoloV5Classifier.create(
                            getAssets(),
                            "models/"+TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_IS_QUANTIZED,
                            TF_OD_API_INPUT_SIZE);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //String imageFileName = "JPEG_" + timeStamp + "_";
        String imageFileName = "JPEG"; //to avoid increase in cache size. same name.

//        //two problems maybe externalcache does
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//            // Android 10 (api 29) 以上のバージョンの処理
//            System.out.println("_________android 29 and up");
//            //storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//            // leaving this here for future functionality to save image to cell phnone
//            storageDir = getCacheDir();
//        } else {
//            System.out.println("_________android 28 and down");
//            storageDir = getCacheDir();
//            //  Android XXX 未満のバージョンの処理
//            //Environment.getExternalStorageDirectory() + "/"+FolderName);
//        }
//        File image = File.createTempFile(
//                imageFileName,  /* prefix */
//                ".jpg",         /* suffix */
//                storageDir      /* directory */
//        );
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                getCacheDir()      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        String currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void showShareChooser() {
        OutputStream fOut1;
        OutputStream fOut2;
        File file1;
        File file2;
        String body;

        body = String.format("#iSeed\n" +
                        "Model Ver.: %s \n" +
                        "Processing Speed (sec): %.3f \n",
                TF_OD_API_MODEL_FILE, SPEED/1000.0f);
        body += Build.MANUFACTURER + "_" + Build.MODEL + "_" + Build.VERSION.RELEASE + "_" + Build.VERSION.SDK_INT;

        ShareCompat.IntentBuilder builder = ShareCompat.IntentBuilder.from(this);
        builder.setChooserTitle("") // シェアする時のタイトル
                .setSubject("") // 件名。使われ方はシェアされた側のアプリによる
                .setText(body) // 本文。使われ方はシェアされた側のアプリによる
//                .setStream(uri) // ファイルをシェアする時は、そのURIを指定
                .setType("image/*"); // ストリームで指定したファイルのMIMEタイプ


        if (mBitmap != null){

            try {
                file1 = createImageFile();
                fOut1 = new FileOutputStream(file1);
                mBitmap.compress(Bitmap.CompressFormat.JPEG,100,fOut1);
                fOut1.flush();


                Uri currentURI1 = FileProvider.getUriForFile(MainActivity.this,
                        BuildConfig.APPLICATION_ID+".provider",
                        file1);



                builder.addStream(currentURI1);


            } catch (IOException ex) {
                System.out.println("Error occurred while creating the File");
            }catch (Exception e) {
                e.printStackTrace();
            }
            if (detected && results != null){
                try {
                    Bitmap mBitmap2 = mBitmap.copy(mBitmap.getConfig(),true);
                    mBitmap2 = Bitmap.createScaledBitmap(mBitmap2, Configs.mInputWidth, Configs.mInputHeight, true);
                    Canvas canvas = new Canvas(mBitmap2);
                    Paint mPaint = new Paint();
                    mPaint.setColor(Color.MAGENTA);
                    //draw on bitmap
                    for (Classifier.Recognition result : results) {
                        RectF location = result.getLocation();
                        if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                            //canvas.drawRect(location, mPaintCircle);
                            //float centerx =  (location.left + location.right) / 2;
                            //float centery = (location.top + location.bottom) / 2;

                            float centerx = (Math.max(0,(location.left -mStartX) / mIvScaleX / mImgScaleX) +
                                    Math.min(mBitmap2.getWidth() -1,(location.right -mStartX) / mIvScaleX / mImgScaleX))/2;
                            float centery = (Math.max(0,(location.top -mStartY) / mIvScaleY / mImgScaleY) +
                                    Math.min(mBitmap2.getHeight() -1,(location.bottom -mStartY) / mIvScaleY / mImgScaleY))/2;

                            canvas.drawCircle(centerx, centery, 5.3f, mPaint);
                            //System.out.println(centery);
                        }
                    }

                    file2 = createImageFile();
                    fOut2 = new FileOutputStream(file2);
                    mBitmap2.compress(Bitmap.CompressFormat.JPEG,100,fOut2);
                    fOut2.flush();

                    Uri currentURI2 = FileProvider.getUriForFile(MainActivity.this,
                            BuildConfig.APPLICATION_ID+".provider",
                            file2);
                    //imageUris.add(currentURI2);
                    builder.addStream(currentURI2);

                } catch (IOException ex) {
                    System.out.println("Error occurred while creating the File");
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        // URIに対する読み取り権限を付与する
        Intent intent = builder.createChooserIntent().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // コールバックを受け取りたい場合は、そのインテントを使ってアクティビティを開始する
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }

        // 結果を受け取らなくても良い場合は、ビルダーからそのまま開始できる
        // builder.startChooser();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_CODE_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    finish();
                }

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //resultTextView.setText(String.format("onActivityResultMethod%s",String.valueOf(resultCode)));
        if (resultCode != RESULT_CANCELED) {
            switch (requestCode) {
                case 0:
                    //took a picture. deprecated
                    if (resultCode == RESULT_OK && data != null) {
                        mBitmap = (Bitmap) data.getExtras().get("data");
                        Matrix matrix = new Matrix();
                        matrix.postRotate(90.0f);
                        mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
                        mImageView.setImageBitmap(mBitmap);

                    }
                    break;

                case 1:
                    //choose from photos
                    //resultTextView.setText("choose from photo");
                    if (resultCode == RESULT_OK && data != null) {
                        resultTextView.setText(textdef);
                        cropIcon.setEnabled(true);
                        detectIcon.setEnabled(true);
                        mResultView.setVisibility(View.INVISIBLE);
                        mSourceView.setText("library image");
                        detected = false;

                        Uri selectedImage = data.getData();
                        String[] filePathColumn = {MediaStore.Images.Media.DATA};
                        if (selectedImage != null) {
                            Cursor cursor = getContentResolver().query(selectedImage,
                                    filePathColumn, null, null, null);
                            if (cursor != null) {
                                cursor.moveToFirst();
                                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                                String picturePath = cursor.getString(columnIndex);
                                mBitmap = BitmapFactory.decodeFile(picturePath);
                                Matrix matrix = new Matrix();
                                //matrix.postRotate(90.0f);
                                mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);
                                mImageView.setImageBitmap(mBitmap);
                                cursor.close();
                                currentURI = selectedImage;

                            }
                        }

                    }

                    break;

                case cameraRequestCode:
                    //customized camera activation
                    //System.out.println("camera image acquired");
                    //resultTextView.setText("camera image acquired");
                    detectIcon.setEnabled(true);
                    mSourceView.setText("camera image");
                    detected = false;
                    shareIcon.setEnabled(false);
                    mailIcon.setEnabled(false);
                    cropIcon.setEnabled(true);

                    if (resultCode == RESULT_OK && data != null) {
//                        try {
//                            postCroppedFile = createImageFile();
//                        } catch (IOException ex) {
//                            System.out.println("Error occurred while creating the File");
//                        }
                        UCrop.of(currentURI, Uri.fromFile(new File(getCacheDir(), "camera_image.jpg")))
                                    .withAspectRatio(1, 1)
//                                    .withMaxResultSize(640, 640)
                                    .start(MainActivity.this);

                    }
                    break;
                case UCrop.REQUEST_CROP:
                    mResultView.setVisibility(View.INVISIBLE);
                    resultTextView.setText(textdef);
                    //after crop
                    if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
                        currentURI = UCrop.getOutput(data);
                        try {
                            mBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), currentURI);
                        }catch (IOException e) {
                            e.printStackTrace();
                        }
                        Matrix matrix = new Matrix();
                        //matrix.postRotate(90.0f);
                        mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);

                        mImageView.setImageBitmap(mBitmap);

                    } else if (resultCode == UCrop.RESULT_ERROR) {
                        final Throwable cropError = UCrop.getError(data);
                    }

                    detectIcon.setEnabled(true);
                    cropIcon.setEnabled(true);
                    detected = false;
                    shareIcon.setEnabled(false);
                    mailIcon.setEnabled(false);

                    break;
            }
        }
    }

    @Override
    public void run() {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(mBitmap, Configs.mInputWidth, Configs.mInputHeight, true);
        results = detector.recognizeImage(resizedBitmap, mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY); //inference, process

//        System.out.println(results);
//        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, Configs.NO_MEAN_RGB, Configs.NO_STD_RGB);
//        IValue[] outputTuple = mModule.forward(IValue.from(inputTensor)).toTuple();
//        final Tensor outputTensor = outputTuple[0].toTensor();
//        final float[] outputs = outputTensor.getDataAsFloatArray();
//
//        //ここに引数増やしてnms threshold, score thresholdをいれれるようにする
//        final ArrayList<Result> results =  PrePostProcessor.outputsToNMSPredictions(outputs, mImgScaleX, mImgScaleY, mIvScaleX, mIvScaleY, mStartX, mStartY);
//
        runOnUiThread(() -> {
                        if (flagToast == 0) {
                            if (mBitmap.getHeight() != mBitmap.getWidth()) {
                                Toast.makeText(this, "cropping may lead to better result.", Toast.LENGTH_LONG).show();
                    flagToast++;
                }
            }
            numberOfObject = String.valueOf(results.size());
            if (results.size() <= 1000){
                resultTextView.setText(numberOfObject);
            }
            else{
                resultTextView.setText(numberOfObject); //内部テスト版では解除
//                //resultTextView.setText("1000<");
            }

            //mButtonDetect.setEnabled(true);

            //mButtonDetect.setText(getString(R.string.detect));
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);
            mResultView.setResults(results);
            mResultView.invalidate();
            mResultView.setVisibility(View.VISIBLE);
            detected = true;

            detectIcon.setEnabled(true);
            shareIcon.setEnabled(true);
            mailIcon.setEnabled(true);

            SPEED = (System.currentTimeMillis() - startTime);
        });
    }



}
