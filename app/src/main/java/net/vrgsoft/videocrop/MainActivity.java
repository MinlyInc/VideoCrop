package net.vrgsoft.videocrop;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
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
        in = new File(Environment.getExternalStorageDirectory(), "minlyCam/videos/BigBuckBunny.mp4");
        out = new File(Environment.getExternalStorageDirectory(), "minlyCam/videos/cropped.mp4");
        Toast.makeText(this, String.valueOf(in.exists()), Toast.LENGTH_SHORT).show();
        startActivityForResult(VideoCropActivity.createIntent(this, in.getPath(), out.getPath()), CROP_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == CROP_REQUEST && resultCode == RESULT_OK){
            Toast.makeText(this, "videoPath", Toast.LENGTH_SHORT).show();

        }
    }
}
