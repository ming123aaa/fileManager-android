package com.ohuang.filemanager.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditOff
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction

import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ohuang.filemanager.ui.utils.DeviceType
import com.ohuang.filemanager.ui.utils.rememberDeviceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
        MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    } else {
        MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
    }
    val hintTextStyle = if (deviceType == DeviceType.TABLET) {
        MaterialTheme.typography.titleMedium
    } else {
        MaterialTheme.typography.bodyMedium
    }

    var isEditMode by remember { mutableStateOf(defaultEditMode) }
    var editContent by remember { mutableStateOf(initialContent) }
    var hasChanges by remember { mutableStateOf(false) }
    var selectionKey by remember { mutableStateOf(0) }

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
                .imePadding()
        ) {


            if (isEditMode && !readOnly) {

                    OutlinedTextField(
                        value = editContent, onValueChange = {
                            editContent = it
                            hasChanges = true
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                        ,


                        textStyle = textStyle,
                        maxLines = Int.MAX_VALUE,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        colors = OutlinedTextFieldDefaults.colors()
                            .copy(unfocusedIndicatorColor = MaterialTheme.colorScheme.primary)


                    )

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



