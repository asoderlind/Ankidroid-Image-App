package anki.image.app;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.EditText;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link DefinitionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DefinitionFragment extends Fragment {

    private EditText mEditText;

    public static DefinitionFragment newInstance(String definition, Boolean isHtml) {
        DefinitionFragment fragment = new DefinitionFragment();
        Bundle args = new Bundle();
        args.putString("definition", definition);
        args.putBoolean("isHtml", isHtml);
        fragment.setArguments(args);
        return fragment;
    }

    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String definition = getArguments().getString("definition");
        definition = (definition == null) ? "Definition does not exist" : definition;
        boolean isHtml = getArguments().getBoolean("isHtml");

        mEditText = getView().findViewById(R.id.editText);
        WebView webView = getView().findViewById(R.id.webView);

        if (isHtml) {
            // Anki centers text by default, which is not optimal for large HTML
            // chunks such as those we get from NE.
            definition = "<div style=\"text-align: left\">" + definition + "</div>";

            mEditText.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
        } else {
            mEditText.setVisibility(View.VISIBLE);
            webView.setVisibility(View.GONE);
        }

        mEditText.setText(definition);
        webView.loadDataWithBaseURL("http://fake", definition, "text/html", "UTF-8", null);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_definition, container, false);
    }

    public String getText(){
        return mEditText.getText().toString();
    }
}