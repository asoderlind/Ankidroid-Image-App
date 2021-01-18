package anki.image.app;

import com.google.android.material.tabs.TabLayout;
import com.ichi2.anki.FlashCardsContract;
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
import android.content.DialogInterface;
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
import java.util.Map;

import static android.media.MediaScannerConnection.*;


public class CardActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{
    private static final String LOG = "CardActivity : ";
    private ImageFragment mImageFragment;

    // Ankidroid variables
    private AddContentApi mApi;
    private AnkiDroidHelper mAnkiDroid;
    private static final String ANKI_DECK_NAME = "test";
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
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .create();

            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }
            });

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

        String word = intent.getStringExtra(Intent.EXTRA_SUBJECT);

        if (word == null) {
            word = intent.getStringExtra(Intent.EXTRA_TEXT);
        }

        if (word == null) {
            finish();
            return;
        }

        setTitle(word);
        mImageFragment = ImageFragment.newInstance(word);
        verifyStoragePermissions(this);

        // Ankidroid permissions
        if (mAnkiDroid.shouldRequestPermission()) {
            // Permission is not granted
            Log.d("msg :", "Permission is not granted");
            mAnkiDroid.requestPermission(CardActivity.this, AD_PERM_REQUEST);
        } else {
            // Permission is granted
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
        setContentView(R.layout.activity_card);

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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

    private Long getDeckId() {
        Map<Long, String> deckList = mApi.getDeckList();
        if (deckList != null) {
            for (Map.Entry<Long, String> entry : deckList.entrySet()) {
                if (entry.getValue().equals(ANKI_DECK_NAME)) {
                    return entry.getKey();
                }
            }
        }
        return mApi.addNewDeck(ANKI_DECK_NAME);
    }

    private Long getModelId() {
        Map<Long, String> modelList = mApi.getModelList();
        if (modelList != null) {
            for (Map.Entry<Long, String> entry : modelList.entrySet()) {
                if (entry.getValue().equals(FlashCardsContract.Model.NAME)) {
                    return entry.getKey();
                }
            }
    }

        // We make a new model according to the config file and returns the id
        return mApi.addNewCustomModel(AnkiDroidConfig.MODEL_NAME, AnkiDroidConfig.FIELDS,
                AnkiDroidConfig.CARD_NAMES, AnkiDroidConfig.QFMT, AnkiDroidConfig.AFMT, AnkiDroidConfig.CSS, getDeckId(), null);
    }

    // TODO: assign model from extras, get deck id from note id, update note through api
    private void updateCard() {
        Log.d(LOG, "createCard() called");
        ArrayList<String> images = mImageFragment.getSelected();
        Log.d(LOG, "images: " + images);

        long id = Long.parseLong(this.getIntent().getStringExtra("noteId"));
        String returned_save_path = "";

        // save base64 encoded image of first element
        if (images.size() > 0){
            returned_save_path = createAndSaveFileFromBase64Url(images.get(0));
            Log.d(LOG, "return from create and savefile: " + returned_save_path);
        }

        Long modelId = getModelId();
        Log.d(LOG, "modelId: " + modelId);

        Long deckId = getDeckId();
        Log.d(LOG, "deckId: " + deckId);

        if ((deckId == null) || (modelId == null)) {
            // we had an API error, report failure and return
            Toast.makeText(this, getResources().getString(R.string.failed_add_card), Toast.LENGTH_LONG).show();
            return;
        }

        String[] fieldNames = mAnkiDroid.getApi().getFieldList(modelId);
        if (fieldNames == null) {
            // we had an API error, report failure and return
            Toast.makeText(this, getResources().getString(R.string.failed_add_card), Toast.LENGTH_LONG).show();
            return;
        }

        // this path corresponds to the local storage for app files of this app
        //File path = new File(String.valueOf(getApplicationContext().getExternalFilesDir(null)),"2bd05276.png");
        File path = new File(returned_save_path, "");
        Log.d(LOG, "path: " + path);

        // pass the file to ankidroid via api
        if (path.exists()){
            Log.d(LOG, "path exists");

            // we use a file provider to get the content URI for the image
            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "com.fileprovider", path);
            Log.d(LOG, "contentUri: " + contentUri);

            // need to grant permission to ankidroid to access the specific content URI
            getApplicationContext().grantUriPermission("com.ichi2.anki", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // returns filename if success otherwise null
            String addedImageFileName = mAnkiDroid.addMediaFromUri(contentUri, "myImageFile", "image");

            // prepare the fields before adding note
            String[] fields = {addedImageFileName, "2", "3", "4", "5", "6", "7", "8"};
            Log.d(LOG, "fields: " + Arrays.toString(fields));

            // add the note to Ankidroid
            Long returnId = mApi.addNote(modelId, deckId, fields, null);
            Toast.makeText(this, returnId == null ? getString(R.string.failed_add_card) : getString(R.string.card_added), Toast.LENGTH_LONG).show();
        } else {
            Log.d(LOG, "path does not exist");
        }

        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create:
                updateCard();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                    new OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            Log.d("ExternalStorage", "Scanned " + path + ":");
                            Log.d("ExternalStorage", "-> uri=" + uri);
                        }
                    });

        } catch (IOException e) {
            Log.w("ExternalStorage", "Error writing " + file, e);
            Toast.makeText(getApplicationContext(), R.string.error_downloading, Toast.LENGTH_LONG).show();
        }

        return file.toString();
    }
}