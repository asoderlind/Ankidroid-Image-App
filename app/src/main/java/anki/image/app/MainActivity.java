package anki.image.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.ichi2.anki.FlashCardsContract;
import com.ichi2.anki.api.AddContentApi;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Android :";
    private EditText wordEdit;
    private AddContentApi mApi;
    private int jap_word_index;
    private int eng_word_index;
    //TODO: add way to save all matching queries

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG,"onCreate ---------------------");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mApi = new AddContentApi(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Load field index options
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Init UI
        wordEdit = findViewById(R.id.wordEdit);

        initSpinner(R.id.deckSpinner);
        initSpinner(R.id.modelSpinner);

        initButton(R.id.button_empty);
        initButton(R.id.button_marked);
        initButton(R.id.button_auto_cards);
    }

    @Override
    protected void onResume(){
        super.onResume();
        SharedPreferences defSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        jap_word_index = Integer.parseInt(defSharedPref.getString("preference_jap_field_index", "0"));
        Log.d(TAG,"jap_word_index" + jap_word_index);
        eng_word_index = Integer.parseInt(defSharedPref.getString("preference_trans_field_index", "3"));
        Log.d(TAG,"eng_word_index" + eng_word_index);

    }

    // create an action bar button
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
    private void initButton(int id){
        Button button = findViewById(id); // get the button
        button.setOnClickListener(v -> {
            Log.d(TAG,"Button clicked");
            String[] emptyImageArray = getWord(id);
            if (emptyImageArray != null){
                Intent intent = new Intent(MainActivity.this, CardActivity.class);

                // Add field contents
                String[] fields = mApi.getNote(Long.parseLong(emptyImageArray[0])).getFields(); // Get field contents
                intent.putExtra("fields", fields);

                // Put Extra for needed note resources
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

    // Get deck or model list and put the names into the spinner
    private void initSpinner(int id){
        final Spinner posSpinner = (Spinner) findViewById(id);
        Map<Long, String> map = (id == R.id.deckSpinner) ? mApi.getDeckList() : mApi.getModelList();
        Collection<String> values = map.values();
        String[] array = values.toArray(new String[values.size()]);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, array);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        posSpinner.setAdapter(adapter);
    }

    /**
     * Gets the word on which the image search will be based on
     * @param button_id the id of the button that was clicked
     * @return string array containing: [note_id, contents of field 0 (japanese word), model_id, contents of field 3 (eng translation)]
     */
    private String[] getWord(int button_id) {
        ContentResolver cr = this.getContentResolver();

        //TODO: save spinner choices between startups
        Spinner deckSpinner = findViewById(R.id.deckSpinner);
        Spinner modelSpinner = findViewById(R.id.modelSpinner);

        String deckName = (String) deckSpinner.getSelectedItem(); // Get selected deck name
        String modelName = (String) modelSpinner.getSelectedItem(); // Get selected model

        final Cursor cardsCursor;

        if (button_id == R.id.button_empty){
            Log.d(TAG, "Querying empty cards");
            cardsCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null, "deck:\"" + deckName + "\"", null, null); // alt: "deck:\"" + deckName + "\"" + " note:\"" + modelName + "\""
            if (cardsCursor != null) {
                Log.d(TAG, "Count: " + cardsCursor.getCount());
                return getCardWithoutImage(cardsCursor);
            }

        } else if (button_id == R.id.button_marked){

            Log.d(TAG, "Checking saved marked cards");
            String[] savedCard = getSavedCard("markedCards");
            if (savedCard != null){
                return savedCard;
            }

            Log.d(TAG, "Querying marked cards and saving");
            cardsCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null, "deck:\"" + deckName + "\"" + " tag:marked ", null, null);
            if (cardsCursor != null) {
                Log.d(TAG, "Count: " + cardsCursor.getCount());
                saveAllQueriedCards(cardsCursor, "markedCards"); // saves all the matching cards to shared prefs
                return getRandomCard(cardsCursor); // checks from queries
            }

        } else if (button_id == R.id.button_auto_cards) {
            Log.d(TAG, "Querying auto cards");
            cardsCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null, "deck:\"" + deckName + "\"" + " tag:auto-generated ", null, null);
            if (cardsCursor != null) {
                Log.d(TAG, "Count: " + cardsCursor.getCount());
                return getRandomCard(cardsCursor);
            }
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
        Set<String> nidSet = new HashSet<String>();
        // Iterate over all matching queries
        for (int i = 0; i < cardsCursor.getCount(); i++){
            cardsCursor.moveToPosition(i);
            String id = cardsCursor.getString(cardsCursor.getColumnIndex("_id"));
            String mid = cardsCursor.getString(cardsCursor.getColumnIndex("mid"));
            nidSet.add(id + "," + mid);
        }
        Log.d(TAG, "Total nidset: " + nidSet);

        // Save nids to shared
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d(TAG, "saving data to key " + prefKey);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(prefKey, nidSet);
        editor.apply();
    }

    private String[] getSavedCard(String prefKey){
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        Set<String> nidSet = sharedPref.getStringSet(prefKey, null);
        if (nidSet != null){
            String nidMid = "";
            for(String aNid: nidSet) {
                nidMid = aNid;
                Log.d(TAG, "Saved nidMid " + nidMid);
                break;
            }
            String nid = nidMid.split(",")[0];
            String mid = nidMid.split(",")[1];
            String word = mApi.getNote(Long.parseLong(nid)).getFields()[jap_word_index];
            String translation = mApi.getNote(Long.parseLong(nid)).getFields()[eng_word_index];
            Log.d(TAG, "Saved card: [" + nid + ", " + word + ", " + mid + ", " + translation + "]");
            return new String[]{nid, word, mid, translation};

        } else {
            return null;
        }
    }

    /**
     *
     * @param cardsCursor the cursor that holds the search query
     * @return the first card that does not conatin 'img' in any of the fields
     */
    private String[] getCardWithoutImage(Cursor cardsCursor){
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
}