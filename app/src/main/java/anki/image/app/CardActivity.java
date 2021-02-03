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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static android.media.MediaScannerConnection.*;

public class CardActivity extends AppCompatActivity {
    private static final String TAG = "CardActivity : ";
    private ImageFragment mImageFragment;
    private AddContentApi mApi;
    private AnkiDroidHelper mAnkiDroid;
    private String mAppendix;
    private String mPrefKey;
    private String [] mFields;
    private Map<String, String> wordMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");
        initAnkiApi();
        getAllExtra();
        setTitle(wordMap.get("word") + mAppendix + ", " + wordMap.get("translation"));
        mImageFragment = ImageFragment.newInstance(wordMap.get("word"), mAppendix);
        setContentView(R.layout.activity_card);
        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        ViewPager myViewPager = initViewPager();
        initTabLayout(myViewPager);
    }

    private void initAnkiApi(){
        mApi = new AddContentApi(this);
        mAnkiDroid = new AnkiDroidHelper(this);
    }

    private void getAllExtra(){
        Intent intent = getIntent();
        wordMap = new HashMap<>();
        wordMap.put("id", intent.getStringExtra("id"));
        wordMap.put("word", intent.getStringExtra("word"));
        wordMap.put("mid", intent.getStringExtra("mid"));
        wordMap.put("translation", intent.getStringExtra("translation"));
        mAppendix = intent.getStringExtra("appendix");
        mPrefKey = intent.getStringExtra("prefKey");
        mFields = intent.getStringArrayExtra("fields");
    }

    private ViewPager initViewPager(){
        ViewPager viewPager = findViewById(R.id.pager);
        try {
            viewPager.setOffscreenPageLimit(10);
            viewPager.setAdapter(new CardFragmentPagerAdapter(getSupportFragmentManager()));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return viewPager;
    }

    private void initTabLayout(ViewPager myViewPager){
        TabLayout tabLayout = findViewById(R.id.tablayout);
        try {
            tabLayout.setupWithViewPager(myViewPager);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    private class CardFragmentPagerAdapter extends FragmentPagerAdapter {
        public CardFragmentPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return mImageFragment; // TODO: add other fragments
            } else {
                return mImageFragment;
            }
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return getString(R.string.images); // TODO: add more fragment titles
            }
            return getString(R.string.images);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity, menu);
        return true;
    }

    private String downloadImageToAnki(String base64Url){
        String returned_save_path = createAndSaveFileFromBase64Url(base64Url);
        File path = new File(returned_save_path, "");
        Log.d(TAG, "return from create and savefile: " + returned_save_path);
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
        ArrayList<String> images = mImageFragment.getSelected();
        Log.d(TAG, "images: " + images);
        ArrayList<String> addedImageFileNames = new ArrayList<>();
        for (String base64Url : images){
            String fileName = downloadImageToAnki(base64Url);
            addedImageFileNames.add(fileName);
        }
        if (addedImageFileNames.size() > 0){
            long id = Long.parseLong(wordMap.get("id"));
            long modelId = Long.parseLong(wordMap.get("mid"));
            boolean noteHasImageField = imageFieldExists(modelId);
            if (noteHasImageField) {
                String[] updatedFieldContents = replaceImageField(modelId, addedImageFileNames);
                Set<String> updatedTags = removeMarkedTag(id);
                removeObjectFromNidSet();
                Log.d(TAG, "New Field Contents: " + Arrays.toString(updatedFieldContents));
                boolean updateSucceed = mApi.updateNoteFields(id, updatedFieldContents);
                boolean tagUpdateSucceed = mApi.updateNoteTags(id, updatedTags);
                Toast.makeText(this, !updateSucceed ? getString(R.string.failed_add_card) : getString(R.string.card_updated), Toast.LENGTH_LONG).show();
                Toast.makeText(this, !tagUpdateSucceed ? getString(R.string.failed_change_tag) : getString(R.string.tag_updated), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Image field not found", Toast.LENGTH_LONG).show();
                Log.d(TAG, "The image field was not found");
            }
        } else {
            Toast.makeText(this, "Image download failed", Toast.LENGTH_LONG).show();
            Log.d(TAG, "The file downloader failed (null returned)");
        }
        finish();
    }

    private boolean imageFieldExists(Long modelId){
        String[] fieldNames = mApi.getFieldList(modelId);
        return Arrays.toString(fieldNames).contains("Image");
    }

    private String[] replaceImageField(Long modelId, ArrayList<String> fileNames){
        String[] fieldContents = mFields;
        String[] fieldNames = mApi.getFieldList(modelId);
            for(int i=0; i < fieldNames.length; i++){
                if(fieldNames[i].equals("Image")){
                    fieldContents[i] = "";
                    for (String filename : fileNames){
                        Log.d(TAG, "Adding " + filename + " to " + fieldContents[i]);
                        fieldContents[i] += filename;
                    }
                }
            }
        return fieldContents;
    }

    private Set<String> removeMarkedTag(Long id){
        Set<String> tags = mApi.getNote(id).getTags();
        String tag = "marked";
        Log.d(TAG, "Tags: " + tags);
        if (tags.remove(tag)) {
            Log.d(TAG, "removed " + tag + " tag successfully");
        } else {
            Log.d(TAG, "Failed to remove " + tag + " from card");
        }
        return tags;
    }

    private String getNidSetObjectName(){
        String nidSetObjectName = wordMap.get("id") + ",";
        nidSetObjectName += wordMap.get("mid") + ",";
        nidSetObjectName += wordMap.get("word") + ",";
        nidSetObjectName += wordMap.get("translation");
        Log.d(TAG, "nidSetObjectName: " + nidSetObjectName);
        return nidSetObjectName;
    }

    private void removeObjectFromNidSet(){
        SharedPreferences sharedPref = getSharedPreferences(mPrefKey, MODE_PRIVATE);
        Set<String> nidSet = getNidSet(sharedPref);
        String nidSetObjectName = getNidSetObjectName();
        try {
            boolean removedSuccessfully = nidSet.remove(nidSetObjectName);
            if (removedSuccessfully){
                Log.d(TAG, "removed word from " + mPrefKey + " set successfully");
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putStringSet(mPrefKey, nidSet);
                editor.apply();
            } else {
                Log.d(TAG, "word was not found in " + mPrefKey + " set");
            }
        } catch (NullPointerException e){
            e.printStackTrace();
            Log.d(TAG, "word was null");
        }
    }

    private Set<String> getNidSet(SharedPreferences sharedPref){
        try {
            return sharedPref.getStringSet(mPrefKey, null);
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.d(TAG, "nidSet not found, has it been created?");
            return Collections.emptySet();
        }
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