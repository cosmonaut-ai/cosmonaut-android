package com.cosmonaut.app.data.store

/**
 * Typed keys for Store5 instances.
 * Each key uniquely identifies a cached resource, analogous to TanStack Query's queryKey arrays.
 */

data class NodeKey(val worldId: String, val nodeId: String)

data class WorldKey(val worldId: String, val invite: String? = null)

data class WorldProgressKey(val worldId: String)

/**
 * Singleton key for resources that have exactly one instance (user profile, voice list).
 * Analogous to TanStack's `['user']` or `['voices']` keys.
 */
object UserKey

/**
 * Singleton key for the voice list (static data, cached indefinitely).
 * Analogous to TanStack's `['voices']` key with `staleTime: Infinity`.
 */
object VoiceListKey

/**
 * Key for paginated world list. Cursor is null for the first page.
 */
data class WorldListKey(val cursor: String? = null)
