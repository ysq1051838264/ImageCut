package com.ysq.imagecut;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    public static void writerStringToFile(String str, String filename) {
        File file = new File(filename);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(str.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isExternalStorageMounted() {

        boolean canRead = Environment.getExternalStorageDirectory().canRead();
        boolean onlyRead = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED_READ_ONLY);
        boolean unMounted = Environment.getExternalStorageState().equals(
                Environment.MEDIA_UNMOUNTED);

        return !(!canRead || onlyRead || unMounted);
    }


    public static String getDiskCacheDir(Context context) {
        String cachePath;
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            File file = context.getExternalCacheDir();
            if (file != null) {
                cachePath = file.getPath();
            } else {
                cachePath = Environment.getExternalStorageDirectory() + "/Android/data/" + context.getPackageName() + "/cache";
            }
            File dir = new File(cachePath);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    //如果创建目录不成功（魅族3遇到过），就直接在SD卡根目录办事！ -_-
                    cachePath = Environment.getExternalStorageDirectory().getAbsolutePath();
                }
            }
        } else {
            cachePath = context.getCacheDir().getPath();
        }
        return cachePath;
    }

    public static void appendStringToFile(String fileName, String content) {
        try {
            //打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            FileWriter writer = new FileWriter(fileName, true);
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getStringFromFile(String filename) {
        return getStringFromFile(new File(filename));
    }

    public static String getStringFromFile(File file) {
        try {
            if (!file.exists() || !file.canRead())
                return null;
            BufferedReader br = new BufferedReader(new FileReader(file));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    public static String getAppFileDir(Context context) {
        return context.getFilesDir().toString() + "/";
    }

    /**
     * 复制单个文件
     *
     * @return boolean
     */
    public static int copyFile(File src, File dest) {
        try {
            int byteread = 0;
            int length = 0;
            if (src.exists()) { // 文件存在时
                InputStream inStream = new FileInputStream(src); // 读入原文件
                if (!dest.getParentFile().exists()) {
                    dest.getParentFile().mkdirs();
                }
                if (!dest.exists()) {
                    dest.createNewFile();
                }
                FileOutputStream fs = new FileOutputStream(dest);
                byte[] buffer = new byte[1444];
                while ((byteread = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
                fs.close();
            }
            return length;
        } catch (Exception e) {

            System.out.println("复制单个文件操作出错");
            e.printStackTrace();
            return 0;
        }
    }

    public static String getRealPath(Context context, Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

}
