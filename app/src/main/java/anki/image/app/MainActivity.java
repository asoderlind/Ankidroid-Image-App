package anki.image.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {
    public static final String POS_ADJECTIVE = "av.*";
    public static final String POS_ADVERB = "ab.*";
    public static final String POS_INTERJECTION = "in.*";
    public static final String POS_NOUN = "n.*";
    public static final String POS_PREPOSITION = "pp.*";
    public static final String POS_VERB = "vb.*";
    public static final String POS_UNKNOWN = ".*";
    public static final String POS_NONE = "";

    private class PosItem {
        private String mName;
        private String mId;

        public PosItem(int resource, String id){
            mName = getString(resource);
            mId = id;
        }

        public String getId() {return mId;}

        public String toString() {return mName;}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Setup spinner and word-edit
        final EditText wordEdit = (EditText) findViewById(R.id.wordEdit);
        final Spinner posSpinner = (Spinner) findViewById(R.id.posSpinner);

        // Add entries to spinner through adapter
        PosItem[] array = {
                new PosItem(R.string.pos_unknown, POS_UNKNOWN),
                new PosItem(R.string.pos_noun, POS_NOUN),
                new PosItem(R.string.pos_adjective, POS_ADJECTIVE),
                new PosItem(R.string.pos_verb, POS_VERB),
                new PosItem(R.string.pos_preposition, POS_PREPOSITION),
                new PosItem(R.string.pos_none, POS_NONE),
        };
        ArrayAdapter<PosItem> adapter = new ArrayAdapter<PosItem>(this, android.R.layout.simple_spinner_item, array);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        posSpinner.setAdapter(adapter);

        // Init button
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Android :","Button clicked");
                Intent intent = new Intent(MainActivity.this, CardActivity.class);

                // Send the contents of the editText input field to next activity
                intent.putExtra(Intent.EXTRA_SUBJECT, wordEdit.getText().toString());

                // Send posSpinner item to next activity
                PosItem posItem = (PosItem) posSpinner.getSelectedItem();
                intent.putExtra("in.rab.extra.pos", posItem.getId());

                startActivity(intent);
            }
        });
    }
}