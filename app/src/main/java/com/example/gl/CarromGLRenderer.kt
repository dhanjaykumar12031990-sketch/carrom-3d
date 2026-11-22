package com.example.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import com.example.game.AppScreen
import com.example.game.ArenaElementType
import com.example.game.BoardCatalog
import com.example.game.CameraMode
import com.example.game.CarromConfig
import com.example.game.CarromEngine
import com.example.game.GameMode
import com.example.game.PieceType
import com.example.game.RenderSceneSnapshot
import com.example.game.StrikerCatalog
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

class CarromGLRenderer(private val engine: CarromEngine) : GLSurfaceView.Renderer {

  private class GpuGeo(val posB: Int, val normB: Int, val uvB: Int, val idxB: Int, val count: Int)

  private class SceneMesh(
    val geo: GpuGeo,
    var color: FloatArray = floatArrayOf(1f, 1f, 1f),
    var roughness: Float = 0.4f,
    var metalness: Float = 0.05f,
    var opacity: Float = 1f,
    val transparent: Boolean = false,
    val unlit: Boolean = false,
    val doubleSide: Boolean = false,
    val castShadow: Boolean = false,
    val receiveShadow: Boolean = false,
    var mapTexId: Int = 0,
    var visible: Boolean = true,
  ) {
    val pos = Vec3()
    val rot = Vec3()
    val scale = Vec3(1f, 1f, 1f)
  }

  private class PieceNode(val body: SceneMesh, val cap: SceneMesh, val h: Float)
  private class ArenaNode(val primary: SceneMesh, val accent: SceneMesh, val type: ArenaElementType)

  private var width = 1
  private var height = 1
  private val shadowSize = 1024

  private var depthProg = 0
  private var mainProg = 0
  private var dLightVP = 0
  private var dModel = 0

  private var mVP = 0
  private var mModel = 0
  private var mNormalMat = 0
  private var mLightVP = 0
  private var mCamPos = 0
  private var mColor = 0
  private var mRoughness = 0
  private var mMetalness = 0
  private var mOpacity = 0
  private var mUnlit = 0
  private var mReceiveShadow = 0
  private var mHasMap = 0
  private var mMap = 0
  private var mShadowMap = 0

  private var shadowFbo = 0
  private var shadowTex = 0
  private var shadowRb = 0

  private val lightProj = FloatArray(16)
  private val lightView = FloatArray(16)
  private val lightVP = FloatArray(16)
  private val projMat = FloatArray(16)
  private val viewMat = FloatArray(16)
  private val vpMat = FloatArray(16)
  private val invVpMat = FloatArray(16)
  private val tmpModel = FloatArray(16)
  private val tmpNorm = FloatArray(9)

  private val staticMeshes = ArrayList<SceneMesh>()
  private val railMeshes = ArrayList<SceneMesh>()
  private val railTrimMeshes = ArrayList<SceneMesh>()
  private val studMeshes = ArrayList<SceneMesh>()
  private lateinit var surfaceMesh: SceneMesh

  private val pieceNodes = HashMap<Int, PieceNode>()
  private val arenaNodes = HashMap<Int, ArenaNode>()
  private val sparkMeshes = ArrayList<SceneMesh>()

  private lateinit var powerRing: SceneMesh
  private lateinit var laserBeam: SceneMesh
  private lateinit var ghostRing: SceneMesh
  private lateinit var deflectBeam: SceneMesh
  private lateinit var pullDot: SceneMesh
  private lateinit var pullTether: SceneMesh
  private lateinit var invalidRing: SceneMesh

  private lateinit var coinBodyGeo: GpuGeo
  private lateinit var coinCapGeo: GpuGeo
  private lateinit var strikerBodyGeo: GpuGeo
  private lateinit var strikerCapGeo: GpuGeo
  private lateinit var bumperBodyGeo: GpuGeo
  private lateinit var bumperCapGeo: GpuGeo
  private lateinit var speedPadOuterGeo: GpuGeo
  private lateinit var speedPadInnerGeo: GpuGeo
  private lateinit var deflectorBodyGeo: GpuGeo
  private lateinit var deflectorCapGeo: GpuGeo

  private var texWhite = 0
  private var texBlack = 0
  private var texQueen = 0
  private var texStriker = 0
  private val strikerTextures = HashMap<String, Int>()
  private val boardTextures = HashMap<String, Int>()
  private val railTextures = HashMap<String, Int>()
  private var activeStrikerSkinId = ""
  private var activeBoardThemeId = ""

  private val camCurPos = Vec3(0f, 10.5f, 12.2f)
  private val camTarPos = Vec3(0f, 10.5f, 12.2f)
  private val camCurLook = Vec3(0f, -0.35f, 0f)
  private val camTarLook = Vec3(0f, -0.35f, 0f)
  private var camCurAngle = 0f
  private var currentFov = 42f
  private var lastNs = 0L

  @Volatile var currentAppScreen: AppScreen = AppScreen.SPLASH
  @Volatile private var hasValidVP = false
  private val vpLock = Any()

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    staticMeshes.clear()
    railMeshes.clear()
    railTrimMeshes.clear()
    studMeshes.clear()
    pieceNodes.clear()
    arenaNodes.clear()
    sparkMeshes.clear()
    initShaders()
    initShadowFbo()

    M4.ortho(lightProj, -7.6f, 7.6f, -7.6f, 7.6f, 2f, 28f)
    M4.lookAt(lightView, Vec3(5f, 14.5f, 6f), Vec3(0f, 0f, 0f), Vec3(0f, 1f, 0f))
    M4.multiply(lightVP, lightProj, lightView)

    boardTextures.clear()
    railTextures.clear()
    val defaultTheme = BoardCatalog.findById(engine.getSelectedBoardId())
    val defaultBoardTex = ensureBoardTexture(defaultTheme.id)
    val defaultRailTex = ensureRailTexture(defaultTheme.id)
    activeBoardThemeId = defaultTheme.id

