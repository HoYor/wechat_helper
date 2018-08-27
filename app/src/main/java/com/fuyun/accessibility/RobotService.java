package com.fuyun.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

/**
 * Created by yym on 2018/8/20.
 */

public class RobotService extends AccessibilityService {

    private final String TAG = "RobotService";
    private boolean isFromNotification = false;
    public static String mSendMsg = "你好，我现在正忙，有急事可以打我电话【自动回复】";
    public static String mNotifyContent = "";
    private boolean isScreenOn = true;
    private AccessibilityNodeInfo nodeInfo;
//    private DevicePolicyManager policyManager;
    private KeyguardManager keyguardManager;
    private PowerManager pm;
    private ComponentName componentName;

    @Override
    public void onCreate() {
        super.onCreate();
        keyguardManager = (KeyguardManager) RobotService.this.getApplicationContext()
                .getSystemService(KEYGUARD_SERVICE);
        pm = (PowerManager) RobotService.this.getApplicationContext()
                .getSystemService(Context.POWER_SERVICE);
        componentName = new ComponentName(this, LockReceiver.class);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        int eventType = accessibilityEvent.getEventType();
        Log.d(TAG, "onAccessibilityEvent-eventType:"+eventType);
        switch (eventType){
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                Log.d(TAG, "onAccessibilityEvent: TYPE_NOTIFICATION_STATE_CHANGED");
                isFromNotification = true;
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
                    sendMsg();
                    isFromNotification = false;
                }
                break;
            case AccessibilityEvent.TYPE_VIEW_CLICKED:
                Log.d(TAG, "onAccessibilityEvent: TYPE_VIEW_CLICKED");
//                dfsnode(getRootInActiveWindow(),0);
                break;
        }
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
                String content = text.toString();
                //检查信息
                if (content.contains(mNotifyContent)) {
                    if (event.getParcelableData() != null && event.getParcelableData() instanceof Notification) {
                        Notification notification = (Notification) event.getParcelableData();
                        PendingIntent pendingIntent = notification.contentIntent;
                        try {
                            pendingIntent.send();
                        } catch (PendingIntent.CanceledException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
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