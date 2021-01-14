package anki.image.app;

import com.google.android.material.tabs.TabLayout;
import com.ichi2.anki.FlashCardsContract;
import com.ichi2.anki.api.AddContentApi;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class CardActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{
    private static final String ANKI_DECK_NAME = "test";
    private AddContentApi mApi;
    private ImageFragment mImageFragment;
    private AnkiDroidHelper mAnkiDroid;
    private static final int AD_PERM_REQUEST = 0;

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

        String definition = intent.getStringExtra(Intent.EXTRA_HTML_TEXT);
        boolean isHtml = false;
        if (definition == null) {
            definition = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (definition == null) {
                definition = word;
            }
        } else {
            isHtml = true;
        }

        mImageFragment = ImageFragment.newInstance(word);

        if (mAnkiDroid.shouldRequestPermission()) {
            // Permission is not granted
            Log.d("msg :", "Permission is not granted");
            mAnkiDroid.requestPermission(CardActivity.this, AD_PERM_REQUEST);
        } else {
            // Permission is granted
            finishCreate();
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

    private void createCard() {
        Log.d("msg :", "createCard() called");
        ArrayList<String> images = mImageFragment.getSelected();

        Long modelId = getModelId();
        Log.d("msg :", "modelId: " + modelId);

        Long deckId = getDeckId();
        Log.d("msg :", "deckId: " + deckId);

        //String[] fields = {new JSONArray(images).toString(), "test", "test"};
        String[] fields = {new JSONArray(images).toString(), "2", "3", "4", "5", "6", "7", "8"};
        Log.d("msg :", "fields: " + Arrays.toString(fields));

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
        Log.d("msg :", "number of fields: " + fieldNames.length);


        Long id = mApi.addNote(modelId, deckId, fields, null);
        Toast.makeText(this, id == null ? getString(R.string.failed_add_card) : getString(R.string.card_added), Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create:
                createCard();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}