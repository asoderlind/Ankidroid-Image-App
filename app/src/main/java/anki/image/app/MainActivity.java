package anki.image.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
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
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.ichi2.anki.api.AddContentApi;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.ichi2.anki.api.AddContentApi.READ_WRITE_PERMISSION;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback{
    public static final String TAG = "Android :";
    private EditText appendixText;
    private AddContentApi ankiDroidApi;
    private final String[] CARD_KEYS = {"no-image", "marked", "auto-generated"};
    private Map<Integer, String> buttonMap;
    private int kanjiWordIndex;
    private int englishWordIndex;
    private static final int EMPTY_IMAGE_REQUEST_CODE = 1;
    private static final int MARKED_CARD_REQUEST_CODE = 2;
    private static final int AUTO_CARD_REQUEST_CODE = 3;
    private static final int ANKIDROID_PERM_REQUEST = 0;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    //TODO: find way to get all tags and put them in a spinner
    //TODO: make spinner for choosing fields

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ANKIDROID_PERM_REQUEST || requestCode == REQUEST_EXTERNAL_STORAGE){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkPermissions();
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
        setContentView(R.layout.activity_main);
        ankiDroidApi = new AddContentApi(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        appendixText = findViewById(R.id.appendixText);
        setSupportActionBar(findViewById(R.id.toolbar));
        checkPermissions();
    }

    private void checkPermissions(){
        if (shouldRequestAnkiDroidPermission()) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{READ_WRITE_PERMISSION}, ANKIDROID_PERM_REQUEST);
        } else if (shouldRequestStoragePermission()) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        } else {
            finishCreate();
        }
    }

    private boolean shouldRequestAnkiDroidPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        return ContextCompat.checkSelfPermission(this, READ_WRITE_PERMISSION) != PackageManager.PERMISSION_GRANTED;
    }

    private boolean shouldRequestStoragePermission(){
        int storagePermissionStatus = ActivityCompat.checkSelfPermission(
                MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return storagePermissionStatus != PackageManager.PERMISSION_GRANTED;
    }

    private void addExampleCard(){
        CardAdder adder = new CardAdder(this);
        adder.addCardsToAnkiDroid();
    }

    private void finishCreate(){
        initSpinner(R.id.deckSpinner);
        initSpinner(R.id.modelSpinner);
        initButton(R.id.button_empty);
        initButton(R.id.button_marked);
        //initButton(R.id.button_auto_cards);
        initButtonMap();
        initClearPreloadedCardsButton();
        initAddExampleCardButton();
    }

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
                    //Not used
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
            int returnCode = getReturnCode(button_id);
            Map<String, String> matchingWordInfo = getWordInfo(prefKey);
            setCountTextForCards(prefKey);
            if (matchingWordInfo != null){
                Intent intent = new Intent(MainActivity.this, CardActivity.class);
                putAllExtra(intent, matchingWordInfo, prefKey);
                startActivityForResult(intent, returnCode);
            } else {
                Log.d(TAG,"Could not find any cards");
                Toast.makeText(this, "Could not find any matching cards", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void initButtonMap(){
        buttonMap = new HashMap<>();
        buttonMap.put(R.id.button_empty, CARD_KEYS[0]);
        buttonMap.put(R.id.button_marked, CARD_KEYS[1]);
        //buttonMap.put(R.id.button_auto_cards, CARD_KEYS[2]);
    }

    private String getKey(int button_id){
        if (buttonMap.containsKey(button_id)){
            return buttonMap.get(button_id);
        } else {
            return "";
        }
    }

    @SuppressLint("NonConstantResourceId")
    private int getReturnCode(int button_id){
        switch (button_id) {
            case R.id.button_empty:
                return EMPTY_IMAGE_REQUEST_CODE;
            case R.id.button_marked:
                return MARKED_CARD_REQUEST_CODE;
            default:
                return AUTO_CARD_REQUEST_CODE;
        }
    }

    private void putAllExtra(Intent intent, Map<String, String> matchingWordMap, String prefKey){
        Long cardId = getCardId(matchingWordMap);
        String[] fieldContents = getContentsIfExists(cardId);
        intent.putExtra("fields", fieldContents);
        intent.putExtra("prefKey",prefKey);
        intent.putExtra("id",matchingWordMap.get("id"));
        intent.putExtra("word", matchingWordMap.get("word"));
        intent.putExtra("mid", matchingWordMap.get("mid"));
        intent.putExtra("translation", matchingWordMap.get("translation"));
        intent.putExtra("searchAppendix", getAppendixText());
    }

    private Long getCardId(Map<String, String> wordMap){
        try{
            String cardId = wordMap.get("id");
            return Long.parseLong(cardId);
        } catch (NumberFormatException e){
            e.printStackTrace();
        }
        return 0L;
    }

    private String[] getContentsIfExists(Long id){
        try{
            return ankiDroidApi.getNote(id).getFields();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        return new String[]{};
    }

    private String getAppendixText(){
        return appendixText.getText().toString();
    }

    private void initClearPreloadedCardsButton(){
        Button button = findViewById(R.id.clear_save);
        button.setOnClickListener(v -> new AlertDialog.Builder(MainActivity.this)
                .setTitle(R.string.erase_data_title).setMessage(R.string.erase_data_long)
                .setPositiveButton("Yes", (dialog, which) -> clearPreloadedCards()).setNegativeButton("No", null).show());
    }

    private void clearPreloadedCards(){
        for (String s : CARD_KEYS){
            SharedPreferences sharedPref = getSharedPreferences(s, MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.clear();
            editor.apply();
        }
        setCountTextForCards("no-image");
        setCountTextForCards("marked");
        Toast.makeText(getBaseContext(), "Saved cards deleted", Toast.LENGTH_SHORT).show();
    }

    private void initAddExampleCardButton(){
        //Button button = findViewById(R.id.add_example_card_btn);
        //button.setOnClickListener(v -> addExampleCard());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.options_button) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    public Set<String> getCardIdSet(String cardKey){
        SharedPreferences sharedPref = getSharedPreferences(cardKey, MODE_PRIVATE);
        try {
            return sharedPref.getStringSet(cardKey, null);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return Collections.emptySet();
        }
    }

    private Map<String, String> getWordInfo(String cardKey) {
        CardHandler cardHandler = getCardHandler();
        Map<String, String> wordInfo = getPreloadedWordInfo(cardKey);
        if (wordInfo == null){
            if (cardKey.equals("no-image")){
                cardHandler.preloadAllWithoutImage();
            } else {
                cardHandler.preloadAllWithTag(cardKey);
            }
            wordInfo = getPreloadedWordInfo(cardKey);
        }
        return wordInfo;
    }

    private CardHandler getCardHandler(){
        Spinner deckSpinner = findViewById(R.id.deckSpinner);
        Spinner modelSpinner = findViewById(R.id.modelSpinner);
        return new CardHandler(this,
                                (String) deckSpinner.getSelectedItem(),
                                (String) modelSpinner.getSelectedItem(),
                                kanjiWordIndex,
                                englishWordIndex);
    }

    private Map<String, String> getPreloadedWordInfo(String cardKey){
        Log.d(TAG, "getPreloadedWordInfo() called");
        CardLoader cardLoader = new CardLoader(this);
        return cardLoader.preloadCard(cardKey);
    }

    @Override
    protected void onResume(){
        super.onResume();
        SharedPreferences defSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        kanjiWordIndex = Integer.parseInt(defSharedPref.getString("preference_jap_field_index", "0"));
        englishWordIndex = Integer.parseInt(defSharedPref.getString("preference_trans_field_index", "3"));
        Log.d(TAG,"jap_word_index" + kanjiWordIndex);
        Log.d(TAG,"eng_word_index" + englishWordIndex);
        setCountTextForCards("marked");
        setCountTextForCards("no-image");
        logSavedData();
        deleteImageFiles();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        clearPreloadedCards();
    }

    public void logSavedData(){
        for (String s : CARD_KEYS){
            Log.d(TAG, "Logging all saved cards for " + s);
            Set<String> nidSet = getCardIdSet(s);
            if (nidSet != null) {
                for (String aNid : nidSet) {
                    Log.d(TAG, "Saved card: [" + aNid + "]");
                }
            }
        }
    }

    private void setCountTextForCards(String key) {
        int size = getCardIsSetSize(key);
        String infoText;
        if (key.equals("no-image")){
            TextView text = findViewById(R.id.empty_count);
            infoText = "Preloaded empty: " + size;
            text.setText(infoText);
        } else if (key.equals("marked")) {
            TextView text =  findViewById(R.id.marked_count);
            infoText = "Preloaded marked: " + size;
            text.setText(infoText);
        }
    }

    private int getCardIsSetSize(String prefkey){
        Set<String> cardIdSet = getCardIdSet(prefkey);
        try {
            return cardIdSet.size();
        } catch (NullPointerException e){
            return 0;
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == CardActivity.EXIT_RESULT_CODE) {
            Log.d(TAG, "returned by back button");
        } else if (resultCode == CardActivity.UPDATE_RESULT_CODE){
            Log.d(TAG, "returned by update button");
            loadNextCardActivity(requestCode);
        }
    }

    private void loadNextCardActivity(int requestCode){
        switch (requestCode) {
            case EMPTY_IMAGE_REQUEST_CODE:
                Log.d(TAG, "returned from empty image adder");
                startNextCardActivity(CARD_KEYS[0], EMPTY_IMAGE_REQUEST_CODE);
                break;
            case MARKED_CARD_REQUEST_CODE:
                Log.d(TAG, "returned from marked card adder");
                startNextCardActivity(CARD_KEYS[1], MARKED_CARD_REQUEST_CODE);
                break;
            case AUTO_CARD_REQUEST_CODE:
                Log.d(TAG, "returned from auto card adder");
                startNextCardActivity(CARD_KEYS[2], AUTO_CARD_REQUEST_CODE);
                break;
        }
    }

    private void startNextCardActivity(String prefKey, int requestCode){
        CardLoader loader = new CardLoader(this);
        Map<String, String> savedCard = loader.preloadCard(prefKey);
        if (savedCard != null){
            Intent intent = new Intent(MainActivity.this, CardActivity.class);
            putAllExtra(intent, savedCard, prefKey);
            startActivityForResult(intent, requestCode);
        }
    }
}