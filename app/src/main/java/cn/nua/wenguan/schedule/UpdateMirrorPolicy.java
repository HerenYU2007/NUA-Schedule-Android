package cn.nua.wenguan.schedule;
import java.net.IDN;
import java.net.URI;
import java.util.*;
import java.util.regex.*;

/** Read-only directory parser. Downloaded JavaScript is never executed. */
public final class UpdateMirrorPolicy {
    public static final String DIRECTORY="https://github.akams.cn/";
    public static final String PREFERRED="https://gh-proxy.org/";
    public static final String[] BUILTIN={
        "https://gh.dpik.top/",
        "https://github.tbap.top/",
        "https://ghfile.geekertao.top/",
        "https://ghproxy.net/",
        "https://gh-proxy.com/",
        "https://cdn.gh-proxy.com/",
        "https://github.dpik.top/",
        "https://j.1lin.dpdns.org/",
        "https://github.starrlzy.cn/",
        "https://github-proxy.memory-echoes.cn/",
        "https://git.yylx.win/",
        "https://ghm.078465.xyz/",
        "https://gh.927223.xyz/",
        "https://ghf.无名氏.top/",
        "https://gh.felicity.ac.cn/",
        "https://gh.bugdey.us.kg/",
        "https://cdn.akaere.online/",
        "https://jiashu.1win.eu.org/",
        "https://tvv.tw/",
        "https://j.1win.ggff.net/",
        "https://gitproxy.127731.xyz/",
        "https://gh.inkchills.cn/",
        "https://gh.catmak.name/",
        "https://gh.b52m.cn/",
        "https://down.mxw.xx.kg/",
        "https://down.mxw.qzz.io/",
        "https://github.mxw.qzz.io/",
        "https://gh.acmsz.top/",
        "https://gh.jjj.gv.uy/",
        "https://slink.ltd/",
        "https://github.tmby.shop/",
        "https://ghpr.cc/",
        "https://gh.tryxd.cn/",
        "https://gitproxy.click/",
        "https://github.chenc.dev/",
        "https://gh.ddlc.top/",
        "https://gitproxy.mrhjx.cn/",
        "https://gh.sixyin.com/",
        "https://gh.monlor.com/",
        "https://ghpxy.hwinzniej.top/",
        "https://git.669966.xyz/",
        "https://ghfast.top/",
        "https://gh.jasonzeng.dev/",
        "https://github.geekery.cn/",
        "https://gp.zkitefly.eu.org/",
        "https://fastgit.cc/",
        "https://ghproxy.1888866.xyz/",
        "https://ghp.arslantu.xyz/",
        "https://github.ednovas.xyz/",
        "https://ghproxy.imciel.com/",
        "https://ghproxy.cxkpro.top/",
        "https://github.xxlab.tech/",
        "https://gh.idayer.com/",
        "https://free.cn.eu.org/",
        "https://gh.chjina.com/",
        "https://ghp.keleyaa.com/",
        "https://proxy.yaoyaoling.net/",
        "https://ghproxy.monkeyray.net/",
        "https://gh.noki.icu/",
        "https://g.blfrp.cn/",
        "https://githubdog.com/",
        "https://gh.meali.top/",
        "https://777.z321.cc.cd/",
        "https://gg.z321.cc.cd/",
        "https://g.z321.cc.cd/",
        "https://js.jiangss.shop/",
        "https://gap.andyjin.website/",
        "https://gh.my-website.ccwu.cc/",
        "https://github.ikgy.top/",
        "https://gh.07150721.xyz/",
        "https://cfgh.ikgy.top/",
        "https://xsadwsd.kdns.fr/",
        "https://gh.ruan.dpdns.org/",
        "https://ghproxy.felicity.land/",
        "https://github.nswrz.cn/",
        "https://gh.zhai.edu.pl/",
        "https://gh.qfmc0721.cc.cd/",
        "https://github-cf.947563.xyz/",
    };
    private UpdateMirrorPolicy(){}
    public static String normalize(String input){
        try{
            URI uri=new URI(input.trim());
            if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getRawUserInfo()!=null||uri.getRawQuery()!=null||uri.getRawFragment()!=null)return null;
            String authority=uri.getRawAuthority();
            if(authority==null||authority.contains("@")||authority.contains(":"))return null;
            String host=IDN.toASCII(authority,IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
            if(!host.contains(".")||host.matches("[0-9.]+")||host.endsWith(".local")||host.endsWith(".localhost")||host.endsWith(".internal")||host.endsWith(".lan"))return null;
            if(uri.getRawPath()!=null&&!uri.getRawPath().isEmpty()&&!"/".equals(uri.getRawPath()))return null;
            return "https://"+host+"/";
        }catch(Exception invalid){return null;}
    }
    public static List<String> extract(String text){
        LinkedHashSet<String> out=new LinkedHashSet<>();
        String decoded=text.replace("\\/","/").replace("\\\"","\"");
        // Only explicit node records, not arbitrary analytics, image or library URLs.
        Matcher nodes=Pattern.compile("[\"']?label[\"']?\\s*:\\s*[\"'](?:contribute|search)[\"']\\s*,\\s*[\"']?value[\"']?\\s*:\\s*[\"']([^\"'\\s]+)[\"']").matcher(decoded);
        while(nodes.find()&&out.size()<256){
            String value=nodes.group(1);String normalized=normalize(value.startsWith("https://")?value:"https://"+value+"/");
            if(normalized!=null)out.add(normalized);
        }
        return new ArrayList<>(out);
    }
    public static List<String> chunks(String page){
        LinkedHashSet<String> out=new LinkedHashSet<>();
        Matcher m=Pattern.compile("/_next/static/chunks/[A-Za-z0-9_.-]+\\.js").matcher(page);
        while(m.find()&&out.size()<24)out.add(DIRECTORY.substring(0,DIRECTORY.length()-1)+m.group());
        return new ArrayList<>(out);
    }
    public static List<String> combine(List<String> dynamic,List<String> cached){
        LinkedHashSet<String> nodes=new LinkedHashSet<>();
        for(List<String> source:Arrays.asList(UpdateRouteRanking.mirrors(),dynamic,cached,Arrays.asList(BUILTIN)))for(String node:source){
            String normalized=normalize(node);if(normalized!=null)nodes.add(normalized);
        }
        return new ArrayList<>(nodes);
    }
    public static List<String> routes(String original,List<String> nodes,boolean download){
        LinkedHashSet<String> routes=new LinkedHashSet<>();
        if(download)routes.add(PREFERRED+original);
        for(String node:nodes){String base=normalize(node);if(base!=null)routes.add(base+original);}
        routes.add(original);
        return UpdateRouteRanking.order(new ArrayList<>(routes),download);
    }
}
