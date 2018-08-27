package com.fuyun.accessibility;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText mKeyword,mContent;
    private Button mConfirm;
    private LinearLayout mDeviceAdmin;
    private ComponentName componentName;
    private DevicePolicyManager policyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initAction();
    }

    private void initView() {
        componentName = new ComponentName(this, LockReceiver.class);
        mKeyword = findViewById(R.id.keyword);
        mKeyword.setText(RobotService.mNotifyContent);
        mContent = findViewById(R.id.content);
        mContent.setText(RobotService.mSendMsg);
        mConfirm = findViewById(R.id.confirm);
        mDeviceAdmin = findViewById(R.id.device_admin);
        //获取设备管理服务
        policyManager = (DevicePolicyManager) getSystemService(Context
                .DEVICE_POLICY_SERVICE);
        if(policyManager != null && policyManager.isAdminActive(componentName)) {
            mDeviceAdmin.setVisibility(View.GONE);
        } else{
            mDeviceAdmin.setVisibility(View.VISIBLE);
            findViewById(R.id.active).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activeManager();
                }
            });
        }
    }

    private void activeManager() {
        //使用隐式意图调用系统方法激活指定的设备管理器
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,"锁屏");
        startActivityForResult(intent,1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1){
            if(policyManager.isAdminActive(componentName)) {
                mDeviceAdmin.setVisibility(View.GONE);
            } else{
                activeManager();
            }
        }
    }

    private void initAction() {
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mKeyword.getText() == null ||
                        mKeyword.getText().toString().equals("") ||
                        mContent.getText() == null ||
                        mContent.getText().toString().equals("")){
                    Toast.makeText(MainActivity.this,"内容不能为空",Toast.LENGTH_LONG).show();
                }else{
                    RobotService.mNotifyContent = mKeyword.getText().toString();
                    RobotService.mSendMsg = mContent.getText().toString();
//                    mKeyword.setHint(RobotService.mNotifyContent);
//                    mContent.setHint(RobotService.mSendMsg);
//                    mKeyword.setText("");
//                    mContent.setText("");
                    Toast.makeText(MainActivity.this,"设置成功",Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
