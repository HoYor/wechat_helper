package com.fuyun.accessibility;

import android.app.Application;

/**
 * from https://weibo.com/oasisfeng
 * Created by lixingtang on 2017/10/9.
 */

public class AppContext {
    public static final Application INSTANCE;

    static {
        Application app = null;
        try {
            app = (Application) Class.forName("android.app.AppGlobals").getMethod("getInitialApplication").invoke(null);
            if (app == null)
                throw new IllegalStateException("Static initialization of Applications must be on main thread.");
        } catch (final Exception e) {
            try {
                app = (Application) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
            } catch (final Exception ex) {
            }
        } finally {
            INSTANCE = app;
        }
    }
}