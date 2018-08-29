package com.fuyun.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by yym on 2018/8/20.
 */

public class RobotService extends AccessibilityService {

    private final String TAG = "RobotService";
    private final String ROBOT_URL = "http://openapi.tuling123.com/openapi/api/v2";
    private final String ROBOT_KEY = "c0816e069eb4436db76e66c9888b4ac6";
    private boolean isFromNotification = false;
    public static String mOtherContent = "";
    public static String[] mFilterKeywords = {};
    public static boolean isOtherOpen = true;
    public static boolean isRobotOpen = false;
    public static boolean isPrimaryOpen = true;
    public static List<Reply> mReplyList = new ArrayList<>();
    private String mSendMsg = "";
    private boolean isScreenOn = true;
    private AccessibilityNodeInfo nodeInfo;
//    private DevicePolicyManager policyManager;
    private KeyguardManager keyguardManager;
    private PowerManager pm;
    private ComponentName componentName;
    private String mNotificationUser = "";
    private String mNotificationContent = "";

    @Override
    public void onCreate() {
        super.onCreate();
        initData();
        keyguardManager = (KeyguardManager) RobotService.this.getApplicationContext()
                .getSystemService(KEYGUARD_SERVICE);
        pm = (PowerManager) RobotService.this.getApplicationContext()
                .getSystemService(Context.POWER_SERVICE);
        componentName = new ComponentName(this, LockReceiver.class);
    }

