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

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.ichi2.anki.FlashCardsContract;
import com.ichi2.anki.api.AddContentApi;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.ichi2.anki.api.AddContentApi.READ_WRITE_PERMISSION;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{
    public static final String TAG = "Android :";
    private EditText wordEdit;
    private AddContentApi mApi;
    private AnkiDroidHelper mAnkiDroid;
    private final String[] keys = {"emptyImageCards", "markedCards", "autoCards"};
    private int jap_word_index;
    private int eng_word_index;
    private static final int ANKIDROID_PERM_REQUEST = 0;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode== ANKIDROID_PERM_REQUEST && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            finishCreate();
        } else if (requestCode== REQUEST_EXTERNAL_STORAGE && grantResults[0] == PackageManager.PERMISSION_GRANTED)  {
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
        Log.d(TAG,"onCreate ---------------------");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mApi = new AddContentApi(this);
        mAnkiDroid = new AnkiDroidHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Load default preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        wordEdit = findViewById(R.id.wordEdit);

        finishCreate();
    }

    private void finishCreate(){
        Log.d(TAG,"the finishCreate method");

        // Check AnkiDroid permission
        if (mAnkiDroid.shouldRequestPermission()) {
            Log.d(TAG,"should Request AD-Permission");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{READ_WRITE_PERMISSION}, ANKIDROID_PERM_REQUEST);
        } else if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        } else {
            Log.d(TAG,"AD-Permission cleared");
            Log.d(TAG,"Storage Permission cleared");

            // Init the rest of the UI elements
            initSpinner(R.id.deckSpinner);
            initSpinner(R.id.modelSpinner);
            initButton(R.id.button_empty);
            initButton(R.id.button_marked);
            initButton(R.id.button_auto_cards);
            initSaveButton();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        SharedPreferences defSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        jap_word_index = Integer.parseInt(defSharedPref.getString("preference_jap_field_index", "0"));
        eng_word_index = Integer.parseInt(defSharedPref.getString("preference_trans_field_index", "3"));
        Log.d(TAG,"jap_word_index" + jap_word_index);
        Log.d(TAG,"eng_word_index" + eng_word_index);
        logSavedData("markedCards");
        logSavedData("emptyImageCards");
        updateCardCountText("markedCards");
        updateCardCountText("emptyImageCards");
    }

    private void updateCardCountText(String key) {
        Log.d(TAG, "Logging all saved cards for " + key);
        SharedPreferences sharedPref = getSharedPreferences(key, MODE_PRIVATE);
        Set<String> nidSet = sharedPref.getStringSet(key, null);
        if (nidSet != null){
            TextView text = (key.equals("emptyImageCards")) ? findViewById(R.id.empty_count) : findViewById(R.id.marked_count); // get the button
            String prefix = (key.equals("emptyImageCards")) ? "Empty cards: " : "Marked Cards: ";
            String dispText = prefix + nidSet.size();
            text.setText(dispText);
        }
    }

    // create an action bar options button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // handle button activities
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.mybutton) {
            Log.d(TAG,"Options clicked");
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    // Init either empty card-button or marked card-button
    private void initButton(int button_id){
        Button button = findViewById(button_id); // get the button
        button.setOnClickListener(v -> {
            Log.d(TAG,"Button clicked");
            String prefKey = getKey(button_id);
            String[] emptyImageArray = getWord(prefKey);
            if (emptyImageArray != null){
                Intent intent = new Intent(MainActivity.this, CardActivity.class);
                String[] fields = mApi.getNote(Long.parseLong(emptyImageArray[0])).getFields(); // Get field contents
                intent.putExtra("fields", fields); // Add field contents
                intent.putExtra("prefKey",prefKey); // Add prefKey
                intent.putExtra("noteId",emptyImageArray[0]);
                intent.putExtra("word", emptyImageArray[1]);
                intent.putExtra("modelId", emptyImageArray[2]);
                intent.putExtra("translation", emptyImageArray[3]);
                intent.putExtra("searchAppendix", wordEdit.getText().toString());
                startActivity(intent);
            } else {
                Log.d(TAG,"Could not find any cards");
                Toast.makeText(this, "Could not find any matching cards", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initSaveButton(){
        Button button = findViewById(R.id.clear_save);
        button.setOnClickListener(v -> {
            Log.d(TAG,"Clear Button clicked");
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Erasing local data").setMessage("Are you sure you want to erase all saved lists? (this will force an re-check")
                    .setPositiveButton("Yes", (dialog, which) -> deleteSharedPreferences()).setNegativeButton("No", null).show();
        });
    }

    private void deleteSharedPreferences(){
        Log.d(TAG, "Deleting sharedPrefs 'boardConfig'...");
        for (String s : keys){
            Log.d(TAG,"Deleting data for " + s);
            SharedPreferences sharedPref = getSharedPreferences(s, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.clear();
            editor.apply();
        }
        Toast.makeText(getBaseContext(), "Data reset", Toast.LENGTH_SHORT).show();
    }

    // Get deck or model list and put the names into the spinner
    private void initSpinner(int id){
        final Spinner posSpinner = findViewById(id);
        // Set the spinner values
        Map<Long, String> map = (id == R.id.deckSpinner) ? mApi.getDeckList() : mApi.getModelList();
        Collection<String> values = map.values();
        String[] array = values.toArray(new String[0]);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, array);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        posSpinner.setAdapter(adapter);
        posSpinner.setSelection(getPersistedItem(id));
        // Set the spinner to save state
        posSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View view, int position, long itemId) {
                setPersistedItem(position, id);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });
    }

    private int getPersistedItem(int id) {
        Log.d(TAG, "Getting spinner selection");
        if (id == R.id.deckSpinner){
            return PreferenceManager.getDefaultSharedPreferences(this).getInt("deckSelection", 0);
        } else {
            return PreferenceManager.getDefaultSharedPreferences(this).getInt("modelSelection", 0);
        }
    }

    @SuppressLint("ApplySharedPref")
    protected void setPersistedItem(int position, int id) {
        Log.d(TAG, "Setting spinner selection");
        if (id == R.id.deckSpinner){
            PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("deckSelection", position).commit();
        } else {
            PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("modelSelection", position).commit();
        }
    }

    private String getKey(int button_id){
        if (button_id == R.id.button_empty){
            return "emptyImageCards";
        } else if (button_id == R.id.button_marked) {
            return "markedCards";
        } else if (button_id == R.id.button_auto_cards) {
            return "autoCards";
        } else {
            return null;
        }
    }

    /**
     * Gets the word on which the image search will be based on
     * @param prefKey the id of the button that was clicked
     * @return string array containing: [note_id, contents of field 0 (japanese word), model_id, contents of field 3 (eng translation)]
     */
    private String[] getWord(String prefKey) {
        ContentResolver cr = this.getContentResolver();
        Spinner deckSpinner = findViewById(R.id.deckSpinner);
        Spinner modelSpinner = findViewById(R.id.modelSpinner);
        String deckName = (String) deckSpinner.getSelectedItem(); // Get selected deck name
        String modelName = (String) modelSpinner.getSelectedItem(); // Get selected model
        final Cursor cardsCursor;

        switch (prefKey) {
            case "emptyImageCards": {
                Log.d(TAG, "Checking saved empty image cards");
                String[] savedCard = getSavedCard(prefKey);
                if (savedCard != null) {
                    Log.d(TAG, "Saved empty image card found!");
                    return savedCard;
                }
                Log.d(TAG, "No save data, querying all cards in deck " + deckName);
                cardsCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null, "deck:\"" + deckName + "\"" + " note:\"" + modelName + "\"", null, null); // alt: "deck:\"" + deckName + "\""

                if (cardsCursor != null) {
                    Log.d(TAG, "Count: " + cardsCursor.getCount());
                    saveAllNoImageCards(cardsCursor, prefKey);
                    return getFirstCardWithoutImage(cardsCursor);
                }
                break;
            }
            case "markedCards": {
                Log.d(TAG, "Checking saved marked cards");
                String[] savedCard = getSavedCard(prefKey);
                if (savedCard != null) {
                    return savedCard;
                }
                Log.d(TAG, "No save data, querying marked cards in deck " + deckName);
                cardsCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null, "deck:\"" + deckName + "\"" + " tag:marked ", null, null);
                if (cardsCursor != null) {
                    Log.d(TAG, "Count: " + cardsCursor.getCount());
                    saveAllQueriedCards(cardsCursor, prefKey); // saves all the matching cards to shared prefs
                    return getRandomCard(cardsCursor); // checks from queries
                }
                break;
            }
            case "autoCards":
                Log.d(TAG, "Querying auto cards in deck " + deckName);
                cardsCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null, "deck:\"" + deckName + "\"" + " tag:auto-generated ", null, null);
                if (cardsCursor != null) {
                    Log.d(TAG, "Count: " + cardsCursor.getCount());
                    return getRandomCard(cardsCursor);
                }
                break;
        }
        Log.d(TAG, "the cursor returned null");
        return null;
    }

    /**
     *
     * @param cardsCursor the cursor that holds the search query
     * @return a random card that matches the query
     */
    private String[] getRandomCard(Cursor cardsCursor){
        Random r = new Random();
        // Get id and mid from the cursor
        cardsCursor.moveToPosition(r.nextInt(cardsCursor.getCount()));
        String id = cardsCursor.getString(cardsCursor.getColumnIndex("_id"));
        String mid = cardsCursor.getString(cardsCursor.getColumnIndex("mid"));
        cardsCursor.close();
        // get the word and translation from the api
        String word = mApi.getNote(Long.parseLong(id)).getFields()[jap_word_index];
        String translation = mApi.getNote(Long.parseLong(id)).getFields()[eng_word_index];
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
        // Iterate over all matching queries
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

        // Save nids to shared
        SharedPreferences sharedPref = getSharedPreferences(prefKey, MODE_PRIVATE);
        Log.d(TAG, "saving data to key " + prefKey);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(prefKey, nidSet);
        editor.apply();
    }

    public String[] getSavedCard(String prefKey){
        SharedPreferences sharedPref = getSharedPreferences(prefKey, MODE_PRIVATE);
        Set<String> nidSet = sharedPref.getStringSet(prefKey, null);
        if (nidSet != null){
            if (nidSet.size() != 0){
                String nidMid = "";
                for(String aNid: nidSet) {
                    nidMid = aNid;
                    break;
                }
                String nid = nidMid.split(",")[0];
                String mid = nidMid.split(",")[1];
                String word = mApi.getNote(Long.parseLong(nid)).getFields()[jap_word_index];
                String translation = mApi.getNote(Long.parseLong(nid)).getFields()[eng_word_index];
                Log.d(TAG, "Saved card: [" + nid + ", " + word + ", " + mid + ", " + translation + "]");
                return new String[]{nid, word, mid, translation};
            }
        }
        return null;
    }

    /**
     *
     * @param cardsCursor the cursor that holds the search query
     * @return the first card that does not conatin 'img' in any of the fields
     */
    private String[] getFirstCardWithoutImage(Cursor cardsCursor){
        // Iterate from top to bottom
        for (int i = cardsCursor.getCount() - 1; i > 0; i--){
            cardsCursor.moveToPosition(i);
            String id = cardsCursor.getString(cardsCursor.getColumnIndex("_id"));
            String flds = cardsCursor.getString(cardsCursor.getColumnIndex("flds"));
            String mid = cardsCursor.getString(cardsCursor.getColumnIndex("mid"));
            if (!flds.contains("img")){
                cardsCursor.close();
                String word = mApi.getNote(Long.parseLong(id)).getFields()[jap_word_index];
                String translation = mApi.getNote(Long.parseLong(id)).getFields()[eng_word_index];
                Log.d(TAG, "Returning word: " + word);
                return new String[]{id, word, mid, translation};
            }
        }
        cardsCursor.close();
        return null; // Found no card without image
    }

    public void logSavedData(String key){
        Log.d(TAG, "Logging all saved cards for " + key);
        SharedPreferences sharedPref = getSharedPreferences(key, MODE_PRIVATE);
        Set<String> nidSet = sharedPref.getStringSet(key, null);
        if (nidSet != null) {
            for (String aNid : nidSet) {
                String nid = aNid.split(",")[0];
                String mid = aNid.split(",")[1];
                String word = mApi.getNote(Long.parseLong(nid)).getFields()[jap_word_index];
                String translation = mApi.getNote(Long.parseLong(nid)).getFields()[eng_word_index];
                Log.d(TAG, "Saved card: [" + nid + ", " + word + ", " + mid + ", " + translation + "]");
            }
        }
    }

}