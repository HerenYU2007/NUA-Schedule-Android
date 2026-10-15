package cn.nua.wenguan.schedule;

/** A verified older manifest also means no newer version; it is not a transport failure. */
public final class UpdateCheckPolicy {
    private UpdateCheckPolicy() { }
    public static boolean hasUpdate(long installed,long offered){
        if(installed<=0||offered<=0)throw new IllegalArgumentException("Invalid version code");
        return offered>installed;
    }
}
