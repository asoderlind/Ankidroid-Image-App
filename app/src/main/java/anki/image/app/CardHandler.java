package anki.image.app;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

import com.ichi2.anki.FlashCardsContract;
import com.ichi2.anki.api.AddContentApi;
import com.ichi2.anki.api.NoteInfo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class CardHandler {
    final String TAG = "CardHandler ::";
    final AddContentApi mAnkiDroidApi;
    final String mDeckName;
    final String mModelName;
    final ContentResolver mContentResolver;
    final Context mContext;
    int mKanjiField;
    int mEnglishField;

    public CardHandler(Context context, String deckName, String modelName, int kanjiField, int englishField){
        mDeckName = deckName;
        mModelName = modelName;
        mContext = context;
        mContentResolver = context.getContentResolver();
        mAnkiDroidApi = new AddContentApi(context);
        mKanjiField = kanjiField;
        mEnglishField = englishField;
    }

    public Map<String, String> getCardWithoutImage(){
        Cursor cardCursor = mContentResolver.query(FlashCardsContract.Note.CONTENT_URI,
                null,
                "deck:\"" + mDeckName + "\"" + " note:\"" + mModelName + "\"",
                null,
                null);
        if (cardCursor != null) {
            Log.d(TAG, "Count for no images: " + cardCursor.getCount());
            for (int i = cardCursor.getCount() - 1; i > 0; i--) {
                Map<String, String> cursorInfo = getCursorInfo(cardCursor, i);
                String fields = cursorInfo.get("flds");
                if (!fields.contains("img")) {
                    return cursorInfo;
                }
            }
            cardCursor.close();
        }
        return null;
    }

    public Map<String, String> getCardFromTag(String tag){
        Cursor cardCursor = mContentResolver.query(FlashCardsContract.Note.CONTENT_URI,
                null,
                "deck:\"" + mDeckName + "\"" + " tag:" + tag,
                null,
                null);
        if (cardCursor != null) {
            Log.d(TAG, "Count for " + tag + ": " + cardCursor.getCount());
            Random r = new Random();
            int randomPos = r.nextInt(cardCursor.getCount());
            Map<String, String> cursorInfo = getCursorInfo(cardCursor, randomPos);
            cardCursor.close();
            return cursorInfo;
        } else {
            return null;
        }
    }

    private Map<String, String> getCursorInfo(Cursor cursor, int position){
        cursor.moveToPosition(position);
        String id = cursor.getString(cursor.getColumnIndex("_id"));
        String mid = cursor.getString(cursor.getColumnIndex("mid"));
        String flds = cursor.getString(cursor.getColumnIndex("flds"));
        NoteInfo note = mAnkiDroidApi.getNote(Long.parseLong(id));
        String word = note.getFields()[mKanjiField];
        String translation = note.getFields()[mEnglishField];
        Map<String, String> cardMap = new HashMap<>();
        cardMap.put("word", word);
        cardMap.put("translation", translation);
        cardMap.put("id", id);
        cardMap.put("mid", mid);
        cardMap.put("flds", flds);
        Log.d(TAG, "Cursor Info: Id = " + id + ", mid = " + mid + " word = " + word + " translation = " + translation);
        return cardMap;
    }

    public void saveAllWithoutImage() {
        Cursor cardCursor = mContentResolver.query(FlashCardsContract.Note.CONTENT_URI,
                null,
                "deck:\"" + mDeckName + "\"" + " note:\"" + mModelName + "\"",
                null,
                null);
        if (cardCursor != null) {
            Set<String> nidSet = new HashSet<>();
            Log.d(TAG, "Count for no images: " + cardCursor.getCount());
            for (int i = 0; i < cardCursor.getCount(); i++) {
                Map<String, String> cursorInfo = getCursorInfo(cardCursor, i);
                String fields = cursorInfo.get("flds");
                if (!fields.contains("img")) {
                    nidSet.add(cursorInfo.get("id") +
                            "," + cursorInfo.get("mid") +
                            "," + cursorInfo.get("word") +
                            "," + cursorInfo.get("translation"));
                }
            }
            cardCursor.close();
            Log.d(TAG, "Total nidset: " + nidSet);
            SharedPreferences.Editor editor = mContext.getSharedPreferences("no-image", Context.MODE_PRIVATE).edit();
            editor.putStringSet("no-image", nidSet);
            editor.apply();
        } else {
            Log.d(TAG, "The cursor returned null for saveAllWithoutImages");
        }
    }

    public void saveAllWithTag(String tag){
        Cursor cardCursor = mContentResolver.query(FlashCardsContract.Note.CONTENT_URI,
                null,
                "deck:\"" + mDeckName + "\"" + " tag:" + tag,
                null,
                null);
        if (cardCursor != null) {
            Set<String> noteStringSet = new HashSet<>();
            for (int i = 0; i < cardCursor.getCount(); i++) {
                Map<String, String> cursorInfo = getCursorInfo(cardCursor, i);
                noteStringSet.add(cursorInfo.get("id") +
                        "," + cursorInfo.get("mid") +
                        "," + cursorInfo.get("word") +
                        "," + cursorInfo.get("translation"));
            }
            cardCursor.close();
            Log.d(TAG, "Total nidset: " + noteStringSet);
            Log.d(TAG, "Saving data with tag: " + tag);
            SharedPreferences.Editor editor = mContext.getSharedPreferences(tag, Context.MODE_PRIVATE).edit();
            editor.putStringSet(tag, noteStringSet);
            editor.apply();
        } else {
            Log.d(TAG, "The cursor returned null for saveAllWithTag");
        }
    }

    public Map<String, String> getSavedCard(String prefKey){
        Set <String> cardIdSet = getCardIdSet(prefKey);
        if (cardIdSet != null){
            if (cardIdSet.size() != 0){
                String savedNoteString = "";
                for(String aNidMid: cardIdSet) {
                    savedNoteString = aNidMid; // get a random element from the set
                    break;
                }
                Map<String, String> cardMap = new HashMap<>();
                cardMap.put("id", savedNoteString.split(",")[0]);
                cardMap.put("mid", savedNoteString.split(",")[1]);
                cardMap.put("word", savedNoteString.split(",")[2]);
                cardMap.put("translation", savedNoteString.split(",")[3]);
                Log.d(TAG, "Saved card: [" + savedNoteString + "]");
                return cardMap;
            }
        }
        return null;
    }

    public void logSavedData(String prefKey){
        Log.d(TAG, "Logging all saved cards for " + prefKey);
        Set<String> nidSet = getCardIdSet(prefKey);
        if (nidSet != null) {
            for (String aNid : nidSet) {
                Log.d(TAG, "Saved card: [" + aNid + "]");
            }
        }
    }

    private Set<String> getCardIdSet(String prefKey){
        return mContext.getSharedPreferences(prefKey, Context.MODE_PRIVATE).getStringSet(prefKey, null);
    }

}
