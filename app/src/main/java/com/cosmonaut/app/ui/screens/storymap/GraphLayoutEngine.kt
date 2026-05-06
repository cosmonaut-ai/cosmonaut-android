package com.cosmonaut.app.ui.screens.storymap

import androidx.compose.ui.geometry.Offset
import com.cosmonaut.app.data.remote.dto.StoryNodeResponse

/**
 * Represents a positioned node in the graph layout.
 */
data class GraphNode(
    val id: String,
    val title: String,
    val position: Offset,
    val isRoot: Boolean,
    val isLeaf: Boolean,
    val isCurrent: Boolean,
    val isEnding: Boolean,
    val choiceCount: Int,
    val depth: Int,
)

/**
 * Represents an edge connecting two nodes in the graph.
 */
data class GraphEdge(val sourceId: String, val targetId: String, val isChoiceLink: Boolean,)

/**
 * Complete graph data ready for rendering.
 */
data class GraphData(val nodes: List<GraphNode>, val edges: List<GraphEdge>,)

private data class NodeWithDepth(val node: StoryNodeResponse, val depth: Int, val children: List<NodeWithDepth>,)

/**
 * Transforms a flat list of StoryNodes into a positioned graph layout.
 *
 * Port of the web's `nodeTransform.ts`. Algorithm:
 * 1. Find root node (no parent_id) and build a tree from parent-child relationships.
 * 2. Group nodes by depth, calculate x/y positions with equal horizontal spacing
 *    per depth layer, centered around x=0.
 * 3. Create edges for parent-child links (solid) and choice-target links (dashed).
 *
 * All positions are in dp units, matching the composable node dimensions.
 * The graph composable converts these to pixels for Canvas drawing.
 *
 * Falls back to a grid layout if no root node is found.
 */
object GraphLayoutEngine {

    private const val NODE_GAP_H = 40f
    private const val NODE_GAP_V = 80f
    private const val VERTICAL_SPACING = NODE_HEIGHT_DP + NODE_GAP_V
    private const val HORIZONTAL_SPACING = NODE_WIDTH_DP + NODE_GAP_H

    fun layout(storyNodes: List<StoryNodeResponse>, currentNodeId: String?,): GraphData {
        if (storyNodes.isEmpty()) return GraphData(emptyList(), emptyList())

        val nodeMap = storyNodes.associateBy { it.id }
        val rootNode = storyNodes.find { it.parentId.isNullOrEmpty() }

        return if (rootNode != null) {
            layoutWithRoot(storyNodes, nodeMap, rootNode, currentNodeId)
        } else {
            layoutWithoutRoot(storyNodes, nodeMap, currentNodeId)
        }
    }

    private fun layoutWithRoot(
        storyNodes: List<StoryNodeResponse>,
        nodeMap: Map<String, StoryNodeResponse>,
        rootNode: StoryNodeResponse,
        currentNodeId: String?,
    ): GraphData {
        val tree = buildTreeWithDepth(rootNode, nodeMap, 0)
        val positions = calculateHierarchicalLayout(tree)
        val parentIds = storyNodes.mapNotNull { it.parentId }.toSet()

        val graphNodes = storyNodes.map { node ->
            val isRoot = node.parentId.isNullOrEmpty()
            val isLeaf = node.id !in parentIds
            val depth = positions[node.id]?.second ?: 0
            val position = positions[node.id]?.first ?: Offset.Zero

            GraphNode(
                id = node.id,
                title = node.title ?: "Untitled Node",
                position = position,
                isRoot = isRoot,
                isLeaf = isLeaf,
                isCurrent = node.id == currentNodeId,
                isEnding = isLeaf && node.choices.isEmpty(),
                choiceCount = node.choices.size,
                depth = depth,
            )
        }

        val edges = buildEdges(storyNodes, nodeMap)

        return GraphData(graphNodes, edges)
    }

