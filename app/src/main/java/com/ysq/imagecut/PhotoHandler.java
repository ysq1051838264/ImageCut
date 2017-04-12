package com.ysq.imagecut;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import me.nereo.multi_image_selector.MultiImageSelectorActivity;
import rx.functions.Action1;

/**
 * 用以拍照截图或本地图片裁剪
 *
 * @author ysq
 */
public class PhotoHandler {
    public static final int REQUEST_CODE_IMAGE = 0;
    public static final int REQUEST_CODE_CAMERA = 1;
    public static final int REQUEST_CODE_CUT = 2;

    public static final int AVATAR_SIZE = 120;

    private static final String[] SELECT_ITEMS = new String[]{"选择本地图片",
            "手机拍照"};

    // 头像名称

    private Activity activity;

    // 设置裁剪的高度、宽度

    private SelectPhotoTaskAdapter mCurSelectPhotoTaskAdapter;
    private File tempFile;

    private ArrayList<String> mSelectPath;

    public Action1<String> mPhotoZoomTask = new Action1<String>() {
        @Override
        public void call(String s) {
            startPhotoZoom(Uri.fromFile(new File(s)), Uri.fromFile(tempFile));
        }
    };

    public Runnable mCallbackTask = new Runnable() {
        @Override
        public void run() {

            if (mCurSelectPhotoTaskAdapter.getMaxCount() == 1) {
                mCurSelectPhotoTaskAdapter.prepareTargetFile(tempFile);
                mCurSelectPhotoTaskAdapter.onFinish();
            }
            mCurSelectPhotoTaskAdapter = null;
        }
    };

    public PhotoHandler(Activity activity) {
        this.activity = activity;
    }

    public void beginSelectPhoto(final SelectPhotoTaskAdapter selectPhotoTaskAdapter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("选择模式")
                .setItems(SELECT_ITEMS, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mCurSelectPhotoTaskAdapter = selectPhotoTaskAdapter;
                        if (SELECT_ITEMS[which].equals("选择本地图片")) {
                            doPickLocalPicture();
                        } else {
                            doTakePhoto();
                        }
                    }
                })
                .create().show();

