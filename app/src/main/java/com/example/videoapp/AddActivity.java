package com.example.videoapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.PackageManagerCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;

public class AddActivity extends AppCompatActivity {

    private ActionBar actionBar;

    private EditText titleVid;
    private VideoView videoView;
    private Button uploadBtn;
    private ImageView pickBtn;

    private static final int VIDEO_PICK_GALLERY_CODE = 100;
    private static final int VIDEO_PICK_CAMERA_CODE = 101;
    private static final int CAMERA_REQUEST_CODE = 102;

    private String[] cameraPermission;
    private String title;
    private Uri videoUri;
    private ProgressDialog progressDialog;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);

        actionBar  = getSupportActionBar();

        actionBar.setTitle("Add New Video");
        //add Back button
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        titleVid = findViewById(R.id.titleVid);
        videoView = findViewById(R.id.videoView);
        uploadBtn = findViewById(R.id.uploadBtn);
        pickBtn = findViewById(R.id.pickVidBtn);

        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Loading...");
        progressDialog.setMessage("Uploading Video...");
        progressDialog.setCanceledOnTouchOutside(false);


        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                title = titleVid.getText().toString().trim();
                if( TextUtils.isEmpty(title)){
                    Toast.makeText(AddActivity.this, "Không được bỏ trống tiêu đề...", Toast.LENGTH_SHORT).show();
                }
                else if(videoUri == null){
                    Toast.makeText(AddActivity.this, "Chưa chọn video...", Toast.LENGTH_SHORT).show();
                }
                else{
                    uploadToFireBase();
                }
            }
        });

        pickBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.d("Hello 2", "onClick: ");
                videoPickDialog();
            }
        });
    }

    private void uploadToFireBase(){
        //show phan progress
        progressDialog.show();

        //Moc thoi gian
        String timestamp = "" + System.currentTimeMillis();

        //ten va duong dan trong firebase
        String filePath = "Videos/"+ "video_" + timestamp;

        StorageReference storageReference = FirebaseStorage.getInstance().getReference(filePath);
        //upload video

        storageReference.putFile(videoUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                //lay url cua video da upload
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isSuccessful());
                Uri downloadUri = uriTask.getResult();
                //Add mo ta vao Firebase
                if(uriTask.isSuccessful()){
                    HashMap<String, Object> hashMap = new HashMap<>();
                    hashMap.put("id", timestamp);
                    hashMap.put("title", ""+ title);
                    hashMap.put("timestamp", ""+ timestamp);
                    hashMap.put("videoUrl", ""+ downloadUri);

                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Videos");
                    reference.child(timestamp).setValue(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            //khi duoc add vao Db
                            progressDialog.dismiss();
                            Toast.makeText(AddActivity.this, "Video uploaded...", Toast.LENGTH_SHORT).show();

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //khi khong upload duoc
                            progressDialog.dismiss();
                            Toast.makeText(AddActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    });

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //Khong upload duoc len storage
                progressDialog.dismiss();
                Toast.makeText(AddActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }


    private void requestCamera(){
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private boolean checkCamera(){
        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean result2 = ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED;

        return result1 && result2;
    }

    private void videoPickGalerry(){

        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), VIDEO_PICK_GALLERY_CODE);
    }

    private void videoPickCamera(){
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(intent, VIDEO_PICK_GALLERY_CODE);
    }

    private void setVideoToVideoView() {
        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);

        //Set media controller cho videoView
        videoView.setMediaController(mediaController);
        //set video uri
        videoView.setVideoURI(videoUri);
        videoView.requestFocus();
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {

                Log.d("Hello2", "onPrepared: ");
                videoView.pause();
            }
        });

    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_REQUEST_CODE:
                if(grantResults.length>0){
                    //check truy cap
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepted && storageAccepted){
                        videoPickCamera();
                    }
                    else{
                        Toast.makeText(this, "Từ chối truy cập", Toast.LENGTH_SHORT).show();
                    }

                }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void videoPickDialog(){

        //Chon Option
        String[] options = {"Camera", "Gallery"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Pick Video From").setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0 ){
                    //chon camera
                    if (!checkCamera()){
                        //khi khong duoc phep truy cap vao camera
                        requestCamera();
                    }
                    else {
                        videoPickCamera();
                    }
                }
                else if (which ==1){
                    //chon Gallery
                    videoPickGalerry();
                }
            }
        }).show();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            Log.d("Hello", "onActivityResult: ");

            if (requestCode == VIDEO_PICK_GALLERY_CODE) {
                videoUri = data.getData();
                Log.d("Hello1", "onActivityResult: " + videoUri);
                setVideoToVideoView();
            } else if (requestCode == VIDEO_PICK_CAMERA_CODE) {
                videoUri = data.getData();
                Log.d("Hello2", "onActivityResult: " + videoUri);
                setVideoToVideoView();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}