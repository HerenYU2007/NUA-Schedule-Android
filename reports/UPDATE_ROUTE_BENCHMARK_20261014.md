# 更新入口延迟与完整响应排序（2026-10-14）

测试时间：2026/10/14 22:21:38 至 2026/10/14 22:27:30（北京时间）。测试环境：发布电脑当前网络，Node.js HTTPS客户端，8路并发；不是手机或校园网测速。

范围：原内置78个节点、目录检出的81个节点、gh-proxy.org及GitHub官方备用（重复节点合并）。清单86条、APK 84条，各3轮，共510次有效性/传输测试。基准是1.2.1、versionCode 30，APK 541644字节；SHA-256为 `4b12f979838339c7b590ccd808fb190b8b40d3fe3f0ddc1b293924e3d69646fe`。

规则：先按三轮校验成功次数降序，再按成功样本的完整传输耗时中位数升序。所有失败入口保留在末尾；动态新节点放在已验证节点之后、全失败节点之前。清单与APK分别排序。排序快照写入UpdateRouteRanking.java，不在App启动时全网测速。

响应头耗时：从发起请求到HTTP响应头可用（包含DNS/连接/TLS；若有跳转则记录首次响应）。完整耗时：到整个文件下载完成，含重定向；API入口另计Release元数据查询和附件传输的合计时间。结果必须通过清单RSA签名/版本/包名或完整APK大小/SHA-256验证，网页、旧清单和错误内容不算成功。时间单位为ms，不是ICMP ping。

请求设置20秒总中止计时，失败DNS查询可能受操作系统影响超出该时限；连接池/TLS缓存复用会影响结果。部分站点缓存可能导致后续版本传播延迟，网络/手机/地区变化也会改变排名。此报告不能证明所有节点永远可用。仅匿名请求公开更新资料，不发送学校账号、Cookie或GitHub凭据。

## 更新清单排序

三轮全部成功：32；至少一次成功：41；全部失败：45。