//        new ListDialog.Builder().texts(SELECT_ITEMS).onItemClickListener(new OnItemClickListener<String>() {
//            @Override
//            public void onItemClick(int index, String s) {
//                mCurSelectPhotoTaskAdapter = selectPhotoTaskAdapter;
//                if (s.equals("选择本地图片")) {
//                    doPickLocalPicture();
//                } else {
//                    doTakePhoto();
//                }
//            }
//        }).setContext(activity).build().show();

    }

    private void doPickLocalPicture() {
        Intent intent = new Intent(activity, MultiImageSelectorActivity.class);
        // 是否显示拍摄图片
        intent.putExtra(MultiImageSelectorActivity.EXTRA_SHOW_CAMERA, false);
        // 最大可选择图片数量
        intent.putExtra(MultiImageSelectorActivity.EXTRA_SELECT_COUNT, mCurSelectPhotoTaskAdapter.getMaxCount());
        // 选择模式,0为单图，1为多图
        intent.putExtra(MultiImageSelectorActivity.EXTRA_SELECT_MODE, mCurSelectPhotoTaskAdapter.getMode());
        // 默认选择
        if (mSelectPath != null && mSelectPath.size() > 0) {
            intent.putExtra(MultiImageSelectorActivity.EXTRA_DEFAULT_SELECTED_LIST, mSelectPath);
        }
        activity.startActivityForResult(intent, REQUEST_CODE_IMAGE);
    }

    private void doTakePhoto() {

                    try {
                        Intent intentFromCapture = new Intent(
                                MediaStore.ACTION_IMAGE_CAPTURE);
                        tempFile = new File(generateTempImagePath(activity));
                        intentFromCapture.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(tempFile));
                        activity.startActivityForResult(intentFromCapture,
                                REQUEST_CODE_CAMERA);
                    } catch (Exception e) {
                        Toast.makeText(activity, "没有权限，打开相机应用失败",Toast.LENGTH_SHORT).show();
                    }

    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == 0 || mCurSelectPhotoTaskAdapter == null) {
            release();
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_IMAGE:
                if (!mCurSelectPhotoTaskAdapter.isMultiMode()) {
                    mSelectPath = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);
                    String realpath = mSelectPath.get(0);
                    tempFile = new File(generateTempImagePath(activity));
                    if (mCurSelectPhotoTaskAdapter.isNeedCut()) {
                        mPhotoZoomTask.call(realpath);
                    } else {
                        doCompress(realpath, tempFile.getAbsolutePath(), mCallbackTask);
                    }
                } else {
                    //如果设置了最大数量超过 1 张，则特殊处理ArrayList<String> mSelectPath
                    ArrayList<String> srcPaths = data.getStringArrayListExtra(MultiImageSelectorActivity.EXTRA_RESULT);
                    mCurSelectPhotoTaskAdapter.filenames = new ArrayList<>(srcPaths.size());
                    for (int i = 0; i < srcPaths.size(); i++) {
                        Collections.addAll(mCurSelectPhotoTaskAdapter.filenames, srcPaths.get(i));
                    }
                    mCurSelectPhotoTaskAdapter.onFinish();
                    release();
                }
                break;

            case REQUEST_CODE_CAMERA:
                if (!mCurSelectPhotoTaskAdapter.isMultiMode()) {
                    if (tempFile == null) {
                        Toast.makeText(activity, "获取图像失败",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (mCurSelectPhotoTaskAdapter.isNeedCut()) {
                        String srcPath = tempFile.getAbsolutePath();
                        tempFile = new File(generateTempImagePath(activity));
                        mPhotoZoomTask.call(srcPath);
                    } else {
                        doCompress(mCallbackTask);
                    }
                } else {
                    mCurSelectPhotoTaskAdapter.filenames = new ArrayList<>(1);
                    mCurSelectPhotoTaskAdapter.filenames.add(tempFile.getAbsolutePath());
                    mCurSelectPhotoTaskAdapter.onFinish();
                    tempFile = null;
                    release();
                }
                break;
            case REQUEST_CODE_CUT:
                try {
                    if (mCurSelectPhotoTaskAdapter.isNeedRound()) {
                        Bitmap photo = BitmapFactory.decodeStream(new FileInputStream(tempFile));
                        try {
                            photo.compress(Bitmap.CompressFormat.PNG, mCurSelectPhotoTaskAdapter.getCompressQuality(), new FileOutputStream(tempFile));
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                    mCurSelectPhotoTaskAdapter.prepareTargetFile(tempFile);
                    mCurSelectPhotoTaskAdapter.onFinish();
                    release();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
        }

    }


    /**
     * 裁剪图片方法实现
     *
     * @param srcUri
     */
    void startPhotoZoom(Uri srcUri, Uri destUri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(srcUri, "image/*");
        intent.putExtra("crop", "true"); // 设置裁剪

        intent.putExtra("aspectX", mCurSelectPhotoTaskAdapter.getTargetWidthProportion()); // 宽的比例
        intent.putExtra("aspectY", mCurSelectPhotoTaskAdapter.getTargetHeightProportion()); // 高的比例
        intent.putExtra("outputX", mCurSelectPhotoTaskAdapter.getTargetWidth());
        intent.putExtra("outputY", mCurSelectPhotoTaskAdapter.getTargetHeight());

        intent.putExtra("return-initChartType", false);
        intent.putExtra("orientation", "portrait");
        intent.putExtra("noFaceDetection", true);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, destUri);
        try {
            activity.startActivityForResult(intent, REQUEST_CODE_CUT);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    void doCompress(String path, Runnable task) {
        doCompress(path, path, task);
    }

    void doCompress(String src, String dest, Runnable task) {
        if (mCurSelectPhotoTaskAdapter.isNeedCompress()) {
            new CompressTask(src, dest, task).execute();
        } else if (task != null) {
            if (!src.equals(dest)) {
                FileUtils.copyFile(new File(src), new File(dest));
            }
            task.run();
        }
    }

    void doCompress(Runnable task) {
        doCompress(tempFile.getAbsolutePath(), task);
    }

    public static String generateTempImagePath(Context context) {
        File dirPath = new File(FileUtils.getDiskCacheDir(context) + "/yolanda/images");
        if (!dirPath.exists()) {
            if (!dirPath.mkdirs()) {
//                LogUtils.error("创建图片缓存目录失败");
            }
        }
        return dirPath + "/" + UUID.randomUUID().toString() + ".png";
    }

    class CompressTask extends AsyncTask {

        public CompressTask(String src, String dest, Runnable uiAction) {
            this.src = src;
            this.dest = dest;
            this.uiAction = uiAction;
        }

        Runnable uiAction;
        String src;
        String dest;
//        WebProgress webProgress;

        @Override
        protected void onPreExecute() {
            if (uiAction != null) {
//                webProgress = WebProgress.createDialog(activity).setCancelAction(new Runnable() {
//                    @Override
//                    public void run() {
//                        cancel(false);
//                    }
//                });
//                webProgress.setLoadingStr("正在处理...");
            }
        }

        @Override
        protected Object doInBackground(Object[] params) {
            CompressOptions compressOptions = new CompressOptions();
            compressOptions.destFile = dest;
            compressOptions.filePath = src;
            compressFromUri(compressOptions);
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {

            if (uiAction != null) {
//                webProgress.dismiss();
                uiAction.run();
            }
        }
    }

    public void release() {

        if (mCurSelectPhotoTaskAdapter != null) {
            if (tempFile != null && mCurSelectPhotoTaskAdapter.isCamera()) {
                tempFile.delete();
            }
        }
    }

    public static int findBestSampleSize(int actualWidth, int actualHeight,
                                         int desiredWidth, int desiredHeight) {
        float wr = (float) actualWidth / desiredWidth;
        float hr = (float) actualHeight / desiredHeight;
        float ratio = Math.min(wr, hr);
        float n = 1.0f;
        while ((n * 2) <= ratio) {
            n *= 2;
        }

        return (int) n;
    }

    public static int getResizedDimension(int maxPrimary, int maxSecondary,
                                          int actualPrimary, int actualSecondary) {
        // If no dominant value at all, just return the actual.
        if (maxPrimary == 0 && maxSecondary == 0) {
            return actualPrimary;
        }

        // If primary is unspecified, scale primary to match secondary's scaling
        // ratio.
        if (maxPrimary == 0) {
            float ratio = (float) maxSecondary / (float) actualSecondary;
            return (int) (actualPrimary * ratio);
        }

        if (maxSecondary == 0) {
            return maxPrimary;
        }

        float ratio = (float) actualSecondary / (float) actualPrimary;
        int resized = maxPrimary;
        if (resized * ratio > maxSecondary) {
            resized = (int) (maxSecondary / ratio);
        }
        return resized;
    }

    public static final String CONTENT = "content";
    public static final String FILE = "file";


    /**
     * 图片压缩参数
     *
     * @author Administrator
     */
    public static class CompressOptions {
        public static final int DEFAULT_WIDTH = 1080;
        public static final int DEFAULT_HEIGHT = 1920;

        public int maxWidth = DEFAULT_WIDTH;
        public int maxHeight = DEFAULT_HEIGHT;
        /**
         * 压缩后图片保存的文件
         */
        public String destFile;
        /**
         * 图片压缩格式,默认为jpg格式
         */
        public Bitmap.CompressFormat imgFormat = Bitmap.CompressFormat.PNG;

        /**
         * 图片压缩比例 默认为30
         */
        public int quality = 30;

        public String filePath;
    }

    public static void commonCompress(String srcFile, String destFile) {
        CompressOptions compressOptions = new CompressOptions();
        compressOptions.destFile = destFile;
        compressOptions.filePath = srcFile;
        compressFromUri(compressOptions);
    }

    public static void compressFromUri(CompressOptions compressOptions) {

        // uri指向的文件路径
        String filePath = compressOptions.filePath;
        if (null == filePath) {
            return;
        }
        //获取原始图片的宽高
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(filePath, options);

        int actualWidth = options.outWidth;
        int actualHeight = options.outHeight;

        //计算出适当的图片大小
        int desiredWidth = getResizedDimension(compressOptions.maxWidth,
                compressOptions.maxHeight, actualWidth, actualHeight);
        int desiredHeight = getResizedDimension(compressOptions.maxHeight,
                compressOptions.maxWidth, actualHeight, actualWidth);

        options.inJustDecodeBounds = false;
        options.inSampleSize = findBestSampleSize(actualWidth, actualHeight,
                desiredWidth, desiredHeight);

        Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);

//        Log.d("hdr","原本:"+actualWidth+","+actualHeight+" 压缩后:"+destBitmap.getWidth()+","+destBitmap.getHeight()+" 目的:"+desiredWidth+","+desiredHeight);
        // compress file if need
        if (null != compressOptions.destFile) {
            compressFile(bitmap, compressOptions.imgFormat, 100, compressOptions.destFile);
        }
        bitmap.recycle();
    }

    /**
     * compress file from bitmap with compressOptions
     */
    public static void compressFile(Bitmap bitmap, Bitmap.CompressFormat format, int quality, String targetFile) {
        OutputStream stream = null;
        try {
            stream = new FileOutputStream(targetFile);
            bitmap.compress(format, quality, stream);
        } catch (FileNotFoundException e) {
            Log.e("ImageCompress", e.getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

}
