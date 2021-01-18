package anki.image.app;

import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
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

    private WebView mWebView;
    private ArrayList<String> mSelected = new ArrayList<>();
    private String mImagePickerJs;

    public static ImageFragment newInstance(String word) {
        ImageFragment fragment = new ImageFragment();

        Bundle args = new Bundle();
        args.putString("word", word);
        fragment.setArguments(args);

        return fragment;
    }

    private String getWord() {
        Log.d("imageFrag :", "the word passed is: " + getArguments().getString("word"));
        return getArguments().getString("word");
    }

    private class WcmJsObject {
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void pushSelected(final String json) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONArray array;
                    try {
                        array = new JSONArray(json);

                        mSelected.clear();

                        for (int i = 0; i < array.length(); i++) {
                            Log.d("imageFrag :", "Added: " + array.getString(i));
                            mSelected.add(array.getString(i));

                            // Writing the base64 string to internal storage as image
                            //writeImageToStorage(array.getString(i));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @SuppressWarnings("unused")
        @JavascriptInterface
        public void pushPickerHtml(final String html) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mWebView.loadDataWithBaseURL(GSTATIC_SERVER,
                            html + "<script>" + getImagePickerJs() + "</script>",
                            "text/html", "UTF-8", null);
                }
            });
        }
    }

    private void writeImageToStorage(String base64ImageData) {
        FileOutputStream fos = null;
        try {
            if (base64ImageData != null) {
                byte[] decodedString = android.util.Base64.decode(base64ImageData, android.util.Base64.DEFAULT);

                File f = new File(Environment.getExternalStorageDirectory() + File.separator + "test.png");
                f.mkdirs();
                f.createNewFile();

                fos = new FileOutputStream(f);
                fos.write(decodedString);
                fos.flush();
                fos.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                fos = null;
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWebView = getView().findViewById(R.id.webView);
        mWebView.setWebChromeClient(new WebChromeClient());
        final ProgressBar progress = getView().findViewById(R.id.image_progress);
        final WebSettings settings = mWebView.getSettings();

        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setJavaScriptEnabled(true);

        // We replace the src urls in imagepicker.js::init(), so don't load
        // images twice.

        settings.setBlockNetworkImage(true);

        mWebView.setInitialScale(100);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (Objects.equals(url, GSTATIC_SERVER)) {
                    Log.d("imageFrag :", "Making visible...");
                    settings.setBlockNetworkImage(false);
                    progress.setVisibility(View.GONE);
                    mWebView.setVisibility(View.VISIBLE);
                } else {
                    Log.d("imageFrag :", "loading javascript url");
                    view.loadUrl("javascript:" + getImagePickerJs() + "getPickerHtml();");
                }
            }
        });

        mWebView.addJavascriptInterface(new WcmJsObject(), "wcm");
        //mWebView.loadUrl("https://www.google.co.in/search?q="  + getWord() + "&source=lnms&tbm=isch");
        mWebView.loadUrl("https://www.google.se/search?tbm=isch&q=" + getWord());
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
            Log.d("imageFrag :", "exception: " + e);
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