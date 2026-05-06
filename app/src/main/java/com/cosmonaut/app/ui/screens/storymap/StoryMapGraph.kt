package com.cosmonaut.app.ui.screens.storymap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.cosmonaut.app.ui.theme.CosmoTheme
import kotlin.math.roundToInt

private const val MIN_ZOOM = 0.1f
private const val MAX_ZOOM = 2.0f
private const val FIT_PADDING_FRACTION = 0.15f

private const val DOT_GRID_SPACING_DP = 18f
private const val DOT_RADIUS = 1.5f

/**
 * Interactive story map graph with pan/zoom gestures, edge rendering, and positioned node composables.
 *
 * Architecture:
 * - Outer Box captures pan/zoom/double-tap gestures
 * - graphicsLayer applies scale+translate transformations to the content
 * - Canvas draws dot background + edge paths between node centers
 * - Node composables are positioned at their layout coordinates using offsets
 *
 * All node positions from GraphLayoutEngine are in dp units. The Canvas converts
 * dp positions to pixels using the device density for drawing.
 */
@Composable
fun StoryMapGraph(
    graphData: GraphData,
    currentNodeId: String?,
    onNodeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val densityScale = density.density
    val colors = CosmoTheme.colors

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var containerWidth by remember { mutableFloatStateOf(0f) }
    var containerHeight by remember { mutableFloatStateOf(0f) }
    var hasFitted by remember { mutableStateOf(false) }

    val nodeWidthPx = NODE_WIDTH_DP * densityScale
    val nodeHeightPx = NODE_HEIGHT_DP * densityScale

    val nodePositions = remember(graphData) {
        graphData.nodes.associate { it.id to it.position }
    }

    LaunchedEffect(graphData, containerWidth, containerHeight) {
        if (containerWidth <= 0f || containerHeight <= 0f || graphData.nodes.isEmpty()) return@LaunchedEffect
        if (hasFitted) return@LaunchedEffect

        val fitted = fitViewToContent(
            graphData.nodes,
            containerWidth,
            containerHeight,
            densityScale,
            nodeWidthPx,
            nodeHeightPx,
        )
        scale = fitted.scale
        offsetX = fitted.offsetX
        offsetY = fitted.offsetY

        if (currentNodeId != null) {
            val currentNode = graphData.nodes.find { it.id == currentNodeId }
            if (currentNode != null) {
                val centeredFit = centerOnNode(
                    currentNode,
                    fitted.scale,
                    containerWidth,
                    containerHeight,
                    densityScale,
                    nodeWidthPx,
                    nodeHeightPx,
                )
                offsetX = centeredFit.first
                offsetY = centeredFit.second
            }
        }

        hasFitted = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    val scaleChange = newScale / scale

                    offsetX = (offsetX - centroid.x) * scaleChange + centroid.x + pan.x
                    offsetY = (offsetY - centroid.y) * scaleChange + centroid.y + pan.y

                    scale = newScale
                }
            }
            .pointerInput(graphData, containerWidth, containerHeight) {
                detectTapGestures(
                    onDoubleTap = {
                        if (graphData.nodes.isEmpty()) return@detectTapGestures
                        val fitted = fitViewToContent(
                            graphData.nodes,
                            containerWidth,
                            containerHeight,
                            densityScale,
                            nodeWidthPx,
                            nodeHeightPx,
                        )
                        scale = fitted.scale
                        offsetX = fitted.offsetX
                        offsetY = fitted.offsetY
                    },
                )
            },
    ) {
        Layout(
            content = {},
            modifier = Modifier.fillMaxSize(),
        ) { _, constraints ->
            containerWidth = constraints.maxWidth.toFloat()
            containerHeight = constraints.maxHeight.toFloat()
            layout(constraints.maxWidth, constraints.maxHeight) {}
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        ) {
            val dotSpacingPx = DOT_GRID_SPACING_DP * densityScale
            drawDotGrid(
                drawScope = this,
                dotColor = colors.graphDot.copy(alpha = 0.4f),
                scale = scale,
                offsetX = offsetX,
                offsetY = offsetY,
                canvasWidth = containerWidth,
                canvasHeight = containerHeight,
                dotSpacing = dotSpacingPx,
            )

            graphData.edges.forEach { edge ->
                val sourceDpPos = nodePositions[edge.sourceId] ?: return@forEach
                val targetDpPos = nodePositions[edge.targetId] ?: return@forEach

                val sourceBottom = Offset(
                    sourceDpPos.x * densityScale + nodeWidthPx / 2f,
                    sourceDpPos.y * densityScale + nodeHeightPx,
                )
                val targetTop = Offset(
                    targetDpPos.x * densityScale + nodeWidthPx / 2f,
                    targetDpPos.y * densityScale,
                )

                drawSmoothStepEdge(
                    drawScope = this,
                    from = sourceBottom,
                    to = targetTop,
                    color = colors.primary,
                    isDashed = edge.isChoiceLink,
                )
            }
        }

        graphData.nodes.forEach { node ->
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                        transformOrigin = TransformOrigin(0f, 0f)
                    }
                    .offset {
                        IntOffset(
                            x = (node.position.x * densityScale).roundToInt(),
                            y = (node.position.y * densityScale).roundToInt(),
                        )
                    },
            ) {
                StoryMapNode(
                    node = node,
                    onClick = onNodeClick,
                )
            }
        }
    }
}

