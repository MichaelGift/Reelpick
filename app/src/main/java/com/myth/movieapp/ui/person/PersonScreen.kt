package com.myth.movieapp.ui.person

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myth.movieapp.domain.model.MediaType
import com.myth.movieapp.domain.model.Person
import com.myth.movieapp.ui.common.ErrorState
import com.myth.core.design.GlassIconButton
import com.myth.core.design.MIN_DISPLAY_MATCH
import com.myth.core.design.MatchBadge
import com.myth.movieapp.ui.common.OpenTitle
import com.myth.movieapp.ui.common.PersonAvatar
import com.myth.movieapp.ui.common.PosterImage
import com.myth.movieapp.ui.common.RowItem
import com.myth.core.design.SectionHeader
import com.myth.movieapp.ui.common.SeriesBadge
import com.myth.core.design.ShimmerBox
import com.myth.movieapp.ui.common.TitleRow
import com.myth.movieapp.ui.common.stableKey
import com.myth.core.design.Brand
import com.myth.core.design.EyebrowStyle
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun PersonScreen(
    onBack: () -> Unit,
    onTitleClick: OpenTitle,
    viewModel: PersonViewModel = hiltViewModel(),
) {
    val content by viewModel.content.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 300 } }
    val scrimAlpha by animateFloatAsState(if (scrolled) 1f else 0f, label = "personScrim")
    var department by rememberSaveable { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 32.dp), modifier = Modifier.navigationBarsPadding()) {
            item(key = "header") { Header(viewModel.preview.name, viewModel.preview.profilePath, viewModel.sharedKey, content) }
            when (val c = content) {
                PersonContent.Loading -> item(key = "loading") { LoadingBody() }
                is PersonContent.Error -> item(key = "error") { ErrorState(c.message, viewModel::load, Modifier.height(320.dp)) }
                is PersonContent.Loaded -> loadedBody(
                    content = c,
                    selected = department ?: c.departments.firstOrNull()?.name,
                    onSelect = { department = it },
                    onTitleClick = onTitleClick,
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(alpha = scrimAlpha))
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Text(
                viewModel.preview.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(end = 44.dp).graphicsLayer { alpha = scrimAlpha },
            )
        }
    }
}

/** Large headshot over a soft brand glow, then name and life dates. */
@Composable
private fun Header(name: String, profilePath: String?, sharedKey: String?, content: PersonContent) {
    val loaded = content as? PersonContent.Loaded
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Brand.tokens.gradientColors.last().copy(alpha = 0.28f), MaterialTheme.colorScheme.background),
                    ),
                ),
        )
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 64.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PersonAvatar(profilePath ?: loaded?.person?.profilePath, contentDescription = name, size = 148.dp, sharedKey = sharedKey)
            Spacer(Modifier.height(16.dp))
            Text(name, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            loaded?.person?.let { person ->
                Spacer(Modifier.height(6.dp))
                person.knownForDepartment?.let {
                    Text(knownForLabel(it).uppercase(), style = EyebrowStyle, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                }
                lifeLine(person, loaded.age)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

/** "Born 30 July 1970 (55) in London" or "1934 – 2016 (82)". */
private fun lifeLine(person: Person, age: Int?): String? {
    val born = person.birthday ?: return person.placeOfBirth?.let { "Born in $it" }
    val ageText = age?.let { " ($it)" }.orEmpty()
    val place = person.placeOfBirth?.let { " in $it" }.orEmpty()
    return person.deathday?.let { "${born.format(dateFormat)} – ${it.format(dateFormat)}$ageText" }
        ?: "Born ${born.format(dateFormat)}$ageText$place"
}

private fun knownForLabel(department: String) = when (department) {
    "Acting" -> "Actor"
    "Directing" -> "Director"
    "Writing" -> "Writer"
    "Production" -> "Producer"
    else -> department
}

@Composable
private fun LoadingBody() {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(4) { ShimmerBox(Modifier.fillMaxWidth().height(14.dp), RoundedCornerShape(6.dp)) }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(3) { ShimmerBox(Modifier.width(110.dp).height(165.dp)) }
        }
    }
}

private fun LazyListScope.loadedBody(
    content: PersonContent.Loaded,
    selected: String?,
    onSelect: (String) -> Unit,
    onTitleClick: OpenTitle,
) {
    if (content.person.biography.isNotBlank()) {
        item(key = "bio") { Biography(content.person.biography, Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp)) }
    }
    if (content.knownFor.isNotEmpty()) {
        item(key = "known-for") {
            TitleRow(
                title = "Known for",
                items = content.knownFor.map { RowItem(it.credit.title, caption = it.credit.role, match = it.match) },
                section = "person-${content.person.id}",
                onMovieClick = onTitleClick,
                modifier = Modifier.padding(top = 28.dp),
            )
        }
    }
    val department = content.departments.firstOrNull { it.name == selected } ?: return
    item(key = "filmography-header") {
        Column(Modifier.padding(top = 32.dp)) {
            SectionHeader("Filmography", subtitle = "${content.departments.sumOf { it.credits.size }} credits")
            if (content.departments.size > 1) {
                Spacer(Modifier.height(12.dp))
                DepartmentTabs(content.departments.map { it.name }, department.name, onSelect)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
    items(department.credits, key = { "${department.name}-${it.credit.title.stableKey}" }) { row ->
        CreditListItem(row, onClick = { onTitleClick(row.credit.title, null) })
    }
}

@Composable
private fun Biography(text: String, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier.animateContentSize()) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
        )
        if (text.length > 280) {
            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                Text(if (expanded) "Show less" else "Read more")
            }
        }
    }
}

@Composable
private fun DepartmentTabs(names: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(names, key = { it }) { name ->
            val isSelected = name == selected
            Surface(
                onClick = { onSelect(name) },
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics {
                    this.selected = isSelected
                    role = Role.Tab
                },
            ) {
                Text(name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}

/** Year, poster, title and role, with the taste match on the right. */
@Composable
private fun CreditListItem(row: CreditRow, onClick: () -> Unit) {
    val title = row.credit.title
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.releaseYear ?: "TBA",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp),
        )
        Box(Modifier.width(48.dp).clip(RoundedCornerShape(8.dp))) {
            PosterImage(title.posterPath, contentDescription = null, shape = RoundedCornerShape(8.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (title.mediaType == MediaType.TV) {
                    Spacer(Modifier.width(6.dp))
                    SeriesBadge()
                }
            }
            val detail = listOfNotNull(
                row.credit.role,
                row.credit.episodeCount?.takeIf { it > 0 }?.let { "$it ep" },
            ).joinToString("  ·  ")
            if (detail.isNotEmpty()) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        row.match?.takeIf { it >= MIN_DISPLAY_MATCH }?.let {
            Spacer(Modifier.width(10.dp))
            MatchBadge(it)
        }
    }
}
