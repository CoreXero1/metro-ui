package com.codeancy.metroui.ads

interface InterstitialAdController {
    fun preload()
    fun show(onFinish: () -> Unit)
    fun showWithResult(onFinish: (adShown: Boolean) -> Unit) {
        show { onFinish(true) }
    }
}