private data class FitResult(val scale: Float, val offsetX: Float, val offsetY: Float)

private fun fitViewToContent(
    nodes: List<GraphNode>,
    containerWidth: Float,
    containerHeight: Float,
    densityScale: Float,
    nodeWidthPx: Float,
    nodeHeightPx: Float,
): FitResult {
    if (nodes.isEmpty()) return FitResult(1f, 0f, 0f)

    var minX = Float.MAX_VALUE
    var minY = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var maxY = Float.MIN_VALUE

    nodes.forEach { node ->
        val px = node.position.x * densityScale
        val py = node.position.y * densityScale
        minX = minOf(minX, px)
        minY = minOf(minY, py)
        maxX = maxOf(maxX, px + nodeWidthPx)
        maxY = maxOf(maxY, py + nodeHeightPx)
    }

    val contentWidth = maxX - minX
    val contentHeight = maxY - minY

    if (contentWidth <= 0f || contentHeight <= 0f) return FitResult(1f, 0f, 0f)

    val paddedContainerWidth = containerWidth * (1f - FIT_PADDING_FRACTION * 2)
    val paddedContainerHeight = containerHeight * (1f - FIT_PADDING_FRACTION * 2)

    val fitScale = minOf(
        paddedContainerWidth / contentWidth,
        paddedContainerHeight / contentHeight,
        MAX_ZOOM,
    ).coerceAtLeast(MIN_ZOOM)

    val centerX = (minX + maxX) / 2f
    val centerY = (minY + maxY) / 2f

    val fitOffsetX = containerWidth / 2f - centerX * fitScale
    val fitOffsetY = containerHeight / 2f - centerY * fitScale

    return FitResult(fitScale, fitOffsetX, fitOffsetY)
}

private fun centerOnNode(
    node: GraphNode,
    scale: Float,
    containerWidth: Float,
    containerHeight: Float,
    densityScale: Float,
    nodeWidthPx: Float,
    nodeHeightPx: Float,
): Pair<Float, Float> {
    val nodeCenterX = node.position.x * densityScale + nodeWidthPx / 2f
    val nodeCenterY = node.position.y * densityScale + nodeHeightPx / 2f

    return Pair(
        containerWidth / 2f - nodeCenterX * scale,
        containerHeight / 2f - nodeCenterY * scale,
    )
}

/**
 * Draws a smoothstep-style edge: a cubic bezier that exits the source vertically
 * downward, then enters the target vertically from above.
 */
private fun drawSmoothStepEdge(
    drawScope: DrawScope,
    from: Offset,
    to: Offset,
    color: androidx.compose.ui.graphics.Color,
    isDashed: Boolean,
) {
    val midY = (from.y + to.y) / 2f
    val path = Path().apply {
        moveTo(from.x, from.y)
        cubicTo(
            from.x,
            midY,
            to.x,
            midY,
            to.x,
            to.y,
        )
    }

    val stroke = if (isDashed) {
        Stroke(
            width = with(drawScope) { 2f * density },
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
        )
    } else {
        Stroke(width = with(drawScope) { 2f * density })
    }

    drawScope.drawPath(path = path, color = color, style = stroke)
}

/**
 * Draws a dot grid background pattern matching the web's SvelteFlow background.
 */
private fun drawDotGrid(
    drawScope: DrawScope,
    dotColor: androidx.compose.ui.graphics.Color,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    canvasWidth: Float,
    canvasHeight: Float,
    dotSpacing: Float,
) {
    val visibleLeft = -offsetX / scale
    val visibleTop = -offsetY / scale
    val visibleWidth = canvasWidth / scale
    val visibleHeight = canvasHeight / scale

    val startCol = ((visibleLeft / dotSpacing).toInt() - 1)
    val endCol = (((visibleLeft + visibleWidth) / dotSpacing).toInt() + 1)
    val startRow = ((visibleTop / dotSpacing).toInt() - 1)
    val endRow = (((visibleTop + visibleHeight) / dotSpacing).toInt() + 1)

    for (col in startCol..endCol) {
        for (row in startRow..endRow) {
            val x = col * dotSpacing
            val y = row * dotSpacing
            drawScope.drawCircle(
                color = dotColor,
                radius = DOT_RADIUS,
                center = Offset(x, y),
            )
        }
    }
}
