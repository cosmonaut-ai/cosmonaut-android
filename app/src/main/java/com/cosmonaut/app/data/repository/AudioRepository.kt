package com.cosmonaut.app.data.repository

import com.cosmonaut.app.data.remote.CosmoApiService
import com.cosmonaut.app.data.remote.dto.GenerateAudioRequest
import com.cosmonaut.app.data.remote.dto.GenerateAudioResponse
import com.cosmonaut.app.data.remote.dto.VoiceResponse
import com.cosmonaut.app.data.store.VoiceListKey
import com.cosmonaut.app.data.store.VoiceStore
import com.cosmonaut.app.data.store.firstData
import javax.inject.Inject
import javax.inject.Singleton
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest

/**
 * Repository for voice data and audio generation.
 *
 * Voice list is cached indefinitely (static data), mirroring the web's
 * `useVoices()` with `staleTime: Infinity`.
 *
 * Audio generation is a one-shot mutation — idempotent per node+voice pair
 * (server returns cached URL if already generated).
 */
@Singleton
class AudioRepository @Inject constructor(
    @param:VoiceStore private val voiceStore: Store<VoiceListKey, List<VoiceResponse>>,
    private val apiService: CosmoApiService,
) {

    suspend fun getVoices(): List<VoiceResponse> =
        voiceStore.stream(StoreReadRequest.cached(VoiceListKey, refresh = false)).firstData()

    /**
     * Generate audio narration for a node with a specific voice.
     * Idempotent: returns the cached CDN URL if audio was already generated.
     */
    suspend fun generateNodeAudio(worldId: String, nodeId: String, voiceId: String,): GenerateAudioResponse =
        apiService.generateNodeAudio(
            worldId = worldId,
            nodeId = nodeId,
            request = GenerateAudioRequest(voiceId = voiceId),
        )
}
