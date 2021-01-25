package anki.image.app;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageFragment extends Fragment {

    static final String GSTATIC_SERVER = "https://encrypted-tbn0.gstatic.com/";
    static final String GOOGLE_DEFAULT = "https://www.google.co.in/";
    static final String TAG = "ImageFragment : ";
    private WebView mWebView;
    private final ArrayList<String> mSelected = new ArrayList<>();
    private String mImagePickerJs;
    private String mTargetUrl;

    public static ImageFragment newInstance(String word, String appendix) {
        ImageFragment fragment = new ImageFragment();

        Bundle args = new Bundle();
        args.putString("word", word);
        args.putString("appendix", appendix);
        fragment.setArguments(args);

        return fragment;
    }

    private String getAppendix() {
        Log.d(TAG, "the appendix passed is: " + getArguments().getString("appendix"));
        return getArguments().getString("appendix");
    }

    private String getWord() {
        Log.d(TAG, "the word passed is: " + getArguments().getString("word"));
        return getArguments().getString("word");
    }

    /**
     * The javascript object that interfaces with
     * the script that is injected into the view.
     * Can be called from inside the js script.
     */
    private class WcmJsObject {
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void pushSelected(final String json) {
            getActivity().runOnUiThread(() -> {
                JSONArray array;
                try {
                    array = new JSONArray(json);
                    mSelected.clear(); // remove all selections

                    // Iterate over selected images and add
                    for (int i = 0; i < array.length(); i++) {
                        //Log.d("imageFrag :", "Added: " + array.getString(i));
                        mSelected.add(array.getString(i));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void pushPickerHtml(final String html) {
            getActivity().runOnUiThread(() -> mWebView.loadDataWithBaseURL(GSTATIC_SERVER,
                    html + "<script>" + getImagePickerJs() + "</script>",
                    "text/html", "UTF-8", null));
        }
    }



    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWebView = getView().findViewById(R.id.webView);
        mWebView.setWebViewClient(new WebViewClient());
        final ProgressBar progress = getView().findViewById(R.id.image_progress);
        final WebSettings settings = mWebView.getSettings();

        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptEnabled(true);
        settings.setBlockNetworkImage(true); // We replace the src urls in imagepicker.js::init(), so don't load images twice.

        mWebView.setInitialScale(100);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (Objects.equals(url, GSTATIC_SERVER)) {
                    Log.d(TAG, "URL: " + url);
                    Log.d(TAG, "Setting visibility");
                    settings.setBlockNetworkImage(false);
                    progress.setVisibility(View.GONE);
                    mWebView.setVisibility(View.VISIBLE);
                } else if (Objects.equals(url, GOOGLE_DEFAULT)){
                    Log.d(TAG, "URL: " + url);
                    Log.d(TAG, "loading javascript url");
                    view.loadUrl("javascript:" + getImagePickerJs() + "getPickerHtml();");
                } else {
                    Log.d(TAG, "URL: " + url);
                    Log.d(TAG, "Fully loaded url detected");
                }
            }
        });

        mWebView.addJavascriptInterface(new WcmJsObject(), "wcm");
        mTargetUrl = "https://www.google.co.in/search?q="  + getWord() + getAppendix() + "&source=lnms&tbm=isch";
        mWebView.loadUrl(mTargetUrl);
    }

    /* Reads js file and returns the contained code */
    private String getImagePickerJs() {
        if (mImagePickerJs != null) {
            return mImagePickerJs;
        }

        try {
            AssetManager assetManager = getActivity().getAssets();
            InputStream is = assetManager.open("imagepicker.js");
            mImagePickerJs = Utils.inputStreamToString(is);
        } catch (java.io.IOException e) {
            Log.d(TAG, "exception: " + e);
            mImagePickerJs = "document.body.innerHtml='Error';";
        }

        return mImagePickerJs;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image, container, false);
    }

    public ArrayList<String> getSelected() {
        return mSelected;
    }
}