| 顺序 | 入口 | 校验成功 | 响应头中位数 | 完整耗时中位数 | 失败类型 |
|---:|---|---:|---:|---:|---|
| 1 | GitHub raw 签名清单备份 | 3/3 | 259 | 268 | — |
| 2 | https://git.yylx.win/ | 3/3 | 962 | 964 | — |
| 3 | https://gh.jjj.gv.uy/ | 3/3 | 993 | 993 | — |
| 4 | https://ghproxy.icu/ | 3/3 | 1080 | 1080 | — |
| 5 | https://gh-proxy.org/ | 3/3 | 1453 | 1453 | — |
| 6 | https://cdn.akaere.online/ | 3/3 | 1077 | 1531 | — |
| 7 | https://cdn.gh-proxy.com/ | 3/3 | 743 | 1615 | — |
| 8 | GitHub API（含元数据查询） | 3/3 | 912 | 1797 | — |
| 9 | https://gh.acmsz.top/ | 3/3 | 873 | 1846 | — |
| 10 | https://fastgit.cc/ | 3/3 | 1962 | 1962 | — |
| 11 | https://gh-proxy.com/ | 3/3 | 1978 | 1978 | — |
| 12 | https://gh.inkchills.cn/ | 3/3 | 2081 | 2146 | — |
| 13 | https://gh.meali.top/ | 3/3 | 1183 | 2286 | — |
| 14 | https://github.ikgy.top/ | 3/3 | 2389 | 2396 | — |
| 15 | https://ghproxy.net/ | 3/3 | 2138 | 2438 | — |
| 16 | https://gh.sixyin.com/ | 3/3 | 2463 | 2466 | — |
| 17 | GitHub Release 直连 | 3/3 | 1127 | 2544 | — |
| 18 | https://down.mxw.xx.kg/ | 3/3 | 1276 | 2580 | — |
| 19 | https://gh.monlor.com/ | 3/3 | 1805 | 2589 | — |
| 20 | https://gh.chjina.com/ | 3/3 | 1557 | 2665 | — |
| 21 | https://free.cn.eu.org/ | 3/3 | 1432 | 2672 | — |
| 22 | https://gh.my-website.ccwu.cc/ | 3/3 | 2842 | 2842 | — |
| 23 | https://ghfast.top/ | 3/3 | 2405 | 2848 | — |
| 24 | https://ghpxy.hwinzniej.top/ | 3/3 | 951 | 3296 | — |
| 25 | https://xsadwsd.kdns.fr/ | 3/3 | 2640 | 3304 | — |
| 26 | https://github.mxw.qzz.io/ | 3/3 | 1367 | 3519 | — |
| 27 | https://gh.noki.icu/ | 3/3 | 2153 | 3985 | — |
| 28 | https://cfgh.ikgy.top/ | 3/3 | 1891 | 4394 | — |
| 29 | https://ghproxy.felicity.land/ | 3/3 | 2901 | 4987 | — |
| 30 | https://gh.07150721.xyz/ | 3/3 | 2728 | 5018 | — |
| 31 | https://ghf.xn--eqrr82bzpe.top/ | 3/3 | 2237 | 6238 | — |
| 32 | https://githubproxy.gohj99.site/ | 3/3 | 991 | 7024 | — |
| 33 | https://github.chenc.dev/ | 2/3 | 186 | 306 | ENOTFOUND |
| 34 | GitHub raw 页面入口 | 2/3 | 313 | 504 | AbortError |
| 35 | https://github.ednovas.xyz/ | 2/3 | 2293 | 2293 | AbortError |
| 36 | https://g.blfrp.cn/ | 2/3 | 4758 | 4758 | TypeError |
| 37 | https://ghm.078465.xyz/ | 2/3 | 3599 | 4999 | TypeError |
| 38 | https://git.669966.xyz/ | 2/3 | 2967 | 5083 | AbortError |
| 39 | https://ghfile.geekertao.top/ | 2/3 | 2411 | 8240 | AbortError |
| 40 | https://github.tbap.top/ | 1/3 | 4555 | 4556 | http-500、AbortError |
| 41 | https://proxy.yaoyaoling.net/ | 1/3 | 7870 | 19376 | AbortError |
| 42 | https://777.z321.cc.cd/ | 0/3 | — | — | http-429 |
| 43 | https://down.mxw.qzz.io/ | 0/3 | — | — | http-403 |
| 44 | https://g.z321.cc.cd/ | 0/3 | — | — | http-429 |
| 45 | https://gap.andyjin.website/ | 0/3 | — | — | ENOTFOUND |
| 46 | https://gg.z321.cc.cd/ | 0/3 | — | — | http-429 |
| 47 | https://gh.927223.xyz/ | 0/3 | — | — | http-403 |
| 48 | https://gh.b52m.cn/ | 0/3 | — | — | http-404 |
| 49 | https://gh.bugdey.us.kg/ | 0/3 | — | — | ENOTFOUND |
| 50 | https://gh.catmak.name/ | 0/3 | — | — | stale-or-metadata |
| 51 | https://gh.ddlc.top/ | 0/3 | — | — | http-429 |
| 52 | https://gh.dpik.top/ | 0/3 | — | — | http-500、AbortError |
| 53 | https://gh.felicity.ac.cn/ | 0/3 | — | — | http-429 |
| 54 | https://gh.idayer.com/ | 0/3 | — | — | http-429 |
| 55 | https://gh.jasonzeng.dev/ | 0/3 | — | — | ENOTFOUND |
| 56 | https://gh.qfmc0721.cc.cd/ | 0/3 | — | — | stale-or-metadata |
| 57 | https://gh.ruan.dpdns.org/ | 0/3 | — | — | AbortError |
| 58 | https://gh.tryxd.cn/ | 0/3 | — | — | SyntaxError |
| 59 | https://gh.zhai.edu.pl/ | 0/3 | — | — | TypeError |
| 60 | https://ghp.arslantu.xyz/ | 0/3 | — | — | http-525 |
| 61 | https://ghp.keleyaa.com/ | 0/3 | — | — | http-429 |
| 62 | https://ghpr.cc/ | 0/3 | — | — | http-404 |
| 63 | https://ghproxy.1888866.xyz/ | 0/3 | — | — | AbortError |
| 64 | https://ghproxy.cxkpro.top/ | 0/3 | — | — | http-429 |
| 65 | https://ghproxy.imciel.com/ | 0/3 | — | — | http-429 |
| 66 | https://ghproxy.monkeyray.net/ | 0/3 | — | — | http-404 |
| 67 | https://github-cf.947563.xyz/ | 0/3 | — | — | AbortError |
| 68 | https://github-proxy.memory-echoes.cn/ | 0/3 | — | — | http-429 |
| 69 | https://github.dpik.top/ | 0/3 | — | — | http-429 |
| 70 | https://github.geekery.cn/ | 0/3 | — | — | http-404 |
| 71 | https://github.gohj99.site/ | 0/3 | — | — | http-404 |
| 72 | https://github.nswrz.cn/ | 0/3 | — | — | TypeError |
| 73 | https://github.starrlzy.cn/ | 0/3 | — | — | ENOTFOUND |
| 74 | https://github.tmby.shop/ | 0/3 | — | — | SyntaxError |
| 75 | https://github.xxlab.tech/ | 0/3 | — | — | http-502 |
| 76 | https://githubdog.com/ | 0/3 | — | — | ENOTFOUND |
| 77 | https://gitproxy.127731.xyz/ | 0/3 | — | — | http-526 |
| 78 | https://gitproxy.click/ | 0/3 | — | — | SyntaxError |
| 79 | https://gitproxy.mrhjx.cn/ | 0/3 | — | — | http-429 |
| 80 | https://gp.zkitefly.eu.org/ | 0/3 | — | — | ENOTFOUND |
| 81 | https://j.1lin.dpdns.org/ | 0/3 | — | — | ENOTFOUND |
| 82 | https://j.1win.ggff.net/ | 0/3 | — | — | ENOTFOUND |
| 83 | https://jiashu.1win.eu.org/ | 0/3 | — | — | http-403 |
| 84 | https://js.jiangss.shop/ | 0/3 | — | — | TypeError |
| 85 | https://slink.ltd/ | 0/3 | — | — | http-404 |
| 86 | https://tvv.tw/ | 0/3 | — | — | http-502 |

