function web3d(){var cb='',_=' top: -1000px;',zb='" for "gwt:onLoadErrorFn"',xb='" for "gwt:onPropertyErrorFn"',ib='");',Ab='#',Nb='.cache.js',Cb='/',Ib='//',Lb='356DA6E192042D12C569942B4ED3F86C',Mb=':',rb='::',Vb=':moduleBase',bb='<!doctype html>',db='<html><head><\/head><body><\/body><\/html>',ub='=',Bb='?',wb='Bad handler "',ab='CSS1Compat',gb='Chrome',fb='DOMContentLoaded',W='DUMMY',Ub='Ignoring non-whitelisted Dev Mode URL: ',Tb='__gwtDevModeHook:web3d',Sb='_gwt_dummy_',Hb='base',Fb='baseUrl',R='begin',X='body',Q='bootstrap',Eb='clear.cache.gif',tb='content',Pb='end',hb='eval("',Rb='file:',S='gwt.codesvr.web3d=',T='gwt.codesvr=',yb='gwt:onLoadErrorFn',vb='gwt:onPropertyErrorFn',sb='gwt:property',nb='head',Qb='http:',Y='iframe',Db='img',kb='javascript',Z='javascript:""',Ob='loadExternalRefs',ob='meta',mb='moduleRequested',lb='moduleStartup',pb='name',$='position:absolute; width:0; height:0; border:none; left: -1000px;',jb='script',Jb='selectingPermutation',V='startup',eb='undefined',U='web3d',Kb='web3d.devmode.js',Gb='web3d.nocache.js',qb='web3d::';var o=window;var p=document;r(Q,R);function q(){var a=o.location.search;return a.indexOf(S)!=-1||a.indexOf(T)!=-1}
function r(a,b){if(o.__gwtStatsEvent){o.__gwtStatsEvent({moduleName:U,sessionId:o.__gwtStatsSessionId,subSystem:V,evtGroup:a,millis:(new Date).getTime(),type:b})}}
web3d.__sendStats=r;web3d.__moduleName=U;web3d.__errFn=null;web3d.__moduleBase=W;web3d.__softPermutationId=0;web3d.__computePropValue=null;web3d.__getPropMap=null;web3d.__gwtInstallCode=function(){};web3d.__gwtStartLoadingFragment=function(){return null};var s=function(){return false};var t=function(){return null};__propertyErrorFunction=null;var u=o.__gwt_activeModules=o.__gwt_activeModules||{};u[U]={moduleName:U};var v;function w(){B();return v}
function A(){B();return v.getElementsByTagName(X)[0]}
function B(){if(v){return}var a=p.createElement(Y);a.src=Z;a.id=U;a.style.cssText=$+_;a.tabIndex=-1;p.body.appendChild(a);v=a.contentDocument;if(!v){v=a.contentWindow.document}v.open();var b=document.compatMode==ab?bb:cb;v.write(b+db);v.close()}
function C(k){function l(a){function b(){if(typeof p.readyState==eb){return typeof p.body!=eb&&p.body!=null}return /loaded|complete/.test(p.readyState)}
var c=b();if(c){a();return}function d(){if(!c){c=true;a();if(p.removeEventListener){p.removeEventListener(fb,d,false)}if(e){clearInterval(e)}}}
if(p.addEventListener){p.addEventListener(fb,d,false)}var e=setInterval(function(){if(b()){d()}},50)}
function m(c){function d(a,b){a.removeChild(b)}
var e=A();var f=w();var g;if(navigator.userAgent.indexOf(gb)>-1&&window.JSON){var h=f.createDocumentFragment();h.appendChild(f.createTextNode(hb));for(var i=0;i<c.length;i++){var j=window.JSON.stringify(c[i]);h.appendChild(f.createTextNode(j.substring(1,j.length-1)))}h.appendChild(f.createTextNode(ib));g=f.createElement(jb);g.language=kb;g.appendChild(h);e.appendChild(g);d(e,g)}else{for(var i=0;i<c.length;i++){g=f.createElement(jb);g.language=kb;g.text=c[i];e.appendChild(g);d(e,g)}}}
web3d.onScriptDownloaded=function(a){l(function(){m(a)})};r(lb,mb);var n=p.createElement(jb);n.src=k;p.getElementsByTagName(nb)[0].appendChild(n)}
web3d.__startLoadingFragment=function(a){return G(a)};web3d.__installRunAsyncCode=function(a){var b=A();var c=w().createElement(jb);c.language=kb;c.text=a;b.appendChild(c);b.removeChild(c)};function D(){var c={};var d;var e;var f=p.getElementsByTagName(ob);for(var g=0,h=f.length;g<h;++g){var i=f[g],j=i.getAttribute(pb),k;if(j){j=j.replace(qb,cb);if(j.indexOf(rb)>=0){continue}if(j==sb){k=i.getAttribute(tb);if(k){var l,m=k.indexOf(ub);if(m>=0){j=k.substring(0,m);l=k.substring(m+1)}else{j=k;l=cb}c[j]=l}}else if(j==vb){k=i.getAttribute(tb);if(k){try{d=eval(k)}catch(a){alert(wb+k+xb)}}}else if(j==yb){k=i.getAttribute(tb);if(k){try{e=eval(k)}catch(a){alert(wb+k+zb)}}}}}t=function(a){var b=c[a];return b==null?null:b};__propertyErrorFunction=d;web3d.__errFn=e}
function F(){function e(a){var b=a.lastIndexOf(Ab);if(b==-1){b=a.length}var c=a.indexOf(Bb);if(c==-1){c=a.length}var d=a.lastIndexOf(Cb,Math.min(c,b));return d>=0?a.substring(0,d+1):cb}
function f(a){if(a.match(/^\w+:\/\//)){}else{var b=p.createElement(Db);b.src=a+Eb;a=e(b.src)}return a}
function g(){var a=t(Fb);if(a!=null){return a}return cb}
function h(){var a=p.getElementsByTagName(jb);for(var b=0;b<a.length;++b){if(a[b].src.indexOf(Gb)!=-1){return e(a[b].src)}}return cb}
function i(){var a=p.getElementsByTagName(Hb);if(a.length>0){return a[a.length-1].href}return cb}
function j(){var a=p.location;return a.href==a.protocol+Ib+a.host+a.pathname+a.search+a.hash}
var k=g();if(k==cb){k=h()}if(k==cb){k=i()}if(k==cb&&j()){k=e(p.location.href)}k=f(k);return k}
function G(a){if(a.match(/^\//)){return a}if(a.match(/^[a-zA-Z]+:\/\//)){return a}return web3d.__moduleBase+a}
function H(){var f=[];var g=0;var h=[];var i=[];function j(a){var b=i[a](),c=h[a];if(b in c){return b}var d=[];for(var e in c){d[c[e]]=e}if(__propertyErrorFunc){__propertyErrorFunc(a,d,b)}throw null}
s=function(a,b){return b in h[a]};web3d.__getPropMap=function(){var a={};for(var b in h){if(h.hasOwnProperty(b)){a[b]=j(b)}}return a};web3d.__computePropValue=j;o.__gwt_activeModules[U].bindings=web3d.__getPropMap;r(Q,Jb);if(q()){return G(Kb)}var k;try{k=Lb;var l=k.indexOf(Mb);if(l!=-1){g=parseInt(k.substring(l+1),10);k=k.substring(0,l)}}catch(a){}web3d.__softPermutationId=g;return G(k+Nb)}
function I(){if(!o.__gwt_stylesLoaded){o.__gwt_stylesLoaded={}}r(Ob,R);r(Ob,Pb)}
D();web3d.__moduleBase=F();u[U].moduleBase=web3d.__moduleBase;var J=H();if(o){var K=!!(o.location.protocol==Qb||o.location.protocol==Rb);o.__gwt_activeModules[U].canRedirect=K;function L(){var b=Sb;try{o.sessionStorage.setItem(b,b);o.sessionStorage.removeItem(b);return true}catch(a){return false}}
if(K&&L()){var M=Tb;var N=o.sessionStorage[M];if(!/^http:\/\/(localhost|127\.0\.0\.1)(:\d+)?\/.*$/.test(N)){if(N&&(window.console&&console.log)){console.log(Ub+N)}N=cb}if(N&&!o[M]){o[M]=true;o[M+Vb]=F();var O=p.createElement(jb);O.src=N;var P=p.getElementsByTagName(nb)[0];P.insertBefore(O,P.firstElementChild||P.children[0]);return false}}}I();r(Q,Pb);C(J);return true}
web3d.succeeded=web3d();