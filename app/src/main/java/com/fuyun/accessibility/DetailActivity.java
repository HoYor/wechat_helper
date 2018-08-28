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

public class DetailActivity extends AppCompatActivity {

    private EditText mKeyword,mContent;
    private SwitchCompat mSwitch;
    private Button mConfirm,mDelete;
    private Reply reply;
    private int index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        initView();
        initData();
        initAction();
    }

    private void initView() {
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle("编辑策略");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mKeyword = findViewById(R.id.keyword);
        mContent = findViewById(R.id.content);
        mSwitch = findViewById(R.id.switchBtn);
        mConfirm = findViewById(R.id.confirm);
        mDelete = findViewById(R.id.delete);
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

    private void initData() {
        reply = getIntent().getParcelableExtra("reply");
        index = getIntent().getIntExtra("index",0);
        mKeyword.setText(reply.getKeyword());
        mContent.setText(reply.getContent());
        mSwitch.setChecked(reply.isOpen());
    }

    private void initAction() {
        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mKeyword.getText() == null ||
                        mKeyword.getText().toString().equals("") ||
                        mContent.getText() == null ||
                        mContent.getText().toString().equals("")){
                    Toast.makeText(DetailActivity.this,"内容不能为空",Toast.LENGTH_LONG).show();
                }else{
                    saveReply();
                    Toast.makeText(DetailActivity.this,"编辑成功",Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });
        mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteReply();
                Toast.makeText(DetailActivity.this,"删除成功",Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void deleteReply() {
        String replyListStr = SpUtils.getString(Constants.SP_REPLY_LIST,"");
        List<Reply> replyList = new Gson().fromJson(replyListStr,new TypeToken<List<Reply>>(){}
                .getType());
        replyList.remove(index);
        RobotService.mReplyList = replyList;
        SpUtils.putString(Constants.SP_REPLY_LIST,new Gson().toJson(replyList));
    }

    private void saveReply() {
        String replyListStr = SpUtils.getString(Constants.SP_REPLY_LIST,"");
        List<Reply> replyList = new Gson().fromJson(replyListStr,new TypeToken<List<Reply>>(){}
                .getType());
        reply.setKeyword(mKeyword.getText().toString());
        reply.setContent(mContent.getText().toString());
        reply.setOpen(mSwitch.isChecked());
        replyList.set(index,reply);
        RobotService.mReplyList = replyList;
        SpUtils.putString(Constants.SP_REPLY_LIST,new Gson().toJson(replyList));
    }
}
