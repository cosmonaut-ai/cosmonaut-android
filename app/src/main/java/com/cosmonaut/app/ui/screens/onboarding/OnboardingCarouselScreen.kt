package com.cosmonaut.app.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmonaut.app.R
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.StarfieldBackground
import com.cosmonaut.app.ui.theme.CosmoMotion
import com.cosmonaut.app.ui.theme.CosmoTheme
import com.cosmonaut.app.ui.theme.OrbitronFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 3
private const val INDICATOR_ALPHA_INACTIVE = 0.25f
private const val ENTRANCE_STAGGER_MS = 120L
private const val ENTRANCE_DURATION_MS = 600
private const val FLOAT_AMPLITUDE_DP = 10f
private const val FLOAT_DURATION_MS = 4000
private val EaseInOutSine = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
private const val ICON_BG_ALPHA = 0.12f
private const val ICON_BORDER_ALPHA = 0.25f

private val EntranceEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
fun OnboardingCarouselScreen(onComplete: () -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == PAGE_COUNT - 1
    val primaryColor = CosmoTheme.colors.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmoTheme.colors.background),
    ) {
        StarfieldBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onComplete) {
                    Text(
                        "Skip",
                        color = CosmoTheme.colors.mutedForeground,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> HowItWorksPage()
                    2 -> GetStartedPage()
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    repeat(PAGE_COUNT) { index ->
                        val isActive = pagerState.currentPage == index
                        val color by animateColorAsState(
                            targetValue = if (isActive) {
                                primaryColor
                            } else {
                                primaryColor.copy(alpha = INDICATOR_ALPHA_INACTIVE)
                            },
                            label = "indicatorColor",
                        )
                        Box(
                            modifier = Modifier
                                .size(if (isActive) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                    }
                }

                CosmoButton(
                    onClick = {
                        if (isLastPage) {
                            onComplete()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                ) {
                    if (isLastPage) {
                        Icon(
                            imageVector = Icons.Outlined.RocketLaunch,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = if (isLastPage) "Begin Your Journey" else "Next",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

// ── Page 1: Welcome ──────────────────────────────────────────────────────

private const val ASTRONAUT_FLOAT_MS = 4000
private const val ASTRONAUT_FLOAT_DP = 14f
private const val PLANET1_FLOAT_MS = 5000
private const val PLANET1_FLOAT_DP = 8f
private const val PLANET2_FLOAT_MS = 6000
private const val PLANET2_FLOAT_DP = 6f
private const val PLANET3_FLOAT_MS = 7000
private const val PLANET3_FLOAT_DP = 4f

@Composable
private fun WelcomePage(modifier: Modifier = Modifier) {
    val primaryColor = CosmoTheme.colors.primary
    val isReduced = CosmoMotion.config.isReducedMotion

    val density = LocalDensity.current
    val floatTransition = rememberInfiniteTransition(label = "heroFloat")
    val astronautY by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isReduced) 0f else with(density) { ASTRONAUT_FLOAT_DP.dp.toPx() },
        animationSpec = infiniteRepeatable(
            animation = tween(ASTRONAUT_FLOAT_MS, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "astronautFloat",
    )
    val planet1Y by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isReduced) 0f else with(density) { PLANET1_FLOAT_DP.dp.toPx() },
        animationSpec = infiniteRepeatable(
            animation = tween(PLANET1_FLOAT_MS, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "planet1Float",
    )
    val planet2Y by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isReduced) 0f else with(density) { PLANET2_FLOAT_DP.dp.toPx() },
        animationSpec = infiniteRepeatable(
            animation = tween(PLANET2_FLOAT_MS, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "planet2Float",
    )
    val planet3Y by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isReduced) 0f else with(density) { PLANET3_FLOAT_DP.dp.toPx() },
        animationSpec = infiniteRepeatable(
            animation = tween(PLANET3_FLOAT_MS, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "planet3Float",
    )

    var showElements by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ENTRANCE_STAGGER_MS)
        showElements = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StaggeredEntrance(visible = showElements, index = 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.art_planet1),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-12).dp, y = 8.dp)
                        .graphicsLayer { translationY = -planet1Y },
                    contentScale = ContentScale.Fit,
                )
                Image(
                    painter = painterResource(R.drawable.art_planet2),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.CenterEnd)
                        .offset(x = (-4).dp, y = 40.dp)
                        .graphicsLayer { translationY = -planet2Y },
                    contentScale = ContentScale.Fit,
                )
                Image(
                    painter = painterResource(R.drawable.art_planet3),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = 4.dp, y = 20.dp)
                        .graphicsLayer { translationY = -planet3Y },
                    contentScale = ContentScale.Fit,
                )
                Image(
                    painter = painterResource(R.drawable.art_hero_astronaut),
                    contentDescription = "Floating astronaut",
                    modifier = Modifier
                        .height(240.dp)
                        .graphicsLayer { translationY = -astronautY },
                    contentScale = ContentScale.Fit,
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        StaggeredEntrance(visible = showElements, index = 1) {
            Text(
                text = "WELCOME TO",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = OrbitronFontFamily,
                    letterSpacing = 4.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = CosmoTheme.colors.foreground,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        StaggeredEntrance(visible = showElements, index = 2) {
            Text(
                text = "COSMONAUT",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = OrbitronFontFamily,
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = primaryColor,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        StaggeredEntrance(visible = showElements, index = 3) {
            Text(
                text = "Custom interactive stories\nfor you & your family",
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                color = CosmoTheme.colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Page 2: How It Works ─────────────────────────────────────────────────

private data class StepData(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

private val steps = listOf(
    StepData(
        icon = Icons.Outlined.AutoAwesome,
        title = "Describe your story",
        description = "You set the stage — the AI fills in the details.",
    ),
    StepData(
        icon = Icons.Outlined.AccountTree,
        title = "Every choice branches",
        description = "Decisions create new paths with every click.",
    ),
    StepData(
        icon = Icons.Outlined.Visibility,
        title = "See the whole picture",
        description = "A visual map of every path through your story.",
    ),
)

@Composable
private fun HowItWorksPage(modifier: Modifier = Modifier) {
    val primaryColor = CosmoTheme.colors.primary

    var showElements by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ENTRANCE_STAGGER_MS)
        showElements = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StaggeredEntrance(visible = showElements, index = 0) {
            Text(
                text = "HOW IT WORKS",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = primaryColor,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        StaggeredEntrance(visible = showElements, index = 1) {
            Text(
                text = "Three steps to\ninfinite stories",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = CosmoTheme.colors.foreground,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        steps.forEachIndexed { index, step ->
            StaggeredEntrance(visible = showElements, index = index + 2) {
                StepRow(step = step)
            }
            if (index < steps.lastIndex) {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun StepRow(step: StepData, modifier: Modifier = Modifier) {
    val primaryColor = CosmoTheme.colors.primary

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(primaryColor.copy(alpha = ICON_BG_ALPHA))
                .drawBehind {
                    drawRoundRect(
                        color = primaryColor.copy(alpha = ICON_BORDER_ALPHA),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = step.icon,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(26.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = CosmoTheme.colors.foreground,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.mutedForeground,
                lineHeight = 20.sp,
            )
        }
    }
}

// ── Page 3: Get Started ──────────────────────────────────────────────────

@Composable
private fun GetStartedPage(modifier: Modifier = Modifier) {
    val primaryColor = CosmoTheme.colors.primary
    val isReduced = CosmoMotion.config.isReducedMotion

    val density = LocalDensity.current
    val floatTransition = rememberInfiniteTransition(label = "ctaFloat")
    val floatOffset by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isReduced) 0f else with(density) { FLOAT_AMPLITUDE_DP.dp.toPx() },
        animationSpec = infiniteRepeatable(
            animation = tween(FLOAT_DURATION_MS + 500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ctaAstronautFloat",
    )

    var showElements by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ENTRANCE_STAGGER_MS)
        showElements = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StaggeredEntrance(visible = showElements, index = 0) {
            Image(
                painter = painterResource(R.drawable.art_tier_cosmonaut),
                contentDescription = null,
                modifier = Modifier
                    .height(260.dp)
                    .graphicsLayer { translationY = -floatOffset }
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Fit,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        StaggeredEntrance(visible = showElements, index = 1) {
            Text(
                text = "What story are\nyou building?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                color = CosmoTheme.colors.foreground,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        StaggeredEntrance(visible = showElements, index = 2) {
            Text(
                text = "One prompt is all it takes. Describe your story,\nstep inside, and see where your choices take you.",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                color = CosmoTheme.colors.mutedForeground,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Shared Animation Utility ─────────────────────────────────────────────

@Composable
private fun StaggeredEntrance(
    visible: Boolean,
    index: Int,
    content: @Composable () -> Unit,
) {
    val isReduced = CosmoMotion.config.isReducedMotion
    val delayMs = if (isReduced) 0 else (index * ENTRANCE_STAGGER_MS).toInt()

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = if (isReduced) 0 else ENTRANCE_DURATION_MS,
                delayMillis = delayMs,
                easing = EntranceEasing,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = if (isReduced) 0 else ENTRANCE_DURATION_MS,
                delayMillis = delayMs,
                easing = EntranceEasing,
            ),
            initialOffsetY = { it / 4 },
        ),
    ) {
        content()
    }
}
