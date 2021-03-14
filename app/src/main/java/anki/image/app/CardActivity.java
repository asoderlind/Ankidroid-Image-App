package anki.image.app;

import com.google.android.material.tabs.TabLayout;
import com.ichi2.anki.api.AddContentApi;

import androidx.annotation.NonNull;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CardActivity extends AppCompatActivity {
    private static final String TAG = "CardActivity : ";
    public static final int EXIT_RESULT_CODE = 5;
    public static final int UPDATE_RESULT_CODE = 4;
    private ImageFragment mImageFragment;
    private DefinitionFragment mDefinitionFragment;
    private AddContentApi mApi;
    private AnkiDroidHelper mAnkiDroid;
    private String mAppendix;
    private String mPrefKey;
    private String [] mFields;
    private Map<String, String> cardInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");
        initAnkiApi();
        cardInfo = getAllExtra();
        setTitle(cardInfo.get("word") + mAppendix + ", " + cardInfo.get("translation"));

        mImageFragment = ImageFragment.newInstance(cardInfo.get("word"), mAppendix);

        String definitionHtml = getDefinitionHtml(cardInfo.get("word"));
        boolean isHtml = definitionHtml != null;
        mDefinitionFragment = DefinitionFragment.newInstance(definitionHtml, isHtml);

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

    private Map<String, String> getAllExtra(){
        Intent intent = getIntent();
        Map<String, String> wordMap = new HashMap<>();
        wordMap.put("id", intent.getStringExtra("id"));
        wordMap.put("word", intent.getStringExtra("word"));
        wordMap.put("mid", intent.getStringExtra("mid"));
        wordMap.put("translation", getTranslationText(intent));
        mAppendix = getAppendixText(intent);
        mPrefKey = intent.getStringExtra("prefKey");
        mFields = intent.getStringArrayExtra("fields");
        return wordMap;
    }

    private String getTranslationText(Intent intent){
        return intent.getStringExtra("translation");
    }

    private String getAppendixText(Intent intent){
        String text = intent.getStringExtra("searchAppendix");
        if (text != null){
            return text;
        } else {
            return "";
        }
    }

    private String getDefinitionHtml(String word){
        long modelId = getPassedId("mid");
        String definitionField = getDefinitionField(modelId);
        return definitionField;
    }

    private String getDefinitionField(long mid){
        String[] fieldContents = mFields;
        String[] fieldNames = mApi.getFieldList(mid);
        for(int i=0; i < fieldNames.length; i++){
            if(fieldNames[i].equals("Sanseido")){
                return fieldContents[i];
            }
        }
        return null;
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
        @NonNull
        public Fragment getItem(int position) {
            switch (position){
                case 0:
                    return mImageFragment;
                case 1:
                    return mDefinitionFragment;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position){
                case 0:
                    return "Images";
                case 1:
                    return "Definition";
                default:
                    return null;
            }
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_create) {
            updateCard();
            return true;
        } else if (item.getItemId() == R.id.action_create_no_image) {
            updateNoImage();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            long id = getPassedId("id");
            long modelId = getPassedId("mid");
            boolean noteHasImageField = imageFieldExists(modelId);
            if (noteHasImageField) {
                String[] updatedFieldContents = replaceImageField(modelId, addedImageFileNames);
                Set<String> updatedTags = removeMarkedTag(id);
                removeObjectFromNidSet(cardInfo);
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
        setResult(UPDATE_RESULT_CODE);
        finish();
    }

    private void updateNoImage(){
        Log.d(TAG, "updateNoImage() called");
        long id = getPassedId("id");
        long modelId = getPassedId("mid");
        boolean noteHasImageField = imageFieldExists(modelId);
        if (noteHasImageField) {
            String[] updatedFieldContents = removeImageField(modelId);
            Set<String> updatedTags = removeMarkedTag(id);
            updatedTags = addNoImageTag(updatedTags);
            removeObjectFromNidSet(cardInfo);
            Log.d(TAG, "New Field Contents: " + Arrays.toString(updatedFieldContents));
            boolean updateSucceed = mApi.updateNoteFields(id, updatedFieldContents);
            boolean tagUpdateSucceed = mApi.updateNoteTags(id, updatedTags);
            Toast.makeText(this, !updateSucceed ? getString(R.string.failed_add_card) : getString(R.string.card_updated), Toast.LENGTH_LONG).show();
            Toast.makeText(this, !tagUpdateSucceed ? getString(R.string.failed_change_tag) : getString(R.string.tag_updated), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Image field not found", Toast.LENGTH_LONG).show();
            Log.d(TAG, "The image field was not found");
        }
        setResult(UPDATE_RESULT_CODE);
        finish();
    }

    private String downloadImageToAnki(String base64Url){
        File dir_path = getApplicationContext().getExternalFilesDir(null);
        String returned_save_path = Utils.createAndSaveFileFromBase64Url(this, dir_path, base64Url);
        File path = new File(returned_save_path, "");
        if (path.exists()){
            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "com.fileprovider", path);
            Log.d(TAG, "contentUri: " + contentUri);
            getApplicationContext().grantUriPermission("com.ichi2.anki", contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return mAnkiDroid.addMediaFromUri(contentUri, "myImageFile", "image");      // returns filename if success otherwise null
        } else {
            Log.d(TAG, "path does not exist");
            return null;
        }
    }

    private Long getPassedId(String key){
        String idValue = cardInfo.get(key);
        if (idValue != null){
            return Long.parseLong(idValue);
        } else {
            return 0L;
        }
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

    private String[] removeImageField(Long modelId){
        String[] fieldContents = mFields;
        String[] fieldNames = mApi.getFieldList(modelId);
        for(int i=0; i < fieldNames.length; i++){
            if(fieldNames[i].equals("Image")){
                fieldContents[i] = "";
            }
        }
        return fieldContents;
    }

    private Set<String> removeMarkedTag(Long id){
        Set<String> tags = mApi.getNote(id).getTags();
        String tag = "marked";
        String tagCapitalized = "Marked";
        Log.d(TAG, "Tags: " + tags);
        if (tags.remove(tag)) {
            Log.d(TAG, "removed " + tag + " tag successfully");
        } else if (tags.remove(tagCapitalized)) {
            Log.d(TAG, "removed " + tagCapitalized + " tag successfully");
        } else {
            Log.d(TAG, "Failed to remove " + tag + " or " + tagCapitalized + " from card");
        }
        return tags;
    }

    private Set<String> addNoImageTag(Set<String> tags){
        String no_image_tag = "no_image";
        Log.d(TAG, "Tags: " + tags);
        tags.add(no_image_tag);
        Log.d(TAG, "Updated tags: " + tags);
        return tags;
    }

    private void removeObjectFromNidSet(Map<String, String> wordInfo){
        SharedPreferences sharedPref = getSharedPreferences(mPrefKey, MODE_PRIVATE);
        Set<String> nidSet = getNidSet(sharedPref);
        String nidSetObjectName = CardHandler.stringifyCardInfo(wordInfo);
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
    public void onBackPressed() {
        setResult(EXIT_RESULT_CODE);
        super.onBackPressed();
        finish();
    }
}