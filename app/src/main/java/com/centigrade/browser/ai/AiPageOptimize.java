package com.centigrade.browser.ai;

import android.os.Handler;
import android.os.Looper;
import android.webkit.WebView;

public class AiPageOptimize {

    public interface OptimizeCallback {
        void onComplete();
    }

    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public void optimize(WebView webView, OptimizeCallback callback) {
        if (webView == null) {
            if (callback != null) callback.onComplete();
            return;
        }

        String js =
                "(function(){" +
                        "function rm(sel){" +
                        " try{" +
                        "  var list=document.querySelectorAll(sel);" +
                        "  for(var i=0;i<list.length;i++){" +
                        "   if(list[i]&&list[i].parentNode) list[i].parentNode.removeChild(list[i]);" +
                        "  }" +
                        " }catch(e){}" +
                        "}" +
                        "rm('script,style,iframe,ins,.ad,.ads,.advertisement,.banner,.pop,.popup,.modal,.recommend,.related,.footer,.header nav,aside');" +
                        "try{" +
                        " var nodes=document.querySelectorAll('*');" +
                        " for(var i=0;i<nodes.length;i++){" +
                        "  var n=nodes[i];" +
                        "  var txt=(n.innerText||'').trim();" +
                        "  if(n.children.length===0&&txt.length===0){continue;}" +
                        "  var cls=((n.className||'')+' '+(n.id||'')).toLowerCase();" +
                        "  if(cls.indexOf('ad')!=-1||cls.indexOf('banner')!=-1||cls.indexOf('popup')!=-1||cls.indexOf('recommend')!=-1){" +
                        "   if(n.parentNode) n.parentNode.removeChild(n);" +
                        "  }" +
                        " }" +
                        "}catch(e){}" +
                        "try{" +
                        " var main=document.querySelector('article,main,[role=\"main\"]')||document.body;" +
                        " document.body.innerHTML='';" +
                        " var wrap=document.createElement('div');" +
                        " wrap.style.maxWidth='860px';" +
                        " wrap.style.margin='0 auto';" +
                        " wrap.style.padding='24px 18px 80px';" +
                        " wrap.style.fontSize='18px';" +
                        " wrap.style.lineHeight='1.85';" +
                        " wrap.style.color='#111111';" +
                        " wrap.style.background='#ffffff';" +
                        " wrap.style.wordBreak='break-word';" +
                        " var h=document.createElement('h1');" +
                        " h.innerText=document.title||'';" +
                        " h.style.fontSize='28px';" +
                        " h.style.lineHeight='1.4';" +
                        " h.style.margin='0 0 18px';" +
                        " wrap.appendChild(h);" +
                        " var content=document.createElement('div');" +
                        " content.innerHTML=main.innerHTML;" +
                        " wrap.appendChild(content);" +
                        " document.body.style.background='#f6f7fb';" +
                        " document.body.appendChild(wrap);" +
                        "}catch(e){}" +
                        "})();";

        MAIN.post(() -> webView.evaluateJavascript(js, value -> {
            if (callback != null) callback.onComplete();
        }));
    }
}