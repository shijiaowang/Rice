package com.cvsong.study.library.net.httpservice;

/**
 * Http常量管理类
 * Created by chenweisong on 2017/10/10.
 */

public class HttpConstants {

    /*********************网络请求配置***********************/

    /*加解密开关*/
    public static boolean RSA_SWITCH = true;
    public static final String BASE_URL = "176.28";
    public static final String URL_PORT = ":8080";



    /**
     * 15秒
     */
    public static final long CONNECT_TIME_OUT = 5 * 3L;
    /**
     * 15秒
     */
    public static final long WRITE_TIME_OUT = 5 * 3L;
    /**
     * 15秒
     */
    public static final long READ_TIME_OUT = 5 * 3L;


    /*********************请求结果码***********************/
    //请求成功
    public static final String RESULT_STATUS_OK = "0";

    //调用失败
    public static final String RESULT_STATUS_FAILE = "1";

    //调用异常
    public static final String RESULT_STATUS_EXCEPTION = "2";

    /* -3token失效 -2异常 -1失败 0成功*/
    //成功
    public static final String RESULT_CODE_OK = "0";

    //token失效
    public static final String RESULT_CODE_TOKEN_INVALIDATION = "-3";

    //异常
    public static final String RESULT_CODE_EXCEPTION = "-2";
    //失败
    public static final String RESULT_CODE_FAILE = "-1";


}
