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
            cardMap.put("id", getId(splitString));
            cardMap.put("mid", getMid(splitString));
            cardMap.put("word", getWord(splitString));
            cardMap.put("translation", getTranslation(splitString));
        }
        Log.d(TAG, "returning " + cardMap);
        return cardMap;
    }

    private String getId(String[] infoArray){
        try{
            return infoArray[0];
        } catch (IndexOutOfBoundsException e){
            return "";
        }
    }

    private String getMid(String[] infoArray){
        try{
            return infoArray[1];
        } catch (IndexOutOfBoundsException e){
            return "";
        }
    }

    private String getWord(String[] infoArray){
        try{
            return infoArray[2];
        } catch (IndexOutOfBoundsException e){
            return "";
        }
    }

    private String getTranslation(String[] infoArray){
        try{
            return infoArray[3];
        } catch (IndexOutOfBoundsException e){
            return "";
        }
    }

    private Set<String> getCardIdSet(String prefKey){
        SharedPreferences sharedPrefs = mContext.getSharedPreferences(prefKey, Context.MODE_PRIVATE);
        return sharedPrefs.getStringSet(prefKey, null);
    }
}
