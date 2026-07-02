package com.ohuang.filemanager.util

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.withSave
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt


private const val TAG = "GlideImage"

@Composable
fun ImageGlide(
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    url: String,
    isPlayGif: Boolean=true,
) {
    ImageGlide(
        modifier = modifier,
        contentDescription = contentDescription,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        key = url,
        isPlayGif=isPlayGif,
        builder = {
            load(url)
        }
    )
}

@Composable
fun ImageGlide(
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    id: Int,
    isPlayGif: Boolean=true
) {
    ImageGlide(
        modifier = modifier,
        contentDescription = contentDescription,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter,
        key = id,
        isPlayGif = isPlayGif,
        builder = {
            load(id)
        }
    )
}

@Composable
fun ImageGlide(
    isPlayGif: Boolean,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null,
    key: Any? = null,
    builder: RequestBuilder<Drawable>.() -> RequestBuilder<Drawable>
) {


    var current = LocalContext.current
    var mPainter: MutableState<Painter> =
        remember(key) { mutableStateOf(BitmapPainter(ImageBitmap(1, 1))) }
    val initDrawable: MutableState<Drawable?> = remember(key) {
        mutableStateOf(null)
    }

    val saveDrawableCallback: MutableState<Drawable.Callback?> = remember {
        mutableStateOf(null)
    }

    val mInitDrawable = initDrawable.value




    DisposableEffect(mInitDrawable,key) {
        onDispose {
            if (mInitDrawable is GifDrawable) {
                mInitDrawable.stop()
                mInitDrawable.callback = null
            }
        }
    }
    LaunchedEffect(mInitDrawable,isPlayGif,key) {
        if (mInitDrawable != null) {
            if ((mInitDrawable is GifDrawable)) {
                if (isPlayGif) {
                    val drawableCallBack =
                        DrawableCallBack(onInvalidate = { mPainter.value = it.toPainter() })
                    mInitDrawable.callback = drawableCallBack //设置的是一个弱引用
                    saveDrawableCallback.value = drawableCallBack //因为设置的是一个弱引用，需要保存一下，否则会出现动图停止的情况

                    if (!mInitDrawable.isRunning) {
                        mInitDrawable.start()
                    }
                }else{
                    mInitDrawable.firstFrame?.let {bitmap ->
                        mPainter.value = BitmapPainter(image = bitmap.asImageBitmap())
                    }

                    mInitDrawable.callback=null
                    if (mInitDrawable.isRunning) {
                        mInitDrawable.stop()
                    }
                }
            } else {
                mPainter.value = mInitDrawable.toPainter()
            }
        }
    }


    LaunchedEffect(key) {
        var glide = builder(
            Glide.with(current)
                .asDrawable()
        )
        withContext(Dispatchers.IO) {
            glide.into<ComposeTarget>(ComposeTarget(initDrawableState = initDrawable))
        }
    }

    Image(
        painter = mPainter.value,
        modifier = modifier,
        contentDescription = contentDescription,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter
    )
}


class ComposeTarget(val initDrawableState: MutableState<Drawable?>) : Target<Drawable> {
    override fun onLoadStarted(placeholder: Drawable?) {

        initDrawableState.value = placeholder
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {

        initDrawableState.value = errorDrawable
    }

    override fun onResourceReady(
        resource: Drawable,
        transition: Transition<in Drawable>?
    ) {

        initDrawableState.value = resource
    }

    override fun onLoadCleared(placeholder: Drawable?) {

    }

    override fun getSize(cb: SizeReadyCallback) {
        cb.onSizeReady(SIZE_ORIGINAL, SIZE_ORIGINAL)
    }

    override fun removeCallback(cb: SizeReadyCallback) {

    }

    var mRequest: Request? = null

    override fun setRequest(request: Request?) {
        mRequest = request
    }

    override fun getRequest(): Request? {
        return mRequest
    }

    override fun onStart() {

    }

    override fun onStop() {

    }

    override fun onDestroy() {
    }

}

class DrawableCallBack(
    val onInvalidate: (who: Drawable) -> Unit = {},
    val onSchedule: (who: Drawable, what: Runnable, `when`: Long) -> Unit = { _, _, _ -> },
    val onUnschedule: (who: Drawable, what: Runnable) -> Unit = { _, _-> },
) : Drawable.Callback {

    var isInvalidate = true //防止出现递归调用
    var isSchedule = true //防止出现递归调用
    var isUnschedule = true //防止出现递归调用

    override fun invalidateDrawable(who: Drawable) {
        if (!isInvalidate) {
            return
        }
        isInvalidate = false
        onInvalidate(who)

        isInvalidate = true
    }

    override fun scheduleDrawable(
        who: Drawable,
        what: Runnable,
        `when`: Long
    ) {
        if (!isSchedule) {
            return
        }
        isSchedule = false
        onSchedule(who,what,`when`)

        isSchedule = true

    }

    override fun unscheduleDrawable(
        who: Drawable,
        what: Runnable
    ) {
        if (!isUnschedule) {
            return
        }
        isUnschedule = false
        onUnschedule(who,what)

        isUnschedule = true

    }

}


internal fun Drawable?.toPainter(): Painter =
    when (this) {
        is BitmapDrawable -> BitmapPainter(bitmap.asImageBitmap())
        is ColorDrawable -> ColorPainter(Color(color))
        null -> ColorPainter(Color.Transparent)
        else -> DrawablePainter(mutate())
    }

private class DrawablePainter(
    val drawable: Drawable
) : Painter() {
    init {
        if (drawable.isIntrinsicSizeValid) {
            drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        }
    }


    private var drawableIntrinsicSize = drawable.intrinsicSize

    private val Drawable.isIntrinsicSizeValid
        get() = intrinsicWidth >= 0 && intrinsicHeight >= 0

    private val Drawable.intrinsicSize: Size
        get() = if (isIntrinsicSizeValid) {
            IntSize(intrinsicWidth, intrinsicHeight).toSize()
        } else {
            Size.Unspecified
        }

    override fun applyAlpha(alpha: Float): Boolean {
        drawable.alpha = (alpha * 255).roundToInt().coerceIn(0, 255)
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        drawable.colorFilter = colorFilter?.asAndroidColorFilter()
        return true
    }

    override fun applyLayoutDirection(layoutDirection: LayoutDirection): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return drawable.setLayoutDirection(
                when (layoutDirection) {
                    LayoutDirection.Ltr -> View.LAYOUT_DIRECTION_LTR
                    LayoutDirection.Rtl -> View.LAYOUT_DIRECTION_RTL
                }
            )
        }
        return false
    }

    override val intrinsicSize: Size get() = drawableIntrinsicSize


    override fun DrawScope.onDraw() {

        drawIntoCanvas { canvas ->
            drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())

            canvas.withSave {

                drawable.draw(canvas.nativeCanvas)
            }
        }
    }
}