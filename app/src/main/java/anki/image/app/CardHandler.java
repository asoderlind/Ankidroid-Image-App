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
import java.util.Set;

public class CardHandler {
    final String TAG = "CardHandler :";
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

    public void preloadAllWithoutImage() {
        Log.d(TAG, "preloadAllWithoutImageFast() called");
        Cursor cardCursor = mContentResolver.query(FlashCardsContract.Note.CONTENT_URI,
                null,
                "deck:\"" + mDeckName + "\"" + " note:\"" + mModelName + "\"" + " -<img*src=*>" + " -tag:no_image",
                null,
                null);
        Set <String> noteStringSet = getMatchingCardSet(cardCursor);
        addSetToSharedPrefs(noteStringSet, "no-image");
    }

    public void preloadAllWithTag(String tag){
        Log.d(TAG, "preloadAllWithTag() called");
        Cursor cardCursor = mContentResolver.query(FlashCardsContract.Note.CONTENT_URI,
                null,
                "deck:\"" + mDeckName + "\"" + " tag:" + tag,
                null,
                null);
        Set <String> noteStringSet = getMatchingCardSet(cardCursor);
        addSetToSharedPrefs(noteStringSet, tag);
    }

    private Set<String> getMatchingCardSet(Cursor cursor){
        int cardCount = getCount(cursor);
        Log.d(TAG, "cardCount is: " + cardCount);
        Set<String> noteStringSet = new HashSet<>();
        for (int i = 0; i < cardCount; i++) {
            Map<String, String> cursorInfo = getCursorInfo(cursor, i);
            String cardString = stringifyCardInfo(cursorInfo);
            noteStringSet.add(cardString);
        }
        if (cursor != null) cursor.close();
        return noteStringSet;
    }

    static String stringifyCardInfo(Map<String, String> cursorInfo){
        String delimiter = "\t";
        String returnString = cursorInfo.get("id") + delimiter +
                            cursorInfo.get("mid") + delimiter +
                            cursorInfo.get("word") + delimiter +
                            cursorInfo.get("translation");
        Log.d("cardHandler ::", "card info string is: " + returnString);
        return returnString;
    }

    private int getCount(Cursor cardCursor){
        int count = 0;
        try{
            count = cardCursor.getCount();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        return count;
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
        //Log.d("cardHandler ::", "word at index " + mKanjiField + " is " + word);
        //Log.d("cardHandler ::", "translation at index " + mEnglishField + " is " + translation);
        cardMap.put("id", id);
        cardMap.put("mid", mid);
        cardMap.put("flds", flds);
        return cardMap;
    }

    private void addSetToSharedPrefs(Set<String> set, String preferenceName){
        Log.d(TAG, "Adding String-set " + set);
        Log.d(TAG, "to preference " + preferenceName);
        SharedPreferences.Editor editor = mContext.getSharedPreferences(preferenceName, Context.MODE_PRIVATE).edit();
        editor.putStringSet(preferenceName, set);
        editor.apply();
    }
}