    private fun layoutWithoutRoot(
        storyNodes: List<StoryNodeResponse>,
        nodeMap: Map<String, StoryNodeResponse>,
        currentNodeId: String?,
    ): GraphData {
        val parentIds = storyNodes.mapNotNull { it.parentId }.toSet()

        val graphNodes = storyNodes.mapIndexed { index, node ->
            val x = (index % 3) * HORIZONTAL_SPACING
            val y = (index / 3) * VERTICAL_SPACING

            GraphNode(
                id = node.id,
                title = node.title ?: "Untitled Node",
                position = Offset(x, y),
                isRoot = node.parentId.isNullOrEmpty(),
                isLeaf = node.id !in parentIds,
                isCurrent = node.id == currentNodeId,
                isEnding = (node.id !in parentIds) && node.choices.isEmpty(),
                choiceCount = node.choices.size,
                depth = 0,
            )
        }

        val edges = storyNodes.mapNotNull { node ->
            if (node.parentId != null && node.parentId in nodeMap) {
                GraphEdge(sourceId = node.parentId, targetId = node.id, isChoiceLink = false)
            } else {
                null
            }
        }

        return GraphData(graphNodes, edges)
    }

    private fun buildTreeWithDepth(
        node: StoryNodeResponse,
        nodeMap: Map<String, StoryNodeResponse>,
        depth: Int,
    ): NodeWithDepth {
        val children = nodeMap.values
            .filter { it.parentId == node.id }
            .map { buildTreeWithDepth(it, nodeMap, depth + 1) }

        return NodeWithDepth(node = node, depth = depth, children = children)
    }

    private fun calculateHierarchicalLayout(tree: NodeWithDepth,): Map<String, Pair<Offset, Int>> {
        val nodesByDepth = mutableMapOf<Int, MutableList<NodeWithDepth>>()
        collectNodesByDepth(tree, nodesByDepth)

        val positions = mutableMapOf<String, Pair<Offset, Int>>()
        nodesByDepth.forEach { (depth, nodesAtDepth) ->
            val y = depth * VERTICAL_SPACING
            val totalWidth = (nodesAtDepth.size - 1) * HORIZONTAL_SPACING
            val startX = -totalWidth / 2f

            nodesAtDepth.forEachIndexed { index, nodeWithDepth ->
                val x = startX + index * HORIZONTAL_SPACING
                positions[nodeWithDepth.node.id] = Pair(Offset(x, y), depth)
            }
        }

        return positions
    }

    private fun collectNodesByDepth(
        nodeWithDepth: NodeWithDepth,
        nodesByDepth: MutableMap<Int, MutableList<NodeWithDepth>>,
    ) {
        nodesByDepth.getOrPut(nodeWithDepth.depth) { mutableListOf() }.add(nodeWithDepth)
        nodeWithDepth.children.forEach { child ->
            collectNodesByDepth(child, nodesByDepth)
        }
    }

    private fun buildEdges(
        storyNodes: List<StoryNodeResponse>,
        nodeMap: Map<String, StoryNodeResponse>,
    ): List<GraphEdge> {
        val edges = mutableListOf<GraphEdge>()
        val edgeSet = mutableSetOf<String>()

        storyNodes.forEach { node ->
            // Parent-child edges (solid)
            if (node.parentId != null && node.parentId in nodeMap) {
                val edgeKey = "${node.parentId}-${node.id}"
                if (edgeKey !in edgeSet) {
                    edges.add(GraphEdge(sourceId = node.parentId, targetId = node.id, isChoiceLink = false))
                    edgeSet.add(edgeKey)
                }
            }

            // Choice-target edges (dashed, only if not already a parent-child edge)
            node.choices.forEach { choice ->
                val target = choice.target
                if (target != null && target in nodeMap) {
                    val edgeKey = "${node.id}-$target"
                    if (edgeKey !in edgeSet) {
                        edges.add(GraphEdge(sourceId = node.id, targetId = target, isChoiceLink = true))
                        edgeSet.add(edgeKey)
                    }
                }
            }
        }

        return edges
    }
}
