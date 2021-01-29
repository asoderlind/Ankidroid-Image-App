package anki.image.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.ichi2.anki.FlashCardsContract;
import com.ichi2.anki.api.AddContentApi;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{
    public static final String TAG = "Android :";
    private EditText wordEdit;
    private AddContentApi ankiDroidApi;
    private AnkiDroidHelper ankiDroidHelper;
    private final String[] cardKeys = {"empty-image", "marked", "auto-generated"};
    private int kanjiWordIndex;
    private int englishWordIndex;
    private static final int ANKIDROID_PERM_REQUEST = 0;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ANKIDROID_PERM_REQUEST || requestCode == REQUEST_EXTERNAL_STORAGE){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermissions();
                finishCreate();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                AlertDialog dialog = builder.setTitle(R.string.permissions_needed)
                        .setMessage(R.string.permissions_needed_long)
                        .setPositiveButton("OK", (dialog1, which) -> finish())
                        .create();
                dialog.setOnDismissListener(dialog12 -> finish());
                dialog.show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate ---------------------");
        super.onCreate(savedInstanceState);
        ankiDroidApi = new AddContentApi(this);
        ankiDroidHelper = new AnkiDroidHelper(this);
        wordEdit = findViewById(R.id.wordEdit);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));
        checkPermissions();
        finishCreate();
    }

    private void checkPermissions(){
        if (ankiDroidHelper.shouldRequestPermission()) {
            ankiDroidHelper.requestPermission(MainActivity.this, ANKIDROID_PERM_REQUEST);
        } else if (shouldRequestStoragePermission()) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        }
    }

    private boolean shouldRequestStoragePermission(){
        int storagePermissionStatus = ActivityCompat.checkSelfPermission(
                MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return storagePermissionStatus != PackageManager.PERMISSION_GRANTED;
    }

    private void finishCreate(){
        initSpinner(R.id.deckSpinner);
        initSpinner(R.id.modelSpinner);
        initButton(R.id.button_empty);
        initButton(R.id.button_marked);
        initButton(R.id.button_auto_cards);
        initClearSavedCardsButton();
    }

    // Get deck or model list and put the names into the spinner
    private void initSpinner(int id){
        final Spinner posSpinner = findViewById(id);
        Map<Long, String> idNamePairs = null;

        if (id == R.id.deckSpinner) {
            idNamePairs = ankiDroidApi.getDeckList();
        } else if (id == R.id.modelSpinner){
            idNamePairs = ankiDroidApi.getModelList();
        }

        if (idNamePairs != null){
            String[] names = idNamePairs.values().toArray(new String[0]);
            ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            posSpinner.setAdapter(adapter);
            posSpinner.setSelection(getPersistedSpinnerItem(id));
            posSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View view, int position, long itemId) {
                    setPersistedSpinnerItem(position, id);
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {

                }
            });
        }
    }

    private int getPersistedSpinnerItem(int id) {
        if (id == R.id.deckSpinner){
            return PreferenceManager.getDefaultSharedPreferences(this).getInt("deckSelection", 0);
        } else if (id == R.id.modelSpinner){
            return PreferenceManager.getDefaultSharedPreferences(this).getInt("modelSelection", 0);
        } else {
            Log.d(TAG, "No matching spinner found");
            return 0;
        }
    }

    @SuppressLint("ApplySharedPref")
    protected void setPersistedSpinnerItem(int position, int id) {
        if (id == R.id.deckSpinner){
            PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("deckSelection", position).commit();
        } else if (id == R.id.modelSpinner){
            PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("modelSelection", position).commit();
        } else {
            Log.d(TAG, "No matching spinner found");
        }
    }

    private void initButton(int button_id){
        Button button = findViewById(button_id);
        button.setOnClickListener(v -> {
            Log.d(TAG, "Button pressed");
            String prefKey = getKey(button_id);
            String[] matchingWordInfo = getMatchingWordInfo(prefKey);
            if (matchingWordInfo != null){
                Intent intent = new Intent(MainActivity.this, CardActivity.class);
                putAllExtra(intent, matchingWordInfo, prefKey);
                startActivity(intent);
            } else {
                Log.d(TAG,"Could not find any cards");
                Toast.makeText(this, "Could not find any matching cards", Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getKey(int button_id){
        if (button_id == R.id.button_empty){
            return cardKeys[0];
        } else if (button_id == R.id.button_marked) {
            return cardKeys[1];
        } else if (button_id == R.id.button_auto_cards) {
            return cardKeys[2];
        } else {
            return null;
        }
    }

    private void putAllExtra(Intent intent, String[] matchingWordInfo, String prefKey){
        String[] fieldContents = ankiDroidApi.getNote(Long.parseLong(matchingWordInfo[0])).getFields();
        intent.putExtra("fields", fieldContents);
        intent.putExtra("prefKey",prefKey);
        intent.putExtra("noteId",matchingWordInfo[0]);
        intent.putExtra("word", matchingWordInfo[1]);
        intent.putExtra("modelId", matchingWordInfo[2]);
        intent.putExtra("translation", matchingWordInfo[3]);
        intent.putExtra("searchAppendix", wordEdit.getText().toString());
    }

    private void initClearSavedCardsButton(){
        Button button = findViewById(R.id.clear_save);
        button.setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.erase_data_title).setMessage(R.string.erase_data_long)
                    .setPositiveButton("Yes", (dialog, which) -> deleteSavedCards()).setNegativeButton("No", null).show();
        });
    }

    private void deleteSavedCards(){
        //TODO: make keys non-global
        for (String s : cardKeys){
            SharedPreferences sharedPref = getSharedPreferences(s, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.clear();
            editor.apply();
        }
        Toast.makeText(getBaseContext(), "Saved cards deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.options_button) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private Set<String> getCardIdSet(String cardKey){
        SharedPreferences sharedPref = getSharedPreferences(cardKey, MODE_PRIVATE);
        return sharedPref.getStringSet(cardKey, null);
    }

    private String[] getMatchingWordInfo(String cardKey) {
        ContentResolver cr = this.getContentResolver();
        Spinner deckSpinner = findViewById(R.id.deckSpinner);
        Spinner modelSpinner = findViewById(R.id.modelSpinner);
        String selectedDeck = (String) deckSpinner.getSelectedItem(); // Get selected deck name
        String selectedModel = (String) modelSpinner.getSelectedItem(); // Get selected model

        CardFinder finder = new CardFinder(selectedDeck, selectedModel, cardKey);

        final Cursor cardsCursor;

        if (cardKey.equals("empty-image")) {
            String[] savedCard = getSavedCard(cardKey);
            if (savedCard != null) {
                return savedCard;
            }
            cardsCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null, "deck:\"" + selectedDeck + "\"" + " note:\"" + selectedModel + "\"", null, null); // alt: "deck:\"" + deckName + "\""

            if (cardsCursor != null) {
                Log.d(TAG, "Count: " + cardsCursor.getCount());
                saveAllNoImageCards(cardsCursor, cardKey);
                return getFirstCardWithoutImage(cardsCursor);
            }

        } else if (cardKey.equals("marked")) {
            String[] savedCard = getSavedCard(cardKey);
            if (savedCard != null) {
                return savedCard;
            }
            cardsCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null, "deck:\"" + selectedDeck + "\"" + " tag:marked ", null, null);
            if (cardsCursor != null) {
                Log.d(TAG, "Count: " + cardsCursor.getCount());
                saveAllQueriedCards(cardsCursor, cardKey); // saves all the matching cards to shared prefs
                return getRandomCard(cardsCursor); // checks from queries
            }

        } else if (cardKey.equals("auto-generated")) {
            cardsCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null, "deck:\"" + selectedDeck + "\"" + " tag:auto-generated ", null, null);
            if (cardsCursor != null) {
                Log.d(TAG, "Count: " + cardsCursor.getCount());
                return getRandomCard(cardsCursor);
            }
        }
        return null;
    }

    /**
     *
     * @param cardsCursor the cursor that holds the search query
     * @return a random card that matches the query
     */
    private String[] getRandomCard(Cursor cardsCursor){
        Random r = new Random();
        cardsCursor.moveToPosition(r.nextInt(cardsCursor.getCount()));
        String id = cardsCursor.getString(cardsCursor.getColumnIndex("_id"));
        String mid = cardsCursor.getString(cardsCursor.getColumnIndex("mid"));
        cardsCursor.close();
        String word = ankiDroidApi.getNote(Long.parseLong(id)).getFields()[kanjiWordIndex];
        String translation = ankiDroidApi.getNote(Long.parseLong(id)).getFields()[englishWordIndex];
        Log.d(TAG, "Returning word: " + word);
        return new String[]{id, word, mid, translation};
    }

    private void saveAllQueriedCards(Cursor cardsCursor, String prefKey){
        Log.d(TAG, "Saving all queried cards...");
        Set<String> nidSet = new HashSet<>();
        // Iterate over all matching queries
        for (int i = 0; i < cardsCursor.getCount(); i++){
            cardsCursor.moveToPosition(i);
            String id = cardsCursor.getString(cardsCursor.getColumnIndex("_id"));
            String mid = cardsCursor.getString(cardsCursor.getColumnIndex("mid"));
            nidSet.add(id + "," + mid);
        }
        Log.d(TAG, "Total nidset: " + nidSet);

        // Save nids to shared
        SharedPreferences sharedPref = getSharedPreferences(prefKey, MODE_PRIVATE);
        Log.d(TAG, "saving data to key " + prefKey);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(prefKey, nidSet);
        editor.apply();
    }

    private void saveAllNoImageCards(Cursor cardsCursor, String prefKey){
        Log.d(TAG, "Saving all no image cards...");
        Set<String> nidSet = new HashSet<>();
        for (int i = 0; i < cardsCursor.getCount(); i++){
            cardsCursor.moveToPosition(i);
            String flds = cardsCursor.getString(cardsCursor.getColumnIndex("flds"));
            if (!flds.contains("img")){
                String id = cardsCursor.getString(cardsCursor.getColumnIndex("_id"));
                String mid = cardsCursor.getString(cardsCursor.getColumnIndex("mid"));
                nidSet.add(id + "," + mid);
            }
        }
        Log.d(TAG, "Total nidset: " + nidSet);
        SharedPreferences sharedPref = getSharedPreferences(prefKey, MODE_PRIVATE);
        Log.d(TAG, "saving data to key " + prefKey);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(prefKey, nidSet);
        editor.apply();
    }

    public String[] getSavedCard(String prefKey){
        Set <String> cardIdSet = getCardIdSet(prefKey);
        if (cardIdSet != null){
            if (cardIdSet.size() != 0){
                String nidMid = "";
                for(String aNid: cardIdSet) {
                    nidMid = aNid;
                    break;
                }
                String nid = nidMid.split(",")[0];
                String mid = nidMid.split(",")[1];
                String word = ankiDroidApi.getNote(Long.parseLong(nid)).getFields()[kanjiWordIndex];
                String translation = ankiDroidApi.getNote(Long.parseLong(nid)).getFields()[englishWordIndex];
                Log.d(TAG, "Saved card: [" + nid + ", " + word + ", " + mid + ", " + translation + "]");
                return new String[]{nid, word, mid, translation};
            }
        }
        return null;
    }
    
    private String[] getFirstCardWithoutImage(Cursor cardsCursor){
        // Iterate from top to bottom
        for (int i = cardsCursor.getCount() - 1; i > 0; i--){
            cardsCursor.moveToPosition(i);
            String id = cardsCursor.getString(cardsCursor.getColumnIndex("_id"));
            String flds = cardsCursor.getString(cardsCursor.getColumnIndex("flds"));
            String mid = cardsCursor.getString(cardsCursor.getColumnIndex("mid"));
            if (!flds.contains("img")){
                cardsCursor.close();
                String word = ankiDroidApi.getNote(Long.parseLong(id)).getFields()[kanjiWordIndex];
                String translation = ankiDroidApi.getNote(Long.parseLong(id)).getFields()[englishWordIndex];
                Log.d(TAG, "Returning word: " + word);
                return new String[]{id, word, mid, translation};
            }
        }
        cardsCursor.close();
        return null;
    }

    public void logSavedData(String key){
        Log.d(TAG, "Logging all saved cards for " + key);
        SharedPreferences sharedPref = getSharedPreferences(key, MODE_PRIVATE);
        Set<String> nidSet = sharedPref.getStringSet(key, null);
        if (nidSet != null) {
            for (String aNid : nidSet) {
                String nid = aNid.split(",")[0];
                String mid = aNid.split(",")[1];
                String word = ankiDroidApi.getNote(Long.parseLong(nid)).getFields()[kanjiWordIndex];
                String translation = ankiDroidApi.getNote(Long.parseLong(nid)).getFields()[englishWordIndex];
                Log.d(TAG, "Saved card: [" + nid + ", " + word + ", " + mid + ", " + translation + "]");
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        SharedPreferences defSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        kanjiWordIndex = Integer.parseInt(defSharedPref.getString("preference_jap_field_index", "0"));
        englishWordIndex = Integer.parseInt(defSharedPref.getString("preference_trans_field_index", "3"));
        Log.d(TAG,"jap_word_index" + kanjiWordIndex);
        Log.d(TAG,"eng_word_index" + englishWordIndex);
        logSavedData("markedCards");
        logSavedData("emptyImageCards");
        setCountTextForCards("marked");
        setCountTextForCards("empty-image");
        deleteImageFiles();
    }

    private void setCountTextForCards(String key) {
        Set<String> cardIdSet = getCardIdSet(key);
        if (cardIdSet != null){
            TextView text = null;
            String infoText = "";
            if(key.equals("empty-image")){
                text = findViewById(R.id.empty_count);
                infoText = "Empty Cards: " + cardIdSet.size();
            } else if (key.equals("marked")) {
                text =  findViewById(R.id.marked_count);
                infoText = "Marked Cards: " + cardIdSet.size();
            }
            if (text != null){
                text.setText(infoText);
            }
        }
    }

    private void deleteImageFiles() {
        Log.d(TAG,"Deleting image files...");
        File fileDir = getApplicationContext().getExternalFilesDir(null);
        for (File child : fileDir.listFiles()){
            if (child.exists()){
                boolean deletedSuccessfully = child.delete();
                if(deletedSuccessfully){
                    Log.d(TAG, child + " deleted successfully");
                }
            }
        }
    }

}