    texWhite = uploadTexture(CarromTextures.createPieceTopBitmap(PieceType.WHITE))
    texBlack = uploadTexture(CarromTextures.createPieceTopBitmap(PieceType.BLACK))
    texQueen = uploadTexture(CarromTextures.createPieceTopBitmap(PieceType.QUEEN))
    strikerTextures.clear()
    activeStrikerSkinId = ""
    val initialSkin = StrikerCatalog.findById(engine.getSelectedStrikerId())
    texStriker = ensureStrikerTexture(initialSkin.id)

    // Studio Table & Under-board Base
    val tableGeo = uploadGeo(CarromGeometry.buildCylinderGeo(17f, 17f, 0.4f, 48, false))
    staticMeshes.add(
      SceneMesh(tableGeo, ColorUtil.hexToRgb(0x100B09), roughness = 0.85f, metalness = 0.08f, receiveShadow = true).apply {
        pos.set(0f, -1.16f, 0f)
      }
    )
    val baseGeo = uploadGeo(CarromGeometry.buildBoxGeo(11.4f, 0.22f, 11.4f))
    staticMeshes.add(
      SceneMesh(baseGeo, ColorUtil.hexToRgb(0x1D110B), roughness = 0.65f, castShadow = true, receiveShadow = true).apply {
        pos.set(0f, -0.85f, 0f)
      }
    )

    // Extruded Playing Surface with 4 Physical Circular Pocket Holes
    val surfaceGeo = uploadGeo(CarromGeometry.buildBoardWithHolesGeo(CarromConfig.BOARD_HALF, CarromConfig.POCKET_OFFSET, CarromConfig.POCKET_R))
    surfaceMesh = SceneMesh(surfaceGeo, floatArrayOf(1f, 1f, 1f), roughness = 0.24f, metalness = 0.04f, receiveShadow = true, mapTexId = defaultBoardTex)
    staticMeshes.add(surfaceMesh)

    // 4 Deep Corner Pocket Shafts, Floor Cups & Rim Bezels
    val wallGeo = uploadGeo(CarromGeometry.buildCylinderGeo(CarromConfig.POCKET_R, CarromConfig.POCKET_R * 0.9f, 0.74f, 32, true))
    val cupGeo = uploadGeo(CarromGeometry.buildDiscGeo(CarromConfig.POCKET_R * 0.92f, 32))
    val rimGeo = uploadGeo(CarromGeometry.buildRingGeo(CarromConfig.POCKET_R - 0.005f, CarromConfig.POCKET_R + 0.045f, 36))
    for (pk in CarromConfig.POCKETS) {
      staticMeshes.add(SceneMesh(wallGeo, ColorUtil.hexToRgb(0x110C0A), roughness = 0.9f, doubleSide = true).apply { pos.set(pk.x, -0.37f, pk.z) })
      staticMeshes.add(SceneMesh(cupGeo, ColorUtil.hexToRgb(0x050404), roughness = 0.95f, receiveShadow = true).apply { pos.set(pk.x, -0.73f, pk.z) })
      staticMeshes.add(SceneMesh(rimGeo, ColorUtil.hexToRgb(0x26140C), roughness = 0.35f, metalness = 0.25f, doubleSide = true).apply { pos.set(pk.x, 0.002f, pk.z) })
    }

    // Raised Wooden Outer Borders + Top Trim + Brass Corner Studs
    val railW = 0.78f
    val railH = 1.24f
    val railY = -0.24f
    val railC = CarromConfig.BOARD_HALF + railW / 2f
    val railSpan = CarromConfig.BOARD_HALF * 2f + railW * 2f
    val railSpecs =
      arrayOf(
        floatArrayOf(railSpan, railW, 0f, -railC),
        floatArrayOf(railSpan, railW, 0f, railC),
        floatArrayOf(railW, CarromConfig.BOARD_HALF * 2f, -railC, 0f),
        floatArrayOf(railW, CarromConfig.BOARD_HALF * 2f, railC, 0f),
      )
    for (r in railSpecs) {
      val railM =
        SceneMesh(uploadGeo(CarromGeometry.buildBoxGeo(r[0], railH, r[1])), floatArrayOf(1f, 1f, 1f), roughness = 0.28f, metalness = 0.08f, castShadow = true, receiveShadow = true, mapTexId = defaultRailTex).apply {
          pos.set(r[2], railY, r[3])
        }
      railMeshes.add(railM)
      staticMeshes.add(railM)

      val trimM =
        SceneMesh(uploadGeo(CarromGeometry.buildBoxGeo(r[0] * 0.99f, 0.06f, r[1] * 0.82f)), ColorUtil.hexToRgb(0x28130A), roughness = 0.32f, metalness = 0.1f, castShadow = true).apply {
          pos.set(r[2], railY + railH / 2f + 0.02f, r[3])
        }
      railTrimMeshes.add(trimM)
      staticMeshes.add(trimM)
    }

    val studGeo = uploadGeo(CarromGeometry.buildCylinderGeo(0.11f, 0.13f, 0.06f, 16, false))
    for (sx in floatArrayOf(-1f, 1f)) {
      for (sz in floatArrayOf(-1f, 1f)) {
        val st =
          SceneMesh(studGeo, ColorUtil.hexToRgb(0xD4A348), roughness = 0.25f, metalness = 0.85f, castShadow = true).apply {
            pos.set(sx * railC, railY + railH / 2f + 0.05f, sz * railC)
          }
        studMeshes.add(st)
        staticMeshes.add(st)
      }
    }

