package com.ohuang.filemanager.ui.screens

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.addTextChangedListener
import com.ohuang.filemanager.ui.utils.DeviceType
import com.ohuang.filemanager.ui.utils.rememberDeviceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    filePath: String,
    fileName: String,
    initialContent: String,
    readOnly: Boolean,
    defaultEditMode: Boolean = false,
    isRemote: Boolean = false,
    onBack: () -> Unit,
    onSaved: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val deviceType = rememberDeviceType()

    val contentPadding = if (deviceType == DeviceType.TABLET) 32.dp else 16.dp
    val verticalPadding = if (deviceType == DeviceType.TABLET) 16.dp else 8.dp
    val innerPadding = if (deviceType == DeviceType.TABLET) 20.dp else 12.dp
    val textStyle = if (deviceType == DeviceType.TABLET) {
        MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    } else {
        MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    }


    var isEditMode by remember { mutableStateOf(defaultEditMode) }
    var editContent by remember { mutableStateOf(initialContent) }
    var hasChanges by remember { mutableStateOf(false) }
    var selectionKey by remember { mutableLongStateOf(0L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (isEditMode) "编辑模式" else "查看模式",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!readOnly) {

                        // 编辑模式下显示保存按钮
                        if (isEditMode) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        try {
                                            if (isRemote) {
                                                onSaved(editContent)
                                            } else {
                                                withContext(Dispatchers.IO) {
                                                    File(filePath).writeText(
                                                        editContent,
                                                        Charsets.UTF_8
                                                    )
                                                }
                                                Toast.makeText(
                                                    context,
                                                    "保存成功",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                onSaved(editContent)
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                "保存失败: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "保存")
                            }
                        }

                        // 切换查看/编辑模式按钮
                        IconButton(onClick = {
                            if (isEditMode) {
                                hasChanges = false
                            }
                            isEditMode = !isEditMode
                        }) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Default.EditOff else Icons.Default.Edit,
                                contentDescription = if (isEditMode) "查看模式" else "编辑模式"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)

        ) {


            if (isEditMode && !readOnly) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = contentPadding, vertical = verticalPadding)
                        .imePadding()
                        .border(
                            width = 1.dp, color = MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(6.dp)
                        )
                ) {


                    EditTextCompose(
                        text = editContent,
                        onTextChange = {
                            editContent = it
                            hasChanges = true
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = innerPadding),
                        style = EditTextStyle(
                            fontSize = textStyle.fontSize,
                            color = textStyle.color,
                            hint = "当前无内容",
                            fontWeight = textStyle.fontWeight
                        )
                    )


                }


            } else {
                // 查看模式：干净无边框，支持点击链接
                val scrollState = rememberScrollState()
                val annotatedString = buildLinkAnnotatedString(initialContent)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = contentPadding, vertical = verticalPadding)
                ) {
                    key(selectionKey) {
                        SelectionContainer {
                            ClickableText(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp).padding(vertical = verticalPadding ),
                                text = annotatedString,
                                style = textStyle,
                                onClick = { offset ->
                                    val annotation =
                                        annotatedString.getStringAnnotations("URL", offset, offset)
                                            .firstOrNull()
                                    if (annotation != null) {
                                        try {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(annotation.item)
                                            )
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(
                                                context,
                                                "无法打开链接",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        // 点击非链接文本区域，取消选中状态
                                        selectionKey++
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private val URL_PATTERN = Regex(
    """https?://[^\s"'<>，。；;！!？?）\)】\]】〗]+|www\.[^\s"'<>，。；;！!？?）\)】\]】〗]+""",
    RegexOption.IGNORE_CASE
)

@Composable
private fun buildLinkAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        var lastIndex = 0
        val matches = URL_PATTERN.findAll(text)
        for (match in matches) {
            if (match.range.first > lastIndex) {
                append(text.substring(lastIndex, match.range.first))
            }
            val url = match.value
            val displayUrl = if (url.startsWith("www.")) "https://$url" else url
            pushStringAnnotation("URL", displayUrl)
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            ) {
                append(url)
            }
            pop()
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}


data class EditTextStyle(
    val color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Black,
    val fontSize: TextUnit = 12.sp,
    val gravity: Int = Gravity.START or Gravity.TOP,
    val hint: String = "",
    val hintColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Gray,
    val fontWeight:FontWeight ?= FontWeight.Normal,
    val italic: Boolean=false
)

@Composable
fun EditTextCompose(
    modifier: Modifier,
    style: EditTextStyle,
    text: String,
    onTextChange: (String) -> Unit
) {


    AndroidView(factory = { context ->
        AppCompatEditText(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addTextChangedListener { text ->
                onTextChange(text.toString())
            }
            setBackgroundColor(Color.TRANSPARENT)

        }
    }, modifier = modifier, update = { editText ->
        if (editText.text.toString() != text) {
            editText.setText(text)
        }
        if (style.fontSize.isSp) {
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, style.fontSize.value)
        }

        editText.gravity = style.gravity

        editText.setTextColor(style.color.toArgb())
        editText.setHintTextColor(style.hintColor.toArgb())

        editText.hint=style.hint


        applyTypeface(style, editText)


    })


}


private fun applyTypeface(
    style: EditTextStyle,
    editText: EditText
) {

    val fontWeight = style.fontWeight ?: FontWeight.Normal
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        editText.typeface = Typeface.create(null, fontWeight.weight, false)
    } else {
        val isBold = fontWeight.weight >= FontWeight.Bold.weight
        val typefaceStyle = if (isBold) {
            if (style.italic) {
                Typeface.BOLD_ITALIC
            } else {
                Typeface.BOLD
            }
        } else {
            if (style.italic) {
                Typeface.ITALIC
            } else {
                Typeface.NORMAL
            }
        }
        editText.typeface = Typeface.create(editText.typeface, typefaceStyle)
    }
}




