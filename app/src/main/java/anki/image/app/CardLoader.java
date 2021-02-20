package anki.image.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class CardLoader {
    private final Context mContext;
    private static final String TAG = "CardLoader :";

    public CardLoader(Context context){
        mContext = context;
    }

    public Map<String, String> preloadCard(String prefKey){
        Log.d(TAG, "preloadCard() called");
        Set<String> cardIdSet = getCardIdSet(prefKey);
        if (cardIdSet != null){
            if (cardIdSet.size() != 0){
                for(String cardInfoSet: cardIdSet) {
                    return getCardInfoString(cardInfoSet);
                }
            }
        } else {
            Log.d(TAG, "No preloaded card with tag '" + prefKey + "' found..");
        }
        return null;
    }

    public Map<String, String> getCardInfoString(String cardInfoString){
        Log.d(TAG, "getCardInfoString() called on " + cardInfoString);
        String delimiter = "\t";
        Map<String, String> cardMap = new HashMap<>();
        String[] splitString = cardInfoString.split(delimiter);
        if (splitString.length > 1) {
            cardMap.put("id", splitString[0]);
            cardMap.put("mid", splitString[1]);
            cardMap.put("word", splitString[2]);
            cardMap.put("translation", splitString[3]);
        }
        Log.d(TAG, "returning " + cardMap);
        return cardMap;
    }

    private Set<String> getCardIdSet(String prefKey){
        SharedPreferences sharedPrefs = mContext.getSharedPreferences(prefKey, Context.MODE_PRIVATE);
        return sharedPrefs.getStringSet(prefKey, null);
    }
}