    // Chaos Arena Element Geometries
    bumperBodyGeo = uploadGeo(CarromGeometry.buildCylinderGeo(0.30f, 0.34f, 0.26f, 24, false))
    bumperCapGeo = uploadGeo(CarromGeometry.buildRingGeo(0.14f, 0.36f, 24))
    speedPadOuterGeo = uploadGeo(CarromGeometry.buildRingGeo(0.34f, 0.56f, 28))
    speedPadInnerGeo = uploadGeo(CarromGeometry.buildRingGeo(0.14f, 0.28f, 24))
    deflectorBodyGeo = uploadGeo(CarromGeometry.buildCylinderGeo(0.20f, 0.24f, 0.22f, 16, false))
    deflectorCapGeo = uploadGeo(CarromGeometry.buildDiscGeo(0.20f, 16))

    // Beveled Coin & Striker Geometries
    fun buildPieceGeos(r: Float, h: Float): Pair<GpuGeo, GpuGeo> {
      val b = r * 0.1f
      val prof = listOf(0f to 0f, (r - b) to 0f, r to h * 0.18f, r to h * 0.82f, (r - b) to h, 0f to h)
      return uploadGeo(CarromGeometry.buildLatheGeo(prof, 32)) to uploadGeo(CarromGeometry.buildDiscGeo(r - b * 0.85f, 32))
    }
    buildPieceGeos(CarromConfig.COIN_R, CarromConfig.COIN_H).let {
      coinBodyGeo = it.first
      coinCapGeo = it.second
    }
    buildPieceGeos(CarromConfig.STRIKER_R, CarromConfig.STRIKER_H).let {
      strikerBodyGeo = it.first
      strikerCapGeo = it.second
    }

    // Aiming & Feedback Visuals
    powerRing = SceneMesh(uploadGeo(CarromGeometry.buildRingGeo(CarromConfig.STRIKER_R * 1.15f, CarromConfig.STRIKER_R * 1.46f, 48)), ColorUtil.hexToRgb(0x00F5D4), opacity = 0.88f, transparent = true, unlit = true, doubleSide = true, visible = false)
    laserBeam = SceneMesh(uploadGeo(CarromGeometry.buildBoxGeo(0.055f, 0.012f, 1f)), ColorUtil.hexToRgb(0x00F5D4), opacity = 0.85f, transparent = true, unlit = true, visible = false)
    ghostRing = SceneMesh(uploadGeo(CarromGeometry.buildRingGeo(CarromConfig.STRIKER_R * 0.86f, CarromConfig.STRIKER_R * 1.05f, 32)), floatArrayOf(1f, 1f, 1f), opacity = 0.78f, transparent = true, unlit = true, doubleSide = true, visible = false)
    deflectBeam = SceneMesh(uploadGeo(CarromGeometry.buildBoxGeo(0.04f, 0.01f, 1f)), ColorUtil.hexToRgb(0xFFD166), opacity = 0.82f, transparent = true, unlit = true, visible = false)
    pullDot = SceneMesh(uploadGeo(CarromGeometry.buildSphereGeo(0.11f, 12, 12)), ColorUtil.hexToRgb(0xFFBE0B), opacity = 0.9f, transparent = true, unlit = true, visible = false)
    pullTether = SceneMesh(uploadGeo(CarromGeometry.buildBoxGeo(0.032f, 0.01f, 1f)), ColorUtil.hexToRgb(0xFFBE0B), opacity = 0.65f, transparent = true, unlit = true, visible = false)
    invalidRing = SceneMesh(uploadGeo(CarromGeometry.buildRingGeo(CarromConfig.STRIKER_R * 1.08f, CarromConfig.STRIKER_R * 1.38f, 32)), ColorUtil.hexToRgb(0xFF3B5C), opacity = 0.85f, transparent = true, unlit = true, doubleSide = true, visible = false)

    val sparkGeo = uploadGeo(CarromGeometry.buildSphereGeo(0.04f, 6, 6))
    repeat(96) {
      sparkMeshes.add(SceneMesh(sparkGeo, ColorUtil.hexToRgb(0xFFD166), opacity = 1f, transparent = true, unlit = true, visible = false))
    }

