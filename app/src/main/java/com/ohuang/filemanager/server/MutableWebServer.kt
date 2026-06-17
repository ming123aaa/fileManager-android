package com.ohuang.filemanager.server


import android.content.Context
import com.yanzhenjie.andserver.ComponentRegister
import com.yanzhenjie.andserver.DispatcherHandler
import com.yanzhenjie.andserver.framework.HandlerInterceptor
import com.yanzhenjie.andserver.framework.handler.HandlerAdapter
import org.apache.httpcore.protocol.HttpRequestHandler
import java.util.LinkedList

/**
 * 可变web
 */
class MutableWebServer(builder: Builder) :
    NormalServer<MutableWebServer.Builder>(builder) {

    companion object {

        fun builder(context: Context, group: String = "default"): Builder {
            return Builder(context, group)
        }
    }


    private var mContext: Context = builder.context
    private var mGroup: String = builder.group

    private val mAdapterList: List<HandlerAdapter> = builder.getAdapters()
    private val mInterceptorList: List<HandlerInterceptor> = builder.getInterceptors()

    override fun requestHandler(): HttpRequestHandler {
        val handler = DispatcherHandler(mContext)
        val register = ComponentRegister(mContext)
        try {
            mAdapterList.forEach {
                handler.addAdapter(it)
            }
            mInterceptorList.forEach {
                handler.addInterceptor(it)
            }

            register.register(handler, mGroup)
        } catch (e: InstantiationException) {
            throw RuntimeException(e)
        } catch (e: IllegalAccessException) {
            throw RuntimeException(e)
        }
        return handler
    }


    class Builder(val context: Context, val group: String) :
        NormalServer.Builder<Builder, MutableWebServer>() {

        private val mAdapterList: MutableList<HandlerAdapter> = LinkedList<HandlerAdapter>()
        private val mInterceptorList: MutableList<HandlerInterceptor> =
            LinkedList<HandlerInterceptor>()

        fun getAdapters() = mAdapterList
        fun getInterceptors() = mInterceptorList

        fun addAdapter(adapter: HandlerAdapter): Builder = apply {
            mAdapterList.add(adapter)
        }

        fun addInterceptor(interceptor: HandlerInterceptor): Builder = apply {
            mInterceptorList.add(interceptor)
        }

        override fun build(): MutableWebServer {
            return MutableWebServer(this)
        }
    }


}





