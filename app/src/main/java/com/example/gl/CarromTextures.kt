package com.example.gl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import com.example.game.BoardCatalog
import com.example.game.BoardTheme
import com.example.game.CarromConfig
import com.example.game.PieceType
import com.example.game.StrikerCatalog
import com.example.game.StrikerSkin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

object CarromTextures {

  private fun worldToCanvas(wx: Float, wz: Float, size: Int): Pair<Float, Float> {
    val span = CarromConfig.BOARD_HALF * 2f
    return ((wx + CarromConfig.BOARD_HALF) / span) * size to
      ((wz + CarromConfig.BOARD_HALF) / span) * size
  }

  fun createBoardBitmap(theme: BoardTheme = BoardCatalog.all.first()): Bitmap {
    val s = 1024
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val lightCol = Color.parseColor(theme.surfaceLightHex)
    val midCol = Color.parseColor(theme.surfaceMidHex)
    val edgeCol = Color.parseColor(theme.surfaceEdgeHex)
    val primaryLineCol = Color.parseColor(theme.linePrimaryHex)
    val accentLineCol = Color.parseColor(theme.lineAccentHex)

    paint.style = Paint.Style.FILL
    paint.shader =
      RadialGradient(
        s * 0.5f,
        s * 0.5f,
        s * 0.72f,
        intArrayOf(lightCol, midCol, edgeCol),
        floatArrayOf(0f, 0.65f, 1f),
        Shader.TileMode.CLAMP,
      )
    canvas.drawRect(0f, 0f, s.toFloat(), s.toFloat(), paint)
    paint.shader = null

    val rng = Random(42)
    paint.style = Paint.Style.STROKE
    val path = Path()
    for (i in 0 until 160) {
      val y = (i / 160f) * s + (rng.nextFloat() - 0.5f) * 3f
      paint.color =
        if (i % 2 == 0) Color.argb(14, 120, 80, 35) else Color.argb(14, 255, 245, 220)
      paint.strokeWidth = 1f + rng.nextFloat() * 1.8f
      path.reset()
      path.moveTo(0f, y)
      path.cubicTo(
        s * 0.33f,
        y + (rng.nextFloat() - 0.5f) * 10f,
        s * 0.66f,
        y + (rng.nextFloat() - 0.5f) * 10f,
        s.toFloat(),
        y,
      )
      canvas.drawPath(path, paint)
    }

    val unit = s / (CarromConfig.BOARD_HALF * 2f)
    val cx = s / 2f
    val cy = s / 2f
    val scale = s / 2048f

    paint.style = Paint.Style.STROKE
    paint.color = primaryLineCol
    paint.strokeWidth = 5f * scale
    canvas.drawRect(24f * scale, 24f * scale, s - 24f * scale, s - 24f * scale, paint)

    val outerCircleR = 1.24f * unit
    val innerQueenR = 0.29f * unit
    paint.color = primaryLineCol
    paint.strokeWidth = 5f * scale
    canvas.drawCircle(cx, cy, outerCircleR, paint)
    paint.color = accentLineCol
    paint.strokeWidth = 3.5f * scale
    canvas.drawCircle(cx, cy, outerCircleR - 14f * scale, paint)

    paint.style = Paint.Style.FILL
    val accentFill = Color.argb(217, Color.red(accentLineCol), Color.green(accentLineCol), Color.blue(accentLineCol))
    val primaryFill = Color.argb(217, Color.red(primaryLineCol), Color.green(primaryLineCol), Color.blue(primaryLineCol))
    for (k in 0 until 8) {
      val ang = (k * PI / 4.0).toFloat()
      val next = ang + (PI / 8.0).toFloat()
      val prev = ang - (PI / 8.0).toFloat()
      path.reset()
      path.moveTo(cx + cos(ang) * (outerCircleR - 16f * scale), cy + sin(ang) * (outerCircleR - 16f * scale))
      path.lineTo(cx + cos(next) * (innerQueenR + 18f * scale), cy + sin(next) * (innerQueenR + 18f * scale))
      path.lineTo(cx, cy)
      path.lineTo(cx + cos(prev) * (innerQueenR + 18f * scale), cy + sin(prev) * (innerQueenR + 18f * scale))
      path.close()
      paint.color = if (k % 2 == 0) accentFill else primaryFill
      canvas.drawPath(path, paint)
    }

    paint.color = accentLineCol
    canvas.drawCircle(cx, cy, innerQueenR, paint)
    paint.style = Paint.Style.STROKE
    paint.color = primaryLineCol
    paint.strokeWidth = 3.5f * scale
    canvas.drawCircle(cx, cy, innerQueenR + 6f * scale, paint)

    fun drawBaseline(horizontal: Boolean, signVal: Float) {
      val d = CarromConfig.BASELINE_Z * signVal
      val halfLen = CarromConfig.BASELINE_LIMIT
      val bandHalf = 0.16f
      val p1 = if (horizontal) worldToCanvas(-halfLen, d - bandHalf, s) else worldToCanvas(d - bandHalf, -halfLen, s)
      val p2 = if (horizontal) worldToCanvas(halfLen, d - bandHalf, s) else worldToCanvas(d - bandHalf, halfLen, s)
      val p3 = if (horizontal) worldToCanvas(-halfLen, d + bandHalf, s) else worldToCanvas(d + bandHalf, -halfLen, s)
      val p4 = if (horizontal) worldToCanvas(halfLen, d + bandHalf, s) else worldToCanvas(d + bandHalf, halfLen, s)

      paint.style = Paint.Style.STROKE
      paint.color = primaryLineCol
      paint.strokeWidth = 5f * scale
      canvas.drawLine(p1.first, p1.second, p2.first, p2.second, paint)
      paint.strokeWidth = 3f * scale
      canvas.drawLine(p3.first, p3.second, p4.first, p4.second, paint)

      for (endCoord in floatArrayOf(-halfLen, halfLen)) {
        val ep = if (horizontal) worldToCanvas(endCoord, d, s) else worldToCanvas(d, endCoord, s)
        val endR = bandHalf * unit
        paint.style = Paint.Style.FILL
        paint.color = accentLineCol
        canvas.drawCircle(ep.first, ep.second, endR, paint)
        paint.style = Paint.Style.STROKE
        paint.color = primaryLineCol
        paint.strokeWidth = 4f * scale
        canvas.drawCircle(ep.first, ep.second, endR, paint)
        paint.style = Paint.Style.FILL
        paint.color = midCol
        canvas.drawCircle(ep.first, ep.second, endR * 0.32f, paint)
      }
    }

    drawBaseline(true, 1f)
    drawBaseline(true, -1f)
    drawBaseline(false, 1f)
    drawBaseline(false, -1f)

    for (p in CarromConfig.POCKETS) {
      val sx = sign(p.x)
      val sz = sign(p.z)
      val start = worldToCanvas(sx * 1.5f, sz * 1.5f, s)
      val end = worldToCanvas(sx * 3.66f, sz * 3.66f, s)
      paint.style = Paint.Style.STROKE
      paint.color = primaryLineCol
      paint.strokeWidth = 4f * scale
      canvas.drawLine(start.first, start.second, end.first, end.second, paint)

      val arcCenter = worldToCanvas(sx * 3.38f, sz * 3.38f, s)
      val baseAngDeg = Math.toDegrees(kotlin.math.atan2(sz.toDouble(), sx.toDouble())).toFloat()
      val rArc = 0.34f * unit
      val rect = RectF(arcCenter.first - rArc, arcCenter.second - rArc, arcCenter.first + rArc, arcCenter.second + rArc)
      canvas.drawArc(rect, baseAngDeg - 120f, 240f, false, paint)

      val tip = worldToCanvas(sx * 3.76f, sz * 3.76f, s)
      paint.style = Paint.Style.FILL
      canvas.drawCircle(tip.first, tip.second, 6f * scale, paint)
    }
    return bmp
  }

