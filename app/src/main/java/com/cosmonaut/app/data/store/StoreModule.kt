package com.cosmonaut.app.data.store

import com.cosmonaut.app.data.remote.CosmoApiService
import com.cosmonaut.app.data.remote.dto.PaginatedWorldsResponse
import com.cosmonaut.app.data.remote.dto.StoryNodeResponse
import com.cosmonaut.app.data.remote.dto.UsageResponse
import com.cosmonaut.app.data.remote.dto.VoiceResponse
import com.cosmonaut.app.data.remote.dto.WorldProgressResponse
import com.cosmonaut.app.data.remote.dto.WorldResponse
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.MemoryPolicy
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import org.mobilenativefoundation.store.store5.Validator

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NodeStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WorldDetailStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WorldListStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WorldProgressStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UserStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class VoiceStore

private const val NODE_STALE_MS = 5 * 60 * 1000L
private const val WORLD_STALE_MS = 5 * 60 * 1000L
private const val USER_STALE_MS = 2 * 60 * 1000L

/**
 * Provides all Store5 instances.
 *
 * Architecture decisions:
 * - Memory-only (no Room SourceOfTruth). The app is online-only; the in-memory cache
 *   with Validator-based staleness gives TanStack-like behavior without offline support.
 *   Room SOT can be added later for offline capability.
 * - Each Store is a Singleton matching the web's global QueryClient-per-resource model.
 * - Validators implement time-based staleness equivalent to TanStack's staleTime.
 */
@Module
@InstallIn(SingletonComponent::class)
object StoreModule {

    @Provides
    @Singleton
    @NodeStore
    fun provideNodeStore(api: CosmoApiService): Store<NodeKey, StoryNodeResponse> = StoreBuilder.from(
        fetcher = Fetcher.of { key: NodeKey ->
            api.getNode(key.worldId, key.nodeId)
        },
    )
        .validator(
            Validator.by { node: StoryNodeResponse ->
                if (!node.isCompleted) return@by false
                val age = System.currentTimeMillis() - node.fetchedAtMs
                age < NODE_STALE_MS
            },
        )
        .cachePolicy(
            MemoryPolicy.builder<NodeKey, StoryNodeResponse>()
                .setMaxSize(100)
                .setExpireAfterAccess(10.minutes)
                .build(),
        )
        .build()

    @Provides
    @Singleton
    @WorldDetailStore
    fun provideWorldDetailStore(api: CosmoApiService): Store<WorldKey, WorldResponse> = StoreBuilder.from(
        fetcher = Fetcher.of { key: WorldKey ->
            api.getWorld(key.worldId, key.invite)
        },
    )
        .validator(
            Validator.by { world: WorldResponse ->
                if (!world.isCompleted) return@by false
                val age = System.currentTimeMillis() - world.fetchedAtMs
                age < WORLD_STALE_MS
            },
        )
        .cachePolicy(
            MemoryPolicy.builder<WorldKey, WorldResponse>()
                .setMaxSize(50)
                .setExpireAfterAccess(10.minutes)
                .build(),
        )
        .build()

    @Provides
    @Singleton
    @WorldListStore
    fun provideWorldListStore(api: CosmoApiService): Store<WorldListKey, PaginatedWorldsResponse> = StoreBuilder.from(
        fetcher = Fetcher.of { key: WorldListKey ->
            api.getWorlds(key.cursor)
        },
    )
        .cachePolicy(
            MemoryPolicy.builder<WorldListKey, PaginatedWorldsResponse>()
                .setMaxSize(20)
                .setExpireAfterAccess(5.minutes)
                .build(),
        )
        .build()

    @Provides
    @Singleton
    @WorldProgressStore
    fun provideWorldProgressStore(api: CosmoApiService,): Store<WorldProgressKey, WorldProgressResponse> =
        StoreBuilder.from(
            fetcher = Fetcher.of { key: WorldProgressKey ->
                api.getWorldProgress(key.worldId)
            },
        )
            .cachePolicy(
                MemoryPolicy.builder<WorldProgressKey, WorldProgressResponse>()
                    .setMaxSize(20)
                    .setExpireAfterAccess(5.minutes)
                    .build(),
            )
            .build()

    @Provides
    @Singleton
    @UserStore
    fun provideUserStore(api: CosmoApiService): Store<UserKey, UsageResponse> = StoreBuilder.from(
        fetcher = Fetcher.of { _: UserKey ->
            api.getUsage()
        },
    )
        .validator(
            Validator.by { usage: UsageResponse ->
                val age = System.currentTimeMillis() - usage.fetchedAtMs
                age < USER_STALE_MS
            },
        )
        .cachePolicy(
            MemoryPolicy.builder<UserKey, UsageResponse>()
                .setMaxSize(1)
                .setExpireAfterAccess(10.minutes)
                .build(),
        )
        .build()

    @Provides
    @Singleton
    @VoiceStore
    fun provideVoiceStore(api: CosmoApiService): Store<VoiceListKey, List<VoiceResponse>> = StoreBuilder.from(
        fetcher = Fetcher.of { _: VoiceListKey ->
            api.listVoices()
        },
    )
        .cachePolicy(
            MemoryPolicy.builder<VoiceListKey, List<VoiceResponse>>()
                .setMaxSize(1)
                .setExpireAfterAccess(24.hours)
                .build(),
        )
        .build()
}
