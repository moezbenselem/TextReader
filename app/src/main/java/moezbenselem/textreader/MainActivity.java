package moezbenselem.textreader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.MainThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.*;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;


public class MainActivity extends AppCompatActivity {

    SurfaceView cameraView;
    TextView textView, tvResult;
    Button btCam, btGal, btCopy, btOk, btX, btScanCam, btScanPic;
    String result = "";
    CameraSource cameraSource;
    final int RequestCameraPermissionID = 1001;
    static String event;
    AdView mAdView;
    InterstitialAd mInterstitialAd;
    Runnable r;
    BarcodeDetector barcodeDetector;
    Handler h;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestCameraPermissionID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.CAMERA},
                                RequestCameraPermissionID);

                        //return;
                    }
                    try {
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                //.addTestDevice("F8EDF04F845FAB4C6D9652E530EBF593")
                .build();
        mAdView.loadAd(adRequest);
        cameraView = (SurfaceView) findViewById(R.id.surfaceView);
        textView = (TextView) findViewById(R.id.text_view);
        tvResult = (TextView) findViewById(R.id.tvResult);

        btScanPic = (Button) findViewById(R.id.btImgCodeScan);
        btScanCam = (Button) findViewById(R.id.btCodeScan);
        btGal = (Button) findViewById(R.id.btGal);
        btOk = (Button) findViewById(R.id.btOk);
        btX = (Button) findViewById(R.id.btX);
        btX.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                cameraSource.stop();


                btOk.setVisibility(View.GONE);
                btX.setVisibility(View.GONE);
                cameraView.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);
                //tvResult.setText("");
                btCopy.setVisibility(View.GONE);

            }
        });

        btCopy = (Button) findViewById(R.id.btCopy);
        btCopy.setVisibility(View.GONE);
        btCam = (Button) findViewById(R.id.btCam);
        btCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                cameraView.setVisibility(View.VISIBLE);
                textView.setVisibility(View.VISIBLE);
                btOk.setVisibility(View.VISIBLE);
                btX.setVisibility(View.VISIBLE);
                capture();


            }
        });


        btOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                result = textView.getText().toString();
                tvResult.setText(result);

                btX.setVisibility(View.GONE);
                cameraView.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);
                textView.setText("...");
                btOk.setVisibility(View.GONE);
                btCopy.setVisibility(View.VISIBLE);

            }
        });

        btCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(getApplication().CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", result);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getApplicationContext(), "Copied to Clipboard !", Toast.LENGTH_SHORT).show();
                r = new Runnable() {

                    @Override
                    public void run() {

                        mInterstitialAd = new InterstitialAd(MainActivity.this);

                        // set the ad unit ID
                        mInterstitialAd.setAdUnitId("ca-app-pub-4844893835585992/4043754625");

                        AdRequest adRequest = new AdRequest.Builder()
                                //.addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                                .build();

                        // Load ads into Interstitial Ads
                        mInterstitialAd.loadAd(adRequest);

                        mInterstitialAd.setAdListener(new AdListener() {
                            public void onAdLoaded() {
                                showInterstitial();
                            }
                        });
                    }

                    private void showInterstitial() {
                        if (mInterstitialAd.isLoaded()) {
                            mInterstitialAd.show();
                        }
                    }
                };

                h = new Handler();
                // The Runnable will be executed after the given delay time
                h.postDelayed(r, 0);

            }
        });

        btGal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                event = "text";
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);


            }
        });


        btScanPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                // Show only images, no videos or anything else
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                event = "code";
                // Always show the chooser (if there are multiple options available)
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), 1);
            }
        });

        btScanCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                cameraView.setVisibility(View.VISIBLE);
                textView.setVisibility(View.VISIBLE);
                //btOk.setVisibility(View.VISIBLE);
                btX.setVisibility(View.VISIBLE);
                captureCode();

            }
        });

    }

    public void capture() {

        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Log.w("MainActivity", "Detector dependencies are not yet available");
        } else {

            textView.setText("...");
            cameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    //.setRequestedFps(2.0f)
                    .setAutoFocusEnabled(true)
                    .build();
            cameraView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {

                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    RequestCameraPermissionID);

                        }
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    cameraSource.stop();
                }
            });

            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {

                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0) {
                        textView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for (int i = 0; i < items.size(); ++i) {
                                    TextBlock item = items.valueAt(i);
                                    System.out.println(item.getLanguage());
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("\n");
                                }
                                textView.setText(stringBuilder.toString());
                            }
                        });
                    }
                }


            });
        }

    }

    public void captureCode() {

        BarcodeDetector detector =
                new BarcodeDetector.Builder(getApplicationContext())
                        .setBarcodeFormats(Barcode.ALL_FORMATS).build();
        if (!detector.isOperational()) {
            System.out.println("Could not set up the detector!");
            System.out.println("non functional !");
            return;
        } else if (detector.isOperational()) {
            System.out.println("functional !");
            textView.setText("");
            cameraSource = new CameraSource.Builder(getApplicationContext(), detector)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedPreviewSize(1280, 1024)
                    .setAutoFocusEnabled(true)
                    .build();
            cameraView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder surfaceHolder) {

                    try {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    RequestCameraPermissionID);
                            return;
                        }
                        cameraSource.start(cameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                    cameraSource.stop();
                }
            });

            detector.setProcessor(new Detector.Processor<Barcode>() {
                @Override
                public void release() {

                }

                @Override
                public void receiveDetections(Detector.Detections<Barcode> detections) {

                    final SparseArray<Barcode> r = detections.getDetectedItems();
                    if (r.size() > 0) {
                        btX.post(new Runnable() {
                            @Override
                            public void run() {

                                btX.setVisibility(View.GONE);
                                cameraView.setVisibility(View.GONE);
                                cameraSource.stop();
                                textView.setVisibility(View.GONE);
                                btOk.setVisibility(View.GONE);
                                btCopy.setVisibility(View.VISIBLE);

                                //Toast.makeText(getApplicationContext(),r.valueAt(0).displayValue,Toast.LENGTH_LONG).show();
                                textView.setText(r.valueAt(0).displayValue);
                                tvResult.setText(r.valueAt(0).displayValue);
                            }
                        });


                    } else
                        Toast.makeText(getApplicationContext(), "Cannot scan input !", Toast.LENGTH_SHORT).show();
                }

                CameraSource.PictureCallback mPictureCallback = new CameraSource.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] bytes) {
                        Bitmap mBitmap = BitmapFactory
                                .decodeByteArray(bytes, 0, bytes.length);

                    }

                };

            });
        }

    }

    public void scanCodePic() {

        BarcodeDetector detector =
                new BarcodeDetector.Builder(getApplicationContext())
                        .build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri uri = data.getData();

            try {

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                // Log.d(TAG, String.valueOf(bitmap));
                if (event.equalsIgnoreCase("text")) {
                    TextRecognizer txtRec = new TextRecognizer.Builder(getApplicationContext()).build();
                    SparseArray<TextBlock> items = txtRec.detect(frame);
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < items.size(); ++i) {
                        TextBlock item = items.valueAt(i);
                        stringBuilder.append(item.getValue());
                        stringBuilder.append("\n");
                    }
                    //textView.setText(stringBuilder.toString());
                    result = stringBuilder.toString();
                    tvResult.setText(result);
                    btCopy.setVisibility(View.VISIBLE);
                } else {
                    BarcodeDetector detector =
                            new BarcodeDetector.Builder(getApplicationContext())
                                    .build();
                    if (!detector.isOperational()) {
                        System.out.println("Could not set up the detector!");
                        return;
                    } else {
                        SparseArray<Barcode> barcodes = detector.detect(frame);
                        if (barcodes.size() > 0) {
                            Barcode thisCode = barcodes.valueAt(0);

                            result = thisCode.rawValue;
                            tvResult.setText(thisCode.rawValue.toString());
                            btCopy.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(getApplicationContext(), "Cannot scan picture !", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
