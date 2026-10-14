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
import java.util.*;

/** Foreground-only checks. No school cookies, credentials, background service or silent install. */
public final class AppUpdates {
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    private static final String REPOSITORY="HerenYU2007/NUA-Schedule-Android";
    private static final String RELEASES="https://github.com/"+REPOSITORY+"/releases/";
    private static final String API="https://api.github.com/repos/"+REPOSITORY+"/releases/";
    // GitHub fallbacks after mirror transport; all routes share the same trust checks.
    private static final String[] MANIFEST_SOURCES={
        RELEASES+"latest/download/update.json",
        API+"latest",
        "https://raw.githubusercontent.com/"+REPOSITORY+"/master/updates/update.json",
        "https://github.com/"+REPOSITORY+"/raw/refs/heads/master/updates/update.json"
    };
    private static long lastCheck; private static volatile boolean busy; private static boolean pendingInstall;
    private static volatile JSONObject pendingRelease;
    private static volatile boolean downloadedReady;
    private static List<String> activeMirrors=Collections.emptyList();
    private AppUpdates() { }
    public static void checkOnOpen(Activity a){if(System.currentTimeMillis()-lastCheck>60000)check(a,false);}
    public static void check(Activity a,boolean manual){
        if(busy){if(manual)toast(a,"正在检查或下载更新");return;}busy=true;lastCheck=System.currentTimeMillis();
        WORK.execute(()->{try{
            JSONObject release=fetchRelease(a);
            long installed=code(a.getPackageManager().getPackageInfo(a.getPackageName(),0));long version=release.getLong("versionCode");
            if(version<=installed){busy=false;if(manual)a.runOnUiThread(()->{if(visible(a))toast(a,"当前已是最新版本");});return;}
            pendingRelease=release;busy=false;a.runOnUiThread(()->presentPending(a));
        }catch(Exception error){busy=false;if(manual)a.runOnUiThread(()->{if(visible(a))toast(a,"更新检查失败，请稍后重试（未更改应用或课表）");});}});
    }
    private static JSONObject fetchRelease(Activity a)throws Exception{
        activeMirrors=loadMirrors(a);
        LinkedHashSet<String> sources=new LinkedHashSet<>(UpdateMirrorPolicy.routes(MANIFEST_SOURCES[0],activeMirrors,false));
        sources.addAll(Arrays.asList(MANIFEST_SOURCES));
        for(String source:sources){try{
            String url=source.equals(API+"latest")?assetUrl(releaseMetadata(source),"update.json",null):source;
            JSONObject outer=new JSONObject(new String(fetchBytes(url,128*1024),java.nio.charset.StandardCharsets.UTF_8));
            if(!"SHA256withRSA".equals(outer.getString("algorithm")))throw new IOException("algorithm");
            byte[] payload=Base64.getDecoder().decode(outer.getString("payload"));byte[] signed=Base64.getDecoder().decode(outer.getString("signature"));
            String pem;try(InputStream in=a.getAssets().open("update-public.pem")){pem=new String(read(in,16384),java.nio.charset.StandardCharsets.US_ASCII);}
            byte[] key=Base64.getMimeDecoder().decode(pem.replace("-----BEGIN PUBLIC KEY-----","").replace("-----END PUBLIC KEY-----",""));
            java.security.Signature verify=java.security.Signature.getInstance("SHA256withRSA");verify.initVerify(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(key)));verify.update(payload);if(!verify.verify(signed))throw new IOException("signature");
            JSONObject release=new JSONObject(new String(payload,java.nio.charset.StandardCharsets.UTF_8));
            if(release.getInt("schemaVersion")!=1||!a.getPackageName().equals(release.getString("packageName")))throw new IOException("package");
            if(release.getLong("versionCode")<=0)throw new IOException("version");
            if(release.getLong("versionCode")<code(a.getPackageManager().getPackageInfo(a.getPackageName(),0)))throw new IOException("Stale manifest");
            String name=release.getString("versionName");
            if(!name.matches("[0-9]+\\.[0-9]+\\.[0-9]+"))throw new IOException("version");
            JSONObject apk=release.getJSONObject("apk");long size=apk.getLong("size");String hash=apk.getString("sha256"),download=apk.getString("url");
            if(size<=0||size>200L*1024*1024||!hash.matches("(?i)[a-f0-9]{64}")||!download.equals(RELEASES+"download/v"+name+"/nua-schedule-"+name+".apk"))throw new IOException("metadata");
            return release;
        }catch(Exception unavailable){recordFailure(a,"manifest",source,unavailable);}}
        throw new IOException("No verified update manifest");
    }
    private static List<String> loadMirrors(Activity a){
        File cache=new File(a.getFilesDir(),"updates/akams-nodes.txt");
        List<String> cached=new ArrayList<>(),dynamic=new ArrayList<>();
        try(InputStream in=new FileInputStream(cache)){cached.addAll(Arrays.asList(new String(read(in,65536),java.nio.charset.StandardCharsets.UTF_8).split("\n")));}catch(IOException absent){/* Builtins remain available. */}
        try{
            String page=new String(fetchBytes(UpdateMirrorPolicy.DIRECTORY,1024*1024),java.nio.charset.StandardCharsets.UTF_8);
            dynamic.addAll(UpdateMirrorPolicy.extract(page));
            for(String chunk:UpdateMirrorPolicy.chunks(page)){
                try{dynamic.addAll(UpdateMirrorPolicy.extract(new String(fetchBytes(chunk,1024*1024),java.nio.charset.StandardCharsets.UTF_8)));}
                catch(IOException unavailable){recordFailure(a,"directory",chunk,unavailable);}
            }
            if(!dynamic.isEmpty()){
                LinkedHashSet<String> unique=new LinkedHashSet<>(dynamic);
                if(!cache.getParentFile().isDirectory()&&!cache.getParentFile().mkdirs())throw new IOException("directory");
                try(OutputStream out=new FileOutputStream(cache)){out.write(String.join("\n",unique).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            }
        }catch(Exception unavailable){recordFailure(a,"directory",UpdateMirrorPolicy.DIRECTORY,unavailable);}
        return UpdateMirrorPolicy.combine(dynamic,cached);
    }
    private static void recordFailure(Activity a,String stage,String url,Exception error){
        // Bounded local status log: no query strings, response body, credentials or school data.
        try{
            File file=new File(a.getFilesDir(),"updates/mirror-attempts.log");
            if(!file.getParentFile().isDirectory()&&!file.getParentFile().mkdirs())return;
            try(OutputStream out=new FileOutputStream(file,file.length()<65536)){
                out.write((System.currentTimeMillis()+" "+stage+" "+new URL(url).getHost()+" "+error.getClass().getSimpleName()+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
        }catch(IOException ignored){/* Logging never changes update validation. */}
    }
    private static JSONObject releaseMetadata(String url)throws Exception{
        JSONObject metadata;
        try(InputStream in=open(url,true)){metadata=new JSONObject(new String(read(in,1024*1024),java.nio.charset.StandardCharsets.UTF_8));}
        if(metadata.getBoolean("draft")||metadata.getBoolean("prerelease"))throw new IOException("Unpublished release");
        return metadata;
    }
    private static String assetUrl(JSONObject release,String filename,String expectedBrowserUrl)throws Exception{
        org.json.JSONArray assets=release.getJSONArray("assets");
        for(int i=0;i<assets.length();i++){
            JSONObject asset=assets.getJSONObject(i);
            if(!filename.equals(asset.getString("name"))||!"uploaded".equals(asset.getString("state")))continue;
            String browser=asset.getString("browser_download_url"),api=asset.getString("url");
            if(expectedBrowserUrl!=null&&!expectedBrowserUrl.equals(browser))throw new IOException("Asset version");
            if(!api.matches(java.util.regex.Pattern.quote(API)+"assets/[0-9]+"))throw new IOException("Asset route");
            return api;
        }
        throw new IOException("Missing release asset");
    }
    private static void fetchApk(Activity a,String url,File part,long size,String hash)throws Exception{
        String identity=hash+":"+size;
        android.content.SharedPreferences prefs=a.getSharedPreferences("app_updates",0);
        if(!identity.equals(prefs.getString("download_identity",""))){
            UpdateDownload.reset(part);
            if(!prefs.edit().putString("download_identity",identity).commit())throw new IOException("Download identity");
        }
        for(String route:UpdateMirrorPolicy.routes(url,activeMirrors,true)){
            try{UpdateDownload.from(route,part,size,hash,(target,offset)->connect(target,false,offset));return;}
            catch(IOException failed){recordFailure(a,"apk",route,failed);}
        }
        // GitHub API is a final independent route, bound to the signed tag and filename.
        String tail=url.substring((RELEASES+"download/").length());String tag=tail.substring(0,tail.indexOf('/')),filename=tail.substring(tail.indexOf('/')+1);
        JSONObject release=releaseMetadata(API+"tags/"+tag);
        if(!tag.equals(release.getString("tag_name")))throw new IOException("Release version");
        UpdateDownload.from(assetUrl(release,filename,url),part,size,hash,(target,offset)->connect(target,false,offset));
    }
    private static void download(Activity a,String url,long size,String hash,long version){
        if(busy)return;busy=true;toast(a,"正在下载软件更新，请稍后");
        WORK.execute(()->{File folder=new File(a.getFilesDir(),"updates"),part=new File(folder,"download.part.apk"),apk=new File(folder,"latest.apk");try{
            if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException("directory");
            fetchApk(a,url,part,size,hash);
            PackageManager pm=a.getPackageManager();int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
            PackageInfo archive=pm.getPackageArchiveInfo(part.getAbsolutePath(),flags),current=pm.getPackageInfo(a.getPackageName(),flags);
            if(archive==null||!a.getPackageName().equals(archive.packageName)||code(archive)!=version||code(archive)<=code(current)||!sameSigner(archive,current))throw new IOException("APK identity");
            if(apk.exists()&&!apk.delete())throw new IOException("old update");if(!part.renameTo(apk))throw new IOException("verified file");
            downloadedReady=true;busy=false;a.runOnUiThread(()->presentPending(a));
        }catch(Exception failed){busy=false;a.runOnUiThread(()->{if(visible(a))toast(a,"更新下载或校验失败，未安装；已保留下载进度和原数据，可稍后重试");});}});
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
        return open(url,false);
    }
    private static InputStream open(String url,boolean json)throws IOException{
        HttpURLConnection c=connect(url,json,0);
        if(c.getResponseCode()!=200){c.disconnect();throw new IOException("HTTP");}
        try{return new FilterInputStream(c.getInputStream()){@Override public void close()throws IOException{try{super.close();}finally{c.disconnect();}}};}catch(IOException failed){c.disconnect();throw failed;}
    }
    private static HttpURLConnection connect(String url,boolean json,long offset)throws IOException{
        for(int i=0;i<6;i++){
            if(!https(url))throw new IOException("HTTPS only");
            URL target=new URL(url);
            if(target.getPort()!=-1&&target.getPort()!=443)throw new IOException("HTTPS port");
            for(java.net.InetAddress address:java.net.InetAddress.getAllByName(target.getHost())){
                byte[] bytes=address.getAddress();
                if(address.isAnyLocalAddress()||address.isLoopbackAddress()||address.isLinkLocalAddress()||address.isSiteLocalAddress()||address.isMulticastAddress()||(bytes.length==16&&(bytes[0]&0xfe)==0xfc))throw new IOException("Public host only");
            }
            HttpURLConnection c=(HttpURLConnection)target.openConnection();
            c.setInstanceFollowRedirects(false);c.setConnectTimeout(12000);c.setReadTimeout(20000);c.setUseCaches(false);
            c.setRequestProperty("Accept-Encoding","identity");c.setRequestProperty("User-Agent","NUA-Schedule-Android");
            c.setRequestProperty("Cache-Control","no-cache");
            c.setRequestProperty("Accept",json?"application/vnd.github+json":"application/octet-stream");
            if(offset>0)c.setRequestProperty("Range","bytes="+offset+"-");
            if("api.github.com".equalsIgnoreCase(target.getHost()))c.setRequestProperty("X-GitHub-Api-Version","2022-11-28");
            int status;try{status=c.getResponseCode();}catch(IOException unavailable){c.disconnect();throw unavailable;}
            if(status==301||status==302||status==303||status==307||status==308){
                String location=c.getHeaderField("Location");String next=location==null?"":new URL(target,location).toString();c.disconnect();url=next;continue;
            }
            return c;
        }
        throw new IOException("redirects");
    }
    private static byte[] fetchBytes(String url,int limit)throws IOException{try(InputStream in=open(url)){return read(in,limit);}}
    private static byte[] read(InputStream in,int limit)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("limit");out.write(b,0,n);}return out.toByteArray();}
    private static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(String.format("%02x",b&255));return s.toString();}
    private static void toast(Activity a,String text){Toast.makeText(a,text,Toast.LENGTH_LONG).show();}
}
