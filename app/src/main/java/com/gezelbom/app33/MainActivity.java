package com.gezelbom.app33;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Class that displays all videos captured with the app.
 * Pressing the camera button in the action bar starts an Intent for recording a video
 *
 * @author Alex
 */
public class MainActivity extends Activity {

    private static final String TAG = "mainActivity";
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;
    private static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 200;
    private Cursor cursor;
    VideoAdapter adapter;
    private File path;
    private static Uri fileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Store the path where the media will be stored
        path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "MyCameraApp");

    }

    /**
     * Scan through the specific folder for all files. And the update the MediaStore for each file
     */
    private void scanForFiles() {

        // Only if the folder is not empty
        if (path.listFiles() != null) {
            // List all files
            File[] files = path.listFiles();
            // Convert to StringArray since MediaScanner needs it that way
            String[] filesString = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                filesString[i] = files[i].toString();
            }

            // MediaScanner.scanFile to force the MediaStore to update a specific file to the db
            MediaScannerConnection.scanFile(this, filesString, null, new MediaScannerConnection.OnScanCompletedListener() {

                public void onScanCompleted(String path, Uri uri) {
                    Log.i("ExternalStorage", "Scanned " + path + ":");
                }
            });

        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        // Scan for files and init to update the gallery onResume
        scanForFiles();
        init();
    }

    /**
     * Initialise the cursor to the path and initialise the grid with adapter and clickListener
     */
    private void init() {
        String[] project = {MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Thumbnails._ID};

        // Only search apps media path. Setup a condition for the query
        String uri = MediaStore.Video.Media.DATA;
        final String condition = uri + " like  '%" + path.toString() + "%'";


        cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                project, condition, null, null);
        //int count = cursor.getCount();

        GridView grid = (GridView) findViewById(R.id.gridView1);
        adapter = new VideoAdapter(getApplicationContext());
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(videogridlistener);

    }


    /**
     * Inflate the menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    /**
     * Only Camera currently in the actionbar to react to
     *
     * @param item the Item clicked
     * @return Calls the super method
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.camera) {
            launchCamera();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Launce the camera Intent when the action bar Camera is clicked
     * Pass the filename from getOutPutMediaFileUri method
     * Pass the wanted Quality 0 for high quality and 1 for low quality
     */
    private void launchCamera() {

        final int LOW_QUALITY = 0;
        final int HIGH_QUALITY = 1;

        Intent i = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        fileUri = getOutputMediaFileUri(MEDIA_TYPE_VIDEO);
        i.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        i.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, LOW_QUALITY);
        startActivityForResult(i, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
    }

    /**
     * When the result from the Intent returns start another intent to show the Video captured.
     *
     * @param requestCode check that the request code is CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE
     * @param resultCode  Check that the result is OK
     * @param data        The Data returned. (For some reason data is empty when passing EXTRA_OUTPUT)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {

                // Start an intent to view the video captured
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.parse("file://" + fileUri), "video/*");
                startActivity(i);

            /* Else statements currently not used
            } else if (resultCode == RESULT_CANCELED) {
                // User cancelled the image capture, currently no action
            } else {
                // Image capture failed, advise user
            */
            }
        }
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private static File getOutputMediaFile(int type) {

        // First check that the External storage is mounted and read/Write access
        // is available
        Log.d(TAG,
                "External storage state: "
                        + Environment.getExternalStorageState());
        if (Environment.MEDIA_MOUNTED.equalsIgnoreCase(Environment
                .getExternalStorageState())) {
            Log.d(TAG, "Inside the if statement");
            File mediaDir = new File(
                    Environment
                            .getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                    "MyCameraApp");

            Log.d(TAG, "Media dir: " + mediaDir);

            // Create the storage directory if it does not exist
            if (!mediaDir.exists()) {
                Log.d(TAG, "Media dir does not exist trying to create it");
                if (!mediaDir.mkdirs()) {
                    Log.d(TAG, "failed to create directory");
                    return null;
                }
            }

            // Create a media file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss",
                    Locale.getDefault()).format(new Date());
            File mediaFile = null;

            if (type == MEDIA_TYPE_IMAGE) {
                mediaFile = new File(mediaDir.getPath() + File.separator
                        + "IMG_" + timeStamp + ".jpg");
            } else if (type == MEDIA_TYPE_VIDEO) {
                mediaFile = new File(mediaDir.getPath() + File.separator
                        + "VID_" + timeStamp + ".mp4");
            }
            Log.d(TAG, "Filename: " + mediaFile);
            return mediaFile;

        }
        return null;

    }

    /**
     * The adapter used by the gridView
     */
    private class VideoAdapter extends BaseAdapter {

        private Context context;

        public VideoAdapter(Context localContext) {
            context = localContext;
        }

        public int getCount() {
            return cursor.getCount();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView picturesView;
            if (convertView == null) {
                picturesView = new ImageView(context);
                // Move cursor to current position
                cursor.moveToPosition(position);
                // Get the current value of Thumbnail_ID for the requested column
                int idColumnIndex = cursor.getColumnIndex(MediaStore.Video.Thumbnails._ID);
                int imageID = cursor.getInt(idColumnIndex);

                // Create a thumbnail bitmap and set it as image for the context
                Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(
                        getContentResolver(), imageID,
                        MediaStore.Video.Thumbnails.MICRO_KIND,
                        null);
                picturesView.setImageBitmap(bitmap);
                picturesView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            } else {
                picturesView = (ImageView) convertView;
            }
            return picturesView;
        }
    }

    /**
     * Item-ClickListener Starts an intent to view the captured video
     */
    private AdapterView.OnItemClickListener videogridlistener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView parent, View v, int position,
                                long id) {
            int video_column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToPosition(position);
            String filename = cursor.getString(video_column_index);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse("file://" + filename), "video/*");
            startActivity(i);
        }
    };
}
