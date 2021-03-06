package hk.ypw.instabtbu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.imid.swipebacklayout.lib.app.SwipeBackActivity;

@SuppressLint({"SetJavaScriptEnabled", "HandlerLeak"})
public class Guancang_web extends SwipeBackActivity {
    static String urlString = "";
    Activity thisActivity = this;
    String loadurlString = "";
    String jsString = "";
    ArrayList<String> beizhu = new ArrayList<>();
    ArrayList<String> bumen = new ArrayList<>();
    String addPlace = "";
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                if (msg.what == 1) {
                    WebView webview = (WebView) findViewById(R.id.guancang_webview);
                    if (jsString.length() != 0)
                        webview.loadUrl(jsString);
                    if (addPlace.length() != 0)
                        webview.loadUrl(addPlace);
                    jsString = "";
                    addPlace = "";
                }
            } catch (Exception ignored) {

            }
        }
    };
    Runnable addimgRunnable = new Runnable() {
        @Override
        public void run() {
            String result;
            result = Common.commonGET(loadurlString);
            try {
                String isbnString = zhongjian(result, "ISBN/价格：</strong>", ":");
                System.out.println("isbn=" + isbnString);
                result = Common.SSLGET("https://api.douban.com/v2/book/isbn/"
                        + isbnString);

                String srcString = zhongjian(result, "large\":\"", "\"");
                srcString = srcString.replace("\\", "");
                String javascriptString;
                if ((srcString.indexOf("book-default") > 0)
                        | (srcString.length() < 5)) {
                    srcString = "http://img3.douban.com/pics/book-default-medium.gif";
                    javascriptString = "javascript:var aEle=document.body.getElementsByTagName('strong');"
                            + "var strong=aEle[aEle.length-1];"
                            + "var s=document.createElement('img');"
                            + "var br=document.createElement('br');"
                            + "s.src='"
                            + srcString
                            + "';"
                            + "strong.appendChild(br);"
                            + "strong.appendChild(s);";
                } else {
                    javascriptString = "javascript:var aEle=document.body.getElementsByTagName('strong');"
                            + "var strong=aEle[aEle.length-1];"
                            + "var s=document.createElement('img');"
                            + "var br=document.createElement('br');"
                            + "s.src='"
                            + srcString
                            + "';"
                            + "s.style.cssText=\"width: 50%;\";"
                            + "strong.appendChild(br);"
                            + "strong.appendChild(s);";
                }
                jsString = javascriptString;
                // 从豆瓣的API获取图片信息
            } catch (Exception ignored) {
            }

            try {
                String bookid = zhongjian(loadurlString, "book%2f", "&");
                String detailUrl = "http://libopac.btbu.edu.cn:8080/opac/book/getHoldingsInformation/"
                        + bookid;
                result = Common.commonGET(detailUrl);
                result = result.replace("\"", "");
                Pattern pattern = Pattern.compile("备注:(.+?),部门名称:(.+?),");
                Matcher matcher = pattern.matcher(result);
                beizhu.clear();
                bumen.clear();
                while (matcher.find()) {
                    beizhu.add(matcher.group(1));
                    bumen.add(matcher.group(2));
                }
                String bumenString = "";
                String beizhuString = "";
                for (int i = 0; i < bumen.size(); i++) {
                    if (!bumenString.contains(bumen.get(i))) {
                        bumenString += bumen.get(i) + " ";
                        beizhuString += beizhu.get(i) + " ";
                    }
                }
                addPlace = "javascript:var bumen=\""
                        + bumenString
                        + "\".split(\" \");"
                        + "beizhu=\""
                        + beizhuString
                        + "\".split(\" \");"
                        + "for(aEle=document.body.getElementsByTagName(\"td\"),i=aEle.length-1;0<=i;i--)"
                        + "for(var j=bumen.length-1;0<=j;j--)"
                        + "if(-1!=aEle[i].innerHTML.indexOf(bumen[j]))"
                        + "{var s=document.createElement(\"strong\");s.innerHTML=\"  \"+beizhu[j];aEle[i].appendChild(s);};";
            } catch (Exception ignored) {
            }
            Message message = new Message();
            message.what = 1;
            handler.sendMessage(message);
        }
    };
    private ExecutorService executorService = Executors.newCachedThreadPool();// 线程池

    public static String zhongjian(String text, String textl, String textr) {
        return zhongjian(text, textl, textr, 0);
    }

    public static String zhongjian(String text, String textl, String textr,
                                   int start) {
        try {
            int left = text.indexOf(textl, start);
            int right = text.indexOf(textr, left + textl.length());
            return text.substring(left + textl.length(), right);
        } catch (Exception e) {
            System.out.println("zhongjian:error:" + e);
            return "";
        }
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guancang_web);

        WebView webview = (WebView) findViewById(R.id.guancang_webview);
        // 设置WebView属性，能够执行Javascript脚本
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setWebViewClient(new MyWebViewClient());
        webview.loadUrl(urlString);
        if (urlString.indexOf("isbnstr") > 0) {
            loadurlString = urlString;
            executorService.submit(addimgRunnable);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        WebView webview = (WebView) findViewById(R.id.guancang_webview);
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webview.canGoBack()) {
            webview.goBack(); // goBack()表示返回WebView的上一页面
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }



    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.indexOf("book") > 0) {
                loadurlString = url;
                executorService.submit(addimgRunnable);
            }
            if (!url.contains("marc_no"))
                view.loadUrl(url);
            return true;
        }

    }

}
