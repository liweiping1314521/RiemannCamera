package com.riemann.camera.ui;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.util.Log;

import com.riemann.camera.CameraPhotoRS;

import java.io.File;
import java.io.FileOutputStream;

public class Storage {
    private static final String TAG = "CameraStorage";

    public static final String DCIM =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString();
    //public static final String SNAP_DATA_PATH = "/data/data" + File.separator + "com.riemann.camera" + File.separator + "snap.jpeg";

    public static final String DIRECTORY = DCIM + "/Camera";

    // Match the code in MediaProvider.computeBucketValues().
    public static final String BUCKET_ID =
            String.valueOf(DIRECTORY.toLowerCase().hashCode());

    public static final long UNAVAILABLE = -1L;
    public static final long PREPARING = -2L;
    public static final long UNKNOWN_SIZE = -3L;
    public static final long LOW_STORAGE_THRESHOLD= 50000000;
    public static final long PICTURE_SIZE = 1500000;

    private static final int BUFSIZE = 4096;

    public static Uri addImage(CameraPhotoRS cameraPhotoRS, ContentResolver resolver, String title, long date, int orientation, byte[] jpeg, int width, int height, int datalength, int filterIndex) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        // Decode picture taken by camera.
        Bitmap bitmap = BitmapFactory.decodeByteArray(
                jpeg, 0,
                datalength, options);
        cameraPhotoRS.applyFilter(bitmap, filterIndex);

        // Save the image.
        String path = generateFilepath(title);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
            bitmap.recycle();
            //out.write(jpeg);
        } catch (Exception e) {
            Log.e(TAG, "Failed to write image", e);
            return null;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Insert into MediaStore.
        ContentValues values = new ContentValues(9);
        values.put(MediaStore.Images.ImageColumns.TITLE, title);
        values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, title + ".jpg");
        values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, date);
        values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.ImageColumns.ORIENTATION, orientation);
        values.put(MediaStore.Images.ImageColumns.DATA, path);
        values.put(MediaStore.Images.ImageColumns.SIZE, jpeg.length);
        values.put(MediaStore.Images.ImageColumns.WIDTH, width);
        values.put(MediaStore.Images.ImageColumns.HEIGHT, height);

        Uri uri = null;
        try {
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Throwable th)  {
            // This can happen when the external volume is already mounted, but
            // MediaScanner has not notify MediaProvider to add that volume.
            // The picture is still safe and MediaScanner will find it and
            // insert it into MediaProvider. The only problem is that the user
            // cannot click the thumbnail to review the picture.
            Log.e(TAG, "Failed to write MediaStore" + th);
        }
        return uri;
    }

    public static String generateFilepath(String title) {
        return DIRECTORY + '/' + title + ".jpg";
    }

    public static long getAvailableSpace() {
        String state = Environment.getExternalStorageState();
        Log.d(TAG, "External storage state=" + state);
        if (Environment.MEDIA_CHECKING.equals(state)) {
            return PREPARING;
        }
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return UNAVAILABLE;
        }

        File dir = new File(DIRECTORY);
        dir.mkdirs();
        if (!dir.isDirectory() || !dir.canWrite()) {
            return UNAVAILABLE;
        }

        try {
            StatFs stat = new StatFs(DIRECTORY);
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
        }
        return UNKNOWN_SIZE;
    }

    /**
     * OSX requires plugged-in USB storage to have path /DCIM/NNNAAAAA to be
     * imported. This is a temporary fix for bug#1655552.
     */
    public static void ensureOSXCompatible() {
        File nnnAAAAA = new File(DCIM, "100ANDRO");
        if (!(nnnAAAAA.exists() || nnnAAAAA.mkdirs())) {
            Log.e(TAG, "Failed to create " + nnnAAAAA.getPath());
        }
    }
}
