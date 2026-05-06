package com.cosmonaut.app.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmonaut.app.R
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.theme.CosmoTheme
import com.cosmonaut.app.ui.theme.OrbitronFontFamily
import kotlinx.coroutines.launch

private data class CarouselPage(val imageRes: Int, val title: String, val description: String,)

private val carouselPages = listOf(
    CarouselPage(
        imageRes = R.drawable.art_new_world_astronaut,
        title = "Interactive Stories",
        description = "Dive into AI-generated stories where every choice shapes the narrative. " +
            "Your decisions create unique branching paths.",
    ),
    CarouselPage(
        imageRes = R.drawable.art_no_worlds_astronaut,
        title = "Branching Narratives",
        description = "Explore a living story tree with dozens of possible paths. " +
            "Revisit choices, discover new endings, and share your adventures.",
    ),
    CarouselPage(
        imageRes = R.drawable.art_ending_sunset,
        title = "Audio Narration",
        description = "Listen to your stories come alive with AI-powered voice narration. " +
            "Choose from multiple voices and enjoy hands-free storytelling.",
    ),
)

private const val INDICATOR_ALPHA_INACTIVE = 0.3f
private const val GLOW_ALPHA = 0.10f
private const val GLOW_CENTER_Y_FRACTION = 0.33f
private const val GLOW_RADIUS_FRACTION = 0.65f

@Composable
fun OnboardingCarouselScreen(onComplete: () -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { carouselPages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == carouselPages.lastIndex
    val primaryColor = CosmoTheme.colors.primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CosmoTheme.colors.background)
            .drawBehind {
                val glowCenter = Offset(size.width / 2f, size.height * GLOW_CENTER_Y_FRACTION)
                val glowRadius = size.width * GLOW_RADIUS_FRACTION
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = GLOW_ALPHA),
                            Color.Transparent,
                        ),
                        center = glowCenter,
                        radius = glowRadius,
                    ),
                    radius = glowRadius,
                    center = glowCenter,
                )
            }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onComplete) {
                Text("Skip", color = CosmoTheme.colors.mutedForeground)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            CarouselPageContent(page = carouselPages[page])
        }

        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            carouselPages.forEachIndexed { index, _ ->
                val isActive = pagerState.currentPage == index
                val color by animateColorAsState(
                    targetValue = if (isActive) {
                        CosmoTheme.colors.primary
                    } else {
                        CosmoTheme.colors.primary.copy(alpha = INDICATOR_ALPHA_INACTIVE)
                    },
                    label = "indicatorColor",
                )
                Box(
                    modifier = Modifier
                        .size(if (isActive) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(color),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CosmoButton(
            text = if (isLastPage) "Get Started" else "Next",
            onClick = {
                if (isLastPage) {
                    onComplete()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CarouselPageContent(page: CarouselPage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(page.imageRes),
            contentDescription = null,
            modifier = Modifier.height(180.dp),
            contentScale = ContentScale.Fit,
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = OrbitronFontFamily,
            ),
            color = CosmoTheme.colors.foreground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            color = CosmoTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )
    }
}
