package com.codeancy.metroui.domain.repository

import com.codeancy.metroui.domain.models.LocationUi
import com.codeancy.metroui.domain.models.MetroMapDataUi
import com.codeancy.metroui.domain.models.NearestMetroStationUi
import com.codeancy.metroui.domain.models.StationUi

interface StationRepository {

    suspend fun getAllStations(): List<StationUi>

    suspend fun getNearestMetroStations(
        locationUi: LocationUi
    ): List<NearestMetroStationUi>

    suspend fun getFirstAndLastMetroTime(
        source: StationUi,
        destination: StationUi
    ): Pair<String, String>?

    suspend fun getMetroMapData(): MetroMapDataUi

}