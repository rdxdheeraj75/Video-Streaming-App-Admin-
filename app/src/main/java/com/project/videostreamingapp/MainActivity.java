package com.project.videostreamingapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.project.videostreamingapp.Model.VideoUploadDetails;

import org.apache.commons.io.FilenameUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    Uri videoUri;
    TextView text_video_selected;
    String videoCategory,videoTitle,currentuid;
    StorageReference mStorageRef;
    StorageTask mUploadsTask;
    DatabaseReference referenceVideos;
    EditText video_description;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text_video_selected=findViewById(R.id.textvideoselected);
        video_description=findViewById(R.id.movies_description);
        referenceVideos = FirebaseDatabase.getInstance().getReference().child("videos");
        mStorageRef = FirebaseStorage.getInstance().getReference().child("videos");



        Spinner spinner = findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);

        List<String> categories = new ArrayList<>();
        categories.add("Action");
        categories.add("Adventure");
        categories.add("Sports");
        categories.add("Romantic");
        categories.add("Comedy");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        videoCategory = parent.getItemAtPosition(position).toString();
        Toast.makeText(this, "selected: "+videoCategory, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
    public void openvideoFiles(View view)
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(intent,101);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 101 && resultCode == RESULT_OK && data.getData() !=null)
        {
            videoUri=data.getData();

            String path=null;
            Cursor cursor;
            int column_index_data;
            String [] projection = {MediaStore.MediaColumns.DATA,MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media._ID,MediaStore.Video.Thumbnails.DATA};
            final String orderby = MediaStore.Video.Media.DEFAULT_SORT_ORDER;
            cursor=MainActivity.this.getContentResolver().query(videoUri,projection,null,null,orderby);
            column_index_data=cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()){
                path=cursor.getString(column_index_data);
                videoTitle = FilenameUtils.getBaseName(path);
            }
            text_video_selected.setText(videoTitle);
        }

    }

    public void uploadFileToFirebase(View view)
    {
        if (text_video_selected.getText().equals("no video selected")){
            Toast.makeText(this, "please select an video!", Toast.LENGTH_SHORT).show();
        }else {
            if(mUploadsTask != null && mUploadsTask.isInProgress()){
                Toast.makeText(this, "Video upload is all ready in progress..", Toast.LENGTH_SHORT).show();
            }else {
                uploadFiles();
            }
        }
    }

    private void uploadFiles() {

        if (videoUri !=null) {
            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("video uploading...");
            progressDialog.show();
            Date date = new Date();
            videoTitle="videoTitle-"+date.toString();
            final StorageReference storageReference = mStorageRef.child(videoTitle);

            mUploadsTask = storageReference.putFile(videoUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String video_url = uri.toString();
                            VideoUploadDetails videoUploadDetails = new VideoUploadDetails("", "", "",
                                    video_url, videoTitle, video_description.getText().toString(), videoCategory);
                            String uploadsid = referenceVideos.push().getKey();
                            referenceVideos.child(uploadsid).setValue(videoUploadDetails);
                            currentuid = uploadsid;
                            progressDialog.dismiss();
                            if (currentuid.equals(uploadsid)){
                                startThumbnailsActivity();
                            }
                        }
                    });
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                    double progress = (100.0 * snapshot.getBytesTransferred() / snapshot.getTotalByteCount());
                    progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                }
            });
        }else {
            Toast.makeText(this, "no video selected to upload", Toast.LENGTH_SHORT).show();
        }
    }
    public void startThumbnailsActivity(){
        Intent intent = new Intent(MainActivity.this,UploadThumbnailActivity.class);
        intent.putExtra("currentuid",currentuid);
        intent.putExtra("thumbnailsName",videoTitle);
        startActivity(intent);
        Toast.makeText(this, "Video uploaded successfully upload video thumbnail!", Toast.LENGTH_LONG).show();
    }
}