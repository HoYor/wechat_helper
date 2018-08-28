package com.fuyun.accessibility;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by yym on 2018/8/28.
 */

public class Reply implements Parcelable {
    private String keyword;
    private String content;
    private boolean isOpen;

    public Reply(String keyword, String content, boolean isOpen) {
        this.keyword = keyword;
        this.content = content;
        this.isOpen = isOpen;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.keyword);
        dest.writeString(this.content);
        dest.writeByte(this.isOpen ? (byte) 1 : (byte) 0);
    }

    protected Reply(Parcel in) {
        this.keyword = in.readString();
        this.content = in.readString();
        this.isOpen = in.readByte() != 0;
    }

    public static final Creator<Reply> CREATOR = new Creator<Reply>() {
        @Override
        public Reply createFromParcel(Parcel source) {
            return new Reply(source);
        }

        @Override
        public Reply[] newArray(int size) {
            return new Reply[size];
        }
    };
}
