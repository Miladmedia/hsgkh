package ir.heyat.khalilshahr

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val periodic = PeriodicWorkRequestBuilder<FeedWorker>(30,TimeUnit.MINUTES).setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("heyat-periodic-feed",ExistingPeriodicWorkPolicy.KEEP,periodic)
        val once = OneTimeWorkRequestBuilder<FeedWorker>().setConstraints(constraints).build()
        WorkManager.getInstance(this).enqueueUniqueWork("heyat-immediate-feed",ExistingWorkPolicy.KEEP,once)
        setContent { HeyatApp() }
    }
}

private val forest = Color(0xFF10231D); private val gold = Color(0xFFD2B06D); private val cream = Color(0xFFF1EEE6)
private enum class Page(val title: String) { Home("خانه"), Martyrs("شهدا"), Programs("برنامه‌ها"), About("درباره ما") }

@Composable private fun HeyatApp() {
    val context= LocalContext.current; val repo= remember { NewsRepository(context) }; val scope= rememberCoroutineScope()
    var page by remember { mutableStateOf(Page.Home) }; var posts by remember { mutableStateOf(repo.cached()) }; var loading by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }; var refreshed by remember { mutableStateOf(false) }
    fun refresh() { scope.launch { loading=true; error=null; val result=withContext(Dispatchers.IO){repo.fetch()}; when(result){ is NewsRepository.Fetch.Success->{posts=result.posts; refreshed=true}; is NewsRepository.Fetch.Failure->{error=result.message} }; loading=false } }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme=darkColorScheme(primary=gold,background=forest,surface=Color(0xFF1A3028),onBackground=cream,onSurface=cream), typography=Typography()) {
            Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1B382C),Color(0xFF101714),Color(0xFF101714))))) {
                Column(Modifier.fillMaxWidth().padding(horizontal=20.dp, vertical=18.dp), horizontalAlignment=Alignment.CenterHorizontally) { Text("✦",color=gold,fontSize=24.sp); Text("هیئت شهدای گمنام خلیل شهر",color=gold,fontSize=21.sp,fontWeight=FontWeight.Bold); Text("خبرهای منتشرشده در منبع عمومی",color=cream.copy(alpha=.7f),fontSize=12.sp) }
                when(page) {
                    Page.About -> AboutPage()
                    else -> {
                        Row(Modifier.fillMaxWidth().padding(horizontal=18.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) { Text(page.title,color=cream,fontSize=19.sp,fontWeight=FontWeight.SemiBold); TextButton(onClick={refresh()},enabled=!loading){ Text(if(loading) "در حال دریافت…" else "تازه‌سازی ⟳",color=gold) } }
                        if(error!=null) Text("خطا: $error${if(posts.isNotEmpty()) " · نمایش نسخهٔ ذخیره‌شده" else ""}",Modifier.padding(horizontal=18.dp, vertical=6.dp),color=Color(0xFFFFC5A8),fontSize=13.sp)
                        val filtered = when(page) { Page.Martyrs -> posts.filter { p -> p.hashtags.any { it in setOf("شهدا","شهید","شهدای") } }; Page.Programs -> posts.filter { p -> p.hashtags.any { it in setOf("برنامه","مراسم","برنامه‌ها") } }; else -> posts }
                        if(posts.isEmpty() && error==null) Box(Modifier.fillMaxSize().weight(1f),contentAlignment=Alignment.Center){ Text(if(loading) "در حال بررسی منبع…" else "هنوز پستی از منبع دریافت نشده است",color=cream.copy(alpha=.8f)) }
                        else if(filtered.isEmpty()) Box(Modifier.fillMaxSize().weight(1f),contentAlignment=Alignment.Center){ Text(if(posts.isNotEmpty() && page!=Page.Home) "پست دارای هشتگ مرتبطی در کش نیست." else "پستی برای نمایش وجود ندارد.",color=cream.copy(alpha=.8f)) }
                        else LazyColumn(Modifier.weight(1f).padding(horizontal=16.dp),contentPadding=PaddingValues(bottom=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){ items(filtered.take(10),key={it.id}) { PostCard(it) } }
                        if(posts.isNotEmpty()) Text(if(refreshed) "آخرین بررسی انجام شد · ذخیرهٔ محلی تا ۱۰ پست" else "ذخیرهٔ محلی · تا ۱۰ پست",Modifier.align(Alignment.CenterHorizontally).padding(bottom=6.dp),color=cream.copy(alpha=.55f),fontSize=11.sp)
                    }
                }
                NavigationBar(containerColor=Color(0xCC14241E), tonalElevation=0.dp) { Page.entries.forEach { p -> NavigationBarItem(selected=page==p,onClick={page=p},icon={ Icon(when(p){Page.Home->Icons.Default.Home;Page.Martyrs->Icons.Default.Groups;Page.Programs->Icons.Default.CalendarMonth;Page.About->Icons.Default.Info},contentDescription=p.title) },label={Text(p.title,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=forest,selectedTextColor=gold,indicatorColor=gold,unselectedIconColor=cream.copy(alpha=.65f),unselectedTextColor=cream.copy(alpha=.65f))) } }
            }
        }
    }
}

@Composable private fun PostCard(post: Post) { val ctx=LocalContext.current; Surface(color=Color.White.copy(alpha=.075f),shape=RoundedCornerShape(20.dp),border=androidx.compose.foundation.BorderStroke(1.dp,gold.copy(alpha=.28f)),modifier=Modifier.fillMaxWidth().then(if(post.url!=null) Modifier.clickable { ctx.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(post.url))) } else Modifier)) { Column(Modifier.padding(16.dp)) { Text(post.text,color=cream,fontSize=16.sp,lineHeight=25.sp); if(post.hashtags.isNotEmpty()) Text(post.hashtags.joinToString("  ") { "#$it" },Modifier.padding(top=9.dp),color=gold,fontSize=13.sp); post.dateMillis?.let { Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it)),Modifier.padding(top=7.dp),color=cream.copy(alpha=.55f),fontSize=11.sp) }; if(post.url!=null) Text("مشاهدهٔ پست اصلی ↗",Modifier.padding(top=8.dp),color=gold,fontSize=12.sp) } } }

@Composable private fun AboutPage() { val ctx=LocalContext.current; Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) { Surface(color=Color.White.copy(alpha=.07f),shape=RoundedCornerShape(20.dp),border=androidx.compose.foundation.BorderStroke(1.dp,gold.copy(alpha=.25f))) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) { Text("دربارهٔ منبع",color=gold,fontSize=19.sp,fontWeight=FontWeight.Bold); Text("این برنامه پست‌ها را از صفحهٔ عمومی ایتا با نشانی howzeh_khalilshahr می‌خواند. این نشانی کانال حوزهٔ علمیهٔ باقرالعلوم را معرفی می‌کند، نه کانال مستقل هیئت. در زمان تهیهٔ پروژه فقط یک پست ویدیویی عمومی در منبع قابل مشاهده گزارش شده است؛ تعداد یا محتوای پست‌ها از پیش ساخته نشده است.",color=cream,fontSize=15.sp,lineHeight=24.sp); Text("پست‌ها فقط پس از دریافت موفق ذخیره می‌شوند. در قطع اتصال نسخهٔ ذخیره‌شده نمایش داده می‌شود و خطای صفحه کش را پاک نمی‌کند.",color=cream.copy(alpha=.8f),fontSize=13.sp,lineHeight=21.sp) } }; TextButton(onClick={ctx.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(NewsRepository.CHANNEL_URL)))}) { Text("بازکردن منبع در مرورگر ↗",color=gold) } } }
