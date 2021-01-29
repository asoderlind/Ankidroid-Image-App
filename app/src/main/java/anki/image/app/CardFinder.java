package anki.image.app;

import android.database.Cursor;

public class CardFinder {
    String mDeckName;
    String mModelName;
    String mCardKey;
    Cursor mCursor;

    public CardFinder(String deckName, String modelName, String cardKey){
        mDeckName = deckName;
        mModelName = modelName;
        mCardKey = cardKey;
    }

}
