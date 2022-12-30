package com.shizq.bika.network.base

import android.net.ParseException
import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.MalformedJsonException
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.disposables.Disposable
import org.apache.http.conn.ConnectTimeoutException
import org.json.JSONException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

abstract class BaseObserver<T> : Observer<BaseResponse<T>> {
    internal object ERROR {
        /**
         * 未知错误
         */
        const val UNKNOWN = 10000

        /**
         * 解析错误
         */
        const val PARSE_ERROR = 10001

        /**
         * 网络错误
         */
        const val NETWORD_ERROR = 10002

        /**
         * 协议出错
         */
        const val HTTP_ERROR = 1003

        /**
         * 证书出错
         */
        const val SSL_ERROR = 10005

        /**
         * 连接超时
         */
        const val TIMEOUT_ERROR = 10006
    }

    override fun onSubscribe(d: Disposable) {}
    override fun onNext(baseResponse: BaseResponse<T>) {
        if (baseResponse.isOk()) {
            onSuccess(baseResponse)
            return
        }

        onCodeError(baseResponse)
    }

    override fun onError(e: Throwable) {
        var baseResponse : BaseResponse<T> = BaseResponse()
        try {
            if (e is HttpException) {   //  处理服务器返回的非成功异常
                val responseBody = e.response()!!.errorBody()
                if (responseBody != null) {
                    val type = object : TypeToken<BaseResponse<T>>() {}.type
                    baseResponse = Gson().fromJson(responseBody.string(), type)

                    onSuccess(baseResponse)
                } else {
                    baseResponse.code=ERROR.HTTP_ERROR
                    onCodeError(baseResponse)
                }
            } else if (e is JsonParseException
                || e is JSONException
                || e is ParseException
                || e is MalformedJsonException
            ) {
                baseResponse.code=ERROR.PARSE_ERROR
                baseResponse.message="解析错误"
                onCodeError(baseResponse)
            } else if (e is ConnectException) {
                baseResponse.code=ERROR.NETWORD_ERROR
                baseResponse.message="连接失败"
                onCodeError(baseResponse)
            } else if (e is ConnectTimeoutException) {
                baseResponse.code=ERROR.TIMEOUT_ERROR
                baseResponse.message="连接超时"
                onCodeError(baseResponse)
            } else if (e is SocketTimeoutException) {
                baseResponse.code=ERROR.TIMEOUT_ERROR
                baseResponse.message="连接超时"
                onCodeError(baseResponse)
            } else if (e is UnknownHostException) {
                baseResponse.code=ERROR.TIMEOUT_ERROR
                baseResponse.message="主机地址未知"
                onCodeError(baseResponse)
            } else if (e is SSLException) {
                baseResponse.code=ERROR.SSL_ERROR
                baseResponse.message="证书验证失败"+e.message
                onCodeError(baseResponse)
            } else {

//                baseResponse.message="未知错误"
                onCodeError(baseResponse)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onComplete() {}

    //请求成功且返回码为200的回调方法，这里抽象方法声明
    abstract fun onSuccess(baseResponse: BaseResponse<T>)

    //请求成功但返回code码不是200的回调方法，这里抽象方法声明
    abstract fun onCodeError(baseResponse: BaseResponse<T>)
    /**
     *
     * 请求失败回调方法，这里抽象方法声明
     * @param isCauseNetReason 是否由于网络造成的失败 true是
     */
    /*
    public abstract void onFailure(Throwable throwable, boolean isCauseNetReason) throws Exception;
    */
}
