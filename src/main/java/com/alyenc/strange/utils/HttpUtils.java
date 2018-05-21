package com.alyenc.strange.utils;


import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class HttpUtils {

    private static HttpUtils instance = new HttpUtils();
    private static HttpClient client;
    private static long startTime = System.currentTimeMillis();
    private  static PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();

    private static ConnectionKeepAliveStrategy keepAliveStrat;

    static {
        keepAliveStrat = new DefaultConnectionKeepAliveStrategy() {
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                long keepAlive = super.getKeepAliveDuration(response, context);
                if (keepAlive == -1) {
                    keepAlive = 5000;
                }
                return keepAlive;
            }
        };
    }

    private HttpUtils() {
        client = HttpClients.custom().setConnectionManager(cm).setKeepAliveStrategy(keepAliveStrat).build();
    }

    private static void IdleConnectionMonitor(){
        if(System.currentTimeMillis() - startTime > 30000){
            startTime = System.currentTimeMillis();
            cm.closeExpiredConnections();
            cm.closeIdleConnections(30, TimeUnit.SECONDS);
        }
    }

    private static RequestConfig requestConfig = RequestConfig.custom()
            .setSocketTimeout(20000)
            .setConnectTimeout(20000)
            .setConnectionRequestTimeout(20000)
            .build();


    public static HttpUtils getInstance() {
        return instance;
    }

    public HttpClient getHttpClient() {
        return client;
    }

    private HttpPost httpPostMethod(String url) {
        return new HttpPost(url);
    }

    private HttpRequestBase httpGetMethod(String url) {
        return new HttpGet(url);
    }

    public String get(String url) throws HttpException, IOException{
        return get(url,  null);
    }

    public String get(String urlPrefix, String url, String param, Map<String, String> headers) throws HttpException, IOException{
        IdleConnectionMonitor();
        url = urlPrefix + url;
        if(param!=null && !param.equals("")){
            if(url.endsWith("?")){
                url = url+param;
            }else{
                url = url+"?"+param;
            }
        }
        return get(url, headers);
    }

    public String get(String url, Map<String, String> headers) throws HttpException, IOException {
        HttpRequestBase method = this.httpGetMethod(url);

        if(headers != null){
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                method.setHeader(key, value);
            }
        }

        method.setConfig(requestConfig);
        HttpResponse response = client.execute(method);
        HttpEntity entity =  response.getEntity();
        if(entity == null){
            return "";
        }
        InputStream is = null;
        String responseData = "";
        try{
            is = entity.getContent();
            responseData = IOUtils.toString(is, "UTF-8");
        }finally{
            if(is!=null){
                is.close();
            }
        }
        return responseData;
    }

    public String post(String url, Map<String,String> params) throws HttpException, IOException {
        return post(url, "", "", null);
    }

    public String post(String urlPrefix, String url, Map<String,String> params, Map<String,String> headers) throws HttpException, IOException{
        IdleConnectionMonitor();
        url = urlPrefix + url;

        HttpPost method = this.httpPostMethod(url);
        List<NameValuePair> valuePairs = this.convertMap2PostParams(params);

        UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(valuePairs, Consts.UTF_8);
        method.setEntity(urlEncodedFormEntity);
        method.setConfig(requestConfig);
        System.out.println(method);

        if(headers != null){
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                method.setHeader(key, value);
            }
        }

        HttpResponse response = client.execute(method);
        HttpEntity entity =  response.getEntity();
        if(entity == null){
            return "";
        }
        InputStream is = null;
        String responseData = "";
        try{
            is = entity.getContent();
            responseData = IOUtils.toString(is, "UTF-8");
        }finally{
            if(is!=null){
                is.close();
            }
        }
        return responseData;
    }

    public String post(String urlPrefix, String url, String param, Map<String, String> headers) throws HttpException, IOException{
        IdleConnectionMonitor();
        if(param != null && !param.equals("")){
            if(url.endsWith("?")){
                url = url + param;
            }else{
                url = url + "?" + param;
            }
        }

        String fullUrl = urlPrefix + url;
        HttpPost method = this.httpPostMethod(fullUrl);

        if(headers != null){
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                method.setHeader(key, value);
            }
        }

        StringEntity reqEntity = new StringEntity(param);
        // 设置类型
        reqEntity.setContentType("application/x-www-form-urlencoded");
        // 设置请求的数据
        method.setEntity(reqEntity);

        HttpResponse response = client.execute(method);
        HttpEntity entity =  response.getEntity();
        if(entity == null){
            return "";
        }
        InputStream is = null;
        String responseData = "";
        try{
            is = entity.getContent();
            responseData = IOUtils.toString(is, "UTF-8");
        }finally{
            if(is!=null){
                is.close();
            }
        }
        return responseData;
    }

    private List<NameValuePair> convertMap2PostParams(Map<String,String> params){
        List<String> keys = new ArrayList<String>(params.keySet());
        if(keys.isEmpty()){
            return null;
        }
        int keySize = keys.size();
        List<NameValuePair> data = new LinkedList<NameValuePair>() ;
        for(int i=0;i<keySize;i++){
            String key = keys.get(i);
            String value = params.get(key);
            data.add(new BasicNameValuePair(key,value));
        }
        return data;
    }
}
