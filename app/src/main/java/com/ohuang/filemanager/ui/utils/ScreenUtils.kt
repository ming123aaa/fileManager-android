package com.ohuang.filemanager.ui.utils

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.platform.LocalConfiguration

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

enum class DeviceType {
    PHONE, TABLET
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun rememberDeviceType(): DeviceType {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp.dp
    val heightDp = configuration.screenHeightDp.dp
    return remember(widthDp, heightDp) {
        if (widthDp >= 720.dp) DeviceType.TABLET else DeviceType.PHONE
    }
}



@Composable
fun rememberSettingScreenWidth(): Dp {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    return remember(widthDp) {
        var value = max(320, (widthDp * 0.3).toInt())
        value = min(400, value)
        value.dp
    }

}

data class FragmentBoxSize(
    val minWidth: Float,

    val maxWidth: Float,

    val minHeight: Float,

    val maxHeight: Float
)

val LocalFragmentBoxSize = compositionLocalOf<FragmentBoxSize> {
    FragmentBoxSize(0f, 0f, 0f, 0f)
}


@Composable
fun FragmentBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    propagateMinConstraints: Boolean = false,
    isChange: Boolean = true,
    content: @Composable @UiComposable BoxWithConstraintsScope.() -> Unit
) {


    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = contentAlignment,
        propagateMinConstraints = propagateMinConstraints
    ) {
        val fragmentBoxSize = remember {
            mutableStateOf(
                FragmentBoxSize(
                    minWidth.value, maxWidth.value,
                    minHeight.value, maxHeight.value
                )
            )
        }
        val lastBoxSize = remember {
            mutableStateOf(
                FragmentBoxSize(
                    minWidth.value, maxWidth.value,
                    minHeight.value, maxHeight.value
                )
            )
        }


        LaunchedEffect(
            minWidth.value, maxWidth.value,
            minHeight.value, maxHeight.value
        ) {
            val data = FragmentBoxSize(
                minWidth.value, maxWidth.value,
                minHeight.value, maxHeight.value
            )
            fragmentBoxSize.value = data
            if (isChange) {
                lastBoxSize.value = data
            }

        }

        CompositionLocalProvider(
            LocalFragmentBoxSize.provides(
                if (isChange) {
                    fragmentBoxSize.value
                } else {
                    lastBoxSize.value
                }
            )
        ) {
            this@BoxWithConstraints.content()
        }
    }
}

@Composable
fun rememberGridColumns(deviceType: DeviceType): Int {
    val configuration = LocalConfiguration.current
    val fragmentSize = LocalFragmentBoxSize.current
    val widthDp = if (fragmentSize.maxWidth >= 100 && fragmentSize.maxWidth <= 5000) {
        fragmentSize.maxWidth.toInt()
    } else {
        configuration.screenWidthDp
    }
    return remember(widthDp, deviceType) {
        when {
            widthDp >= 1680 -> widthDp/240
            widthDp >= 1200 -> 6
            widthDp >= 900 -> 5
            widthDp >= 720 -> 4
            widthDp >= 500 -> 3
            widthDp >= 300 -> 2
            else -> 1
        }
    }
}


@Composable
fun rememberPreViewGridColumns(deviceType: DeviceType): Int {
    val configuration = LocalConfiguration.current
    val fragmentSize = LocalFragmentBoxSize.current
    val widthDp = if (fragmentSize.maxWidth >= 100 && fragmentSize.maxWidth <= 5000) {
        fragmentSize.maxWidth.toInt()
    } else {
        configuration.screenWidthDp
    }
    return remember(widthDp, deviceType) {
        when {
            widthDp >= 1960 -> widthDp/280
            widthDp >= 1560 -> 6
            widthDp >= 1200 -> 5
            widthDp >= 880 -> 4
            widthDp >= 600 -> 3
            widthDp >= 360 -> 2
            else -> 1
        }
    }
}


