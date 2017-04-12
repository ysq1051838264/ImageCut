package com.ysq.imagecut;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public abstract class SelectPhotoTaskAdapter {
    //目标 ImageView
    ImageView targetIv;

    //是否需要裁剪
    protected boolean needCut = false;

    protected boolean needRound = false;

    //最大图片张数，如果最大数量超过1张，则忽略压缩
    protected int maxCount = 1;

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    //是单图模式还是多图模式，单图为0，多图为1
    protected int mode = 0;

    //是否需要压缩
    protected boolean needCompress = true;

    protected int compressQuality = 100;

    //希望的宽高
    protected int targetWidth = 1600;
    protected int targetHeight = 1600;

    //裁剪时的宽高比例
    protected int targetWidthProportion = 1;
    protected int targetHeightProportion = 1;

    //多图模式
    boolean multiMode;

    boolean isCamera;

    public File targetFile;
    public List<String> filenames;
    private Context mContext;
    protected ImageUploader imageUploader;

    public void prepareTargetFile(File tempFile) {
        if (tempFile.getAbsolutePath().startsWith(FileUtils.getDiskCacheDir(mContext))) {
            targetFile = tempFile;
        } else {
            targetFile = new File(generateImagePath(mContext));
            FileUtils.copyFile(tempFile, targetFile);
        }
    }

    public void setTargetBitmap(Bitmap bitmap) {
        targetFile = new File(generateImagePath(mContext));
        FileOutputStream stream = null;
        try {

            stream = new FileOutputStream(targetFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        } catch (FileNotFoundException e) {
            Log.e("ImageCompress", e.getMessage());
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public SelectPhotoTaskAdapter(Context context) {
        this.mContext = context;
    }

    public SelectPhotoTaskAdapter(Context context, ImageUploader imageUploader) {
        this.mContext = context;
        this.imageUploader = imageUploader;
    }

    public ImageView getTargetIv() {
        return targetIv;
    }

    public void setTargetIv(ImageView targetIv) {
        this.targetIv = targetIv;
    }

    public boolean isNeedCut() {
        return needCut;
    }

    public void setNeedCut(boolean needCut) {
        this.needCut = needCut;
    }

    public int getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    public boolean isNeedCompress() {
        return false;//先改成全部都不压缩
    }

    public void setNeedCompress(boolean needCompress) {
        this.needCompress = needCompress;
    }

    public int getTargetWidth() {
        return targetWidth;
    }

    public void setTargetWidth(int targetWidth) {
        this.targetWidth = targetWidth;
    }

    public int getTargetHeight() {
        return targetHeight;
    }

    public void setTargetHeight(int targetHeight) {
        this.targetHeight = targetHeight;
    }

    public int getTargetWidthProportion() {
        return targetWidthProportion;
    }

    public void setTargetWidthProportion(int targetWidthProportion) {
        this.targetWidthProportion = targetWidthProportion;
    }

    public int getTargetHeightProportion() {
        return targetHeightProportion;
    }

    public void setTargetHeightProportion(int targetHeightProportion) {
        this.targetHeightProportion = targetHeightProportion;
    }

    public boolean isNeedRound() {
        return needRound;
    }

    public void setNeedRound(boolean needRound) {
        this.needRound = needRound;
    }

    public int getCompressQuality() {
        return compressQuality;
    }

    public void setCompressQuality(int compressQuality) {
        this.compressQuality = compressQuality;
    }

    public boolean isMultiMode() {
        return multiMode;
    }

    public void setMultiMode(boolean multiMode) {
        this.multiMode = multiMode;
    }

    public boolean isCamera() {
        return isCamera;
    }

    public void setIsCamera(boolean isCamera) {
        this.isCamera = isCamera;
    }

    public void deleteImage() {

        if (targetFile != null && targetFile.exists()) {
            if (!targetFile.delete()) {
                Log.d("photo", "删除文件失败");
            }
        }
    }

    public String getTargetFilename() {
        return targetFile == null ? "" : targetFile.getAbsolutePath();
    }

    public void onFinish() {
        if (this.targetIv != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(targetFile.getAbsolutePath());
            this.targetIv.setImageBitmap(bitmap);
        }
        if (imageUploader != null) {
            //上传文件
//            imageUploader.uploadImage(targetFile);
        }
    }

    //选取头像是使用的类
    public static class AvatarSelectTaskAdapter extends SelectPhotoTaskAdapter {
        public AvatarSelectTaskAdapter(Context context, ImageView targetIv) {
            this(context, targetIv, null);
        }

        public AvatarSelectTaskAdapter(Context context, ImageView targetIv, ImageUploader imageUploader) {
            super(context);
            this.needCut = true;
            this.needRound = true;
            this.needCompress = false;
            this.targetWidth = 400;
            this.targetHeight = 400;
            this.targetIv = targetIv;
            this.imageUploader = imageUploader;
        }
    }

    //普通的选取普通，不会剪裁
    public static class CommonSelectPhotoTaskAdapter extends SelectPhotoTaskAdapter {

        public CommonSelectPhotoTaskAdapter(Context context) {
            super(context);
            this.needCut = false;
        }

        public CommonSelectPhotoTaskAdapter(Context context, ImageView targetIv) {
            this(context);
            this.targetIv = targetIv;
        }
    }

    //普通的剪裁任务类
    public static class ZoomSelectPhotoTaskAdapter extends SelectPhotoTaskAdapter {
        public ZoomSelectPhotoTaskAdapter(Context context, ImageView targetIv, int targetWidth, int targetHeight, int targetWidthProportion, int targetHeightProportion) {
            this(context, targetIv, targetWidth, targetHeight, targetWidthProportion, targetHeightProportion, null);
        }

        public ZoomSelectPhotoTaskAdapter(Context context, ImageView targetIv, int targetWidth, int targetHeight, int targetWidthProportion, int targetHeightProportion, ImageUploader imageUploader) {
            super(context);
            this.needCut = true;
            this.targetIv = targetIv;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
            this.targetWidthProportion = targetWidthProportion;
            this.targetHeightProportion = targetHeightProportion;
            this.imageUploader = imageUploader;
        }

        public ZoomSelectPhotoTaskAdapter(Context context, int targetWidth, int targetHeight, int targetWidthProportion, int targetHeightProportion) {
            super(context);
            this.needCut = true;
            this.targetWidth = targetWidth;
            this.targetHeight = targetHeight;
            this.targetWidthProportion = targetWidthProportion;
            this.targetHeightProportion = targetHeightProportion;
        }
    }


    public static String generateImagePath(Context context) {
        File dirPath = new File(FileUtils.getDiskCacheDir(context) + "/images/");
        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }
        return dirPath + "/" + UUID.randomUUID().toString() + ".png";
    }


}
