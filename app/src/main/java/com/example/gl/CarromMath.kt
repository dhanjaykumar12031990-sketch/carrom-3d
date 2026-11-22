package com.example.gl

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

data class Vec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
  fun set(nx: Float, ny: Float, nz: Float): Vec3 {
    x = nx
    y = ny
    z = nz
    return this
  }

  fun lerp(target: Vec3, t: Float) {
    x += (target.x - x) * t
    y += (target.y - y) * t
    z += (target.z - z) * t
  }
}

object M4 {
  fun identity(out: FloatArray): FloatArray {
    out.fill(0f)
    out[0] = 1f
    out[5] = 1f
    out[10] = 1f
    out[15] = 1f
    return out
  }

  fun multiply(out: FloatArray, a: FloatArray, b: FloatArray): FloatArray {
    val a00 = a[0]; val a01 = a[1]; val a02 = a[2]; val a03 = a[3]
    val a10 = a[4]; val a11 = a[5]; val a12 = a[6]; val a13 = a[7]
    val a20 = a[8]; val a21 = a[9]; val a22 = a[10]; val a23 = a[11]
    val a30 = a[12]; val a31 = a[13]; val a32 = a[14]; val a33 = a[15]
    for (i in 0 until 4) {
      val b0 = b[i * 4]; val b1 = b[i * 4 + 1]; val b2 = b[i * 4 + 2]; val b3 = b[i * 4 + 3]
      out[i * 4] = b0 * a00 + b1 * a10 + b2 * a20 + b3 * a30
      out[i * 4 + 1] = b0 * a01 + b1 * a11 + b2 * a21 + b3 * a31
      out[i * 4 + 2] = b0 * a02 + b1 * a12 + b2 * a22 + b3 * a32
      out[i * 4 + 3] = b0 * a03 + b1 * a13 + b2 * a23 + b3 * a33
    }
    return out
  }

  fun perspective(out: FloatArray, fovDeg: Float, aspect: Float, near: Float, far: Float): FloatArray {
    val f = 1f / tan((fovDeg * Math.PI.toFloat() / 180f) * 0.5f)
    val nf = 1f / (near - far)
    out.fill(0f)
    out[0] = f / aspect
    out[5] = f
    out[10] = (far + near) * nf
    out[11] = -1f
    out[14] = 2f * far * near * nf
    return out
  }

  fun ortho(out: FloatArray, l: Float, r: Float, b: Float, t: Float, n: Float, f: Float): FloatArray {
    val lr = 1f / (l - r)
    val bt = 1f / (b - t)
    val nf = 1f / (n - f)
    out.fill(0f)
    out[0] = -2f * lr
    out[5] = -2f * bt
    out[10] = 2f * nf
    out[15] = 1f
    out[12] = (l + r) * lr
    out[13] = (t + b) * bt
    out[14] = (f + n) * nf
    return out
  }

  fun lookAt(out: FloatArray, eye: Vec3, target: Vec3, up: Vec3): FloatArray {
    var zx = eye.x - target.x
    var zy = eye.y - target.y
    var zz = eye.z - target.z
    var len = hypot(zx, hypot(zy, zz))
    if (len < 1e-6f) len = 1f
    zx /= len; zy /= len; zz /= len

    var xx = up.y * zz - up.z * zy
    var xy = up.z * zx - up.x * zz
    var xz = up.x * zy - up.y * zx
    len = hypot(xx, hypot(xy, xz))
    if (len < 1e-6f) {
      xx = 1f; xy = 0f; xz = 0f
    } else {
      xx /= len; xy /= len; xz /= len
    }

    val yx = zy * xz - zz * xy
    val yy = zz * xx - zx * xz
    val yz = zx * xy - zy * xx

    out[0] = xx; out[4] = xy; out[8] = xz; out[12] = -(xx * eye.x + xy * eye.y + xz * eye.z)
    out[1] = yx; out[5] = yy; out[9] = yz; out[13] = -(yx * eye.x + yy * eye.y + yz * eye.z)
    out[2] = zx; out[6] = zy; out[10] = zz; out[14] = -(zx * eye.x + zy * eye.y + zz * eye.z)
    out[3] = 0f; out[7] = 0f; out[11] = 0f; out[15] = 1f
    return out
  }

