package anki.image.app;

import com.google.android.material.tabs.TabLayout;
import com.ichi2.anki.api.AddContentApi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

import static android.media.MediaScannerConnection.*;


public class CardActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{
    private static final String TAG = "CardActivity : ";
    private ImageFragment mImageFragment;

    // Ankidroid variables
    private AddContentApi mApi;
    private AnkiDroidHelper mAnkiDroid;
    private static final int AD_PERM_REQUEST = 0;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /** Runs finish if the permission check failed, otherwise finishCreate */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode==AD_PERM_REQUEST && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            finishCreate();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog dialog = builder.setTitle(R.string.anki_needed)
                    .setMessage(R.string.anki_needed_long)
                    .setPositiveButton("OK", (dialog1, which) -> finish())
                    .create();

            dialog.setOnDismissListener(dialog12 -> finish());

            dialog.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("msg :", "onCreate() called");

        mApi = new AddContentApi(this);
        mAnkiDroid = new AnkiDroidHelper(this);
        Intent intent = getIntent();

        String word = intent.getStringExtra("word");
        String appendix = intent.getStringExtra("searchAppendix");
        String translation =  intent.getStringExtra("translation");

        if (word == null) {
            finish();
            return;
        } else {
            setTitle(word + appendix + ", " + translation);
            mImageFragment = ImageFragment.newInstance(word, appendix);
        }

        // Permissions
        verifyStoragePermissions(this);
        if (mAnkiDroid.shouldRequestPermission()) {
            mAnkiDroid.requestPermission(CardActivity.this, AD_PERM_REQUEST);
        } else {
            finishCreate();
        }
    }

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    /* Adapter where you input how many pages of fragments you want and the title etc. */
    private class CardFragmentPagerAdapter extends FragmentPagerAdapter {
        public CardFragmentPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return mImageFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return getString(R.string.images);
            }
            return null;
        }
    }

    private void finishCreate() {
        setContentView(R.layout.activity_card); // Set our view to activity_card.xml

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        //Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true); // Back button does not work

        ViewPager viewPager = findViewById(R.id.pager);
        assert viewPager != null;
        viewPager.setOffscreenPageLimit(10);
        viewPager.setAdapter(new CardFragmentPagerAdapter(getSupportFragmentManager()));

        TabLayout tabLayout = findViewById(R.id.tablayout);
        assert tabLayout != null;
        tabLayout.setupWithViewPager(viewPager);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    private String downloadImageToAnki(){
        // Images from js-script
        ArrayList<String> images = mImageFragment.getSelected();
        Log.d(TAG, "images: " + images);

        // save base64 encoded image of first element
        String returned_save_path = "";
        if (images.size() > 0){
            returned_save_path = createAndSaveFileFromBase64Url(images.get(0)); // TODO: allow for multiple images
            Log.d(TAG, "return from create and savefile: " + returned_save_path);
        }

        // Get the file path for the saved file
        File path = new File(returned_save_path, "");
        Log.d(TAG, "path: " + path);

        // pass the file to ankidroid via api
        if (path.exists()){

            // we use a file provider to get the content URI for the image
            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "com.fileprovider", path);
            Log.d(TAG, "contentUri: " + contentUri);

            // need to grant permission to ankidroid to access the specific content URI
            getApplicationContext().grantUriPermission("com.ichi2.anki", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // returns filename if success otherwise null
            return mAnkiDroid.addMediaFromUri(contentUri, "myImageFile", "image");

        } else {
            Log.d(TAG, "path does not exist");
            return null;
        }
    }

    private void updateCard() {
        Log.d(TAG, "updateCard() called");
        String addedImageFileName = downloadImageToAnki();

        if (addedImageFileName != null){
            // Variables from intent
            long id = Long.parseLong(this.getIntent().getStringExtra("noteId"));
            long modelId = Long.parseLong(this.getIntent().getStringExtra("modelId"));
            String[] fields = this.getIntent().getStringArrayExtra("fields");

            // get the names of the fields from the model id
            String[] fieldNames = mApi.getFieldList(modelId);
            Set<String> tags = mApi.getNote(id).getTags();


            // Change image field
            for(int i=0; i<fieldNames.length; i++){
                // Case for image
                if(fieldNames[i].equals("Image")){
                    Log.d(TAG, "Changing " + fields[i] + " to " + addedImageFileName);
                    fields[i] = addedImageFileName;
                }
            }

            // Remove marked from tags
            Log.d(TAG, "Tags: " + String.valueOf(tags));
            tags.remove("marked");

            Log.d(TAG, "fields: " + Arrays.toString(fields)); // print fields

            boolean updateSucceed = mApi.updateNoteFields(id,fields);
            boolean tagUpdateSucceed = mApi.updateNoteTags(id, tags);

            Toast.makeText(this, !updateSucceed ? getString(R.string.failed_add_card) : getString(R.string.card_updated), Toast.LENGTH_LONG).show();
            Toast.makeText(this, !tagUpdateSucceed ? getString(R.string.failed_change_tag) : getString(R.string.tag_updated), Toast.LENGTH_LONG).show();

        } else {
            Log.d(TAG, "The file downloader failed (null returned)");
        }

        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_create) {
            updateCard();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public String createAndSaveFileFromBase64Url(String url) {
        //File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); // save in public external dir
        File path = getApplicationContext().getExternalFilesDir(null);
        String filetype = url.substring(url.indexOf("/") + 1, url.indexOf(";"));
        String filename = System.currentTimeMillis() + "." + filetype;
        File file = new File(path, filename);
        try {
            if(!path.exists())
                path.mkdirs();
            if(!file.exists())
                file.createNewFile();

            String base64EncodedString = url.substring(url.indexOf(",") + 1);
            byte[] decodedBytes = Base64.decode(base64EncodedString, Base64.DEFAULT);
            OutputStream os = new FileOutputStream(file);
            os.write(decodedBytes);
            os.close();

            //Tell the media scanner about the new file so that it is immediately available to the user.
            scanFile(this,
                    new String[]{file.toString()}, null,
                    (path1, uri) -> {
                        Log.d("ExternalStorage", "Scanned " + path1 + ":");
                        Log.d("ExternalStorage", "-> uri=" + uri);
                    });

        } catch (IOException e) {
            Log.w("ExternalStorage", "Error writing " + file, e);
            Toast.makeText(getApplicationContext(), R.string.error_downloading, Toast.LENGTH_LONG).show();
        }

        return file.toString();
    }
}