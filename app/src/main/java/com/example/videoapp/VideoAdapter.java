package com.example.videoapp;

import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.HolderVideo>{

    private Context context;
    private ArrayList<ModelVideo> videoArrayList;

    public VideoAdapter(Context context, ArrayList<ModelVideo> videoArrayList) {
        this.context = context;
        this.videoArrayList = videoArrayList;
    }

    @NonNull
    @Override
    public HolderVideo onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(context).inflate(R.layout.row_video, parent, false);
        return new HolderVideo(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderVideo holder, int position) {
        //Lay data
        final ModelVideo modelVideo = videoArrayList.get(position);

        String id = modelVideo.getId();
        String title = modelVideo.getTitle();
        String timestamp = modelVideo.getTimestamp();
        String videoUrl = modelVideo.getVideoUrl();

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(timestamp));
        //String date = DateFormat.getDateTimeInstance().format(calendar);

        //set data
        holder.txtTitle.setText(title);
        holder.txtTime.setText(timestamp);
        setVideoUrl(modelVideo, holder);

        //button download
        holder.downloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadVideo(modelVideo);
            }
        });

        //button delete
        holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //hien thong bao xac nhan
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("Delete")
                        .setMessage("Are you sure to delete this video?")
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteVideo(modelVideo);
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();

            }
        });
    }

    private void setVideoUrl(ModelVideo modelVideo, HolderVideo holder) {
        holder.progressBar.setVisibility(View.VISIBLE);

        String videoUrl = modelVideo.getVideoUrl();

        MediaController mediaController = new MediaController(context);
        mediaController.setAnchorView(holder.videoView);

        Uri videoUri = Uri.parse(videoUrl);
        holder.videoView.setMediaController(mediaController);
        holder.videoView.setVideoURI(videoUri);

        holder.videoView.requestFocus();
        holder.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                //Video co the su dung
                mp.start();
            }
        });

        holder.videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                switch (what){
                    case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:{
                        holder.progressBar.setVisibility(View.VISIBLE);
                        return true;
                    }
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:{
                        holder.progressBar.setVisibility(View.VISIBLE);
                    }
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:{
                        holder.progressBar.setVisibility(View.GONE);
                    }
                }
                return false;
            }
        });

        holder.videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.start();
            }
        });



    }

    private void downloadVideo(ModelVideo modelVideo){
        String videoUrl = modelVideo.getVideoUrl(); //get videoUrl

        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl);
        storageReference.getMetadata().addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
            @Override
            public void onSuccess(StorageMetadata storageMetadata) {
                String fileName = storageMetadata.getName();
                String fileType = storageMetadata.getContentType();
                String fileDirectory = Environment.DIRECTORY_DOWNLOADS;

                DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);

                Uri uri = Uri.parse(videoUrl);

                //tao request download
                DownloadManager.Request request = new DownloadManager.Request(uri);

                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                request.setDestinationInExternalPublicDir(""+fileDirectory, ""+fileName+".mp4");

                //them vao danh sach cho, co the download nhieu
                downloadManager.enqueue(request);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void deleteVideo(ModelVideo modelVideo){
        String videoId = modelVideo.getId(); //get id cua video tren firebase
        String videoUrl = modelVideo.getVideoUrl(); //get url cua video tren firebase

        StorageReference reference = FirebaseStorage.getInstance().getReferenceFromUrl(videoUrl);
        reference.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void unused) {

                //Xoa khoi database
                DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Videos");
                databaseReference.child(videoId)
                        .removeValue()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                //thong bao xoa khoi firebase
                                Toast.makeText(context, "Delete succeed ...", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(context, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoArrayList.size();
    }


    class HolderVideo extends RecyclerView.ViewHolder{

        //UI View su dung row_video

        VideoView videoView;
        TextView txtTitle, txtTime;
        ProgressBar progressBar;
        FloatingActionButton downloadBtn;
        FloatingActionButton deleteBtn;

        public HolderVideo(@NonNull View itemView) {
            super(itemView);

            videoView = itemView.findViewById(R.id.videoView);
            txtTitle = itemView.findViewById(R.id.txtTitle);
            txtTime = itemView.findViewById(R.id.txtTime);
            progressBar = itemView.findViewById(R.id.progressBar);
            downloadBtn = itemView.findViewById(R.id.downloadBtn);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);

        }
    }
}
