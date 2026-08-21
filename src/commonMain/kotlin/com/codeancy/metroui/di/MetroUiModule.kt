package com.codeancy.metroui.di

import com.codeancy.metroui.app.DeepLinkNavigator
import com.codeancy.metroui.home.HomeViewModel
import com.codeancy.metroui.map.MapViewModel
import com.codeancy.metroui.route.RouteViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val metroUiModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::RouteViewModel)
    viewModelOf(::MapViewModel)
    singleOf(::DeepLinkNavigator)
    includes(platFormMetroUiModule)
}

expect val platFormMetroUiModule: Module