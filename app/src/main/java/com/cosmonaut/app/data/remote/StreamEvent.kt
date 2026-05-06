package com.cosmonaut.app.data.remote

import com.cosmonaut.app.data.remote.dto.StoryNodeResponse

/**
 * Events emitted by [StreamingService] during SSE text generation.
 */
sealed interface StreamEvent {
    /** Token chunk received. [text] is the accumulated full text so far. */
    data class Token(val text: String) : StreamEvent

    /**
     * Stream completed successfully. [completedNode] is the final node
     * fetched from the server after the stream finishes.
     */
    data class Done(val completedNode: StoryNodeResponse) : StreamEvent

    /**
     * Stream-level error. May be quota exceeded, already-processed, or server error.
     */
    data class Error(val error: ApiError) : StreamEvent

    /**
     * Non-streaming JSON response (pre-generated or already-completed node).
     */
    data class PreGenerated(val node: StoryNodeResponse) : StreamEvent
}
