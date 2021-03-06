package com.cvsong.study.rice.activity.start;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.cvsong.study.library.net.download.VersionUpdateManager;
import com.cvsong.study.library.net.entity.HttpCallBack;
import com.cvsong.study.library.net.entity.Result;
import com.cvsong.study.library.util.app_tools.AppSpUtils;
import com.cvsong.study.library.util.permission.AppPermissionEntity;
import com.cvsong.study.library.util.permission.AppPermissionsManager;
import com.cvsong.study.library.util.permission.AppSettingsHolderActivity;
import com.cvsong.study.library.util.permission.IPermissionCallbacks;
import com.cvsong.study.library.util.permission.PermissionRequestCallback;
import com.cvsong.study.library.util.utilcode.util.ActivityUtils;
import com.cvsong.study.library.util.utilcode.util.AppUtils;
import com.cvsong.study.rice.R;
import com.cvsong.study.rice.activity.HomeActivity;
import com.cvsong.study.rice.base.AppBaseActivity;
import com.cvsong.study.rice.entity.TestVersionUpdateEntity;
import com.cvsong.study.rice.manager.http.AppHttpManage;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * 启动页
 * Created by chenweisong on 2018/5/24.
 */
public class LauncherActivity extends AppBaseActivity {


    @BindView(R.id.iv_img)
    ImageView ivImg;
    @BindView(R.id.tv_count_down)
    TextView tvCountDown;

    private AppPermissionEntity[] perms;


    private IPermissionCallbacks permissionCallback = new PermissionRequestCallback() {

        @Override
        public void onAllPermissionsGranted(int requestCode, @NonNull List<AppPermissionEntity> appPermissions) {
            super.onAllPermissionsGranted(requestCode, appPermissions);
            judgeIsNeedUpdate();//判断是否需要更新应用

        }
    };
    private VersionUpdateManager versionUpdateManager;

    private CountDownTimer countDownTimer = new CountDownTimer(1000 * 10, 1000) {

        @Override
        public void onTick(long millisUntilFinished) {
            if (tvCountDown != null) {
                tvCountDown.setText(String.valueOf(millisUntilFinished / 1000));
            }
        }

        @Override
        public void onFinish() {
            judgeIsSkipGuidePage(); //判断是否跳转引导页
        }
    };

    @Override
    public int bindLayout() {
        return R.layout.activity_launcher;
    }

    @Override
    public void initView(Bundle savedInstanceState, View view) {
        titleView.setTitleVisibility(View.GONE);
        ivImg.setOnClickListener(this);
        tvCountDown.setOnClickListener(this);


    }


    @Override
    public void loadData() {
        //App6.0权限申请
        requestAppNeedPermissions();
    }

    /**
     * 判断是否需要更新应用
     */
    private void judgeIsNeedUpdate() {

        //先网络请求获取版本更新信息
        //判断是否需要更新
        //如果需要更新则进行下载,不需要的话进行后面流程-->页面跳转

        AppHttpManage.checkVersionUpdate(this, new HttpCallBack<TestVersionUpdateEntity>() {
            @Override
            public void onSuccess(Result result, TestVersionUpdateEntity entity) {
                super.onSuccess(result, entity);
                checkVersion(entity);//校验版本

            }
        });

    }

    /**
     * 校验版本
     */
    private void checkVersion(TestVersionUpdateEntity entity) {
        int versionCode = entity.getVersionCode();
        String downloadUrl = entity.getDownloadUrl();
        String updateDesc = entity.getUpdateDesc();
        boolean haveToUpdate = entity.isHaveToUpdate();//是否必须更新

        int appVersionCode = AppUtils.getAppVersionCode();
        if (versionCode > appVersionCode && downloadUrl != null) {//需要进行版本更新
            //弹窗提示进行版本更新
            versionUpdateManager = new VersionUpdateManager(activity);
            versionUpdateManager.makeVersionUpdate(versionCode, downloadUrl, updateDesc, haveToUpdate, new VersionUpdateManager.VersionUpdateCallback() {
                @Override
                public void onNextStep() {//下一步
                    judgeIsSkipGuidePage(); //判断是否跳转引导页
                }
            });

        } else {//不需要版本更新
            if (countDownTimer != null) {
                tvCountDown.setVisibility(View.VISIBLE);
                countDownTimer.start();
            } else {
                judgeIsSkipGuidePage(); //判断是否跳转引导页
            }
        }


    }


    /**
     * 请求App所需权限
     */
    public void requestAppNeedPermissions() {
        perms = new AppPermissionEntity[]{new AppPermissionEntity(Manifest.permission.CAMERA, "相机"), new AppPermissionEntity(Manifest.permission.WRITE_EXTERNAL_STORAGE, "读写SD卡")};
        AppPermissionsManager.requestPermissions(this, permissionCallback, perms);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AppPermissionsManager.onRequestPermissionsResult(this, requestCode, perms, grantResults, permissionCallback);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AppSettingsHolderActivity.RC_APP_SETTING) {//跳转设置页面
            requestAppNeedPermissions();
        }
    }


    /**
     * 判断是否跳转引导页
     */
    private void judgeIsSkipGuidePage() {
        ActivityUtils.finishActivity(LauncherActivity.class);//结束当前页面
        //是否再次打开
        boolean isOpenAgain = AppSpUtils.getInstance().getBoolean(AppSpUtils.IS_OPEN_AGAIN);
        //第一次启动应用--->启动引导页面否则跳转主页面
        ActivityUtils.startActivity(isOpenAgain ? HomeActivity.class : StartGuideActivity.class);

    }


    @Override
    protected void onWidgetClick(View view) {
        super.onWidgetClick(view);
        switch (view.getId()) {
            case R.id.iv_img:
                break;

            case R.id.tv_count_down:
                countDownTimer.onFinish();
                break;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (versionUpdateManager != null) {
            versionUpdateManager.close();//注销
        }

    }


}
