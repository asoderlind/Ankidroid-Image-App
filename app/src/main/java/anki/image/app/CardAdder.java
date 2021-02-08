package anki.image.app;

import android.content.Context;
import android.widget.Toast;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CardAdder {
    private AnkiDroidHelper mAnkiDroid;
    private Context mContext;
    private final String FAIL_STRING = "Failed to add card";
    private final String FAIL_API = "Api failed to connect";
    private final String SUCCESS_STRING = "Added card";
    private List<Map<String, String>> mCardData;

    public CardAdder(Context context){
        mContext = context;
        mAnkiDroid = new AnkiDroidHelper(context);
        initExampleData();
    }

    private void initExampleData(){
        mCardData = AnkiDroidConfig.getExampleData();
    }

    public void addCardsToAnkiDroid() {
        Long deckId = getDeckId();
        Long modelId = getModelId();
        if ((deckId == null) || (modelId == null)) {
            // we had an API error, report failure and return
            Toast.makeText(mContext, FAIL_API, Toast.LENGTH_LONG).show();
            return;
        }
        String[] fieldNames = mAnkiDroid.getApi().getFieldList(modelId);
        if (fieldNames == null) {
            // we had an API error, report failure and return
            Toast.makeText(mContext, FAIL_API, Toast.LENGTH_LONG).show();
            return;
        }
        // Build list of fields and tags
        LinkedList<String []> fields = new LinkedList<>();
        LinkedList<Set<String>> tags = new LinkedList<>();
        for (Map<String, String> fieldMap: mCardData) {
            // Build a field map accounting for the fact that the user could have changed the fields in the model
            String[] flds = new String[fieldNames.length];
            for (int i = 0; i < flds.length; i++) {
                // Fill up the fields one-by-one until either all fields are filled or we run out of fields to send
                if (i < AnkiDroidConfig.FIELDS.length) {
                    flds[i] = fieldMap.get(AnkiDroidConfig.FIELDS[i]);
                }
            }
            tags.add(AnkiDroidConfig.TAGS);
            fields.add(flds);
        }
        // Remove any duplicates from the LinkedLists and then add over the API
        mAnkiDroid.removeDuplicates(fields, tags, modelId);
        int added = mAnkiDroid.getApi().addNotes(modelId, deckId, fields, tags);
        if (added != 0) {
            Toast.makeText(mContext, SUCCESS_STRING, Toast.LENGTH_LONG).show();
        } else {
            // API indicates that a 0 return value is an error
            Toast.makeText(mContext, FAIL_STRING, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * get the deck id
     * @return might be null if there was a problem
     */
    private Long getDeckId() {
        Long did = mAnkiDroid.findDeckIdByName(AnkiDroidConfig.DECK_NAME);
        if (did == null) {
            did = mAnkiDroid.getApi().addNewDeck(AnkiDroidConfig.DECK_NAME);
            mAnkiDroid.storeDeckReference(AnkiDroidConfig.DECK_NAME, did);
        }
        return did;
    }

    /**
     * get model id
     * @return might be null if there was an error
     */
    private Long getModelId() {
        Long mid = mAnkiDroid.findModelIdByName(AnkiDroidConfig.MODEL_NAME, AnkiDroidConfig.FIELDS.length);
        if (mid == null) {
            mid = mAnkiDroid.getApi().addNewCustomModel(AnkiDroidConfig.MODEL_NAME, AnkiDroidConfig.FIELDS,
                    AnkiDroidConfig.CARD_NAMES, AnkiDroidConfig.QFMT, AnkiDroidConfig.AFMT, AnkiDroidConfig.CSS, getDeckId(), null);
            mAnkiDroid.storeModelReference(AnkiDroidConfig.MODEL_NAME, mid);
        }
        return mid;
    }


}
