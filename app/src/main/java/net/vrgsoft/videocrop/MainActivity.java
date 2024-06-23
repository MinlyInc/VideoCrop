package net.vrgsoft.videocrop;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import net.vrgsoft.videcrop.VideoCropActivity;

import java.io.File;


public class MainActivity extends AppCompatActivity {
    private static final int CROP_REQUEST = 200;
    private static final int REQUEST_PICK_VIDEO = 300;
    private File in;
    private File out;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        in = new File(Environment.getExternalStorageDirectory(), "minlyCam/videos/BigBuckBunny.mp4");
//        out = new File(Environment.getExternalStorageDirectory(), "minlyCam/videos/cropped.mp4");
//        Toast.makeText(this, String.valueOf(in.exists()), Toast.LENGTH_SHORT).show();
//        startActivityForResult(VideoCropActivity.createIntent(this, in.getPath(), out.getPath()), CROP_REQUEST);
        openGalleryForVideo();

    }


    private void openGalleryForVideo() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_PICK);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), 500);
    }

    private String parsePath(Uri uri) {
        String[] projection = new String[]{MediaStore.Video.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
         if (cursor != null) {
             int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(columnIndex);
        }
         return  null;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 500){
            if (resultCode == Activity.RESULT_OK){
                if (data.getData() != null) {
                    String parsedPath = parsePath(data.getData());
                    if (parsedPath == null) {
                        return;
                    }
                    in = new File(parsedPath);
                    out = new File(Environment.getExternalStorageDirectory(), "minlyCam/videos/"+ System.currentTimeMillis() +".mp4");
                    startActivityForResult(VideoCropActivity.createIntent(this, in.getPath(), out.getPath()), CROP_REQUEST);

                }
            }
        }
    }
}
