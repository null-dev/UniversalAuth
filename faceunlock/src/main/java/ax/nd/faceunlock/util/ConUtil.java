package ax.nd.faceunlock.util;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ConUtil {
    public static String getRaw(Context context, int i, String str, String str2, boolean z) {
        File dir = new File(context.getDir("faceunlock_data", 0), str);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        File file = new File(dir, str2);
        if (!z && file.exists()) {
            return file.getAbsolutePath();
        }
        byte[] bArr = new byte[1024];
        try (FileOutputStream fileOutputStream = new FileOutputStream(file); InputStream inputStream = context.getResources().openRawResource(i)) {
            while (true) {
                int read = inputStream.read(bArr);
                if (read == -1) {
                    break;
                }
                fileOutputStream.write(bArr, 0, read);
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
