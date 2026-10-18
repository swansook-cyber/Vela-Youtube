package com.google.android.youtube.pro;

import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.widget.Button;

// Import the separated components
import com.google.android.youtube.pro.webview.YTProWebView;
import com.google.android.youtube.pro.webview.YTProWebViewClient;
import com.google.android.youtube.pro.webview.YTProWebChromeClient;
import com.google.android.youtube.pro.webview.WebAppInterface;
import com.google.android.youtube.pro.webview.BinaryStreamManager;
import android.view.MotionEvent;
import android.os.SystemClock;

import com.google.android.youtube.pro.receivers.MediaCommandReceiver;

public class MainActivity extends Activity {

    public boolean portrait = false;
    public boolean isPlaying = false;
    public boolean mediaSession = false;
    public boolean isPip = false;
    public boolean dL = false;

    private YTProWebView web;
    private MediaCommandReceiver broadcastReceiver;
    private OnBackInvokedCallback backCallback;
    public BinaryStreamManager streamManager;
    
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        SharedPreferences prefs = getSharedPreferences("YTPRO", MODE_PRIVATE);
        if (!prefs.contains("bgplay")) {
            prefs.edit().putBoolean("bgplay", true).apply();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        load(false);
    }

    public void load(boolean dl) {
              
        
        this.dL = dl;
        web = findViewById(R.id.web);
        // Let Android TV D-pad, keyboard and accessibility services focus WebView.
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        web.requestFocus();
        
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setSupportZoom(true);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false); 
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(web, true);
        }

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();
        String url = "https://m.youtube.com/";
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            url = data.toString();
        } else if (Intent.ACTION_SEND.equals(action)) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null && (sharedText.contains("youtube.com") || sharedText.contains("youtu.be"))) {
                url = sharedText;
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }


        web.addJavascriptInterface(new WebAppInterface(this, web), "Android");
        web.setWebChromeClient(new YTProWebChromeClient(this, web));
        web.setWebViewClient(new YTProWebViewClient(this, web));
        
        web.loadUrl(url);

        setupReceiver();
        setupBackNavigation();
        streamManager = new BinaryStreamManager(web,this);
        
        
    }
         

   

    private void setupReceiver() {
        broadcastReceiver = new MediaCommandReceiver(web);
        if (Build.VERSION.SDK_INT >= 34 && getApplicationInfo().targetSdkVersion >= 34) {
            registerReceiver(broadcastReceiver, new IntentFilter("TRACKS_TRACKS"), RECEIVER_EXPORTED);
        } else {
            registerReceiver(broadcastReceiver, new IntentFilter("TRACKS_TRACKS"));
        }
    }

    private void setupBackNavigation() {
        if (Build.VERSION.SDK_INT >= 33) {
            OnBackInvokedDispatcher dispatcher = getOnBackInvokedDispatcher();
            backCallback = new OnBackInvokedCallback() {
                @Override
                public void onBackInvoked() {
                    handleBackPress();
                }
            };
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
        }
    }

    // TV remote navigation for the mobile YouTube WebView.
    // Only active on TV; phone/tablet touch behavior remains unchanged.
    private boolean isTelevision() {
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_TYPE_MASK)
                == Configuration.UI_MODE_TYPE_TELEVISION
                || getPackageManager().hasSystemFeature("android.software.leanback");
    }

    @Override
    public boolean dispatchKeyEvent(android.view.KeyEvent event) {
        if (isTelevision() && event.getAction() == android.view.KeyEvent.ACTION_DOWN && web != null) {
            int code = event.getKeyCode();
            String direction = null;
            if (code == android.view.KeyEvent.KEYCODE_DPAD_UP) direction = "up";
            else if (code == android.view.KeyEvent.KEYCODE_DPAD_DOWN) direction = "down";
            else if (code == android.view.KeyEvent.KEYCODE_DPAD_LEFT) direction = "left";
            else if (code == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) direction = "right";
            else if (code == android.view.KeyEvent.KEYCODE_DPAD_CENTER
                    || code == android.view.KeyEvent.KEYCODE_ENTER
                    || code == android.view.KeyEvent.KEYCODE_NUMPAD_ENTER) direction = "select";
            if (direction != null) {
                moveTvFocus(direction);
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void moveTvFocus(String direction) {
        // Select only on-screen interactive elements, skipping hidden/disabled controls.
        // Focus outline is injected locally; no remote scripts or extra permissions.
        String js = "(function(dir){" +
                "var style=document.getElementById('vela-tv-focus-style');" +
                "if(!style){style=document.createElement('style');style.id='vela-tv-focus-style';" +
                "style.textContent='.vela-tv-selected{outline:4px solid #ff4fb8!important;outline-offset:3px!important;box-shadow:0 0 0 3px #161b30!important}';document.head.appendChild(style);}" +
                "var els=Array.from(document.querySelectorAll('a[href],button,[role=button],[role=link],input,[tabindex]'))" +
                ".filter(function(e){var r=e.getBoundingClientRect();var st=getComputedStyle(e);" +
                "return r.width>8&&r.height>8&&r.bottom>0&&r.top<innerHeight&&r.right>0&&r.left<innerWidth&&st.visibility!=='hidden'&&st.display!=='none'&&!e.disabled;});" +
                "var prev=document.querySelector('.vela-tv-selected');" +
                "if(dir==='select'){if(prev){prev.focus();prev.click();}return;}" +
                "if(!els.length)return;" +
                "var origin=prev&&els.includes(prev)?prev:null;" +
                "var a=origin?origin.getBoundingClientRect():null;" +
                "var ax=a?(a.left+a.right)/2:innerWidth/2;var ay=a?(a.top+a.bottom)/2:innerHeight/2;" +
                "var best=null;var score=Infinity;" +
                "els.forEach(function(e){if(e===origin)return;var r=e.getBoundingClientRect();" +
                "var dx=(r.left+r.right)/2-ax,dy=(r.top+r.bottom)/2-ay;" +
                "if(origin&&((dir==='left'&&dx>=-4)||(dir==='right'&&dx<=4)||(dir==='up'&&dy>=-4)||(dir==='down'&&dy<=4)))return;" +
                "var primary=(dir==='left'||dir==='right')?Math.abs(dx):Math.abs(dy);" +
                "var secondary=(dir==='left'||dir==='right')?Math.abs(dy):Math.abs(dx);" +
                "var v=primary+secondary*2;if(v<score){score=v;best=e;}});" +
                "if(!best&&!origin)best=els[0];" +
                "if(best){if(prev)prev.classList.remove('vela-tv-selected');" +
                "best.classList.add('vela-tv-selected');best.focus({preventScroll:true});" +
                "best.scrollIntoView({block:'nearest',inline:'nearest'});}" +
                "})('" + direction + "');";
        web.evaluateJavascript(js, null);
    }

    private void handleBackPress() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        handleBackPress();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                web.loadUrl("https://m.youtube.com");
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.grant_mic), Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(getApplicationContext(), getString(R.string.grant_storage), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        isPip = isInPictureInPictureMode;
        web.evaluateJavascript(isInPictureInPictureMode ? "PIPlayer();" : "removePIP();", null);
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= 26 && web.getUrl() != null && (web.getUrl().contains("watch") || web.getUrl().contains("shorts"))) {
            if (isPlaying) {
                try {

         /**Webview requires gesture for FullScreenView which is used for PiP mode , this does that ig.*/
         web.loadUrl("javascript:showCurtain();");
         long downTime = SystemClock.uptimeMillis();
         long eventTime = downTime;

         float x = web.getWidth() / 2f;
         float y = 10f; 
         MotionEvent down = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0);
         web.dispatchTouchEvent(down);

         eventTime += 15; 
         MotionEvent up = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_UP, x, y, 0);
         web.dispatchTouchEvent(up);
         down.recycle();
         up.recycle();
         web.loadUrl( "javascript:hideCurtain();");
                 
                    PictureInPictureParams params = new PictureInPictureParams.Builder()
                            .setAspectRatio(new Rational(portrait ? 9 : 16, portrait ? 16 : 9))
                            .build();
                    enterPictureInPictureMode(params);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(new Intent(getApplicationContext(), ForegroundService.class));
        if (broadcastReceiver != null) unregisterReceiver(broadcastReceiver);
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null) {
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        }
        if (streamManager != null) {
            streamManager.cleanup();
        }
    }
}
