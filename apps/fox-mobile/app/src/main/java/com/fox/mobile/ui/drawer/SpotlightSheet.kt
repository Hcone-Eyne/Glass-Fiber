package com.fox.mobile.ui.drawer

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.fox.mobile.ui.theme.FoxColors
import com.fox.mobile.ui.theme.FoxFonts

data class FoxContact(val name: String, val role: String, val sub: String, val phone: String)

private data class LauncherApp(val label: String, val packageName: String, val category: String, val icon: Drawable)

private val categories = listOf("All", "Work", "Social", "Media", "Utilities")
private val sampleContacts = listOf(
    FoxContact("Kavin", "CSE-B · mobile", "Lab tomorrow at 9?", "+919840122104"),
    FoxContact("Mom", "Home", "Called yesterday", "+919840110082"),
    FoxContact("Arun", "Lab partner", "Bring the record book", "+919840155819"),
    FoxContact("Priya", "Classmate", "Study group · Monday", "+919840177412"),
    FoxContact("Prof. Ramesh", "Faculty", "Office 304", "+914422574350")
)
private val quickActions = listOf("Continue on Mac", "Send a file", "Timer", "Music", "Order food", "Set 7:00 alarm")

/** App drawer over the home screen. App inventory is queried from MAIN/LAUNCHER activities. */
@Composable
fun SpotlightSheet(
    query: String,
    onQueryChange: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onContactAction: (FoxContact, String) -> Unit = { _, _ -> },
    onAction: (String) -> Unit = {},
    bottomSafeInset: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pm = context.packageManager
    var category by remember { mutableStateOf("All") }
    var listMode by remember { mutableStateOf(true) }
    var appsExpanded by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val expansion by animateFloatAsState(if (isExpanded) 1f else 0f, label = "drawerExpansion")
    val apps = remember(pm) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolve ->
                val info = resolve.activityInfo?.applicationInfo ?: return@mapNotNull null
                val pkg = info.packageName
                if (pkg == context.packageName) return@mapNotNull null
                LauncherApp(
                    label = resolve.loadLabel(pm).toString(), packageName = pkg,
                    category = categoryName(info), icon = resolve.loadIcon(pm)
                )
            }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
    val filtered = remember(apps, query, category) {
        apps.filter { app ->
            (query.isBlank() || app.label.contains(query.trim(), true) || app.packageName.contains(query.trim(), true)) &&
                (category == "All" || app.category == category)
        }
    }
    val matchingContacts = remember(query) {
        val q = query.trim()
        if (q.isBlank()) sampleContacts.take(3) else sampleContacts.filter {
            listOf(it.name, it.role, it.sub).any { value -> value.contains(q, ignoreCase = true) }
        }
    }
    val matchingActions = remember(query) {
        val q = query.trim()
        if (q.isBlank()) emptyList() else quickActions.filter { it.contains(q, ignoreCase = true) }
    }
    val recents = remember { mutableStateListOf<String>().apply {
        val saved = context.getSharedPreferences("fox_launcher", 0).getString("recents", "").orEmpty()
        addAll(saved.split('|').filter(String::isNotBlank))
    } }
    LaunchedEffect(isExpanded) { if (!isExpanded) appsExpanded = false }
    BackHandler(isExpanded) {
        if (appsExpanded) appsExpanded = false else onToggleExpand()
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val panelHeight = maxHeight * 0.56f
        val searchHeight = 44.dp
        val bottomOffset = 30.dp
        Box(Modifier.fillMaxSize()) {
            if (isExpanded) {
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 10.dp).padding(bottom = bottomOffset + bottomSafeInset + searchHeight + 9.dp)) {
                    Column(
                        Modifier.fillMaxWidth().height(panelHeight).clip(RoundedCornerShape(30.dp))
                            .background(FoxColors.inkSurface).border(1.dp, FoxColors.hair, RoundedCornerShape(30.dp))
                    ) {
                        if (appsExpanded) {
                            Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 14.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                categories.forEach { item ->
                                    Text(item, Modifier.clip(CircleShape).background(if (category == item) FoxColors.ember.copy(alpha = .18f) else Color.Transparent)
                                        .clickable { category = item }.padding(horizontal = 11.dp, vertical = 7.dp),
                                        color = if (category == item) FoxColors.ember else FoxColors.t2, fontSize = 12.sp)
                                }
                            }
                            if (query.isBlank() && recents.isNotEmpty()) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    recents.take(5).mapNotNull { pkg -> apps.firstOrNull { it.packageName == pkg } }.forEach { app ->
                                        Column(Modifier.weight(1f).clickable { launchApp(context, app, recents) }, horizontalAlignment = Alignment.CenterHorizontally) {
                                            AppIcon(app.icon, 42)
                                            Text(app.label, color = FoxColors.t2, fontSize = 10.sp, maxLines = 1)
                                        }
                                    }
                                }
                            }
                            Box(Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 14.dp).background(FoxColors.hair))
                            LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 10.dp, vertical = 6.dp)) {
                                if (listMode) {
                                    items(filtered, key = { it.packageName }) { app ->
                                        Row(Modifier.fillMaxWidth().height(48.dp).clickable { launchApp(context, app, recents) }.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            AppIcon(app.icon, 32)
                                            Column(Modifier.padding(start = 12.dp)) {
                                                Text(app.label, color = FoxColors.t1, fontSize = 14.sp, maxLines = 1)
                                                Text(app.category, color = FoxColors.t2, fontSize = 11.sp, maxLines = 1)
                                            }
                                        }
                                    }
                                } else {
                                    items(filtered.chunked(4)) { row ->
                                        Row(Modifier.fillMaxWidth().height(76.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            row.forEach { item ->
                                                Column(Modifier.weight(1f).fillMaxHeight().clickable { launchApp(context, item, recents) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                                    AppIcon(item.icon, 34)
                                                    Text(item.label, color = FoxColors.t1, fontSize = 10.sp, maxLines = 1)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = if (query.isBlank()) "Recent conversations" else "People and chats",
                                modifier = Modifier.padding(start = 18.dp, top = 16.dp, bottom = 8.dp),
                                color = FoxColors.t2, fontFamily = FoxFonts.Figtree, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                            )
                            LazyColumn(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp)) {
                                items(matchingContacts, key = { it.name }) { contact ->
                                    Row(Modifier.fillMaxWidth().height(58.dp).clickable { onContactAction(contact, "message") }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(38.dp).clip(CircleShape).background(FoxColors.ember.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                                            Text(contact.name.take(1), color = FoxColors.ember, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                            Text(contact.name, color = FoxColors.t1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                            Text(contact.sub, color = FoxColors.t2, fontSize = 12.sp, maxLines = 1)
                                        }
                                        Text("Message", Modifier.clickable { onContactAction(contact, "message") }.padding(10.dp), color = FoxColors.t2, fontSize = 12.sp)
                                    }
                                }
                                items(matchingActions) { action ->
                                    Row(Modifier.fillMaxWidth().height(48.dp).clickable { onAction(action) }.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("↗", color = FoxColors.ember, fontSize = 18.sp)
                                        Text(action, Modifier.padding(start = 12.dp), color = FoxColors.t1, fontSize = 14.sp)
                                    }
                                }
                                if (query.isNotBlank() && matchingContacts.isEmpty() && matchingActions.isEmpty()) {
                                    item { Text("No people or chats match. Swipe up to browse apps.", Modifier.padding(12.dp), color = FoxColors.t2, fontSize = 13.sp) }
                                }
                            }
                            Text(
                                "Swipe up to browse apps",
                                Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp),
                                color = FoxColors.t2, fontSize = 12.sp
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 10.dp).padding(bottom = bottomOffset + bottomSafeInset).height(searchHeight)
                    .clip(CircleShape).background(FoxColors.inkSurface)
                    .border(1.dp, FoxColors.hair, CircleShape)
                    .pointerInput(isExpanded, appsExpanded) {
                        detectVerticalDragGestures(
                            onVerticalDrag = { _, dy -> dragY += dy },
                            onDragEnd = {
                                if (dragY < -24f) {
                                    if (!isExpanded) onToggleExpand()
                                    appsExpanded = true
                                } else if (dragY > 24f) {
                                    if (appsExpanded) appsExpanded = false else if (isExpanded) onToggleExpand()
                                }
                                dragY = 0f
                            },
                            onDragCancel = { dragY = 0f }
                        )
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("⌕", color = FoxColors.t2, fontSize = 22.sp)
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query, onValueChange = onQueryChange, singleLine = true,
                    textStyle = TextStyle(color = FoxColors.t1, fontFamily = FoxFonts.Figtree, fontSize = 14.sp),
                    cursorBrush = SolidColor(FoxColors.ember),
                    decorationBox = { inner ->
                        if (query.isEmpty()) Text("Search contacts, chats and files", color = FoxColors.t2, fontSize = 14.sp)
                        inner()
                    }, modifier = Modifier.weight(1f).onFocusChanged { focusState ->
                        if (focusState.isFocused && !isExpanded) onToggleExpand()
                    }
                )
                if (appsExpanded) {
                    Box {
                        Text("⋯", Modifier.clickable { menuOpen = !menuOpen }.padding(horizontal = 8.dp), color = FoxColors.t1, fontSize = 20.sp)
                        if (menuOpen) Column(Modifier.align(Alignment.BottomEnd).width(150.dp).clip(RoundedCornerShape(14.dp)).background(FoxColors.inkMenu).border(1.dp, FoxColors.hair, RoundedCornerShape(14.dp)).padding(8.dp)) {
                            Text("View as List", Modifier.fillMaxWidth().clickable { listMode = true; menuOpen = false }.padding(8.dp), color = FoxColors.t1, fontSize = 14.sp)
                            Text("View as Grid", Modifier.fillMaxWidth().clickable { listMode = false; menuOpen = false }.padding(8.dp), color = FoxColors.t1, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun categoryName(info: ApplicationInfo): String {
    if (android.os.Build.VERSION.SDK_INT < 26) return "Utilities"
    return when (info.category) {
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Work"
        ApplicationInfo.CATEGORY_SOCIAL -> "Social"
        ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO, ApplicationInfo.CATEGORY_IMAGE -> "Media"
        ApplicationInfo.CATEGORY_GAME -> "Media"
        else -> "Utilities"
    }
}

private fun launchApp(context: android.content.Context, app: LauncherApp, recents: MutableList<String>) {
    runCatching { context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) } }
    recents.remove(app.packageName)
    recents.add(0, app.packageName)
    while (recents.size > 5) recents.removeAt(recents.lastIndex)
    context.getSharedPreferences("fox_launcher", 0).edit().putString("recents", recents.joinToString("|")).apply()
}

@Composable
private fun AppIcon(drawable: Drawable, size: Int) {
    val bitmap = remember(drawable) {
        val width = drawable.intrinsicWidth.coerceAtLeast(1).coerceAtMost(192)
        val height = drawable.intrinsicHeight.coerceAtLeast(1).coerceAtMost(192)
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = AndroidCanvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
        }.asImageBitmap()
    }
    Image(bitmap, contentDescription = null, modifier = Modifier.size(size.dp))
}
