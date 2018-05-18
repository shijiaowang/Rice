package com.cvsong.study.library.net.httpservice;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.cvsong.study.library.ndk.JniUtil;
import com.cvsong.study.library.util.GsonUtil;
import com.cvsong.study.library.net.entity.Result;
import com.cvsong.study.library.net.exception.AppHttpException;
import com.cvsong.study.library.net.exception.HttpExceptionConstant;
import com.cvsong.study.library.net.interfaces.IHttpRequest;
import com.cvsong.study.library.net.interfaces.IHttpResponseCallBack;
import com.cvsong.study.library.net.interfaces.IHttpUrlManage;
import com.cvsong.study.library.util.RSAUtil;
import com.cvsong.study.library.util.app_tools.AppSpUtils;
import com.cvsong.study.library.util.utilcode.util.LogUtils;


import com.cvsong.study.library.util.utilcode.util.Utils;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * OkHttp网络请求管理类
 * Created by chenweisong on 2017/10/1.
 */

public class OkHttpRequestManage implements IHttpRequest {

    private static final String TAG = OkHttpRequestManage.class.getSimpleName();

    //登录令牌
    private static final String ACCESSTOKEN = "token";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    private static Handler handler;

    private volatile static OkHttpRequestManage okHttpRequestManage;
    public final OkHttpClient okHttpClient;

    private OkHttpRequestManage() {

        handler = new Handler(Looper.getMainLooper());
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(HttpConstants.CONNECT_TIME_OUT, TimeUnit.SECONDS)
                .writeTimeout(HttpConstants.WRITE_TIME_OUT, TimeUnit.SECONDS)
                .readTimeout(HttpConstants.READ_TIME_OUT, TimeUnit.SECONDS)
                .build();

    }

    public static OkHttpRequestManage getInstance() {

        if (okHttpRequestManage == null) {
            synchronized (HttpRequestUtil.class) {
                if (okHttpRequestManage == null) {
                    okHttpRequestManage = new OkHttpRequestManage();
                }

            }

        }
        return okHttpRequestManage;
    }


