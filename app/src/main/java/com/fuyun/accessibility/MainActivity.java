package com.fuyun.accessibility;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText mOtherContent,mFilterContent;
    private Button mOtherConfirm,mAdd,mFilterConfirm;
    private LinearLayout mDeviceAdmin;
    private ComponentName componentName;
    private DevicePolicyManager policyManager;
    private SwitchCompat mOtherSwitch,mPrimarySwitch,mRobotSwitch;
    private LinearLayout mItemsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initAction();
    }

    private void initView() {
        componentName = new ComponentName(this, LockReceiver.class);
        mOtherContent = findViewById(R.id.other_reply);
        mOtherConfirm = findViewById(R.id.other_confirm);
        mFilterContent = findViewById(R.id.filter_keywords);
        mFilterConfirm = findViewById(R.id.filter_confirm);
        mAdd = findViewById(R.id.add);
        mItemsLayout = findViewById(R.id.items);
        mOtherSwitch = findViewById(R.id.other_switchBtn);
        mPrimarySwitch = findViewById(R.id.primarySwitch);
        mRobotSwitch = findViewById(R.id.robotSwitch);
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
        mPrimarySwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                RobotService.isPrimaryOpen = mPrimarySwitch.isChecked();
                SpUtils.putBoolean(Constants.SP_PRIMARY_SWITCHER,mPrimarySwitch.isChecked());
            }
        });
        mOtherSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                RobotService.isOtherOpen = mOtherSwitch.isChecked();
                SpUtils.putBoolean(Constants.SP_OTHER_ISOPEN,mOtherSwitch.isChecked());
            }
        });
        mRobotSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                RobotService.isRobotOpen = mRobotSwitch.isChecked();
                SpUtils.putBoolean(Constants.SP_ROBOT_ISOPEN,mRobotSwitch.isChecked());
            }
        });
        mOtherConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mOtherContent.getText() == null ||
                        mOtherContent.getText().toString().equals("")){
                    Toast.makeText(MainActivity.this,"内容不能为空",Toast.LENGTH_LONG).show();
                }else{
                    RobotService.mOtherContent = mOtherContent.getText().toString();
                    SpUtils.putString(Constants.SP_OTHER_CONTENT,mOtherContent.getText().toString());
                    Toast.makeText(MainActivity.this,"设置成功",Toast.LENGTH_SHORT).show();
                }
            }
        });
        mFilterConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RobotService.mFilterKeywords = mFilterContent.getText().toString().split(",");
                SpUtils.putString(Constants.SP_FILTER_KEYWORDS,mFilterContent.getText().toString());
                Toast.makeText(MainActivity.this,"设置成功",Toast.LENGTH_SHORT).show();
            }
        });
        mAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,AddActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        initData();
    }

    private void initData() {
        String otherContent = SpUtils.getString(Constants.SP_OTHER_CONTENT,"");
        if(!otherContent.equals("")){
            mOtherContent.setText(otherContent);
        }
        boolean otherIsOpen = SpUtils.getBoolean(Constants.SP_OTHER_ISOPEN,true);
        mOtherSwitch.setChecked(otherIsOpen);
        boolean robotIsOpen = SpUtils.getBoolean(Constants.SP_ROBOT_ISOPEN,false);
        mRobotSwitch.setChecked(robotIsOpen);
        boolean primarySwither = SpUtils.getBoolean(Constants.SP_PRIMARY_SWITCHER,true);
        mPrimarySwitch.setChecked(primarySwither);
        String replyListStr = SpUtils.getString(Constants.SP_REPLY_LIST,"");
        mItemsLayout.removeAllViews();
        if(!replyListStr.equals("")){
            final List<Reply> replyList = new Gson().fromJson(replyListStr,new TypeToken<List<Reply>>(){}
            .getType());
            for (int i=0;i<replyList.size();i++) {
                View view = LayoutInflater.from(this).inflate(R.layout.item_reply,mItemsLayout,false);
                final Integer finalI = i;
                final Reply reply = replyList.get(i);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MainActivity.this,DetailActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putParcelable("reply",reply);
                        bundle.putInt("index",finalI);
                        intent.putExtras(bundle);
                        startActivity(intent);
                    }
                });
                ((TextView)view.findViewById(R.id.keyword)).setText(reply.getKeyword());
                ((TextView)view.findViewById(R.id.reply)).setText(reply.getContent());
                ((SwitchCompat)view.findViewById(R.id.switchBtn)).setChecked(reply.isOpen());
                ((SwitchCompat)view.findViewById(R.id.switchBtn)).setOnCheckedChangeListener(
                        new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                                replySwitchChanged(finalI);
                            }
                        }
                );
                mItemsLayout.addView(view);
            }
        }
        String filterKeyword = SpUtils.getString(Constants.SP_FILTER_KEYWORDS,"");
        if(!filterKeyword.equals("")){
            mFilterContent.setText(filterKeyword);
        }
    }

    private void replySwitchChanged(Integer finalI) {
        String replyListStr = SpUtils.getString(Constants.SP_REPLY_LIST,"");
        final List<Reply> replyList = new Gson().fromJson(replyListStr,new TypeToken<List<Reply>>(){}
                .getType());
        Reply reply = replyList.get(finalI);
        reply.setOpen(!reply.isOpen());
        replyList.set(finalI,reply);
        RobotService.mReplyList = replyList;
        SpUtils.putString(Constants.SP_REPLY_LIST,new Gson().toJson(replyList));
    }
}
