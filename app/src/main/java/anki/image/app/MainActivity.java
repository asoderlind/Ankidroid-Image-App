package anki.image.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.ichi2.anki.FlashCardsContract;
import com.ichi2.anki.api.AddContentApi;

import java.util.Collection;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Android :";
    private EditText wordEdit;
    private AddContentApi mApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mApi = new AddContentApi(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        wordEdit = findViewById(R.id.wordEdit);

        initSpinner(R.id.deckSpinner);
        initSpinner(R.id.modelSpinner);

        initButton(R.id.button_empty);
        initButton(R.id.button_marked);
        initButton(R.id.button_auto_cards);
    }

    // Init either empty card or marked card spinner
    private void initButton(int id){
        Button button = findViewById(id);
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
            cardsCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null, "deck:\"" + deckName + "\"", null, null);
            //cardsCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null, "deck:\"" + deckName + "\"" + " note:\"" + modelName + "\"", null, null); // includes model query
            return getCardWithoutImage(cardsCursor);

        } else if (button_id == R.id.button_marked){
            Log.d(TAG, "Querying marked cards");
            cardsCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null, "deck:\"" + deckName + "\"" + " tag:marked ", null, null);
            Log.d(TAG, "Count: " + cardsCursor.getCount());
            return getRandomCard(cardsCursor);

        } else if (button_id == R.id.button_auto_cards) {
            Log.d(TAG, "Querying auto cards");
            cardsCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null, "deck:\"" + deckName + "\"" + " tag:auto-generated ", null, null);
            Log.d(TAG, "Count: " + cardsCursor.getCount());
            return getRandomCard(cardsCursor);
        }
        return null;
    }

    private String[] getRandomCard(Cursor cardsCursor){
        Random r = new Random();
        cardsCursor.moveToPosition(r.nextInt(cardsCursor.getCount()));
        String id = cardsCursor.getString(cardsCursor.getColumnIndex("_id"));
        String mid = cardsCursor.getString(cardsCursor.getColumnIndex("mid"));
        cardsCursor.close();
        String word = mApi.getNote(Long.parseLong(id)).getFields()[0];
        String translation = mApi.getNote(Long.parseLong(id)).getFields()[3];
        Log.d(TAG, "Returning word: " + word);
        return new String[]{id, word, mid, translation};
    }

    private String[] getCardWithoutImage(Cursor cardsCursor){
        // Iterate from top to bottom
        for (int i = cardsCursor.getCount() - 1; i > 0; i--){
            cardsCursor.moveToPosition(i);
            String id = cardsCursor.getString(cardsCursor.getColumnIndex("_id"));
            String flds = cardsCursor.getString(cardsCursor.getColumnIndex("flds"));
            String mid = cardsCursor.getString(cardsCursor.getColumnIndex("mid"));
            if (!flds.contains("img")){
                cardsCursor.close();
                String word = mApi.getNote(Long.parseLong(id)).getFields()[0];
                String translation = mApi.getNote(Long.parseLong(id)).getFields()[3];
                Log.d(TAG, "Returning word: " + word);
                return new String[]{id, word, mid, translation};
            }
        }
        cardsCursor.close();
        return null; // Found no card without image
    }
}