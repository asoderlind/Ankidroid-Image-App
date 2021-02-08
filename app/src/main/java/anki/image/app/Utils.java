package anki.image.app;

import android.content.Context;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

import static android.media.MediaScannerConnection.scanFile;

public class Utils {
    private static final String TAG = "Utils :: ";
    static String inputStreamToString(InputStream input) throws IOException {
        Scanner scanner = new Scanner(input).useDelimiter("\\A");
        String str = scanner.hasNext() ? scanner.next() : "";
        input.close();
        return str;
    }
    /**
     * Saved the file with the given content URI
     * @param url the content-URI url of the image
     * @return the path that the file was saved at
     */
    static String createAndSaveFileFromBase64Url(Context context, File path, String url) {
        Log.d(TAG, "createAndSaveFileFromBase64Url() called");
        //File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); // save in public external dir
        Log.d(TAG, "Path: " + path);
        String filetype = url.substring(url.indexOf("/") + 1, url.indexOf(";"));
        String filename = System.currentTimeMillis() + "." + filetype;
        File file = new File(path, filename);
        try {
            if(!path.exists())
                path.mkdirs();
            if(!file.exists())
                file.createNewFile();

            String base64EncodedString = url.substring(url.indexOf(",") + 1);
            byte[] decodedBytes = Base64.decode(base64EncodedString, Base64.DEFAULT);
            OutputStream os = new FileOutputStream(file);
            os.write(decodedBytes);
            os.close();

            //Tell the media scanner about the new file so that it is immediately available to the user.
            scanFile(context,
                    new String[]{file.toString()}, null,
                    (path1, uri) -> {
                        Log.d("ExternalStorage", "Scanned " + path1 + ":");
                        Log.d("ExternalStorage", "-> uri=" + uri);
                    });

        } catch (IOException e) {
            Log.w("ExternalStorage", "Error writing " + file, e);
            Toast.makeText(context, R.string.error_downloading, Toast.LENGTH_LONG).show();
        }

        return file.toString();
    }
}
