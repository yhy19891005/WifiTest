package com.yhy.wifitest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int DEFAULT_TIMEOUT = 60;
    private ListView mLvConnectTime;
    private CAdapter mAdapter;
    private TextView mTvStatus;

    //权限请求码
    private static final int PERMISSION_REQUEST_CODE = 200;

    private DecimalFormat mFormat = new DecimalFormat("0.0000");
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initPermission();
    }

    private void initView() {
        ActionBar bar = getSupportActionBar();
        if(bar != null){
            bar.hide();
        }

        mTvStatus = findViewById(R.id.tv_status);
        mLvConnectTime = findViewById(R.id.lv_connect_time);
        mAdapter = new CAdapter(this);
        mLvConnectTime.setAdapter(mAdapter);
    }

    private void initPermission() {
       if(!hasPermission()){
           requestPermission();
       }else {
           doHasPermission();
       }
    }

    /**
     * 检查是否已经授予权限
     * @return
     */
    private boolean hasPermission() {
        for (String permission : NEEDED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 申请权限
     */
    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                NEEDED_PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    private void doHasPermission() {
        if(WifiSupport.isOpenWifi(MainActivity.this)){
            if(WifiSupport.isNetworkConnected(MainActivity.this)){
                if(WifiSupport.isWifiCanuse(MainActivity.this)){
                    startTime = System.currentTimeMillis();
                    mTvStatus.setVisibility(View.VISIBLE);
                    mTvStatus.setText("正在发送请求...");
                    sendRequest();
                }else {
                    Toast.makeText(MainActivity.this,"当前热点不可用", Toast.LENGTH_SHORT).show();
                }
            }else {
                connectWifi();
            }
        }else {
            Toast.makeText(MainActivity.this,"WIFI处于关闭状态", Toast.LENGTH_SHORT).show();
        }
    }

    private void connectWifi() {
        mTvStatus.setVisibility(View.VISIBLE);
        mTvStatus.setText("正在连接WIFI...");
        startTime = System.currentTimeMillis();
        List<ScanResult> wifiScanResult = WifiSupport.getWifiScanResult(MainActivity.this);
        Log.e(TAG,"size = " + wifiScanResult.size());
        if(WifiSupport.containName(wifiScanResult,"TestSu")){
            WifiConfiguration tempConfig  = WifiSupport.isExsits("TestSu",MainActivity.this);
            if(tempConfig == null){
                Log.e(TAG,"bbbbbbbbbbbbbbbbbbbb");
                WifiConfiguration wifiConfiguration =  WifiSupport.createWifiConfig("TestSu","zxcvbnma",WifiSupport.WifiCipherType.WIFICIPHER_WPA);
                boolean b = WifiSupport.addNetWork(wifiConfiguration, MainActivity.this);
                if(b){
                    if(WifiSupport.isWifiCanuse(MainActivity.this)){
                        mHandler.sendEmptyMessageDelayed(REQUEST_START,5000);
                    }else {
                        Toast.makeText(MainActivity.this,"当前热点不可用", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(MainActivity.this,"WIFI连接失败", Toast.LENGTH_SHORT).show();
                }
            }else{
                Log.e(TAG,"ccccccccccccccccccccc");
                boolean b = WifiSupport.addNetWork(tempConfig, MainActivity.this);
                if(b){
                    Log.e(TAG,"WIFI连接成功");
                    if(WifiSupport.isWifiCanuse(MainActivity.this)){
                        mHandler.sendEmptyMessageDelayed(REQUEST_START,5000);
                    }else {
                        Toast.makeText(MainActivity.this,"当前热点不可用", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(MainActivity.this,"WIFI连接失败", Toast.LENGTH_SHORT).show();
                }
            }
        }else {
            Toast.makeText(MainActivity.this,"抱歉，没有扫描到指定热点", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendRequest() {
        mTvStatus.setVisibility(View.VISIBLE);
        mTvStatus.setText("正在发送请求...");
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        clientBuilder.connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true);
        OkHttpClient okHttpClient = clientBuilder.build();

        String url = "http://wwww.baidu.com";

        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "onFailure: " + e.toString());
                mHandler.sendEmptyMessage(REQUEST_FAIL);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.e(TAG, "onResponse: " + response.body().string());
                mHandler.sendEmptyMessage(REQUEST_SUC);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_REQUEST_CODE){
            boolean allowPermission = true;
            for (int result : grantResults) {
                if(result != PackageManager.PERMISSION_GRANTED){
                    allowPermission = false;
                    break;
                }
            }

            if(allowPermission){
                doHasPermission();
            }else {
                Toast.makeText(MainActivity.this,"获取权限失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static final int REQUEST_SUC = 100;
    private static final int REQUEST_FAIL = 101;
    private static final int REQUEST_START = 102;
    private long startTime = 0,endTime = 0;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what){
                case REQUEST_SUC:
                    doRequestSuc();
                    break;
                case REQUEST_FAIL:
                    doRequestFail();
                    break;
                case REQUEST_START:
                    sendRequest();
                    break;
            }
        }
    };

    private void doRequestSuc() {
        mTvStatus.setVisibility(View.GONE);
        List<ResultBean> dataList = mAdapter.getDataList();
        endTime = System.currentTimeMillis();
        long time = endTime - startTime;
        Log.e(TAG,"endTime = " + endTime + " ,startTime = " + startTime);
        double timed = time / 1000.0;
        ResultBean sucBean = new ResultBean();
        sucBean.setSuc(true);
        sucBean.setRequestTime(mFormat.format(timed));
        mAdapter.addData(sucBean);

        if(dataList.size() > 10){
            mLvConnectTime.setSelection(dataList.size() - 1);
        }

        mTvStatus.setVisibility(View.VISIBLE);
        mTvStatus.setText("正在断开WIFI...");

        boolean b = WifiSupport.disconnectWifi(MainActivity.this);
        if(b){
            connectWifi();
        }else {
            Toast.makeText(MainActivity.this,"断开WIFI失败", Toast.LENGTH_SHORT).show();
        }

    }

    private void doRequestFail() {
        List<ResultBean> dataList = mAdapter.getDataList();
        endTime = System.currentTimeMillis();
        Log.e(TAG,"endTime = " + endTime + " ,startTime = " + startTime);
        long time = endTime - startTime;
        double timed = time / 1000.0;
        ResultBean failBean = new ResultBean();
        failBean.setSuc(false);
        failBean.setRequestTime(mFormat.format(timed));
        mAdapter.addData(failBean);
        if(dataList.size() > 10){
            mLvConnectTime.setSelection(dataList.size() - 1);
        }

        mTvStatus.setVisibility(View.VISIBLE);
        mTvStatus.setText("正在断开WIFI...");

        boolean b = WifiSupport.disconnectWifi(MainActivity.this);
        if(b){
            connectWifi();
        }else {
            mTvStatus.setVisibility(View.GONE);
            Toast.makeText(MainActivity.this,"断开WIFI失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacksAndMessages(null);
    }
}