    /**
     * 同步Pos请求
     *
     * @param httpUrlManage 网络请求Url对象
     * @param object        请求体
     * @param clazz         响应体
     * @param callBack      回调
     */
    @Override
    public void postSynRequest(final Activity activity, final IHttpUrlManage httpUrlManage, final Object object, final Class clazz, final IHttpResponseCallBack callBack) {
        //请求前校验
        if (checkBeforeRequest(activity, httpUrlManage, callBack)) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Request request = buildPostRequest(httpUrlManage, object, httpUrlManage.getCreditJSON(), callBack);
                    final Response response = okHttpClient.newCall(request).execute();
                    if (activity == null || activity.isFinishing()) {
                        return;
                    }
                    handleSuccessResponse(request, response, callBack, httpUrlManage, clazz);

                } catch (IOException e) {
                    handleFailResponse(null, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_RESPONSE, e.getMessage()), callBack);
                }

            }
        }).start();


    }


    /**
     * 异步Pos请求
     *
     * @param httpUrlManage 网络请求Url对象
     * @param object        请求体
     * @param clazz         响应体
     * @param callBack      回调
     */
    @Override
    public void postAsynRequest(final Activity activity, final IHttpUrlManage httpUrlManage, Object object, final Class clazz, final IHttpResponseCallBack callBack) {
        //请求前校验
        if (checkBeforeRequest(activity, httpUrlManage, callBack)) return;
        //构建Post请求体
        final Request request = buildPostRequest(httpUrlManage, object, httpUrlManage.getCreditJSON(), callBack);
        if (request == null) {
            return;
        }
        okHttpClient.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(final Call call, final IOException e) {
                        if (activity == null || activity.isFinishing()) {
                            return;
                        }
                        handleFailResponse(call.request(), new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_RESPONSE, e.getMessage()), callBack);//处理失败结果
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        if (activity == null || activity.isFinishing()) {
                            return;
                        }
                        handleSuccessResponse(request, response, callBack, httpUrlManage, clazz);
                    }
                });
    }

    /**
     * 异步Get请求
     *
     * @param activity
     * @param httpUrlManage
     * @param object
     * @param clazz
     * @param callBack
     */
    @Override
    public void getAsynRequest(final Activity activity, final IHttpUrlManage httpUrlManage, Object object, final Class clazz, final IHttpResponseCallBack callBack) {
        if (activity == null || activity.isFinishing()) {
            return;
        }

        if (callBack == null) {
            throw new NullPointerException("网络请求回调不能为空");
        }

        if (httpUrlManage == null) {
            throw new NullPointerException("网络请求URL管理不能为空");
        }

        StringBuffer sb = new StringBuffer(httpUrlManage.getUrl());
        try {
            if (null != object) {
                Field[] fields = object.getClass().getFields();
                String str = HttpRequestHelper.fieldArray2Str(object, fields);
                sb.append(str);
            }

        } catch (IllegalAccessException e) {
            handleFailResponse(null, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_REQUEST, "网络请求对象字段反射失败"), callBack);
            return;
        }

        final String httpUrl = sb.toString();
        LogUtils.e(TAG, "网络请求Url以及参数:" + httpUrl);
        //请求的http协议
        final Request request = new Request.Builder().url(httpUrl).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(final Call call, final IOException e) {
                if (activity == null || activity.isFinishing()) {
                    return;
                }
                //处理失败结果
                handleFailResponse(call.request(), new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_RESPONSE, e.getMessage()), callBack);
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (activity == null || activity.isFinishing()) {
                    return;
                }
                handleSuccessResponse(request, response, callBack, httpUrlManage, clazz);
            }
        });

    }

    /**
     * 请求前校验
     */
    private boolean checkBeforeRequest(Activity activity, IHttpUrlManage httpUrlManage, IHttpResponseCallBack callBack) {
        if (activity == null || activity.isFinishing()) {
            return true;
        }

        if (callBack == null) {
            throw new NullPointerException("网络请求回调不能为空");
        }

        if (httpUrlManage == null) {
            throw new NullPointerException("网络请求URL管理不能为空");
        }

        if (httpUrlManage.isNeedCache()) {//是否需要缓存
            //TODO
//                Object result = LocalCacheHandler.getDefaultCacheHandler().read(httpUrlManage.getUrl());
//                if (result != null) {
//                    callBack.onSuccess(null, result);
//                }
        }

        // 网络连接检查
        if (!HttpRequestHelper.isNetworkConnected()) {
            callBack.onFailure(null, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_NET_ERROR, "网络连接异常"));
            return true;
        }

        //网络请求URL是否为空检查
        if (TextUtils.isEmpty(httpUrlManage.getUrl())) {
            callBack.onFailure(null, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_REQUEST, "网络请求URL为空"));
            return true;
        }
        return false;
    }


    /****************************************GET请求***********************************************/

