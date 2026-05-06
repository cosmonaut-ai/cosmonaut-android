package com.cosmonaut.app.data.remote

import com.cosmonaut.app.BuildConfig
import com.cosmonaut.app.data.remote.dto.StoryNodeResponse
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource

private const val POST_STREAM_DELAY_MS = 500L
private const val MIN_TOKEN_INTERVAL_MS = 30L

/**
 * Handles SSE-based story text generation using the streaming OkHttpClient.
 * Emits [StreamEvent]s as a cold [Flow] that can be cancelled via coroutine cancellation.
 *
 * The flow lifecycle:
 * 1. POST to /generate-text
 * 2. Parse SSE events, emitting [StreamEvent.Token] with accumulated text
 * 3. On [DONE] or stream end, wait 500ms then GET the final node
 * 4. Emit [StreamEvent.Done] with the completed node
 *
 * Cancelling the collecting coroutine closes the HTTP connection cleanly.
 */
@Singleton
class StreamingService @Inject constructor(
    @param:Named("streaming") private val streamingClient: OkHttpClient,
    private val apiService: CosmoApiService,
    private val json: Json,
) {

    fun generateNodeText(worldId: String, nodeId: String): Flow<StreamEvent> = flow {
        val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')
        val url = "$baseUrl/worlds/$worldId/nodes/$nodeId/generate-text"

        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody("application/json".toMediaType()))
            .build()

        val response = streamingClient.newCall(request).execute()

        try {
            if (!response.isSuccessful) {
                val body = response.body.string()
                emit(StreamEvent.Error(ApiError.fromResponseBody(response.code, body)))
                return@flow
            }

            val contentType = response.header("Content-Type") ?: ""

            if (contentType.contains("text/event-stream") || contentType.contains("text/plain")) {
                val source = response.body.source()
                parseAndEmitSSE(source, worldId, nodeId)
            } else {
                val body = response.body.string()
                val node = json.decodeFromString<StoryNodeResponse>(body)
                emit(StreamEvent.PreGenerated(node))
            }
        } finally {
            response.close()
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun kotlinx.coroutines.flow.FlowCollector<StreamEvent>.parseAndEmitSSE(
        source: BufferedSource,
        worldId: String,
        nodeId: String,
    ) {
        var fullText = ""
        var streamComplete = false
        var currentEventType: String? = null
        var lastEmitTime = 0L

        while (!source.exhausted()) {
            currentCoroutineContext().ensureActive()

            val line = source.readUtf8Line() ?: break

            when {
                line.startsWith("event:") -> {
                    currentEventType = line.removePrefix("event:").trim()
                }
                line.startsWith("data:") -> {
                    val data = line.removePrefix("data:").trim()

                    if (currentEventType == "error") {
                        emit(StreamEvent.Error(ApiError.fromStreamEvent(data)))
                        return
                    }

                    if (data == "[DONE]") {
                        streamComplete = true
                        break
                    }

                    val processedContent = data.replace("\\n", "\n")
                    fullText += processedContent

                    val now = System.currentTimeMillis()
                    val elapsed = now - lastEmitTime
                    if (elapsed < MIN_TOKEN_INTERVAL_MS) {
                        delay(MIN_TOKEN_INTERVAL_MS - elapsed)
                    }
                    emit(StreamEvent.Token(fullText))
                    lastEmitTime = System.currentTimeMillis()

                    currentEventType = null
                }
                line.isBlank() -> {
                    currentEventType = null
                }
            }
        }

        if (!streamComplete && fullText.isNotEmpty()) {
            emit(StreamEvent.Token(fullText))
        }

        currentCoroutineContext().ensureActive()
        delay(POST_STREAM_DELAY_MS)

        val completedNode = apiService.getNode(worldId, nodeId)
        emit(StreamEvent.Done(completedNode))
    }
}
