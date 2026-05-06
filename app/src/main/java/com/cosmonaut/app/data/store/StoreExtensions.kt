package com.cosmonaut.app.data.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Collects a Store stream until the first Data or Error emission.
 * Skips Loading, NoNewData, and Initial responses.
 * Throws on error responses, returns the value on Data.
 *
 * Use this when you need a one-shot suspend call from a Store stream,
 * equivalent to TanStack's awaited query result.
 */
suspend fun <T : Any> Flow<StoreReadResponse<T>>.firstData(): T {
    val response = first { it is StoreReadResponse.Data || it is StoreReadResponse.Error }
    return when (response) {
        is StoreReadResponse.Data -> response.value
        is StoreReadResponse.Error.Exception -> throw response.error
        is StoreReadResponse.Error.Message -> throw IllegalStateException(response.message)
        else -> throw IllegalStateException("Store stream completed without emitting data")
    }
}
