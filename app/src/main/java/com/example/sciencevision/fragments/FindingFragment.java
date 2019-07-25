package com.example.sciencevision.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.camerakit.CameraKitView;

import com.example.sciencevision.R;
import com.example.sciencevision.SearchClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.parse.ParseUser;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import static android.widget.Toast.LENGTH_SHORT;

/**
 * A simple {@link Fragment} subclass.
 */
public class FindingFragment extends Fragment {

    private static final String TAG = FindingFragment.class.getSimpleName();
    private String photoFileName = "photo.jpg";
    private File photoFile;
    private TextView tvDescription;
    private SearchClient searchClient;
    private ParseUser currUser;
    private ListeningExecutorService service;
    private CameraKitView cameraKitView;
    private Button btnCapture;

    public FindingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_finding, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        cameraKitView = view.findViewById(R.id.camera);
        btnCapture= view.findViewById(R.id.btnCapture);
        searchClient = new SearchClient();
        currUser = ParseUser.getCurrentUser();
        service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
        btnCapture.setOnClickListener(photoOnClickListener);

    }

    @Override
    public void onStart() {
        super.onStart();
        cameraKitView.onStart();
    }
    @Override
    public void onResume() {
        super.onResume();
        cameraKitView.onResume();
    }
    @Override
    public void onPause() {
        cameraKitView.onPause();
        super.onPause();
    }
    @Override
    public void onStop() {
        cameraKitView.onStop();
        super.onStop();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        cameraKitView.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }
    private View.OnClickListener photoOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            cameraKitView.captureImage(new CameraKitView.ImageCallback() {
                @Override
                public void onImage(CameraKitView cameraKitView, byte[] capturedImage) {
                    File savedPhoto = new File(Environment.getExternalStorageDirectory(), "photo.jpg");
                    try {
                        FileOutputStream outputStream = new FileOutputStream(savedPhoto.getPath());

                        outputStream.write(capturedImage);
                        //Converts Photofile to Bitmap for Firebase
                        Bitmap bitmap = BitmapFactory.decodeByteArray(capturedImage, 0, capturedImage.length);
                        // CLOUD : THIS COST MONEY DONT BE DUMB
                        // FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance().getCloudImageLabeler();
                        // ON-DEVICE : THIS IS FREE, USE A LOT
                        FirebaseVisionImage firebaseVisionImage= FirebaseVisionImage.fromBitmap(bitmap);
                        FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler();
                        labeler.processImage(firebaseVisionImage)
                                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                                        // Task completed successfully
                                        // This function sets the text of the TextView given as the parameter
                                        // to be the definition of the object in the image.
                                        Toast.makeText(getContext(), labels.get(0).getText(), LENGTH_SHORT).show();
                                        String query = labels.get(0).getText();

                                        ListenableFuture<String> getWiki = service.submit(searchClient.getWiki(query));
                                        ListenableFuture<String> getDataFromGoogle = service.submit(searchClient.getDataFromGoogle(query));

                                        List<ListenableFuture<String>> networkCalls = new ArrayList<>();
                                        networkCalls.add(getWiki);
                                        networkCalls.add(getDataFromGoogle);

                                        ListenableFuture<List<String>> successfulNetworkCalls = Futures.successfulAsList(networkCalls);
                                        Futures.addCallback(successfulNetworkCalls, new FutureCallback<List<String>>() {
                                            @Override
                                            public void onSuccess(@NullableDecl List<String> result) {
                                                Log.d("FINAL", result.toString());
                                                // TODO: Intent to detail view.
                                            }
                                            @Override
                                            public void onFailure(Throwable t) {

                                            }
                                        }, service);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        e.printStackTrace();
                                        Log.d(TAG, e.toString());
                                    }
                                });
                        outputStream.close();
                    } catch (java.io.IOException e) {
                        e.printStackTrace();
                        Log.e("Photo","Error");
                    }
                }
            });
        }
    };

//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        btnTakePicture = view.findViewById(R.id.btnTakePicture);
//        ivPostImage = view.findViewById(R.id.ivPostImage);
//        tvDescription = view.findViewById(R.id.tvDescription);
//        searchClient = new SearchClient();
//        currUser = ParseUser.getCurrentUser();
//        service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
//        btnTakePicture.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                launchCamera();
//            }
//        });
//
//
//    }
//
//    private void launchCamera() {
//        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        photoFile = CameraHelper.getPhotoFileUri(photoFileName, getContext(), TAG);
//        Uri fileProvider = FileProvider.getUriForFile(getContext(), "com.example.sciencevision", photoFile);
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);
//
//        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
//            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
//        }
//    }
//
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
//            if (resultCode == RESULT_OK) {
//                final Bitmap rotatedImage = CameraHelper.rotateBitmapOrientation(photoFile.getAbsolutePath());
//                ivPostImage.setImageBitmap(rotatedImage);
//
//
//                FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(rotatedImage);
//                // CLOUD : THIS COST MONEY DONT BE DUMB
//                // FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance().getCloudImageLabeler();
//                // ON-DEVICE : THIS IS FREE, USE A LOT
//                FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance().getOnDeviceImageLabeler();
//                labeler.processImage(firebaseVisionImage)
//                        .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
//                            @Override
//                            public void onSuccess(List<FirebaseVisionImageLabel> labels) {
//                                String query = labels.get(0).getText();
//
//                                ListenableFuture<String> getWiki = service.submit(searchClient.getWiki(query));
//                                ListenableFuture<String> getDataFromGoogle = service.submit(searchClient.getDataFromGoogle(query));
//
//                                List<ListenableFuture<String>> networkCalls = new ArrayList<>();
//                                networkCalls.add(getWiki);
//                                networkCalls.add(getDataFromGoogle);
//
//                                ListenableFuture<List<String>> successfulNetworkCalls = Futures.successfulAsList(networkCalls);
//                                Futures.addCallback(successfulNetworkCalls, new FutureCallback<List<String>>() {
//                                    @Override
//                                    public void onSuccess(@NullableDecl List<String> result) {
//                                        Log.d("FINAL", result.toString());
//                                        // TODO: Intent to detail view.
//                                    }
//
//                                    @Override
//                                    public void onFailure(Throwable t) {
//
//                                    }
//                                }, service);
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                e.printStackTrace();
//                                Log.d("FindingFragment", e.toString());
//                            }
//                        });
//
//            } else {
//                Toast.makeText(getContext(), "Picture wasn't taken!", LENGTH_SHORT).show();
//            }
//        }
//    }


}
