package com.example.gl

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import com.example.game.AppScreen
import com.example.game.CarromEngine

@SuppressLint("ViewConstructor")
class CarromGLSurfaceView(
  context: Context,
  private val engine: CarromEngine,
) : GLSurfaceView(context) {

  private val glRenderer = CarromGLRenderer(engine)
  private var lastTouchX = 0f

  init {
    setEGLContextClientVersion(2)
    setEGLConfigChooser(8, 8, 8, 8, 16, 0)
    preserveEGLContextOnPause = true
    setRenderer(glRenderer)
    renderMode = RENDERMODE_CONTINUOUSLY
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    if (glRenderer.currentAppScreen == AppScreen.LOBBY) {
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          lastTouchX = event.x
          return true
        }
        MotionEvent.ACTION_MOVE -> {
          val dx = event.x - lastTouchX
          lastTouchX = event.x
          engine.addPreviewOrbitDelta(dx * 0.006f)
          return true
        }
      }
      return true
    }

    if (glRenderer.currentAppScreen != AppScreen.GAMEPLAY) return false

    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        val pt = glRenderer.screenToBoardXZ(event.x, event.y) ?: return true
        engine.onPointerDown(pt.first, pt.second)
        return true
      }
      MotionEvent.ACTION_MOVE -> {
        val pt = glRenderer.screenToBoardXZ(event.x, event.y) ?: return true
        engine.onPointerMove(pt.first, pt.second)
        return true
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        engine.onPointerUp()
        return true
      }
    }
    return super.onTouchEvent(event)
  }
}
