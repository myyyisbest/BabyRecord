package com.babyrecord.app.ui.knowledge

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.babyrecord.app.BabyApp
import com.babyrecord.app.ui.theme.Coral
import com.babyrecord.app.ui.theme.CoralContainer
import com.babyrecord.app.ui.theme.InkSoft
import com.babyrecord.app.ui.theme.OnCoralContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class KnowledgeViewModel(application: Application) : AndroidViewModel(application) {
    val babyMonths: StateFlow<Int?> = (application as BabyApp).database.babyDao().observeBaby()
        .map { baby ->
            baby?.let {
                ChronoUnit.MONTHS.between(LocalDate.ofEpochDay(it.birthdayEpochDay), LocalDate.now()).toInt()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

@Composable
fun KnowledgeListScreen(babyMonths: Int?, onBack: () -> Unit, onOpenArticle: (KnowledgeArticle) -> Unit) {
    val currentArticles = babyMonths?.let { m ->
        KnowledgeData.articles.filter { m in it.ageMinMonths..it.ageMaxMonths }
    } ?: emptyList()
    val otherArticles = KnowledgeData.articles.filter { it !in currentArticles }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("育儿百科", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            "参考国家规范与喂养指南整理 · 仅供家庭参考",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp)
        )
        Spacer(Modifier.height(16.dp))

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            if (currentArticles.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "适合宝宝现阶段 · ${babyMonths?.let { KnowledgeData.ageLabel(it) } ?: ""}",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Coral
                    )
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.weight(0.5f).height(1.dp).background(Coral.copy(alpha = 0.25f)))
                }
                Spacer(Modifier.height(10.dp))
                currentArticles.forEach { article ->
                    ArticleRow(article, highlight = true) { onOpenArticle(article) }
                    Spacer(Modifier.height(10.dp))
                }
                Spacer(Modifier.height(16.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("全部主题", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                Box(Modifier.weight(0.5f).height(1.dp).background(MaterialTheme.colorScheme.surfaceVariant))
            }
            Spacer(Modifier.height(10.dp))
            otherArticles.forEach { article ->
                ArticleRow(article, highlight = false) { onOpenArticle(article) }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ArticleRow(article: KnowledgeArticle, highlight: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (highlight) CoralContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) { Text(article.emoji, fontSize = 21.sp) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                article.title, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(3.dp))
            Text(
                "${article.category} · ${KnowledgeData.ageLabel(article.ageMinMonths)}" +
                    if (article.ageMaxMonths != article.ageMinMonths) " ~ ${KnowledgeData.ageLabel(article.ageMaxMonths)}" else "",
                fontSize = 12.sp,
                color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun KnowledgeDetailScreen(article: KnowledgeArticle, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("育儿百科", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(article.title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 30.sp)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(CoralContainer)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${article.emoji} ${article.category} · ${KnowledgeData.ageLabel(article.ageMinMonths)}" +
                            if (article.ageMaxMonths != article.ageMinMonths) " ~ ${KnowledgeData.ageLabel(article.ageMaxMonths)}" else "",
                        fontSize = 12.sp, color = OnCoralContainer, fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            article.sections.forEach { section ->
                Text(section.heading, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                section.body.forEach { line ->
                    Row(Modifier.padding(bottom = 6.dp)) {
                        Box(
                            Modifier.padding(top = 8.dp).size(5.dp).clip(CircleShape)
                                .background(Coral)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(line, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                Text(
                    "内容参考国家卫生健康委员会相关规范、中国营养学会《中国居民膳食指南》婴幼儿部分及世界卫生组织建议整理，仅供家庭参考，不能替代医生诊断；宝宝如有异常请及时就医。",
                    fontSize = 11.sp, color = InkSoft, lineHeight = 18.sp
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
