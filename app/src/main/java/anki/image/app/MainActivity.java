package anki.image.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.widget.Toolbar;

import com.ichi2.anki.FlashCardsContract;
import com.ichi2.anki.api.AddContentApi;
import com.ichi2.anki.api.NoteInfo;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Android :";
    private AddContentApi mApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init UI elements
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final EditText wordEdit = findViewById(R.id.wordEdit);

        // Init button
        Button button = findViewById(R.id.button);
        button.setOnClickListener(v -> {
            Log.d("Android :","Button clicked");
            //Intent intent = new Intent(MainActivity.this, CardActivity.class);
            //intent.putExtra(Intent.EXTRA_SUBJECT, wordEdit.getText().toString());
            //startActivity(intent);

            Intent intent = new Intent(MainActivity.this, CardActivity.class);
            intent.putExtra("noteId",getEmptyImageWord()[0]);
            intent.putExtra(Intent.EXTRA_SUBJECT, getEmptyImageWord()[1]);
            startActivity(intent);
        });
    }

    // TODO: pass model id as return param
    private String[] getEmptyImageWord() {
        mApi = new AddContentApi(this);
        ContentResolver cr = this.getContentResolver();
        final Cursor cardsCursor = cr.query(FlashCardsContract.Note.CONTENT_URI, null, null, null, null);

        // Go through all notes from pos 0 of cursor
        for (int i = 0; i < cardsCursor.getCount(); i++){
            cardsCursor.moveToPosition(i);
            String id = cardsCursor.getString(cardsCursor.getColumnIndex("_id"));
            String flds = cardsCursor.getString(cardsCursor.getColumnIndex("flds"));
            Log.d(TAG, "id: " + id + " flds: " + flds);
            //String [] _strArray= {id, flds};
            if (!flds.contains("img")){
                String word = mApi.getNote(Long.parseLong(id)).getFields()[0];
                Log.d(TAG, "word: " + word);
                return new String[]{id, word};
            }
        }
        cardsCursor.close();
        return new String[] {"", ""};

        // Print note info for all the notes
        /*
        for (int i = 0; i < noteArrayList.size(); i++){
            Log.d(TAG, "index: " + i);
            String s = noteArrayList.get(i)[0];
            NoteInfo nInfo = mApi.getNote(Long.parseLong(s));
            for (String field : nInfo.getFields()){
                Log.d(TAG,field);
            }
        }*/
    }


}