//    /**
//     * 同步Get请求
//     *
//     * @param activity
//     * @param httpUrlManage
//     * @param object
//     * @param clazz
//     * @param callBack
//     */
//    @Override
//    public void getSynRequest(Activity activity, IHttpUrlManage httpUrlManage, Object object, Class clazz, IHttpResponseCallBack callBack) {
//
//    }

    /**
     * 构建Post请求体
     */

    private static Request buildPostRequest(final IHttpUrlManage httpUrlManage, Object object, boolean json, IHttpResponseCallBack callBack) {

        String jsonStr = object == null ? "" : GsonUtil.GsonString(object);
        final String httpUrl = httpUrlManage.getUrl();
        boolean userIsLogin = AppSpUtils.getInstance().getBoolean(AppSpUtils.IS_USER_LOGINED);
        String token = AppSpUtils.getInstance().getString(AppSpUtils.ACCESS_TOKEN);

        LogUtils.e(TAG, "用户是否已登录:" + userIsLogin);
        LogUtils.e(TAG, "token:" + token);
        LogUtils.e(TAG, "网络请求URL:" + httpUrl);
        LogUtils.e(TAG, "网络请求Json串:" + jsonStr);

        //添加特殊处理 针对特定使用的JSON对象传递
        RequestBody body = null;
        if (json) {

            try {
                if (HttpConstants.RSA_SWITCH) {//加解密开关
                    String rsaKey = new JniUtil().getRsaKey(Utils.getApp());
                    byte[] reqStr = RSAUtil.encryptByPrivateKey(jsonStr.getBytes("utf-8"), rsaKey);
                    String req = RSAUtil.encodeBase64ToString(reqStr);
                    body = RequestBody.create(JSON, req);
                } else {
                    body = RequestBody.create(JSON, jsonStr);
                }

            } catch (Exception e) {
                if (e instanceof IOException) {
                    handleFailResponse(null, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_REQUEST, "IO异常"), callBack);
                } else {
                    handleFailResponse(null, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_REQUEST, "加密异常"), callBack);
                }
                return null;
            }


        } else {
            Map<String, String> map = new HashMap();
            map.put("jsonStr", jsonStr);
            FormBody.Builder builder = new FormBody.Builder();
            Set<Map.Entry<String, String>> entries = map.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                builder.add(entry.getKey(), entry.getValue());
            }

            body = builder.build();
        }

        return new Request.Builder()
                .addHeader(ACCESSTOKEN, token)//请求头中添加token
                .url(httpUrlManage.getUrl())
                .post(body)
                .build();
    }

    /**
     * 对成功请求结果进行处理
     */
    private void handleSuccessResponse(final Request request, final Response response, final IHttpResponseCallBack callBack, final IHttpUrlManage httpUrlManage, Class clazz) {

        if (!response.isSuccessful()) {//校验是否成功
            handleFailResponse(request, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_RESPONSE, "http状态码" + response.code()), callBack);
            return;
        }

        if (response.body().contentLength() == 0) {
            handleFailResponse(request, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_RESPONSE, "数据转换异常"), callBack);
            return;
        }


        String token = response.headers().get(ACCESSTOKEN);
        if (token != null) {//TODO 从响应头中获取token并保存
            AppSpUtils.getInstance().put(AppSpUtils.ACCESS_TOKEN, token);
        }

        try {
            //对成功结果进行处理
            String resStr = response.body().string();
            if (TextUtils.isEmpty(resStr)) {
                handleFailResponse(request, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_RESPONSE, "请求结果为空"), callBack);
                return;
            }
            String result;
            if (HttpConstants.RSA_SWITCH) {//加解密开关
                //对数据进行解密
                String rsaKey = new JniUtil().getRsaKey(Utils.getApp());
                result = RSAUtil.decryptByPrivateKey(resStr, rsaKey);//内部已做Base64转换
            } else {
                result = resStr;
            }
            LogUtils.e(TAG, "Url类型：" + httpUrlManage.getUrlType() + "，网络请求返回数据:" + result);
            //解析最外层数据
            final Result res = GsonUtil.GsonToBean(result, Result.class);

            final Object obj;

            //把Object对象转换成json字符串然后在根据对象进行重新转换
            String retValueString = GsonUtil.GsonString(res.getData());
            if (!TextUtils.isEmpty(retValueString) && !((clazz == String.class) || (clazz == Object.class))) {
                obj = GsonUtil.GsonToBean(retValueString, clazz);
            } else {
                obj = retValueString;
            }
            if (res == null) {
                handleFailResponse(request, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_RESPONSE, "请求结果外层数据异常"), callBack);
                return;
            }
            handler.post(new Runnable() {
                @Override
                public void run() {

                    switch (res.getStatus()) {

                        case HttpConstants.RESULT_STATUS_OK://请求成功

                            switch (res.getCode()) {
                                case HttpConstants.RESULT_CODE_TOKEN_INVALIDATION://token失效
                                    callBack.onTokenInvalidation();
                                    break;

                                case HttpConstants.RESULT_CODE_OK:

                                    if (httpUrlManage.isNeedCache()) {//是否需要缓存
                                        //TODO 缓存处理
                                        //  ACache.get(AppUtils.getContext()).put(httpUrlManage.getUrl(),result);
                                    }
                                    callBack.onSuccess(res, obj);
                                    break;
                                default:
                                    callBack.onFailure(request, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_RESPONSE, res.getMessage()));
                                    break;

                            }
                            break;

                        default:
                            handleFailResponse(request, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_RESPONSE, res.getMessage()), callBack);
                            break;
                    }
                }
            });
        } catch (Exception e) {
            handleFailResponse(request, e, callBack);
        }
    }


    /**
     * 处理失败结果
     */
    private static void handleFailResponse(final Request request, final Exception e, final IHttpResponseCallBack callBack) {
        if (e != null) {
            LogUtils.e(TAG, e.getMessage());
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (e instanceof SocketTimeoutException) {
                    callBack.onFailure(request, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_RESPONSE, "服务器响应超时"));
                } else if (e instanceof JsonSyntaxException) {
                    callBack.onFailure(request, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_RESPONSE, "JSON转Bean解析异常"));
                } else if (e instanceof JsonIOException) {
                    callBack.onFailure(request, new AppHttpException(HttpExceptionConstant.HTTP_EXCEPTION_RESPONSE, "Bean转JSON异常"));
                } else {
                    callBack.onFailure(request, e);
                }

            }
        });
    }
}