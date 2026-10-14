package cn.nua.wenguan.schedule;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/** Foreground-only checks. No school cookies, credentials, background service or silent install. */
public final class AppUpdates {
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    private static long lastCheck; private static volatile boolean busy; private static boolean pendingInstall;
    private static volatile JSONObject pendingRelease;
    private static volatile boolean downloadedReady;
    private AppUpdates() { }
    public static void checkOnOpen(Activity a){if(System.currentTimeMillis()-lastCheck>60000)check(a,false);}
    public static void configure(Activity a){EditText input=new EditText(a);input.setHint("HTTPS 更新清单地址（GitHub Release）");input.setText(a.getSharedPreferences("app_updates",0).getString("url",""));new AlertDialog.Builder(a).setTitle("配置软件更新源").setMessage("仓库创建后填入 Releases/latest/download/update.json 的 HTTPS 地址。只接受本应用发布公钥签名的清单。留空关闭联网检查。").setView(input).setNegativeButton("取消",null).setPositiveButton("保存",(d,w)->{String value=input.getText().toString().trim();if(!value.isEmpty()&&!https(value)){toast(a,"只接受无账号密码的 HTTPS 地址");return;}if(a.getSharedPreferences("app_updates",0).edit().putString("url",value).commit())toast(a,"更新源已保存，下次打开 App 时检查");else toast(a,"保存失败");}).show();}
    public static void check(Activity a,boolean manual){
        String url=a.getSharedPreferences("app_updates",0).getString("url","");
        if(url.isEmpty()){if(manual)toast(a,"更新仓库尚未配置，请先配置更新源");return;}
        if(busy){if(manual)toast(a,"正在检查或下载更新");return;}busy=true;lastCheck=System.currentTimeMillis();
        WORK.execute(()->{try{
            byte[] envelope=fetchBytes(url,128*1024);JSONObject outer=new JSONObject(new String(envelope,java.nio.charset.StandardCharsets.UTF_8));
            byte[] payload=Base64.getDecoder().decode(outer.getString("payload"));byte[] signed=Base64.getDecoder().decode(outer.getString("signature"));
            String pem;try(InputStream in=a.getAssets().open("update-public.pem")){pem=new String(read(in,16384),java.nio.charset.StandardCharsets.US_ASCII);}
            byte[] key=Base64.getMimeDecoder().decode(pem.replace("-----BEGIN PUBLIC KEY-----","").replace("-----END PUBLIC KEY-----",""));
            java.security.Signature verify=java.security.Signature.getInstance("SHA256withRSA");verify.initVerify(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key)));verify.update(payload);if(!verify.verify(signed))throw new IOException("signature");
            JSONObject release=new JSONObject(new String(payload,java.nio.charset.StandardCharsets.UTF_8));
            if(release.getInt("schemaVersion")!=1||!a.getPackageName().equals(release.getString("packageName")))throw new IOException("package");
            long installed=code(a.getPackageManager().getPackageInfo(a.getPackageName(),0));long version=release.getLong("versionCode");
            if(version<=installed){busy=false;if(manual)a.runOnUiThread(()->{if(visible(a))toast(a,"当前已是最新版本");});return;}
            JSONObject apk=release.getJSONObject("apk");long size=apk.getLong("size");String hash=apk.getString("sha256"),download=apk.getString("url");
            if(size<=0||size>200L*1024*1024||!hash.matches("(?i)[a-f0-9]{64}")||!https(download))throw new IOException("metadata");
            pendingRelease=release;busy=false;a.runOnUiThread(()->presentPending(a));
        }catch(Exception error){busy=false;if(manual)a.runOnUiThread(()->{if(visible(a))toast(a,"更新检查失败，请稍后重试（未更改应用或课表）");});}});
    }
    private static void download(Activity a,String url,long size,String hash,long version){
        if(busy)return;busy=true;toast(a,"正在下载软件更新，请稍后");
        WORK.execute(()->{File folder=new File(a.getFilesDir(),"updates"),part=new File(folder,"download.part.apk"),apk=new File(folder,"latest.apk");try{
            if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("directory");MessageDigest digest=MessageDigest.getInstance("SHA-256");long total=0;
            try(InputStream input=open(url);OutputStream out=new FileOutputStream(part)){byte[] block=new byte[32768];int n;while((n=input.read(block))!=-1){total+=n;if(total>size)throw new IOException("size");digest.update(block,0,n);out.write(block,0,n);}}
            if(total!=size||!hex(digest.digest()).equalsIgnoreCase(hash))throw new IOException("hash");
            PackageManager pm=a.getPackageManager();int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
            PackageInfo archive=pm.getPackageArchiveInfo(part.getAbsolutePath(),flags),current=pm.getPackageInfo(a.getPackageName(),flags);
            if(archive==null||!a.getPackageName().equals(archive.packageName)||code(archive)!=version||code(archive)<=code(current)||!sameSigner(archive,current))throw new IOException("APK identity");
            if(apk.exists()&&!apk.delete())throw new IOException("old update");if(!part.renameTo(apk))throw new IOException("verified file");
            downloadedReady=true;busy=false;a.runOnUiThread(()->presentPending(a));
        }catch(Exception failed){busy=false;part.delete();a.runOnUiThread(()->{if(visible(a))toast(a,"更新下载或校验失败，未安装，旧版和数据保持不变");});}});
    }
    private static long code(PackageInfo p){return Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode;}
    private static boolean sameSigner(PackageInfo a,PackageInfo b){Signature[] x=Build.VERSION.SDK_INT>=28?a.signingInfo.getApkContentsSigners():a.signatures,y=Build.VERSION.SDK_INT>=28?b.signingInfo.getApkContentsSigners():b.signatures;if(x==null||y==null||x.length!=y.length)return false;for(int i=0;i<x.length;i++)if(!x[i].equals(y[i]))return false;return true;}
    public static void resumeInstall(Activity a){if(pendingInstall&&a.getPackageManager().canRequestPackageInstalls()){pendingInstall=false;install(a);}}
    public static void presentPending(Activity a){
        if(!visible(a))return;
        if(downloadedReady){downloadedReady=false;install(a);return;}
        JSONObject release=pendingRelease;if(release==null)return;pendingRelease=null;
        JSONObject apk=release.optJSONObject("apk");if(apk==null)return;
        new AlertDialog.Builder(a).setTitle("发现新版本 "+release.optString("versionName"))
                .setMessage(release.optString("notes","软件更新")+"\n\n是否下载并安装？原课表与日程保留。安装须由系统确认。")
                .setNegativeButton("稍后",null).setPositiveButton("下载并安装",(d,w)->download(a,apk.optString("url"),apk.optLong("size"),apk.optString("sha256"),release.optLong("versionCode"))).show();
    }
    private static void install(Activity a){
        if(!a.getPackageManager().canRequestPackageInstalls()){pendingInstall=true;new AlertDialog.Builder(a).setTitle("允许安装本应用的更新").setMessage("需在系统页面允许此 App 安装更新。授权后返回即可弹出系统安装确认。").setNegativeButton("取消",(d,w)->pendingInstall=false).setPositiveButton("去授权",(d,w)->{try{a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+a.getPackageName())));}catch(RuntimeException noPage){pendingInstall=false;toast(a,"无法打开安装授权页面");}}).show();return;}
        try{Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://"+a.getPackageName()+".updates/apk"),"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);a.startActivity(intent);}catch(RuntimeException failed){toast(a,"无法打开系统安装器，请稍后重试");}
    }
    private static boolean visible(Activity a){return !a.isFinishing()&&!a.isDestroyed()&&a.hasWindowFocus();}
    private static boolean https(String url){try{java.net.URI u=java.net.URI.create(url);return "https".equalsIgnoreCase(u.getScheme())&&u.getHost()!=null&&u.getUserInfo()==null;}catch(RuntimeException invalid){return false;}}
    private static InputStream open(String url)throws IOException{
        for(int i=0;i<6;i++){if(!https(url))throw new IOException("HTTPS only");HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setRequestProperty("Accept-Encoding","identity");int status=c.getResponseCode();if(status>=300&&status<400){String location=c.getHeaderField("Location");String next=location==null?"":new URL(new URL(url),location).toString();c.disconnect();url=next;continue;}if(status!=200){c.disconnect();throw new IOException("HTTP");}return new FilterInputStream(c.getInputStream()){@Override public void close()throws IOException{try{super.close();}finally{c.disconnect();}}};}throw new IOException("redirects");
    }
    private static byte[] fetchBytes(String url,int limit)throws IOException{try(InputStream in=open(url)){return read(in,limit);}}
    private static byte[] read(InputStream in,int limit)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("limit");out.write(b,0,n);}return out.toByteArray();}
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format("%02x",b&255));return s.toString();}
    private static void toast(Activity a,String text){Toast.makeText(a,text,Toast.LENGTH_LONG).show();}
}
