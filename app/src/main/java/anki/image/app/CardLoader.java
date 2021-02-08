package anki.image.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CardLoader {
    private final Context mContext;
    private static final String TAG = "CardLoader ::";

    public CardLoader(Context context){
        mContext = context;
    }
    public Map<String, String> loadSavedCard(String prefKey){
        Set<String> cardIdSet = getCardIdSet(prefKey);
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

    private Set<String> getCardIdSet(String prefKey){
        SharedPreferences sharedPrefs = mContext.getSharedPreferences(prefKey, Context.MODE_PRIVATE);
        return sharedPrefs.getStringSet(prefKey, null);
    }
}
