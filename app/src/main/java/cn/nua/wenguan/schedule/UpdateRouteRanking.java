package cn.nua.wenguan.schedule;
import java.net.URI;
import java.util.*;

/**
 * Measured on the publisher's network, not a security allowlist or a phone-speed promise.
 * Three-pass success counts first, then median complete verified transfer time.
 * See reports/UPDATE_ROUTE_BENCHMARK_20261014.json for every sample.
 */
public final class UpdateRouteRanking {
    public static final String MEASURED_AT="2026-10-14T14:27:30.206Z";
    public static final String BASELINE_VERSION="1.2.1";
    private static final String[] MANIFEST_ORDER={
        "raw",
        "https://git.yylx.win/",
        "https://gh.jjj.gv.uy/",
        "https://ghproxy.icu/",
        "https://gh-proxy.org/",
        "https://cdn.akaere.online/",
        "https://cdn.gh-proxy.com/",
        "api",
        "https://gh.acmsz.top/",
        "https://fastgit.cc/",
        "https://gh-proxy.com/",
        "https://gh.inkchills.cn/",
        "https://gh.meali.top/",
        "https://github.ikgy.top/",
        "https://ghproxy.net/",
        "https://gh.sixyin.com/",
        "direct",
        "https://down.mxw.xx.kg/",
        "https://gh.monlor.com/",
        "https://gh.chjina.com/",
        "https://free.cn.eu.org/",
        "https://gh.my-website.ccwu.cc/",
        "https://ghfast.top/",
        "https://ghpxy.hwinzniej.top/",
        "https://xsadwsd.kdns.fr/",
        "https://github.mxw.qzz.io/",
        "https://gh.noki.icu/",
        "https://cfgh.ikgy.top/",
        "https://ghproxy.felicity.land/",
        "https://gh.07150721.xyz/",
        "https://ghf.xn--eqrr82bzpe.top/",
        "https://githubproxy.gohj99.site/",
        "https://github.chenc.dev/",
        "github-raw",
        "https://github.ednovas.xyz/",
        "https://g.blfrp.cn/",
        "https://ghm.078465.xyz/",
        "https://git.669966.xyz/",
        "https://ghfile.geekertao.top/",
        "https://github.tbap.top/",
        "https://proxy.yaoyaoling.net/",
    };
    private static final String[] MANIFEST_FAILED={
        "https://777.z321.cc.cd/",
        "https://down.mxw.qzz.io/",
        "https://g.z321.cc.cd/",
        "https://gap.andyjin.website/",
        "https://gg.z321.cc.cd/",
        "https://gh.927223.xyz/",
        "https://gh.b52m.cn/",
        "https://gh.bugdey.us.kg/",
        "https://gh.catmak.name/",
        "https://gh.ddlc.top/",
        "https://gh.dpik.top/",
        "https://gh.felicity.ac.cn/",
        "https://gh.idayer.com/",
        "https://gh.jasonzeng.dev/",
        "https://gh.qfmc0721.cc.cd/",
        "https://gh.ruan.dpdns.org/",
        "https://gh.tryxd.cn/",
        "https://gh.zhai.edu.pl/",
        "https://ghp.arslantu.xyz/",
        "https://ghp.keleyaa.com/",
        "https://ghpr.cc/",
        "https://ghproxy.1888866.xyz/",
        "https://ghproxy.cxkpro.top/",
        "https://ghproxy.imciel.com/",
        "https://ghproxy.monkeyray.net/",
        "https://github-cf.947563.xyz/",
        "https://github-proxy.memory-echoes.cn/",
        "https://github.dpik.top/",
        "https://github.geekery.cn/",
        "https://github.gohj99.site/",
        "https://github.nswrz.cn/",
        "https://github.starrlzy.cn/",
        "https://github.tmby.shop/",
        "https://github.xxlab.tech/",
        "https://githubdog.com/",
        "https://gitproxy.127731.xyz/",
        "https://gitproxy.click/",
        "https://gitproxy.mrhjx.cn/",
        "https://gp.zkitefly.eu.org/",
        "https://j.1lin.dpdns.org/",
        "https://j.1win.ggff.net/",
        "https://jiashu.1win.eu.org/",
        "https://js.jiangss.shop/",
        "https://slink.ltd/",
        "https://tvv.tw/",
    };
    private static final String[] APK_ORDER={
        "https://git.yylx.win/",
        "https://cdn.gh-proxy.com/",
        "https://gh.jjj.gv.uy/",
        "https://gh.qfmc0721.cc.cd/",
        "https://xsadwsd.kdns.fr/",
        "https://free.cn.eu.org/",
        "https://gh-proxy.org/",
        "https://ghfast.top/",
        "https://gh.monlor.com/",
        "https://tvv.tw/",
        "https://jiashu.1win.eu.org/",
        "https://gh.meali.top/",
        "https://down.mxw.xx.kg/",
        "https://gh.chjina.com/",
        "https://ghpxy.hwinzniej.top/",
        "https://github.mxw.qzz.io/",
        "https://ghproxy.icu/",
        "https://github.ikgy.top/",
        "https://cdn.akaere.online/",
        "https://gh-proxy.com/",
        "https://fastgit.cc/",
        "https://gh.sixyin.com/",
        "https://gh.inkchills.cn/",
        "https://github.ednovas.xyz/",
        "https://git.669966.xyz/",
        "https://gh.catmak.name/",
        "https://ghproxy.felicity.land/",
        "https://cfgh.ikgy.top/",
        "https://gh.my-website.ccwu.cc/",
        "https://gh.07150721.xyz/",
        "https://github.tbap.top/",
        "https://githubproxy.gohj99.site/",
        "https://github.chenc.dev/",
        "https://gh.acmsz.top/",
        "https://ghm.078465.xyz/",
        "https://ghf.xn--eqrr82bzpe.top/",
        "https://gh.dpik.top/",
        "api",
        "https://ghfile.geekertao.top/",
        "https://github.xxlab.tech/",
        "https://g.blfrp.cn/",
        "direct",
    };
    private static final String[] APK_FAILED={
        "https://777.z321.cc.cd/",
        "https://down.mxw.qzz.io/",
        "https://g.z321.cc.cd/",
        "https://gap.andyjin.website/",
        "https://gg.z321.cc.cd/",
        "https://gh.927223.xyz/",
        "https://gh.b52m.cn/",
        "https://gh.bugdey.us.kg/",
        "https://gh.ddlc.top/",
        "https://gh.felicity.ac.cn/",
        "https://gh.idayer.com/",
        "https://gh.jasonzeng.dev/",
        "https://gh.noki.icu/",
        "https://gh.ruan.dpdns.org/",
        "https://gh.tryxd.cn/",
        "https://gh.zhai.edu.pl/",
        "https://ghp.arslantu.xyz/",
        "https://ghp.keleyaa.com/",
        "https://ghpr.cc/",
        "https://ghproxy.1888866.xyz/",
        "https://ghproxy.cxkpro.top/",
        "https://ghproxy.imciel.com/",
        "https://ghproxy.monkeyray.net/",
        "https://ghproxy.net/",
        "https://github-cf.947563.xyz/",
        "https://github-proxy.memory-echoes.cn/",
        "https://github.dpik.top/",
        "https://github.geekery.cn/",
        "https://github.gohj99.site/",
        "https://github.nswrz.cn/",
        "https://github.starrlzy.cn/",
        "https://github.tmby.shop/",
        "https://githubdog.com/",
        "https://gitproxy.127731.xyz/",
        "https://gitproxy.click/",
        "https://gitproxy.mrhjx.cn/",
        "https://gp.zkitefly.eu.org/",
        "https://j.1lin.dpdns.org/",
        "https://j.1win.ggff.net/",
        "https://js.jiangss.shop/",
        "https://proxy.yaoyaoling.net/",
        "https://slink.ltd/",
    };
    private UpdateRouteRanking(){}
    public static List<String> mirrors(){
        LinkedHashSet<String> nodes=new LinkedHashSet<>();
        for(String[] order:new String[][]{MANIFEST_ORDER,APK_ORDER,MANIFEST_FAILED,APK_FAILED})
            for(String key:order){String normalized=UpdateMirrorPolicy.normalize(key);if(normalized!=null)nodes.add(normalized);}
        return new ArrayList<>(nodes);
    }
    public static List<String> order(List<String> candidates,boolean download){
        List<String> unique=new ArrayList<>(new LinkedHashSet<>(candidates));
        String[] good=download?APK_ORDER:MANIFEST_ORDER,bad=download?APK_FAILED:MANIFEST_FAILED;
        // Stable sort: new/unmeasured dynamic nodes precede measured all-failure fallbacks.
        unique.sort(Comparator.comparingInt(url->priority(key(url),good,bad)));
        return unique;
    }
    private static int priority(String key,String[] good,String[] bad){
        for(int i=0;i<good.length;i++)if(good[i].equals(key))return i;
        for(int i=0;i<bad.length;i++)if(bad[i].equals(key))return good.length+1+i;
        return good.length;
    }
    private static String key(String url){
        try{
            URI uri=URI.create(url);String host=uri.getHost(),path=uri.getPath();
            if("api.github.com".equalsIgnoreCase(host))return "api";
            if("raw.githubusercontent.com".equalsIgnoreCase(host))return "raw";
            if("github.com".equalsIgnoreCase(host))return path!=null&&path.contains("/raw/")?"github-raw":"direct";
            String origin=UpdateMirrorPolicy.normalize(uri.getScheme()+"://"+uri.getRawAuthority()+"/");
            return origin==null?url:origin;
        }catch(RuntimeException invalid){return url;}
    }
}
