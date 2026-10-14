import cn.nua.wenguan.schedule.UpdateMirrorPolicy;
import cn.nua.wenguan.schedule.UpdateDownload;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
public class ValidateUpdateMirrors {
    static void check(boolean b,String what){if(!b)throw new AssertionError(what);}
    static class Response extends HttpURLConnection {
        final int status;final String range;final byte[] bytes;final int failAfter;
        Response(int status,String range,byte[] bytes,int failAfter)throws Exception{super(new URL("https://example.com"));this.status=status;this.range=range;this.bytes=bytes;this.failAfter=failAfter;}
        public int getResponseCode(){return status;}
        public String getHeaderField(String name){return "Content-Range".equals(name)?range:null;}
        public InputStream getInputStream(){return new InputStream(){int pos;public int read()throws IOException{if(failAfter>=0&&pos>=failAfter)throw new IOException("network");return pos<bytes.length?bytes[pos++]&255:-1;}public int read(byte[] b,int off,int len)throws IOException{if(failAfter>=0&&pos>=failAfter)throw new IOException("network");if(pos>=bytes.length)return -1;int n=Math.min(len,bytes.length-pos);if(failAfter>=0)n=Math.min(n,failAfter-pos);System.arraycopy(bytes,pos,b,off,n);pos+=n;return n;}};}
        public void connect(){}public void disconnect(){}public boolean usingProxy(){return false;}
    }
    static String hash(byte[] bytes)throws Exception{StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format("%02x",b&255));return s.toString();}
    public static void main(String[] args)throws Exception{
        check(UpdateMirrorPolicy.BUILTIN.length==78,"all 78 snapshot nodes");
        List<String> nodes=UpdateMirrorPolicy.combine(Collections.emptyList(),Collections.emptyList());check(nodes.size()==78,"normalized unique snapshot");
        check(nodes.stream().anyMatch(n->n.contains("xn--")),"IDN node retained");
        for(String bad:Arrays.asList("http://example.com/","https://a:b@example.com/","https://127.0.0.1/","https://example.local/","https://example.com/path","https://example.com/?token=a","https://example.com:444/"))check(UpdateMirrorPolicy.normalize(bad)==null,"reject unsafe origin "+bad);
        List<String> dynamic=UpdateMirrorPolicy.extract("const nodes=[{label:\"search\",value:\"new.example.com\"},{\"label\":\"contribute\",\"value\":\"https://ghproxy.net/\"}];eval('danger'); https://analytics.example.com");
        check(dynamic.size()==2,"only explicit node records, JS not executed");
        check(UpdateMirrorPolicy.combine(dynamic,nodes).containsAll(nodes),"dynamic cannot remove builtins");
        check(UpdateMirrorPolicy.chunks("<script src='/_next/static/chunks/a.js'></script><script src='https://evil.com/a.js'></script>").equals(Arrays.asList("https://github.akams.cn/_next/static/chunks/a.js")),"own chunks only");
        String url="https://github.com/owner/repo/releases/download/v1.2.0/a.apk";
        List<String> routes=UpdateMirrorPolicy.routes(url,nodes,true);
        check(routes.get(0).equals("https://gh-proxy.org/"+url)&&routes.get(routes.size()-1).equals(url)&&routes.size()==80,"preferred plus all nodes plus direct");
        byte[] body="abcdefgh".getBytes("UTF-8");String digest=hash(body);
        File part=Paths.get(args[0],"mirror-test.part").toFile();UpdateDownload.reset(part);
        try{UpdateDownload.from(url,part,8,digest,(u,o)->{try{return new Response(200,null,body,3);}catch(Exception e){throw new IOException(e);}});throw new AssertionError("expected network failure");}catch(IOException expected){}
        check(part.length()==3,"network failure retains prefix");
        UpdateDownload.from(url,part,8,digest,(u,o)->{check(o==3,"cross-node offset");try{return new Response(206,"bytes 3-7/8",Arrays.copyOfRange(body,3,8),-1);}catch(Exception e){throw new IOException(e);}});
        check(UpdateDownload.verified(part,8,digest),"cross-node resumed file verified");
        Files.write(part.toPath(),new byte[]{0,0});
        UpdateDownload.from(url,part,8,digest,(u,o)->{try{return new Response(200,null,body,-1);}catch(Exception e){throw new IOException(e);}});
        check(UpdateDownload.verified(part,8,digest),"ignored Range restarts, not append");
        Files.write(part.toPath(),new byte[]{'a','b'});
        try{UpdateDownload.from(url,part,8,digest,(u,o)->{try{return new Response(206,"bytes 0-7/8",body,-1);}catch(Exception e){throw new IOException(e);}});throw new AssertionError("bad Range accepted");}catch(IOException expected){}
        check(part.length()==2,"invalid Range does not damage partial");
        UpdateDownload.reset(part);int[] calls={0};
        UpdateDownload.from(url,part,8,digest,(u,o)->{try{return new Response(200,null,++calls[0]==1?"bad-data".getBytes("UTF-8"):body,-1);}catch(Exception e){throw new IOException(e);}});
        check(calls[0]==2&&UpdateDownload.verified(part,8,digest),"checksum mismatch one clean retry");
        UpdateDownload.reset(part);calls[0]=0;
        try{UpdateDownload.from(url,part,8,digest,(u,o)->{calls[0]++;try{return new Response(200,null,"oversized-data".getBytes("UTF-8"),-1);}catch(Exception e){throw new IOException(e);}});throw new AssertionError("oversize accepted");}catch(UpdateDownload.IntegrityException expected){}
        check(calls[0]==2&&!part.exists(),"oversize rejected and removed");
        UpdateDownload.reset(part);
        System.out.println("Update mirror policy/transport passed: 78 nodes, dynamic/cache merge, IDN, safe origins, route order, resumed 206, restart 200, bad Range, corruption retry and size rejection.");
    }
}
