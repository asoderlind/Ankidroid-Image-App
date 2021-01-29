package anki.image.app;

import com.google.android.material.tabs.TabLayout;
import com.ichi2.anki.api.AddContentApi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.content.SharedPreferences;
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
import java.util.Set;

import static android.media.MediaScannerConnection.*;

public class CardActivity extends AppCompatActivity {
    private static final String TAG = "CardActivity : ";
    private ImageFragment mImageFragment;

    private AddContentApi mApi;
    private AnkiDroidHelper mAnkiDroid;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("msg :", "onCreate() called");

        mApi = new AddContentApi(this);
        mAnkiDroid = new AnkiDroidHelper(this);
        Intent intent = getIntent();

        // Get passed values
        String word = intent.getStringExtra("word");
        String appendix = intent.getStringExtra("searchAppendix");
        String translation =  intent.getStringExtra("translation");

        if (word == null) {
            finish();
        } else {
            setTitle(word + appendix + ", " + translation);
            mImageFragment = ImageFragment.newInstance(word, appendix);
        }

        setContentView(R.layout.activity_card); // Set our view to activity_card.xml

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        //Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true); //TODO: fix back button

        // Setup view pager
        ViewPager viewPager = findViewById(R.id.pager);
        if (viewPager == null) throw new AssertionError();
        viewPager.setOffscreenPageLimit(10);
        viewPager.setAdapter(new CardFragmentPagerAdapter(getSupportFragmentManager()));

        // setup tab layout
        TabLayout tabLayout = findViewById(R.id.tablayout);
        if (tabLayout == null) throw new AssertionError();
        tabLayout.setupWithViewPager(viewPager);
    }

    /* Adapter where you input how many pages of fragments you want and the title etc. */
    private class CardFragmentPagerAdapter extends FragmentPagerAdapter {
        public CardFragmentPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return mImageFragment; // TODO: add other fragments
            } else {
                return null;
            }
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

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity, menu);
        return true;
    }

    private String downloadImageToAnki(String base64Url){

        // save base64 encoded image
        String returned_save_path = createAndSaveFileFromBase64Url(base64Url);
        Log.d(TAG, "return from create and savefile: " + returned_save_path);

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

        // Images from js-script
        ArrayList<String> images = mImageFragment.getSelected();
        Log.d(TAG, "images: " + images);

        // List of image field content
        ArrayList<String> addedImageFileNames = new ArrayList<>();
        for (String base64Url : images){
            addedImageFileNames.add(downloadImageToAnki(base64Url));
        }

        if (addedImageFileNames.size() > 0){
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
                    fields[i] = ""; // reset
                    for (String filename : addedImageFileNames){
                        Log.d(TAG, "Adding " + filename + " to " + fields[i]);
                        fields[i] += filename;
                    }
                }
            }

            // Remove tag from card
            String tag = "marked";
            Log.d(TAG, "Tags: " + tags);
            if (tags.remove(tag)) {
                Log.d(TAG, "removed " + tag + " tag successfully");
            } else {
                Log.d(TAG, "Failed to remove " + tag + " from card");
            }

            // Remove card from saved data
            String key = this.getIntent().getStringExtra("prefKey");
            SharedPreferences sharedPref = getSharedPreferences(key, MODE_PRIVATE);
            Set<String> nidSet = sharedPref.getStringSet(key, null);
            if (nidSet.remove(this.getIntent().getStringExtra("noteId") + "," + this.getIntent().getStringExtra("modelId"))){
                Log.d(TAG, "removed word from " + key + " set successfully");
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putStringSet(key, nidSet);
                editor.apply();
            } else {
                Log.d(TAG, "word was not found in " + key + " set");
            }

            Log.d(TAG, "fields: " + Arrays.toString(fields)); // print fields

            boolean updateSucceed = mApi.updateNoteFields(id,fields);
            boolean tagUpdateSucceed = mApi.updateNoteTags(id, tags);

            Toast.makeText(this, !updateSucceed ? getString(R.string.failed_add_card) : getString(R.string.card_updated), Toast.LENGTH_LONG).show();
            Toast.makeText(this, !tagUpdateSucceed ? getString(R.string.failed_change_tag) : getString(R.string.tag_updated), Toast.LENGTH_LONG).show();

        } else {
            Log.d(TAG, "The file downloader failed (null returned)");
        }
        //TODO: maybe not finish if there are still saved nids
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

    /**
     * Saved the file with the given content URI
     * @param url the content-URI url of the image
     * @return the path that the file was saved at
     */
    public String createAndSaveFileFromBase64Url(String url) {
        Log.d(TAG, "createAndSaveFileFromBase64Url() called");
        //File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); // save in public external dir
        File path = getApplicationContext().getExternalFilesDir(null);
        Log.d(TAG, "Path: " + path);
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