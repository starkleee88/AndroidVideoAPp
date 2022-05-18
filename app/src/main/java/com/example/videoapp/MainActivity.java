package com.example.videoapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SnapHelper;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton addVidBtn;
    private RecyclerView rvVideos;
    private ArrayList<ModelVideo> videoArrayList;
    private VideoAdapter videoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("Videos");

        addVidBtn = findViewById(R.id.addVideosBtn);
        rvVideos = findViewById(R.id.rvVideos);
        
        loadVideoFromFirebase();

        addVidBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddActivity.class));
            }
        });
    }
    private void loadVideoFromFirebase() {
        videoArrayList = new ArrayList<>();

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Videos");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //clear list truoc khi add du lieu vao
                videoArrayList.clear();
                for(DataSnapshot dataSnapshot: snapshot.getChildren()){
                    //get data
                    ModelVideo modelVideo = dataSnapshot.getValue(ModelVideo.class);
                    //add model va data vao list
                    videoArrayList.add(modelVideo);
                }
                //setup adapter
                videoAdapter = new VideoAdapter(MainActivity.this, videoArrayList);
                //truyen adapter vao recyclerview
                rvVideos.setAdapter(videoAdapter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        SnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvVideos);
    }
}