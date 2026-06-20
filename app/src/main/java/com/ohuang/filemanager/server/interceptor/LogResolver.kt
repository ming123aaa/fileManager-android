package com.ohuang.filemanager.server.interceptor

import android.util.Log
import com.yanzhenjie.andserver.annotation.Interceptor
import com.yanzhenjie.andserver.annotation.Resolver
import com.yanzhenjie.andserver.framework.ExceptionResolver
import com.yanzhenjie.andserver.http.HttpRequest
import com.yanzhenjie.andserver.http.HttpResponse

@Resolver
@Interceptor
class LogResolver:  ExceptionResolver {



    override fun onResolve(
        request: HttpRequest,
        response: HttpResponse,
        e: Throwable
    ) {

        Log.d("LogResolver",""+request.path+"e="+e.message)
//        Log.d("LogResolver",e.stackTraceToString())


    }


}