package cn.nua.wenguan.schedule;
import java.io.*;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.security.*;
import java.util.regex.*;

/** Resumable, hash-bound APK transport. Never installs an unchecked partial file. */
public final class UpdateDownload {
    public interface Connections { HttpURLConnection open(String url,long offset)throws IOException; }
    public static final class IntegrityException extends IOException {
        IntegrityException(String reason){super(reason);}
    }
    private UpdateDownload(){}
    public static boolean verified(File file,long size,String hash)throws IOException{
        if(!file.isFile()||file.length()!=size)return false;
        try{
            MessageDigest digest=MessageDigest.getInstance("SHA-256");
            try(InputStream in=new FileInputStream(file)){byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)digest.update(b,0,n);}
            StringBuilder text=new StringBuilder();for(byte b:digest.digest())text.append(String.format("%02x",b&255));
            return text.toString().equalsIgnoreCase(hash);
        }catch(NoSuchAlgorithmException impossible){throw new IOException(impossible);}
    }
    public static void reset(File part)throws IOException{Files.deleteIfExists(part.toPath());}
    public static void from(String url,File part,long size,String hash,Connections connections)throws IOException{
        // One clean retry on this node if its completed data fails integrity.
        for(int clean=0;clean<2;clean++){
            try{
                if(part.length()>size)reset(part);
                if(part.length()==size){
                    if(verified(part,size,hash))return;
                    reset(part);
                }
                long offset=part.length();HttpURLConnection c=connections.open(url,offset);
                try{
                    int status=c.getResponseCode();boolean append=false;
                    if(status==206){
                        Matcher range=Pattern.compile("bytes ([0-9]+)-([0-9]+)/([0-9]+)").matcher(String.valueOf(c.getHeaderField("Content-Range")));
                        if(!range.matches()||Long.parseLong(range.group(1))!=offset||Long.parseLong(range.group(2))!=size-1||Long.parseLong(range.group(3))!=size)throw new IOException("range");
                        append=offset>0;
                    }else if(status==200){offset=0;}else throw new IOException("HTTP");
                    long total=offset;
                    try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(part,append)){
                        byte[] block=new byte[32768];int n;
                        while((n=in.read(block))!=-1){total+=n;if(total>size)throw new IntegrityException("size");out.write(block,0,n);}
                    }
                    if(total!=size)throw new IOException("incomplete");
                    if(!verified(part,size,hash))throw new IntegrityException("hash");
                    return;
                }finally{c.disconnect();}
            }catch(IntegrityException invalid){reset(part);if(clean==1)throw invalid;}
        }
    }
}