## 完整 APK 下载排序

三轮全部成功：32；至少一次成功：42；全部失败：42。

| 顺序 | 入口 | 校验成功 | 响应头中位数 | 完整耗时中位数 | 失败类型 |
|---:|---|---:|---:|---:|---|
| 1 | https://git.yylx.win/ | 3/3 | 544 | 1146 | — |
| 2 | https://cdn.gh-proxy.com/ | 3/3 | 1028 | 2216 | — |
| 3 | https://gh.jjj.gv.uy/ | 3/3 | 624 | 2563 | — |
| 4 | https://gh.qfmc0721.cc.cd/ | 3/3 | 884 | 3352 | — |
| 5 | https://xsadwsd.kdns.fr/ | 3/3 | 700 | 3375 | — |
| 6 | https://free.cn.eu.org/ | 3/3 | 1285 | 3717 | — |
| 7 | https://gh-proxy.org/ | 3/3 | 692 | 3968 | — |
| 8 | https://ghfast.top/ | 3/3 | 1214 | 4076 | — |
| 9 | https://gh.monlor.com/ | 3/3 | 1656 | 4218 | — |
| 10 | https://tvv.tw/ | 3/3 | 1172 | 4226 | — |
| 11 | https://jiashu.1win.eu.org/ | 3/3 | 1333 | 4343 | — |
| 12 | https://gh.meali.top/ | 3/3 | 1168 | 4396 | — |
| 13 | https://down.mxw.xx.kg/ | 3/3 | 902 | 4430 | — |
| 14 | https://gh.chjina.com/ | 3/3 | 1323 | 4482 | — |
| 15 | https://ghpxy.hwinzniej.top/ | 3/3 | 845 | 4750 | — |
| 16 | https://github.mxw.qzz.io/ | 3/3 | 1407 | 4769 | — |
| 17 | https://ghproxy.icu/ | 3/3 | 763 | 4837 | — |
| 18 | https://github.ikgy.top/ | 3/3 | 1173 | 5046 | — |
| 19 | https://cdn.akaere.online/ | 3/3 | 1880 | 5058 | — |
| 20 | https://gh-proxy.com/ | 3/3 | 1468 | 5091 | — |
| 21 | https://fastgit.cc/ | 3/3 | 1050 | 5096 | — |
| 22 | https://gh.sixyin.com/ | 3/3 | 2801 | 5694 | — |
| 23 | https://gh.inkchills.cn/ | 3/3 | 2663 | 5992 | — |
| 24 | https://github.ednovas.xyz/ | 3/3 | 1825 | 6364 | — |
| 25 | https://git.669966.xyz/ | 3/3 | 2125 | 6379 | — |
| 26 | https://gh.catmak.name/ | 3/3 | 1682 | 6913 | — |
| 27 | https://ghproxy.felicity.land/ | 3/3 | 3019 | 7022 | — |
| 28 | https://cfgh.ikgy.top/ | 3/3 | 1780 | 8872 | — |
| 29 | https://gh.my-website.ccwu.cc/ | 3/3 | 1783 | 9901 | — |
| 30 | https://gh.07150721.xyz/ | 3/3 | 2450 | 11414 | — |
| 31 | https://github.tbap.top/ | 3/3 | 1908 | 12426 | — |
| 32 | https://githubproxy.gohj99.site/ | 3/3 | 685 | 18225 | — |
| 33 | https://github.chenc.dev/ | 2/3 | 162 | 497 | ENOTFOUND |
| 34 | https://gh.acmsz.top/ | 2/3 | 483 | 1991 | TypeError |
| 35 | https://ghm.078465.xyz/ | 2/3 | 1775 | 9995 | AbortError |
| 36 | https://ghf.xn--eqrr82bzpe.top/ | 2/3 | 2015 | 12112 | AbortError |
| 37 | https://gh.dpik.top/ | 2/3 | 5695 | 13601 | AbortError |
| 38 | GitHub API（含元数据查询） | 2/3 | 837 | 13949 | AbortError |
| 39 | https://ghfile.geekertao.top/ | 1/3 | 1936 | 7630 | AbortError |
| 40 | https://github.xxlab.tech/ | 1/3 | 4244 | 12925 | AbortError |
| 41 | https://g.blfrp.cn/ | 1/3 | 5045 | 14803 | AbortError |
| 42 | GitHub Release 直连 | 1/3 | 577 | 18184 | AbortError |
| 43 | https://777.z321.cc.cd/ | 0/3 | — | — | http-429 |
| 44 | https://down.mxw.qzz.io/ | 0/3 | — | — | http-403 |
| 45 | https://g.z321.cc.cd/ | 0/3 | — | — | http-429 |
| 46 | https://gap.andyjin.website/ | 0/3 | — | — | ENOTFOUND |
| 47 | https://gg.z321.cc.cd/ | 0/3 | — | — | http-429 |
| 48 | https://gh.927223.xyz/ | 0/3 | — | — | AbortError、http-403 |
| 49 | https://gh.b52m.cn/ | 0/3 | — | — | http-404 |
| 50 | https://gh.bugdey.us.kg/ | 0/3 | — | — | ENOTFOUND |
| 51 | https://gh.ddlc.top/ | 0/3 | — | — | http-429 |
| 52 | https://gh.felicity.ac.cn/ | 0/3 | — | — | http-429 |
| 53 | https://gh.idayer.com/ | 0/3 | — | — | http-429 |
| 54 | https://gh.jasonzeng.dev/ | 0/3 | — | — | ENOTFOUND |
| 55 | https://gh.noki.icu/ | 0/3 | — | — | AbortError |
| 56 | https://gh.ruan.dpdns.org/ | 0/3 | — | — | AbortError |
| 57 | https://gh.tryxd.cn/ | 0/3 | — | — | apk-hash-size |
| 58 | https://gh.zhai.edu.pl/ | 0/3 | — | — | TypeError |
| 59 | https://ghp.arslantu.xyz/ | 0/3 | — | — | http-525 |
| 60 | https://ghp.keleyaa.com/ | 0/3 | — | — | http-429 |
| 61 | https://ghpr.cc/ | 0/3 | — | — | apk-hash-size |
| 62 | https://ghproxy.1888866.xyz/ | 0/3 | — | — | AbortError |
| 63 | https://ghproxy.cxkpro.top/ | 0/3 | — | — | http-429 |
| 64 | https://ghproxy.imciel.com/ | 0/3 | — | — | http-429 |
| 65 | https://ghproxy.monkeyray.net/ | 0/3 | — | — | http-404 |
| 66 | https://ghproxy.net/ | 0/3 | — | — | AbortError |
| 67 | https://github-cf.947563.xyz/ | 0/3 | — | — | http-522、AbortError |
| 68 | https://github-proxy.memory-echoes.cn/ | 0/3 | — | — | http-429 |
| 69 | https://github.dpik.top/ | 0/3 | — | — | http-429 |
| 70 | https://github.geekery.cn/ | 0/3 | — | — | http-404 |
| 71 | https://github.gohj99.site/ | 0/3 | — | — | http-404 |
| 72 | https://github.nswrz.cn/ | 0/3 | — | — | TypeError |
| 73 | https://github.starrlzy.cn/ | 0/3 | — | — | ENOTFOUND |
| 74 | https://github.tmby.shop/ | 0/3 | — | — | apk-hash-size |
| 75 | https://githubdog.com/ | 0/3 | — | — | ENOTFOUND |
| 76 | https://gitproxy.127731.xyz/ | 0/3 | — | — | http-526 |
| 77 | https://gitproxy.click/ | 0/3 | — | — | apk-hash-size、TypeError |
| 78 | https://gitproxy.mrhjx.cn/ | 0/3 | — | — | http-429 |
| 79 | https://gp.zkitefly.eu.org/ | 0/3 | — | — | ENOTFOUND |
| 80 | https://j.1lin.dpdns.org/ | 0/3 | — | — | ENOTFOUND |
| 81 | https://j.1win.ggff.net/ | 0/3 | — | — | ENOTFOUND |
| 82 | https://js.jiangss.shop/ | 0/3 | — | — | TypeError |
| 83 | https://proxy.yaoyaoling.net/ | 0/3 | — | — | AbortError |
| 84 | https://slink.ltd/ | 0/3 | — | — | apk-hash-size |