  fun createRailBitmap(theme: BoardTheme = BoardCatalog.all.first()): Bitmap {
    val s = 256
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(Color.parseColor(theme.railBaseHex))
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val rng = Random(99)
    val path = Path()
    for (i in 0 until 80) {
      val y = (i / 80f) * s
      paint.color = if (i % 3 == 0) Color.argb(97, 20, 8, 4) else Color.argb(55, 215, 165, 95)
      paint.strokeWidth = 1.2f + rng.nextFloat() * 2.4f
      path.reset()
      path.moveTo(0f, y)
      path.cubicTo(
        85f,
        y + (rng.nextFloat() - 0.5f) * 12f,
        170f,
        y + (rng.nextFloat() - 0.5f) * 12f,
        s.toFloat(),
        y,
      )
      canvas.drawPath(path, paint)
    }
    return bmp
  }

  fun createPieceTopBitmap(type: PieceType): Bitmap {
    val s = 256
    val m = s / 2f
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    when (type) {
      PieceType.WHITE -> {
        paint.shader =
          RadialGradient(
            m * 0.85f,
            m * 0.85f,
            m,
            intArrayOf(Color.parseColor("#FFFDF7"), Color.parseColor("#F2E6CF"), Color.parseColor("#D9C5A0")),
            floatArrayOf(0f, 0.75f, 1f),
            Shader.TileMode.CLAMP,
          )
        canvas.drawRect(0f, 0f, s.toFloat(), s.toFloat(), paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        intArrayOf(96, 72, 46, 22).forEachIndexed { idx, r ->
          paint.color = if (idx % 2 == 0) Color.argb(115, 170, 145, 105) else Color.argb(165, 255, 255, 255)
          canvas.drawCircle(m, m, r.toFloat(), paint)
        }
      }
      PieceType.BLACK -> {
        paint.shader =
          RadialGradient(
            m * 0.85f,
            m * 0.85f,
            m,
            intArrayOf(Color.parseColor("#4A505C"), Color.parseColor("#23262E"), Color.parseColor("#12141A")),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP,
          )
        canvas.drawRect(0f, 0f, s.toFloat(), s.toFloat(), paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        intArrayOf(96, 72, 46, 22).forEachIndexed { idx, r ->
          paint.color = if (idx % 2 == 0) Color.argb(38, 255, 255, 255) else Color.argb(128, 0, 0, 0)
          canvas.drawCircle(m, m, r.toFloat(), paint)
        }
      }
      PieceType.QUEEN -> {
        paint.shader =
          RadialGradient(
            m * 0.85f,
            m * 0.85f,
            m,
            intArrayOf(Color.parseColor("#FF4D6D"), Color.parseColor("#C9184A"), Color.parseColor("#7A0928")),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP,
          )
        canvas.drawRect(0f, 0f, s.toFloat(), s.toFloat(), paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(200, 255, 214, 102)
        paint.strokeWidth = 5f
        canvas.drawCircle(m, m, 82f, paint)
        paint.strokeWidth = 3f
        canvas.drawCircle(m, m, 44f, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#FFD166")
        canvas.drawCircle(m, m, 15f, paint)
      }
      PieceType.STRIKER -> return createStrikerSkinBitmap(StrikerCatalog.all.first())
    }
    return bmp
  }

  fun createStrikerSkinBitmap(skin: StrikerSkin): Bitmap {
    val s = 256
    val m = s / 2f
    val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader =
      RadialGradient(
        m * 0.85f,
        m * 0.85f,
        m,
        intArrayOf(
          Color.parseColor(skin.baseLightHex),
          Color.parseColor(skin.baseEdgeHex),
        ),
        floatArrayOf(0f, 1f),
        Shader.TileMode.CLAMP,
      )
    canvas.drawRect(0f, 0f, s.toFloat(), s.toFloat(), paint)
    paint.shader = null
    paint.style = Paint.Style.STROKE
    paint.color = Color.parseColor(skin.outerRingHex)
    paint.strokeWidth = 8f
    canvas.drawCircle(m, m, 100f, paint)
    paint.color = Color.parseColor(skin.innerRingHex)
    paint.strokeWidth = 4f
    canvas.drawCircle(m, m, 74f, paint)
    val star = Path()
    for (i in 0 until 12) {
      val r = if (i % 2 == 0) 64f else 28f
      val a = (i * PI / 6.0).toFloat()
      val px = m + cos(a) * r
      val py = m + sin(a) * r
      if (i == 0) star.moveTo(px, py) else star.lineTo(px, py)
    }
    star.close()
    paint.style = Paint.Style.FILL
    paint.color = Color.parseColor(skin.starHex)
    canvas.drawPath(star, paint)
    paint.color = Color.parseColor(skin.centerDotHex)
    canvas.drawCircle(m, m, 15f, paint)
    return bmp
  }
}