    private void initData() {
        mOtherContent = SpUtils.getString(Constants.SP_OTHER_CONTENT,"");
        isOtherOpen = SpUtils.getBoolean(Constants.SP_OTHER_ISOPEN,true);
        isPrimaryOpen = SpUtils.getBoolean(Constants.SP_PRIMARY_SWITCHER,true);
        String replyListStr = SpUtils.getString(Constants.SP_REPLY_LIST,"");
        if(!replyListStr.equals("")){
            mReplyList = new Gson().fromJson(replyListStr,new TypeToken<List<Reply>>(){}
                    .getType());
        }
        mFilterKeywords = SpUtils.getString(Constants.SP_FILTER_KEYWORDS,"").split(",");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        int eventType = accessibilityEvent.getEventType();
        Log.d(TAG, "onAccessibilityEvent-eventType:"+eventType);
        switch (eventType){
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Log.d(TAG, "onAccessibilityEvent: TYPE_NOTIFICATION_STATE_CHANGED");
                if(!isPrimaryOpen)return;
                handleNotification(accessibilityEvent);
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                Log.d(TAG, "onAccessibilityEvent: TYPE_WINDOW_STATE_CHANGED");
                break;
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                Log.d(TAG, "onAccessibilityEvent: TYPE_WINDOW_CONTENT_CHANGED");
                String className = accessibilityEvent.getClassName().toString();
                Log.d(TAG, "className: "+className);
                if(isFromNotification){
                    if(isRobotOpen){
                        requestRobotMsg();
                    }else {
                        sendMsg();
                    }
                    isFromNotification = false;
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                Log.d(TAG, "onAccessibilityEvent: TYPE_VIEW_CLICKED");
//                dfsnode(getRootInActiveWindow(),0);
                break;
        }
    }

    /**
     * 请求机器人消息
     */
    private void requestRobotMsg() {
        Observable.create(new ObservableOnSubscribe<RobotResponse>() {
            @Override
            public void subscribe(ObservableEmitter<RobotResponse> e) throws Exception {
                Log.d(TAG, "subscribe-thread: "+Thread.currentThread().getName());
                URL url = new URL(ROBOT_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(1000*10);
                RobotRequest request = new RobotRequest(ROBOT_KEY,
                        MD5(mNotificationUser),
                        mNotificationContent);
                String body = new Gson().toJson(request);
                Log.d(TAG, "subscribe-body:"+body);
//                conn.setRequestProperty("Content-Length", String.valueOf(body.length()));
                conn.setRequestProperty("Cache-Control", "max-age=0");
                conn.setRequestProperty("Charset", "UTF-8");
                conn.setRequestProperty("Content-Type","application/json; charset=UTF-8");
                conn.setDoOutput(true);
                conn.getOutputStream().write(body.getBytes("UTF-8"));
                conn.getOutputStream().flush();
                conn.getOutputStream().close();
//                conn.setDoInput(true);
                int code = conn.getResponseCode();
//                Log.d(TAG, "subscribe-code:"+code);
                if (code == 200) {
                    InputStream inputStream = conn.getInputStream();
                    String result = inputStream2String(inputStream);
                    inputStream.close();
                    Log.d(TAG, "subscribe-result:"+result);
                    RobotResponse response = new Gson()
                            .fromJson(result, new TypeToken<RobotResponse>(){}.getType());
                    e.onNext(response);
                }else{
                    InputStream inputStream = conn.getInputStream();
                    String result = inputStream2String(inputStream);
                    Log.d(TAG, "subscribe-result:"+result);
                    e.onError(new Throwable());
                }
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<RobotResponse>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(RobotResponse s) {
                Log.d(TAG, "onNext: ");
                Log.d(TAG, "onNext-thread: "+Thread.currentThread().getName());
                if(s != null &&
                        s.getResults() != null &&
                        s.getResults().size()>0){
                    for (RobotResponse.ResultsBean resultsBean:s.getResults()) {
                        if(resultsBean.getResultType() != null &&
                                resultsBean.getResultType().equals("text")){
                            mSendMsg = resultsBean.getValues().getText()+"【机器人胡荣】";
                            sendMsg();
                            return;
                        }
                    }
                }
                sendMsg();
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG, "onError: ");
                e.printStackTrace();
                Log.d(TAG, "onError-thread: "+Thread.currentThread().getName());
                sendMsg();
            }

            @Override
            public void onComplete() {

            }
        });
    }

    public static String inputStream2String(InputStream in_st){
        BufferedReader in = new BufferedReader(new InputStreamReader(in_st));
        StringBuilder builder = new StringBuilder();
        String line;
        try {
            while ((line = in.readLine()) != null){
                builder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    private String MD5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(s.getBytes("utf-8"));
            return toHex(bytes);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String toHex(byte[] bytes) {

        final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
        StringBuilder ret = new StringBuilder(bytes.length * 2);
        for (int i=0; i<bytes.length; i++) {
            ret.append(HEX_DIGITS[(bytes[i] >> 4) & 0x0f]);
            ret.append(HEX_DIGITS[bytes[i] & 0x0f]);
        }
        return ret.toString();
    }

    private void sendMsg() {
        nodeInfo = getRootInActiveWindow();
        if(nodeInfo == null)return;
        List<AccessibilityNodeInfo> editInfos = nodeInfo.findAccessibilityNodeInfosByViewId(
                "com.tencent.mm:id/aep");
        if(editInfos != null && editInfos.size()>0){
            AccessibilityNodeInfo editInfo = editInfos.get(0);
            Bundle bundle = new Bundle();
            bundle.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,mSendMsg);
            editInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            editInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,bundle);
        }
        List<AccessibilityNodeInfo> sendInfos = nodeInfo.findAccessibilityNodeInfosByViewId(
                "com.tencent.mm:id/aev");
        if(sendInfos != null && sendInfos.size()>0){
            AccessibilityNodeInfo sendInfo = sendInfos.get(0);
            sendInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
//        performGlobalAction(GLOBAL_ACTION_HOME);
        sleepAndLock();
    }

    /**
     * 唤醒手机屏幕并解锁
     */
    private void wakeUpAndUnlock() {
        // 获取电源管理器对象
        isScreenOn = pm.isScreenOn();
        if (!isScreenOn) {

            // 获取PowerManager.WakeLock对象,后面的参数|表示同时传入两个值,最后的是LogCat里用的Tag
            PowerManager.WakeLock wl = pm.newWakeLock(
                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                            PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "bright");
            wl.acquire(1000); // 点亮屏幕
            wl.release(); // 释放
            KeyguardManager.KeyguardLock keyguardLock = keyguardManager.newKeyguardLock("unLock");
            // 屏幕锁定
//            keyguardLock.reenableKeyguard();
            keyguardLock.disableKeyguard(); // 解锁
        }
    }

    /**
     * 手机休眠并锁屏
     */
    private void sleepAndLock(){
        if(!isScreenOn) {
            //获取设备管理服务
            DevicePolicyManager policyManager = (DevicePolicyManager) getSystemService(Context
                    .DEVICE_POLICY_SERVICE);
            if(policyManager.isAdminActive(componentName)) {
                policyManager.lockNow();
            } else{
//                activeManager();
            }
        }
    }

    private void activeManager() {
        // 不行，因为这里是service？
//        //使用隐式意图调用系统方法激活指定的设备管理器
//        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
//        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
//        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,"锁屏");
//        startActivity(intent);
    }

    private void handleNotification(AccessibilityEvent event) {
        wakeUpAndUnlock();
        List<CharSequence> texts = event.getText();
        if (!texts.isEmpty()) {
            for (CharSequence text : texts) {
//                Log.d(TAG, "handleNotification-text:"+text);
                String content = text.toString();
                String[] info = content.split(": ");
                if(info.length > 1) {
                    mNotificationUser = info[0];
                    mNotificationContent = info[1];
                }
                //检查信息
                if(isContainsFilterKeyword(content)){
                    return;
                }
                if(!isContainsReplyKeyword(content)){
                    if(isOtherOpen){
                        mSendMsg = mOtherContent;
                    }else{
                        continue;
                    }
                }
                if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                    Notification notification = (Notification) event.getParcelableData();
                    PendingIntent pendingIntent = notification.contentIntent;
                    try {
                        isFromNotification = true;
                        pendingIntent.send();
                        break;
                    } catch (PendingIntent.CanceledException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean isContainsReplyKeyword(String content) {
        for (Reply reply:mReplyList) {
            if(reply.isOpen()){
                if(content.contains(reply.getKeyword())){
                    mSendMsg = reply.getContent();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isContainsFilterKeyword(String content) {
        for (String keyword:mFilterKeywords) {
            if(keyword.equals(""))continue;
            if(content.contains(keyword)){
                return true;
            }
        }
        return false;
    }

    public void dfsnode(AccessibilityNodeInfo node , int num){
        if(node == null){
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0 ;i < num ; i++){
            stringBuilder.append("__ ");    //父子节点之间的缩进
        }
        Log.i("####",stringBuilder.toString() + node.toString());   //打印
        for(int i = 0 ; i < node.getChildCount()  ; i++){      //遍历子节点
            dfsnode(node.getChild(i),num+1);
        }
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt: ");
    }
}