## 动态目录网页及本站脚本

辅助目录仅测一次，用于发现节点，不参与清单或APK优先序；脚本仅作为文本解析，不执行。

| 资源 | 成功 | 响应头耗时 | 完整耗时 |
|---|---|---:|---:|
| page | 是 | 125 | 163 |
| /_next/static/chunks/1570bc024e57dd81.js | 是 | 152 | 153 |
| /_next/static/chunks/f3c1304f15e21378.js | 是 | 153 | 264 |
| /_next/static/chunks/c94886839a754ecb.js | 是 | 101 | 143 |
| /_next/static/chunks/turbopack-23ee38bbef37ea15.js | 是 | 102 | 103 |
| /_next/static/chunks/42879de7b8087bc9.js | 是 | 100 | 128 |
| /_next/static/chunks/0a47290756c091a0.js | 是 | 110 | 110 |
| /_next/static/chunks/4b935edd0cea1ee3.js | 是 | 109 | 109 |
| /_next/static/chunks/096f2b9eccbdd6b4.js | 是 | 106 | 221 |
| /_next/static/chunks/4ed424c366f600d8.js | 是 | 80 | 110 |
| /_next/static/chunks/a6dad97d9634a72d.js | 是 | 102 | 137 |

每轮原始结果及所有失败记录见同目录JSON；客户端仍按原签名、公钥、版本、包名、安装证书和HTTPS检查安装，排序不授予第三方发布权限。
