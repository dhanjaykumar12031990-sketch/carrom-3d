package com.example.gl

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

class RawGeo(
  val pos: FloatArray,
  val norm: FloatArray,
  val uv: FloatArray,
  val idx: ShortArray,
)

object CarromGeometry {
  fun buildBoxGeo(w: Float, h: Float, d: Float): RawGeo {
    val hw = w / 2f
    val hh = h / 2f
    val hd = d / 2f
    val pos = ArrayList<Float>()
    val norm = ArrayList<Float>()
    val uv = ArrayList<Float>()
    val idx = ArrayList<Short>()

    fun face(
      p0: FloatArray,
      p1: FloatArray,
      p2: FloatArray,
      p3: FloatArray,
      nx: Float,
      ny: Float,
      nz: Float,
    ) {
      val base = (pos.size / 3).toShort()
      pos.addAll(p0.toList())
      pos.addAll(p1.toList())
      pos.addAll(p2.toList())
      pos.addAll(p3.toList())
      repeat(4) {
        norm.add(nx)
        norm.add(ny)
        norm.add(nz)
      }
      uv.addAll(listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f))
      idx.add(base)
      idx.add((base + 1).toShort())
      idx.add((base + 2).toShort())
      idx.add(base)
      idx.add((base + 2).toShort())
      idx.add((base + 3).toShort())
    }

    face(floatArrayOf(-hw, -hh, hd), floatArrayOf(hw, -hh, hd), floatArrayOf(hw, hh, hd), floatArrayOf(-hw, hh, hd), 0f, 0f, 1f)
    face(floatArrayOf(hw, -hh, -hd), floatArrayOf(-hw, -hh, -hd), floatArrayOf(-hw, hh, -hd), floatArrayOf(hw, hh, -hd), 0f, 0f, -1f)
    face(floatArrayOf(-hw, hh, hd), floatArrayOf(hw, hh, hd), floatArrayOf(hw, hh, -hd), floatArrayOf(-hw, hh, -hd), 0f, 1f, 0f)
    face(floatArrayOf(-hw, -hh, -hd), floatArrayOf(hw, -hh, -hd), floatArrayOf(hw, -hh, hd), floatArrayOf(-hw, -hh, hd), 0f, -1f, 0f)
    face(floatArrayOf(hw, -hh, hd), floatArrayOf(hw, -hh, -hd), floatArrayOf(hw, hh, -hd), floatArrayOf(hw, hh, hd), 1f, 0f, 0f)
    face(floatArrayOf(-hw, -hh, -hd), floatArrayOf(-hw, -hh, hd), floatArrayOf(-hw, hh, hd), floatArrayOf(-hw, hh, -hd), -1f, 0f, 0f)

