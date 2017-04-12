package com.ysq.imagecut;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private SelectPhotoTaskAdapter mAvatarSelectPhotoTaskAdapter;
    private PhotoHandler photoHandler;

    ImageView userHead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userHead = (ImageView) findViewById(R.id.user_head);

        mAvatarSelectPhotoTaskAdapter = new SelectPhotoTaskAdapter.AvatarSelectTaskAdapter(this, userHead);
        //如果裁剪长方形的，就是以下这个方法
//        mAvatarSelectPhotoTaskAdapter = new SelectPhotoTaskAdapter.ZoomSelectPhotoTaskAdapter(this, userHead,1080,1080,2,1);
        photoHandler = new PhotoHandler(this);

        userHead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                photoHandler.beginSelectPhoto(mAvatarSelectPhotoTaskAdapter);
            }
        });

        //选择图片之后，可以得到图片路径
        String path = mAvatarSelectPhotoTaskAdapter.getTargetFilename();
        Log.d("ysq 输出", path);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        photoHandler.onActivityResult(requestCode, resultCode, data);
    }

}