    lastNs = System.nanoTime()
  }

  override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
    width = max(1, w)
    height = max(1, h)
  }

  override fun onDrawFrame(gl: GL10?) {
    val nowNs = System.nanoTime()
    val dt = if (lastNs == 0L) 0.016f else ((nowNs - lastNs) / 1_000_000_000.0).toFloat()
    lastNs = nowNs

    val snap = engine.stepFrame(dt)
    currentAppScreen = snap.appScreen
    updateCamera(snap, dt)
    syncSnapshotToMeshes(snap, nowNs)
    renderScene()
  }

  private fun updateCamera(snap: RenderSceneSnapshot, dt: Float) {
    val aspect = width.toFloat() / max(1, height).toFloat()
    val portraitScale = if (aspect < 0.78f) 0.78f / max(0.42f, aspect) else 1f
    currentFov = if (aspect < 0.7f) 45f else 40f

    when (snap.appScreen) {
      AppScreen.SPLASH, AppScreen.AUTH -> {
        val m = portraitScale.pow(0.82f)
        val r = 12.6f * m
        val ang = snap.previewOrbitRad
        camTarPos.set(sin(ang) * r, 9.2f * m, cos(ang) * r)
        camTarLook.set(0f, -0.3f, 0f)
      }
      AppScreen.LOBBY -> {
        val m = portraitScale.pow(0.84f)
        val r = 12.8f * m
        val ang = snap.previewOrbitRad
        camTarPos.set(sin(ang) * r, 9.4f * m, cos(ang) * r)
        camTarLook.set(0f, -0.7f, 0f)
      }
      AppScreen.GAMEPLAY -> {
        val targetAngle =
          when (snap.gameMode) {
            GameMode.DESI_ADDA_2V2, GameMode.DESI_ADDA_FFA -> (snap.currentPlayer % 4) * (PI / 2.0).toFloat()
            else -> 0f
          }
        val diff = atan2(sin(targetAngle - camCurAngle), cos(targetAngle - camCurAngle))
        val angleLerp = 1f - exp(-6.5f * min(0.05f, max(0.001f, dt)))
        camCurAngle += diff * angleLerp

        val sinA = sin(camCurAngle)
        val cosA = cos(camCurAngle)
        if (snap.cameraMode == CameraMode.TOP_DOWN) {
          val dist = 0.025f
          camTarPos.set(sinA * dist, 15.8f * portraitScale.pow(0.88f), cosA * dist)
          camTarLook.set(0f, 0f, 0f)
        } else {
          val m = portraitScale.pow(0.85f)
          val dist = 10.6f * m
          camTarPos.set(sinA * dist, 11.8f * m, cosA * dist)
          camTarLook.set(sinA * 0.25f, -0.35f, cosA * 0.25f)
        }
      }
    }

    val lerpT = 1f - exp(-7.5f * min(0.05f, max(0.001f, dt)))
    camCurPos.lerp(camTarPos, lerpT)
    camCurLook.lerp(camTarLook, lerpT)

    synchronized(vpLock) {
      M4.perspective(projMat, currentFov, aspect, 0.1f, 100f)
      val horizDist = hypot(camCurPos.x - camCurLook.x, camCurPos.z - camCurLook.z)
      val up =
        if (snap.cameraMode == CameraMode.TOP_DOWN && snap.appScreen == AppScreen.GAMEPLAY && horizDist < 0.2f) {
          Vec3(-sin(camCurAngle), 0f, -cos(camCurAngle))
        } else {
          Vec3(0f, 1f, 0f)
        }
      M4.lookAt(viewMat, camCurPos, camCurLook, up)
      M4.multiply(vpMat, projMat, viewMat)
      hasValidVP = M4.invert(invVpMat, vpMat)
    }
  }

  private fun syncSnapshotToMeshes(snap: RenderSceneSnapshot, nowNs: Long) {
    if (activeBoardThemeId != snap.selectedBoardId) {
      val theme = BoardCatalog.findById(snap.selectedBoardId)
      surfaceMesh.mapTexId = ensureBoardTexture(theme.id)
      val rTex = ensureRailTexture(theme.id)
      for (rm in railMeshes) rm.mapTexId = rTex
      val trimCol = ColorUtil.hexToRgb(theme.railTrimHex)
      for (tm in railTrimMeshes) tm.color = trimCol
      val studCol = ColorUtil.hexToRgb(theme.studHex)
      for (sm in studMeshes) sm.color = studCol
      activeBoardThemeId = theme.id
    }

    for (p in snap.pieces) {
      val node =
        pieceNodes.getOrPut(p.id) {
          val isStriker = p.type == PieceType.STRIKER
          val bGeo = if (isStriker) strikerBodyGeo else coinBodyGeo
          val cGeo = if (isStriker) strikerCapGeo else coinCapGeo
          val (col, rough, metal, tex) =
            when (p.type) {
              PieceType.WHITE -> Quadruple(ColorUtil.hexToRgb(0xF2E6CF), 0.20f, 0.08f, texWhite)
              PieceType.BLACK -> Quadruple(ColorUtil.hexToRgb(0x1B1D24), 0.18f, 0.15f, texBlack)
              PieceType.QUEEN -> Quadruple(ColorUtil.hexToRgb(0xB5173E), 0.16f, 0.12f, texQueen)
              PieceType.STRIKER -> Quadruple(ColorUtil.hexToRgb(0xDBE9F1), 0.14f, 0.18f, texStriker)
            }
          PieceNode(
            body = SceneMesh(bGeo, col, roughness = rough, metalness = metal, castShadow = true, receiveShadow = true),
            cap = SceneMesh(cGeo, floatArrayOf(1f, 1f, 1f), roughness = if (isStriker) 0.14f else 0.22f, metalness = 0.06f, receiveShadow = true, mapTexId = tex),
            h = p.height,
          )
        }
      if (p.type == PieceType.STRIKER && activeStrikerSkinId != snap.selectedStrikerId) {
        val skin = StrikerCatalog.findById(snap.selectedStrikerId)
        node.body.color = ColorUtil.hexToRgb(skin.bodyColorHex)
        node.body.roughness = skin.roughness
        node.body.metalness = skin.metalness
        node.cap.mapTexId = ensureStrikerTexture(skin.id)
        activeStrikerSkinId = skin.id
      }
      node.body.visible = p.visible
      node.cap.visible = p.visible
      node.body.pos.set(p.x, p.y, p.z)
      node.body.rot.set(p.tiltX, p.spinY, p.tiltZ)
      val h = node.h + 0.001f
      node.cap.pos.set(
        p.x + sin(p.tiltZ) * h,
        p.y + cos(p.tiltX) * cos(p.tiltZ) * h,
        p.z - sin(p.tiltX) * h,
      )
      node.cap.rot.set(p.tiltX, p.spinY, p.tiltZ)
    }

    // Remove unused piece nodes when switching racks (e.g. Challenge -> Classic)
    val activePieceIds = snap.pieces.mapTo(HashSet()) { it.id }
    val pIter = pieceNodes.entries.iterator()
    while (pIter.hasNext()) {
      if (pIter.next().key !in activePieceIds) pIter.remove()
    }

    // Chaos Arena Elements
    val activeArenaIds = snap.arenaElements.mapTo(HashSet()) { it.id }
    val aIter = arenaNodes.entries.iterator()
    while (aIter.hasNext()) {
      if (aIter.next().key !in activeArenaIds) aIter.remove()
    }
    val timeSec = (nowNs / 1_000_000_000.0).toFloat()
    val pulse = 0.92f + 0.12f * sin(timeSec * 4.5f)
    for (el in snap.arenaElements) {
      val aNode =
        arenaNodes.getOrPut(el.id) {
          when (el.type) {
            ArenaElementType.BUMPER ->
              ArenaNode(
                primary = SceneMesh(bumperBodyGeo, ColorUtil.hexToRgb(0xFFD166), roughness = 0.15f, metalness = 0.78f, castShadow = true, receiveShadow = true),
                accent = SceneMesh(bumperCapGeo, ColorUtil.hexToRgb(0xFF4D6D), opacity = 0.92f, transparent = true, unlit = true, doubleSide = true),
                type = el.type,
              )
            ArenaElementType.SPEED_PAD ->
              ArenaNode(
                primary = SceneMesh(speedPadOuterGeo, ColorUtil.hexToRgb(0x00F5D4), opacity = 0.85f, transparent = true, unlit = true, doubleSide = true),
                accent = SceneMesh(speedPadInnerGeo, ColorUtil.hexToRgb(0xFFBE0B), opacity = 0.9f, transparent = true, unlit = true, doubleSide = true),
                type = el.type,
              )
            ArenaElementType.DEFLECTOR ->
              ArenaNode(
                primary = SceneMesh(deflectorBodyGeo, ColorUtil.hexToRgb(0x3B1E12), roughness = 0.3f, metalness = 0.25f, castShadow = true, receiveShadow = true),
                accent = SceneMesh(deflectorCapGeo, ColorUtil.hexToRgb(0xD4A348), roughness = 0.2f, metalness = 0.85f),
                type = el.type,
              )
          }
        }
      when (aNode.type) {
        ArenaElementType.BUMPER -> {
          aNode.primary.pos.set(el.x, 0.13f, el.z)
          aNode.accent.pos.set(el.x, 0.27f, el.z)
          aNode.accent.scale.set(pulse, 1f, pulse)
        }
        ArenaElementType.SPEED_PAD -> {
          aNode.primary.pos.set(el.x, 0.014f, el.z)
          aNode.primary.scale.set(pulse, 1f, pulse)
          aNode.accent.pos.set(el.x, 0.017f, el.z)
          aNode.accent.scale.set(1.1f - (pulse - 0.92f), 1f, 1.1f - (pulse - 0.92f))
        }
        ArenaElementType.DEFLECTOR -> {
          aNode.primary.pos.set(el.x, 0.11f, el.z)
          aNode.accent.pos.set(el.x, 0.222f, el.z)
        }
      }
    }

    val aim = snap.aim
    if (aim.visible) {
      val rgb = ColorUtil.hslToRgb(0.48f - aim.power * 0.48f, 0.95f, 0.56f)
      val yaw = atan2(aim.dirX, aim.dirZ)
      powerRing.visible = true
      powerRing.color = rgb
      powerRing.pos.set(aim.strikerX, 0.016f, aim.strikerZ)
      val rs = 0.85f + aim.power * 0.65f
      powerRing.scale.set(rs, 1f, rs)

      laserBeam.visible = true
      laserBeam.color = rgb
      laserBeam.pos.set(aim.strikerX + aim.dirX * (aim.beamLength * 0.5f), 0.04f, aim.strikerZ + aim.dirZ * (aim.beamLength * 0.5f))
      laserBeam.scale.set(1f, 1f, aim.beamLength)
      laserBeam.rot.set(0f, yaw, 0f)

      ghostRing.visible = true
      ghostRing.color = rgb
      ghostRing.pos.set(aim.impactX, 0.016f, aim.impactZ)

      if (aim.hasTargetHit) {
        val defLen = 1.4f
        deflectBeam.visible = true
        deflectBeam.pos.set(aim.targetX + aim.deflectDirX * (defLen * 0.5f), 0.04f, aim.targetZ + aim.deflectDirZ * (defLen * 0.5f))
        deflectBeam.scale.set(1f, 1f, defLen)
        deflectBeam.rot.set(0f, atan2(aim.deflectDirX, aim.deflectDirZ), 0f)
      } else {
        deflectBeam.visible = false
      }

      val tx = aim.pullX - aim.strikerX
      val tz = aim.pullZ - aim.strikerZ
      val tLen = hypot(tx, tz)
      pullDot.visible = true
      pullDot.pos.set(aim.pullX, 0.08f, aim.pullZ)
      pullTether.visible = tLen > 0.05f
      if (pullTether.visible) {
        pullTether.pos.set(aim.strikerX + tx * 0.5f, 0.05f, aim.strikerZ + tz * 0.5f)
        pullTether.scale.set(1f, 1f, tLen)
        pullTether.rot.set(0f, atan2(tx, tz), 0f)
      }
    } else {
      powerRing.visible = false
      laserBeam.visible = false
      ghostRing.visible = false
      deflectBeam.visible = false
      pullDot.visible = false
      pullTether.visible = false
    }

    invalidRing.visible = snap.invalidRingVisible
    if (invalidRing.visible) {
      invalidRing.pos.set(snap.strikerX, 0.018f, snap.strikerZ)
      val invPulse = 1f + sin(timeSec * 12f) * 0.1f
      invalidRing.scale.set(invPulse, 1f, invPulse)
    }

    for (i in sparkMeshes.indices) {
      val m = sparkMeshes[i]
      if (i < snap.sparks.size) {
        val sp = snap.sparks[i]
        m.visible = true
        m.pos.set(sp.x, sp.y, sp.z)
        m.scale.set(sp.scale, sp.scale, sp.scale)
        m.opacity = sp.opacity
        m.color = floatArrayOf(sp.r, sp.g, sp.b)
      } else {
        m.visible = false
      }
    }
  }

  private fun renderScene() {
    val allMeshes = ArrayList<SceneMesh>(staticMeshes.size + pieceNodes.size * 2 + arenaNodes.size * 2 + 110)
    allMeshes.addAll(staticMeshes)
    for (node in arenaNodes.values) {
      allMeshes.add(node.primary)
      allMeshes.add(node.accent)
    }
    for (node in pieceNodes.values) {
      allMeshes.add(node.body)
      allMeshes.add(node.cap)
    }
    allMeshes.add(powerRing)
    allMeshes.add(laserBeam)
    allMeshes.add(ghostRing)
    allMeshes.add(deflectBeam)
    allMeshes.add(pullDot)
    allMeshes.add(pullTether)
    allMeshes.add(invalidRing)
    allMeshes.addAll(sparkMeshes)

    // 1. Shadow Pass
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, shadowFbo)
    GLES20.glViewport(0, 0, shadowSize, shadowSize)
    GLES20.glClearColor(1f, 1f, 1f, 1f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    GLES20.glEnable(GLES20.GL_DEPTH_TEST)
    GLES20.glDepthMask(true)
    GLES20.glDisable(GLES20.GL_BLEND)
    GLES20.glEnable(GLES20.GL_CULL_FACE)
    GLES20.glCullFace(GLES20.GL_BACK)

    GLES20.glUseProgram(depthProg)
    GLES20.glUniformMatrix4fv(dLightVP, 1, false, lightVP, 0)
    GLES20.glEnableVertexAttribArray(0)
    GLES20.glDisableVertexAttribArray(1)
    GLES20.glDisableVertexAttribArray(2)

    for (m in allMeshes) {
      if (!m.visible || !m.castShadow) continue
      M4.compose(tmpModel, m.pos, m.rot, m.scale)
      GLES20.glUniformMatrix4fv(dModel, 1, false, tmpModel, 0)
      GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m.geo.posB)
      GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, 0)
      GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, m.geo.idxB)
      GLES20.glDrawElements(GLES20.GL_TRIANGLES, m.geo.count, GLES20.GL_UNSIGNED_SHORT, 0)
    }

    // 2. Main Studio PBR Pass
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    GLES20.glViewport(0, 0, width, height)
    GLES20.glClearColor(0.036f, 0.028f, 0.024f, 1f)
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

    GLES20.glUseProgram(mainProg)
    GLES20.glUniformMatrix4fv(mVP, 1, false, vpMat, 0)
    GLES20.glUniformMatrix4fv(mLightVP, 1, false, lightVP, 0)
    GLES20.glUniform3f(mCamPos, camCurPos.x, camCurPos.y, camCurPos.z)

    GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shadowTex)
    GLES20.glUniform1i(mShadowMap, 1)
    GLES20.glUniform1i(mMap, 0)

    GLES20.glEnableVertexAttribArray(0)
    GLES20.glEnableVertexAttribArray(1)
    GLES20.glEnableVertexAttribArray(2)

    fun drawMesh(m: SceneMesh) {
      M4.compose(tmpModel, m.pos, m.rot, m.scale)
      val a = tmpModel
      val a00 = a[0]; val a01 = a[1]; val a02 = a[2]
      val a10 = a[4]; val a11 = a[5]; val a12 = a[6]
      val a20 = a[8]; val a21 = a[9]; val a22 = a[10]
      val b01 = a22 * a11 - a12 * a21
      val b11 = -a22 * a10 + a12 * a20
      val b21 = a21 * a10 - a11 * a20
      var det = a00 * b01 + a01 * b11 + a02 * b21
      det = if (det != 0f) 1f / det else 1f
      val n = tmpNorm
      n[0] = b01 * det; n[1] = b11 * det; n[2] = b21 * det
      n[3] = (-a22 * a01 + a02 * a21) * det; n[4] = (a22 * a00 - a02 * a20) * det; n[5] = (-a21 * a00 + a01 * a20) * det
      n[6] = (a12 * a01 - a02 * a11) * det; n[7] = (-a12 * a00 + a02 * a10) * det; n[8] = (a11 * a00 - a01 * a10) * det

      if (m.doubleSide) GLES20.glDisable(GLES20.GL_CULL_FACE) else GLES20.glEnable(GLES20.GL_CULL_FACE)
      GLES20.glUniformMatrix4fv(mModel, 1, false, tmpModel, 0)
      GLES20.glUniformMatrix3fv(mNormalMat, 1, false, n, 0)
      GLES20.glUniform3f(mColor, m.color[0], m.color[1], m.color[2])
      GLES20.glUniform1f(mRoughness, m.roughness)
      GLES20.glUniform1f(mMetalness, m.metalness)
      GLES20.glUniform1f(mOpacity, m.opacity)
      GLES20.glUniform1f(mUnlit, if (m.unlit) 1f else 0f)
      GLES20.glUniform1f(mReceiveShadow, if (m.receiveShadow) 1f else 0f)

      if (m.mapTexId != 0) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, m.mapTexId)
        GLES20.glUniform1f(mHasMap, 1f)
      } else {
        GLES20.glUniform1f(mHasMap, 0f)
      }

      GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m.geo.posB)
      GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, 0)
      GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m.geo.normB)
      GLES20.glVertexAttribPointer(1, 3, GLES20.GL_FLOAT, false, 0, 0)
      GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m.geo.uvB)
      GLES20.glVertexAttribPointer(2, 2, GLES20.GL_FLOAT, false, 0, 0)
      GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, m.geo.idxB)
      GLES20.glDrawElements(GLES20.GL_TRIANGLES, m.geo.count, GLES20.GL_UNSIGNED_SHORT, 0)
    }

    GLES20.glDepthMask(true)
    GLES20.glDisable(GLES20.GL_BLEND)
    for (m in allMeshes) if (m.visible && !m.transparent) drawMesh(m)

    GLES20.glDepthMask(false)
    GLES20.glEnable(GLES20.GL_BLEND)
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    for (m in allMeshes) if (m.visible && m.transparent) drawMesh(m)
    GLES20.glDepthMask(true)
  }

  fun screenToBoardXZ(touchX: Float, touchY: Float): Pair<Float, Float>? {
    synchronized(vpLock) {
      if (!hasValidVP || width <= 0 || height <= 0) return null
      val ndcX = (touchX / width.toFloat()) * 2f - 1f
      val ndcY = -(touchY / height.toFloat()) * 2f + 1f
      val p0 = M4.transformVec4(invVpMat, ndcX, ndcY, -1f, 1f)
      val p1 = M4.transformVec4(invVpMat, ndcX, ndcY, 1f, 1f)
      val dy = p1.y - p0.y
      if (kotlin.math.abs(dy) < 1e-6f) return null
      val t = -p0.y / dy
      return (p0.x + (p1.x - p0.x) * t) to (p0.z + (p1.z - p0.z) * t)
    }
  }

  private fun compileShader(type: Int, src: String): Int {
    val s = GLES20.glCreateShader(type)
    GLES20.glShaderSource(s, src)
    GLES20.glCompileShader(s)
    return s
  }

  private fun linkProgram(vs: String, fs: String): Int {
    val p = GLES20.glCreateProgram()
    GLES20.glAttachShader(p, compileShader(GLES20.GL_VERTEX_SHADER, vs))
    GLES20.glAttachShader(p, compileShader(GLES20.GL_FRAGMENT_SHADER, fs))
    GLES20.glBindAttribLocation(p, 0, "aPos")
    GLES20.glBindAttribLocation(p, 1, "aNorm")
    GLES20.glBindAttribLocation(p, 2, "aUv")
    GLES20.glLinkProgram(p)
    return p
  }

  private fun initShaders() {
    val depthVS =
      """
      precision highp float;
      attribute vec3 aPos;
      uniform mat4 uLightVP;
      uniform mat4 uModel;
      void main() {
        gl_Position = uLightVP * uModel * vec4(aPos, 1.0);
      }
      """.trimIndent()
    val depthFS =
      """
      precision highp float;
      vec4 packDepth(float d) {
        float d255 = clamp(d, 0.0, 0.99999) * 255.0;
        float b0 = floor(d255);
        float r1 = (d255 - b0) * 255.0;
        float b1 = floor(r1);
        float b2 = r1 - b1;
        return vec4(b0 / 255.0, b1 / 255.0, b2, 1.0);
      }
      void main() {
        gl_FragColor = packDepth(gl_FragCoord.z);
      }
      """.trimIndent()
    depthProg = linkProgram(depthVS, depthFS)
    dLightVP = GLES20.glGetUniformLocation(depthProg, "uLightVP")
    dModel = GLES20.glGetUniformLocation(depthProg, "uModel")

    val mainVS =
      """
      precision highp float;
      attribute vec3 aPos;
      attribute vec3 aNorm;
      attribute vec2 aUv;
      uniform mat4 uVP;
      uniform mat4 uModel;
      uniform mat3 uNormalMat;
      uniform mat4 uLightVP;
      varying vec3 vWorldPos;
      varying vec3 vNorm;
      varying vec2 vUv;
      varying vec4 vLightPos;
      void main() {
        vec4 wp = uModel * vec4(aPos, 1.0);
        vWorldPos = wp.xyz;
        vNorm = normalize(uNormalMat * aNorm);
        vUv = aUv;
        vLightPos = uLightVP * wp;
        gl_Position = uVP * wp;
      }
      """.trimIndent()
    val mainFS =
      """
      precision highp float;
      varying vec3 vWorldPos;
      varying vec3 vNorm;
      varying vec2 vUv;
      varying vec4 vLightPos;
      uniform vec3 uCamPos;
      uniform vec3 uColor;
      uniform float uRoughness;
      uniform float uMetalness;
      uniform float uOpacity;
      uniform float uUnlit;
      uniform float uReceiveShadow;
      uniform float uHasMap;
      uniform sampler2D uMap;
      uniform sampler2D uShadowMap;

      float unpackDepth(vec4 c) {
        return c.r + c.g / 255.0 + c.b / 65025.0;
      }

      float calcShadow(vec3 N, vec3 L) {
        if (uReceiveShadow < 0.5) return 1.0;
        vec3 proj = vLightPos.xyz / vLightPos.w;
        proj = proj * 0.5 + 0.5;
        if (proj.x < 0.0 || proj.x > 1.0 || proj.y < 0.0 || proj.y > 1.0 || proj.z > 1.0) return 1.0;
        float bias = max(0.0028 * (1.0 - dot(N, L)), 0.0011);
        float texel = 1.25 / 1024.0;
        float vis = 0.0;
        for (int x = -1; x <= 1; x++) {
          for (int y = -1; y <= 1; y++) {
            float sd = unpackDepth(texture2D(uShadowMap, proj.xy + vec2(float(x), float(y)) * texel));
            vis += (proj.z - bias <= sd) ? 1.0 : 0.0;
          }
        }
        return vis / 9.0;
      }

      void main() {
        vec3 albedo = uColor;
        if (uHasMap > 0.5) albedo *= pow(texture2D(uMap, vUv).rgb, vec3(2.2));
        else albedo = pow(albedo, vec3(2.2));

        if (uUnlit > 0.5) {
          gl_FragColor = vec4(pow(albedo, vec3(1.0 / 2.2)), uOpacity);
          return;
        }

        vec3 N = normalize(vNorm);
        vec3 V = normalize(uCamPos - vWorldPos);
        vec3 L = normalize(vec3(5.0, 14.5, 6.0));
        vec3 L2 = normalize(vec3(-7.0, 6.0, -7.0));
        vec3 H = normalize(L + V);
        vec3 H2 = normalize(L2 + V);

        float shadow = calcShadow(N, L);
        float hemiT = N.y * 0.5 + 0.5;
        vec3 ambient = mix(vec3(0.08, 0.06, 0.05), vec3(0.34, 0.29, 0.23), hemiT) * albedo * (1.0 - uMetalness * 0.65);

        float ndl = max(dot(N, L), 0.0);
        float rough = clamp(uRoughness, 0.08, 1.0);
        float shininess = mix(220.0, 12.0, rough * rough);
        float specPow = pow(max(dot(N, H), 0.0), shininess) * (0.15 + (1.0 - rough) * 1.45);
        vec3 F0 = mix(vec3(0.045), albedo, uMetalness);
        vec3 fresnel = F0 + (1.0 - F0) * pow(1.0 - max(dot(N, V), 0.0), 4.0);
        vec3 diffuse = albedo * (1.0 - uMetalness) * ndl * vec3(1.42, 1.36, 1.24);
        vec3 spec = fresnel * specPow * vec3(1.35, 1.32, 1.26);

        float ndl2 = max(dot(N, L2), 0.0);
        float spec2 = pow(max(dot(N, H2), 0.0), shininess * 0.6) * (1.0 - rough) * 0.55;
        vec3 rim = (albedo * (1.0 - uMetalness) * ndl2 * 0.34 + fresnel * spec2) * vec3(0.95, 0.78, 0.45);

        vec3 R = reflect(-V, N);
        float softbox = smoothstep(0.62, 0.94, R.y) * (1.0 - rough);
        vec3 envRefl = fresnel * softbox * 0.68;

        vec3 linearCol = ambient + (diffuse + spec) * (0.28 + 0.72 * shadow) + rim + envRefl;
        linearCol = (linearCol * (2.51 * linearCol + 0.03)) / (linearCol * (2.43 * linearCol + 0.59) + 0.14);
        gl_FragColor = vec4(pow(clamp(linearCol, 0.0, 1.0), vec3(1.0 / 2.2)), uOpacity);
      }
      """.trimIndent()
    mainProg = linkProgram(mainVS, mainFS)
    mVP = GLES20.glGetUniformLocation(mainProg, "uVP")
    mModel = GLES20.glGetUniformLocation(mainProg, "uModel")
    mNormalMat = GLES20.glGetUniformLocation(mainProg, "uNormalMat")
    mLightVP = GLES20.glGetUniformLocation(mainProg, "uLightVP")
    mCamPos = GLES20.glGetUniformLocation(mainProg, "uCamPos")
    mColor = GLES20.glGetUniformLocation(mainProg, "uColor")
    mRoughness = GLES20.glGetUniformLocation(mainProg, "uRoughness")
    mMetalness = GLES20.glGetUniformLocation(mainProg, "uMetalness")
    mOpacity = GLES20.glGetUniformLocation(mainProg, "uOpacity")
    mUnlit = GLES20.glGetUniformLocation(mainProg, "uUnlit")
    mReceiveShadow = GLES20.glGetUniformLocation(mainProg, "uReceiveShadow")
    mHasMap = GLES20.glGetUniformLocation(mainProg, "uHasMap")
    mMap = GLES20.glGetUniformLocation(mainProg, "uMap")
    mShadowMap = GLES20.glGetUniformLocation(mainProg, "uShadowMap")
  }

  private fun initShadowFbo() {
    val texArr = IntArray(1)
    GLES20.glGenTextures(1, texArr, 0)
    shadowTex = texArr[0]
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, shadowTex)
    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, shadowSize, shadowSize, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

    val rbArr = IntArray(1)
    GLES20.glGenRenderbuffers(1, rbArr, 0)
    shadowRb = rbArr[0]
    GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, shadowRb)
    GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, shadowSize, shadowSize)

    val fboArr = IntArray(1)
    GLES20.glGenFramebuffers(1, fboArr, 0)
    shadowFbo = fboArr[0]
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, shadowFbo)
    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, shadowTex, 0)
    GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, shadowRb)
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
  }

  private fun uploadGeo(geo: RawGeo): GpuGeo {
    val ids = IntArray(4)
    GLES20.glGenBuffers(4, ids, 0)

    val posBuf = ByteBuffer.allocateDirect(geo.pos.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(geo.pos); position(0) }
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ids[0])
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, geo.pos.size * 4, posBuf, GLES20.GL_STATIC_DRAW)

    val normBuf = ByteBuffer.allocateDirect(geo.norm.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(geo.norm); position(0) }
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ids[1])
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, geo.norm.size * 4, normBuf, GLES20.GL_STATIC_DRAW)

    val uvBuf = ByteBuffer.allocateDirect(geo.uv.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(geo.uv); position(0) }
    GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ids[2])
    GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, geo.uv.size * 4, uvBuf, GLES20.GL_STATIC_DRAW)

    val idxBuf = ByteBuffer.allocateDirect(geo.idx.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer().apply { put(geo.idx); position(0) }
    GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ids[3])
    GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, geo.idx.size * 2, idxBuf, GLES20.GL_STATIC_DRAW)

    return GpuGeo(ids[0], ids[1], ids[2], ids[3], geo.idx.size)
  }

  private fun ensureBoardTexture(themeId: String): Int =
    boardTextures.getOrPut(themeId) {
      uploadTexture(CarromTextures.createBoardBitmap(BoardCatalog.findById(themeId)))
    }

  private fun ensureRailTexture(themeId: String): Int =
    railTextures.getOrPut(themeId) {
      uploadTexture(CarromTextures.createRailBitmap(BoardCatalog.findById(themeId)))
    }

  private fun ensureStrikerTexture(skinId: String): Int =
    strikerTextures.getOrPut(skinId) {
      uploadTexture(CarromTextures.createStrikerSkinBitmap(StrikerCatalog.findById(skinId)))
    }

  private fun uploadTexture(bmp: Bitmap): Int {
    val texArr = IntArray(1)
    GLES20.glGenTextures(1, texArr, 0)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texArr[0])
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
    GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    bmp.recycle()
    return texArr[0]
  }

  private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
