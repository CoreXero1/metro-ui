package com.codeancy.metroui.domain.repository

import com.codeancy.metroui.domain.models.RecentRouteResult
import com.codeancy.metroui.domain.models.RouteResultUi
import kotlinx.coroutines.flow.Flow

interface RouteRepository {

    suspend fun getRoute(
        sourceId: Long,
        destinationId: Long
    ): RouteResultUi

    suspend fun updatePlatForms(
        routeResultUi: RouteResultUi
    ): RouteResultUi

    suspend fun getRecentSearches(): Flow<List<RecentRouteResult>>

    suspend fun getRecentSearch(sourceId: Long, destinationId: Long): RecentRouteResult?

}

/**
 * Thrown by [RouteRepository.getRoute] when no path exists between the two
 * stations (disconnected lines, sparse city data, or an unknown station
 * code in the adjacency topology).
 *
 * Lives in the `domain.repository` package because it's part of the
 * repository contract — callers in metro-ui (ViewModels) catch this to
 * render an error state instead of letting the coroutine crash the app.
 *
 * The previous crash was a raw `NoSuchElementException` from `List.first()`
 * on the empty `findRoutes(...)` result; that bubbled to the IO dispatcher's
 * uncaught handler and killed the process.
 */
class NoRouteFoundException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)