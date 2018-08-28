package com.fuyun.accessibility;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class AddActivity extends AppCompatActivity {

    private EditText mKeyword,mContent;
    private SwitchCompat mSwitch;
    private Button mConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        initView();
        initAction();
    }

    private void initView() {
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle("添加策略");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mKeyword = findViewById(R.id.keyword);
        mContent = findViewById(R.id.content);
        mSwitch = findViewById(R.id.switchBtn);
        mConfirm = findViewById(R.id.confirm);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void initAction() {
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mKeyword.getText() == null ||
                        mKeyword.getText().toString().equals("") ||
                        mContent.getText() == null ||
                        mContent.getText().toString().equals("")){
                    Toast.makeText(AddActivity.this,"内容不能为空",Toast.LENGTH_LONG).show();
                }else{
                    addReply();
                    Toast.makeText(AddActivity.this,"添加成功",Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
    }

    private void addReply() {
        String replyListStr = SpUtils.getString(Constants.SP_REPLY_LIST,"[]");
        List<Reply> replyList = new Gson().fromJson(replyListStr,new TypeToken<List<Reply>>(){}
                .getType());
        Reply reply = new Reply(mKeyword.getText().toString(),
                mContent.getText().toString(),
                mSwitch.isChecked());
        replyList.add(reply);
        RobotService.mReplyList = replyList;
        SpUtils.putString(Constants.SP_REPLY_LIST,new Gson().toJson(replyList));
    }
}