  fun compose(out: FloatArray, p: Vec3, r: Vec3, s: Vec3): FloatArray {
    val cx = cos(r.x); val sx = sin(r.x)
    val cy = cos(r.y); val sy = sin(r.y)
    val cz = cos(r.z); val sz = sin(r.z)
    out[0] = (cy * cz) * s.x
    out[1] = (cx * sz + sx * sy * cz) * s.x
    out[2] = (sx * sz - cx * sy * cz) * s.x
    out[3] = 0f
    out[4] = (-cy * sz) * s.y
    out[5] = (cx * cz - sx * sy * sz) * s.y
    out[6] = (sx * cz + cx * sy * sz) * s.y
    out[7] = 0f
    out[8] = sy * s.z
    out[9] = (-sx * cy) * s.z
    out[10] = (cx * cy) * s.z
    out[11] = 0f
    out[12] = p.x
    out[13] = p.y
    out[14] = p.z
    out[15] = 1f
    return out
  }

  fun invert(out: FloatArray, m: FloatArray): Boolean {
    val a00 = m[0]; val a01 = m[1]; val a02 = m[2]; val a03 = m[3]
    val a10 = m[4]; val a11 = m[5]; val a12 = m[6]; val a13 = m[7]
    val a20 = m[8]; val a21 = m[9]; val a22 = m[10]; val a23 = m[11]
    val a30 = m[12]; val a31 = m[13]; val a32 = m[14]; val a33 = m[15]
    val b00 = a00 * a11 - a01 * a10; val b01 = a00 * a12 - a02 * a10; val b02 = a00 * a13 - a03 * a10
    val b03 = a01 * a12 - a02 * a11; val b04 = a01 * a13 - a03 * a11; val b05 = a02 * a13 - a03 * a12
    val b06 = a20 * a31 - a21 * a30; val b07 = a20 * a32 - a22 * a30; val b08 = a20 * a33 - a23 * a30
    val b09 = a21 * a32 - a22 * a31; val b10 = a21 * a33 - a23 * a31; val b11 = a22 * a33 - a23 * a32
    var det = b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06
    if (abs(det) < 1e-8f) return false
    det = 1f / det
    out[0] = (a11 * b11 - a12 * b10 + a13 * b09) * det
    out[1] = (-a01 * b11 + a02 * b10 - a03 * b09) * det
    out[2] = (a31 * b05 - a32 * b04 + a33 * b03) * det
    out[3] = (-a21 * b05 + a22 * b04 - a23 * b03) * det
    out[4] = (-a10 * b11 + a12 * b08 - a13 * b07) * det
    out[5] = (a00 * b11 - a02 * b08 + a03 * b07) * det
    out[6] = (-a30 * b05 + a32 * b02 - a33 * b01) * det
    out[7] = (a20 * b05 - a22 * b02 + a23 * b01) * det
    out[8] = (a10 * b10 - a11 * b08 + a13 * b06) * det
    out[9] = (-a00 * b10 + a01 * b08 - a03 * b06) * det
    out[10] = (a30 * b04 - a31 * b02 + a33 * b00) * det
    out[11] = (-a20 * b04 + a21 * b02 - a23 * b00) * det
    out[12] = (-a10 * b09 + a11 * b07 - a12 * b06) * det
    out[13] = (a00 * b09 - a01 * b07 + a02 * b06) * det
    out[14] = (-a30 * b03 + a31 * b01 - a32 * b00) * det
    out[15] = (a20 * b03 - a21 * b01 + a22 * b00) * det
    return true
  }

  fun transformVec4(m: FloatArray, x: Float, y: Float, z: Float, w: Float): Vec3 {
    var rw = m[3] * x + m[7] * y + m[11] * z + m[15] * w
    if (abs(rw) < 1e-6f) rw = 1f
    return Vec3(
      (m[0] * x + m[4] * y + m[8] * z + m[12] * w) / rw,
      (m[1] * x + m[5] * y + m[9] * z + m[13] * w) / rw,
      (m[2] * x + m[6] * y + m[10] * z + m[14] * w) / rw,
    )
  }
}

object ColorUtil {
  fun hexToRgb(hex: Int): FloatArray =
    floatArrayOf(((hex ushr 16) and 255) / 255f, ((hex ushr 8) and 255) / 255f, (hex and 255) / 255f)

  fun hslToRgb(h: Float, s: Float, l: Float): FloatArray {
    val a = s * min(l, 1f - l)
    fun f(n: Float): Float {
      val k = (n + h * 12f) % 12f
      return l - a * max(-1f, min(k - 3f, min(9f - k, 1f)))
    }
    return floatArrayOf(f(0f), f(8f), f(4f))
  }
}