    return RawGeo(pos.toFloatArray(), norm.toFloatArray(), uv.toFloatArray(), idx.toShortArray())
  }

  fun buildLatheGeo(profile: List<Pair<Float, Float>>, segs: Int): RawGeo {
    val pos = ArrayList<Float>()
    val norm = ArrayList<Float>()
    val uv = ArrayList<Float>()
    val idx = ArrayList<Short>()
    var maxR = 0.001f
    for ((r, _) in profile) if (r > maxR) maxR = r

    for (i in 0 until profile.size - 1) {
      val (r0, y0) = profile[i]
      val (r1, y1) = profile[i + 1]
      val dr = r1 - r0
      val dy = y1 - y0
      val nLen = hypot(dr, dy).coerceAtLeast(1e-5f)
      val nr = dy / nLen
      val ny = -dr / nLen
      val base = pos.size / 3

      for (s in 0..segs) {
        val t = (s.toFloat() / segs) * (PI * 2.0).toFloat()
        val ct = cos(t)
        val st = -sin(t)
        pos.add(r0 * ct); pos.add(y0); pos.add(r0 * st)
        norm.add(nr * ct); norm.add(ny); norm.add(nr * st)
        uv.add(0.5f + (r0 * ct) / (2f * maxR)); uv.add(0.5f + (r0 * st) / (2f * maxR))

        pos.add(r1 * ct); pos.add(y1); pos.add(r1 * st)
        norm.add(nr * ct); norm.add(ny); norm.add(nr * st)
        uv.add(0.5f + (r1 * ct) / (2f * maxR)); uv.add(0.5f + (r1 * st) / (2f * maxR))
      }
      for (s in 0 until segs) {
        val a = (base + s * 2).toShort()
        val b = (a + 1).toShort()
        val c = (a + 2).toShort()
        val d = (a + 3).toShort()
        idx.add(a); idx.add(c); idx.add(b)
        idx.add(c); idx.add(d); idx.add(b)
      }
    }
    return RawGeo(pos.toFloatArray(), norm.toFloatArray(), uv.toFloatArray(), idx.toShortArray())
  }

  fun buildCylinderGeo(rTop: Float, rBot: Float, h: Float, segs: Int, openEnded: Boolean): RawGeo {
    val prof = ArrayList<Pair<Float, Float>>()
    if (!openEnded) prof.add(0f to -h / 2f)
    prof.add(rBot to -h / 2f)
    prof.add(rTop to h / 2f)
    if (!openEnded) prof.add(0f to h / 2f)
    return buildLatheGeo(prof, segs)
  }

  fun buildDiscGeo(r: Float, segs: Int): RawGeo {
    val pos = ArrayList<Float>()
    val norm = ArrayList<Float>()
    val uv = ArrayList<Float>()
    val idx = ArrayList<Short>()
    pos.addAll(listOf(0f, 0f, 0f))
    norm.addAll(listOf(0f, 1f, 0f))
    uv.addAll(listOf(0.5f, 0.5f))
    for (s in 0..segs) {
      val t = (s.toFloat() / segs) * (PI * 2.0).toFloat()
      val x = cos(t) * r
      val z = -sin(t) * r
      pos.addAll(listOf(x, 0f, z))
      norm.addAll(listOf(0f, 1f, 0f))
      uv.addAll(listOf(0.5f + cos(t) * 0.5f, 0.5f - sin(t) * 0.5f))
    }
    for (s in 1..segs) {
      idx.add(0)
      idx.add(s.toShort())
      idx.add((s + 1).toShort())
    }
    return RawGeo(pos.toFloatArray(), norm.toFloatArray(), uv.toFloatArray(), idx.toShortArray())
  }

  fun buildRingGeo(rIn: Float, rOut: Float, segs: Int): RawGeo {
    val pos = ArrayList<Float>()
    val norm = ArrayList<Float>()
    val uv = ArrayList<Float>()
    val idx = ArrayList<Short>()
    for (s in 0..segs) {
      val t = (s.toFloat() / segs) * (PI * 2.0).toFloat()
      val ct = cos(t)
      val st = -sin(t)
      pos.addAll(listOf(rIn * ct, 0f, rIn * st))
      norm.addAll(listOf(0f, 1f, 0f))
      uv.addAll(listOf(s.toFloat() / segs, 0f))
      pos.addAll(listOf(rOut * ct, 0f, rOut * st))
      norm.addAll(listOf(0f, 1f, 0f))
      uv.addAll(listOf(s.toFloat() / segs, 1f))
    }
    for (s in 0 until segs) {
      val a = (s * 2).toShort()
      val b = (a + 1).toShort()
      val c = (a + 2).toShort()
      val d = (a + 3).toShort()
      idx.add(a); idx.add(b); idx.add(c)
      idx.add(b); idx.add(d); idx.add(c)
    }
    return RawGeo(pos.toFloatArray(), norm.toFloatArray(), uv.toFloatArray(), idx.toShortArray())
  }

  fun buildSphereGeo(r: Float, segW: Int, segH: Int): RawGeo {
    val pos = ArrayList<Float>()
    val norm = ArrayList<Float>()
    val uv = ArrayList<Float>()
    val idx = ArrayList<Short>()
    for (y in 0..segH) {
      val v = y.toFloat() / segH
      val phi = v * PI.toFloat()
      for (x in 0..segW) {
        val u = x.toFloat() / segW
        val theta = u * (PI * 2.0).toFloat()
        val nx = -cos(theta) * sin(phi)
        val ny = cos(phi)
        val nz = sin(theta) * sin(phi)
        pos.addAll(listOf(r * nx, r * ny, r * nz))
        norm.addAll(listOf(nx, ny, nz))
        uv.addAll(listOf(u, v))
      }
    }
    for (y in 0 until segH) {
      for (x in 0 until segW) {
        val a = (y * (segW + 1) + x).toShort()
        val b = (a + segW + 1).toShort()
        idx.add(a); idx.add(b); idx.add((a + 1).toShort())
        idx.add(b); idx.add((b + 1).toShort()); idx.add((a + 1).toShort())
      }
    }
    return RawGeo(pos.toFloatArray(), norm.toFloatArray(), uv.toFloatArray(), idx.toShortArray())
  }

  fun buildBoardWithHolesGeo(half: Float, pOff: Float, pRad: Float): RawGeo {
    val pos = ArrayList<Float>()
    val norm = ArrayList<Float>()
    val uv = ArrayList<Float>()
    val idx = ArrayList<Short>()

    fun addTri(a: FloatArray, b: FloatArray, c: FloatArray) {
      val base = (pos.size / 3).toShort()
      pos.addAll(a.toList()); pos.addAll(b.toList()); pos.addAll(c.toList())
      repeat(3) {
        norm.add(0f); norm.add(1f); norm.add(0f)
      }
      uv.add((a[0] + half) / (half * 2f)); uv.add((a[2] + half) / (half * 2f))
      uv.add((b[0] + half) / (half * 2f)); uv.add((b[2] + half) / (half * 2f))
      uv.add((c[0] + half) / (half * 2f)); uv.add((c[2] + half) / (half * 2f))
      idx.add(base); idx.add((base + 1).toShort()); idx.add((base + 2).toShort())
    }

    fun rayToBox(ox: Float, oz: Float, dx: Float, dz: Float, minX: Float, maxX: Float, minZ: Float, maxZ: Float): FloatArray {
      var t = 1e6f
      if (dx > 1e-6f) t = min(t, (maxX - ox) / dx) else if (dx < -1e-6f) t = min(t, (minX - ox) / dx)
      if (dz > 1e-6f) t = min(t, (maxZ - oz) / dz) else if (dz < -1e-6f) t = min(t, (minZ - oz) / dz)
      return floatArrayOf(ox + dx * t, 0f, oz + dz * t)
    }

    data class Quad(val minX: Float, val maxX: Float, val minZ: Float, val maxZ: Float, val px: Float, val pz: Float)
    val quads =
      listOf(
        Quad(-half, 0f, -half, 0f, -pOff, -pOff),
        Quad(0f, half, -half, 0f, pOff, -pOff),
        Quad(-half, 0f, 0f, half, -pOff, pOff),
        Quad(0f, half, 0f, half, pOff, pOff),
      )

    for (q in quads) {
      val angles = ArrayList<Float>()
      for (i in 0 until 36) angles.add((-PI + (i / 36.0) * PI * 2.0).toFloat())
      val corners = arrayOf(q.minX to q.minZ, q.maxX to q.minZ, q.maxX to q.maxZ, q.minX to q.maxZ)
      for ((cx, cz) in corners) angles.add(atan2(cz - q.pz, cx - q.px))
      angles.sort()
      val uniq = ArrayList<Float>()
      for (a in angles) if (uniq.isEmpty() || a - uniq.last() > 1e-4f) uniq.add(a)
      for (i in uniq.indices) {
        val a0 = uniq[i]
        val a1 = if (i + 1 < uniq.size) uniq[i + 1] else uniq[0] + (PI * 2.0).toFloat()
        val d0x = cos(a0); val d0z = sin(a0)
        val d1x = cos(a1); val d1z = sin(a1)
        val in0 = floatArrayOf(q.px + d0x * pRad, 0f, q.pz + d0z * pRad)
        val in1 = floatArrayOf(q.px + d1x * pRad, 0f, q.pz + d1z * pRad)
        val out0 = rayToBox(q.px, q.pz, d0x, d0z, q.minX, q.maxX, q.minZ, q.maxZ)
        val out1 = rayToBox(q.px, q.pz, d1x, d1z, q.minX, q.maxX, q.minZ, q.maxZ)
        addTri(in0, in1, out0)
        addTri(in1, out1, out0)
      }
    }
    return RawGeo(pos.toFloatArray(), norm.toFloatArray(), uv.toFloatArray(), idx.toShortArray())
  }
}
