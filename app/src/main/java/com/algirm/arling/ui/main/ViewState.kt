package com.algirm.arling.ui.main

import com.algirm.arling.data.model.ARPoint

sealed class ViewState {
    data class SelectedPoint(val arPoint: ARPoint?) : ViewState()
    data class TouchedXY(val x: Float, val y: Float) : ViewState()
    object Init : ViewState()
}
