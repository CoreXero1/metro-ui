package com.codeancy.metroui.firebase

import org.corexero.sutradhar.analytics.FirebaseAnalyticsTracker


object AnalyticsEvents {
    const val APP_LAUNCH = "app_launch"
    const val GET_ROUTE = "get_route"
    const val GET_RECENT_ROUTE = "get_recent_route"
    const val BOOK_TICKET = "book_ticket"
    const val METRO_MAP = "metro_map"
    const val FIRST_LAST_METRO = "first_last_metro"
    const val SOURCE_SELECT = "source_select"
    const val DEST_SELECT = "dest_select"
    const val ROUTE_LOAD_TIME = "route_load_time"
    const val REVIEW_POP_UP_SHOW = "review_pop_up_show"
    const val LAST_METRO_TIME_SOURCE_SELECT = "last_metro_time_source_select"
    const val LAST_METRO_TIME_DEST_SELECT = "last_metro_time_dest_select"

    const val LIVE_LOCATION_ENABLED = "live_location_enabled"
    const val LIVE_LOCATION_DISABLED = "live_location_disabled"
    const val LIVE_LOCATION_PERMISSION_DENIED = "live_location_permission_denied"
    const val NOT_INSIDE_METRO_ERROR = "not_inside_metro_error"
    const val LIVE_LOCATION_STARTED = "live_location_started"
    const val LIVE_LOCATION_STOPPED = "live_location_stopped"
    const val PREMIUM = "premium"
    const val ROUTE_SHARE_CLICKED = "route_share_clicked"
    const val ROUTE_VIEW_INTERCHANGE = "route_view_interchange"
    const val ROUTE_VIEW_STATION_LIST = "route_view_station_list"

}

object AnalyticsParams {
    const val APP_VERSION = "app_version"
    const val PLATFORM_TYPE = "platform_type"
    const val ERROR = "error"
    const val SOURCE_ID = "source_id"
    const val DEST_ID = "dest_id"
    const val SOURCE_NAME = "source_name"
    const val DEST_NAME = "dest_name"
    const val TIME = "time"
    const val VIEW_TYPE = "view_type"
    const val STATIONS = "stations"
    const val INTERCHANGES = "interchanges"
    const val FARE = "fare"
}

enum class ScreenName {
    HOME_SCREEN,
    ROUTE_SCREEN
}

fun FirebaseAnalyticsTracker.logEvent(
    eventName: String,
    screenName: ScreenName,
    eventParams: Map<String, Any> = emptyMap(),
) {
    val eventParams = mutableMapOf<String, Any>().apply {
        putAll(eventParams)
        put("screen_name", screenName.name)
    }
    logEvent(eventName, eventParams)
}
