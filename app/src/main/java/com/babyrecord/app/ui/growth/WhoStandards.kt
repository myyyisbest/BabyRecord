package com.babyrecord.app.ui.growth

import com.babyrecord.app.data.MeasureType

data class RefPoint(val month: Int, val low: Float, val median: Float, val high: Float)

/**
 * WHO 儿童生长标准参考值（0~24 月龄），取 -2SD / 中位数 / +2SD。
 * 数值为对 WHO Child Growth Standards 的近似整理，仅供家庭参考，不作医学判断依据。
 */
object WhoStandards {

    private fun rows(vararg r: FloatArray): List<RefPoint> =
        r.mapIndexed { i, a -> RefPoint(i, a[0], a[1], a[2]) }

    // 体重 kg：女宝 / 男宝
    private val WEIGHT_GIRLS = rows(
        floatArrayOf(2.4f, 3.2f, 4.2f), floatArrayOf(3.2f, 4.2f, 5.5f), floatArrayOf(3.9f, 5.1f, 6.6f),
        floatArrayOf(4.5f, 5.8f, 7.5f), floatArrayOf(5.0f, 6.4f, 8.2f), floatArrayOf(5.4f, 6.9f, 8.8f),
        floatArrayOf(5.7f, 7.3f, 9.3f), floatArrayOf(6.0f, 7.6f, 9.8f), floatArrayOf(6.3f, 7.9f, 10.2f),
        floatArrayOf(6.5f, 8.2f, 10.5f), floatArrayOf(6.7f, 8.5f, 10.9f), floatArrayOf(6.9f, 8.7f, 11.2f),
        floatArrayOf(7.0f, 8.9f, 11.5f), floatArrayOf(7.2f, 9.2f, 11.7f), floatArrayOf(7.4f, 9.4f, 12.0f),
        floatArrayOf(7.6f, 9.6f, 12.4f), floatArrayOf(7.7f, 9.8f, 12.6f), floatArrayOf(7.9f, 10.0f, 12.9f),
        floatArrayOf(8.1f, 10.2f, 13.2f), floatArrayOf(8.2f, 10.4f, 13.5f), floatArrayOf(8.4f, 10.6f, 13.7f),
        floatArrayOf(8.6f, 10.9f, 14.0f), floatArrayOf(8.7f, 11.1f, 14.3f), floatArrayOf(8.9f, 11.3f, 14.6f),
        floatArrayOf(9.0f, 11.5f, 14.8f)
    )
    private val WEIGHT_BOYS = rows(
        floatArrayOf(2.5f, 3.3f, 4.4f), floatArrayOf(3.4f, 4.5f, 5.8f), floatArrayOf(4.3f, 5.6f, 7.1f),
        floatArrayOf(5.0f, 6.4f, 8.0f), floatArrayOf(5.6f, 7.0f, 8.7f), floatArrayOf(6.0f, 7.5f, 9.3f),
        floatArrayOf(6.4f, 7.9f, 9.8f), floatArrayOf(6.7f, 8.3f, 10.3f), floatArrayOf(6.9f, 8.6f, 10.7f),
        floatArrayOf(7.1f, 8.9f, 11.0f), floatArrayOf(7.4f, 9.2f, 11.4f), floatArrayOf(7.6f, 9.4f, 11.7f),
        floatArrayOf(7.7f, 9.6f, 12.0f), floatArrayOf(7.9f, 9.9f, 12.3f), floatArrayOf(8.1f, 10.1f, 12.6f),
        floatArrayOf(8.3f, 10.3f, 12.8f), floatArrayOf(8.4f, 10.5f, 13.1f), floatArrayOf(8.6f, 10.7f, 13.4f),
        floatArrayOf(8.8f, 10.9f, 13.7f), floatArrayOf(8.9f, 11.1f, 13.9f), floatArrayOf(9.1f, 11.3f, 14.2f),
        floatArrayOf(9.2f, 11.5f, 14.5f), floatArrayOf(9.4f, 11.7f, 14.7f), floatArrayOf(9.5f, 11.9f, 15.0f),
        floatArrayOf(9.7f, 12.2f, 15.3f)
    )

    // 身高 cm：女宝 / 男宝
    private val HEIGHT_GIRLS = rows(
        floatArrayOf(45.4f, 49.1f, 52.9f), floatArrayOf(49.8f, 53.7f, 58.0f), floatArrayOf(53.0f, 57.1f, 61.4f),
        floatArrayOf(55.6f, 59.8f, 64.5f), floatArrayOf(57.8f, 62.1f, 66.2f), floatArrayOf(59.6f, 64.0f, 68.5f),
        floatArrayOf(61.2f, 65.7f, 70.0f), floatArrayOf(62.7f, 67.3f, 71.9f), floatArrayOf(64.0f, 68.7f, 73.5f),
        floatArrayOf(65.3f, 70.1f, 75.0f), floatArrayOf(66.5f, 71.5f, 76.4f), floatArrayOf(67.7f, 72.8f, 77.8f),
        floatArrayOf(68.9f, 74.0f, 79.2f), floatArrayOf(70.0f, 75.2f, 80.6f), floatArrayOf(71.0f, 76.4f, 81.9f),
        floatArrayOf(72.0f, 77.5f, 83.0f), floatArrayOf(73.0f, 78.6f, 84.2f), floatArrayOf(74.0f, 79.7f, 85.4f),
        floatArrayOf(74.9f, 80.7f, 86.5f), floatArrayOf(75.8f, 81.7f, 87.6f), floatArrayOf(76.7f, 82.7f, 88.8f),
        floatArrayOf(77.5f, 83.7f, 89.8f), floatArrayOf(78.4f, 84.6f, 90.9f), floatArrayOf(79.2f, 85.5f, 91.9f),
        floatArrayOf(80.0f, 86.4f, 92.9f)
    )
    private val HEIGHT_BOYS = rows(
        floatArrayOf(46.1f, 49.9f, 54.7f), floatArrayOf(50.8f, 54.7f, 59.5f), floatArrayOf(54.4f, 58.4f, 63.3f),
        floatArrayOf(57.3f, 61.4f, 66.1f), floatArrayOf(59.7f, 63.9f, 68.0f), floatArrayOf(61.7f, 65.9f, 70.1f),
        floatArrayOf(63.3f, 67.6f, 71.9f), floatArrayOf(64.8f, 69.2f, 73.5f), floatArrayOf(66.2f, 70.6f, 75.0f),
        floatArrayOf(67.5f, 72.0f, 76.5f), floatArrayOf(68.7f, 73.3f, 77.9f), floatArrayOf(69.9f, 74.5f, 79.2f),
        floatArrayOf(71.0f, 75.7f, 80.5f), floatArrayOf(72.1f, 76.9f, 81.8f), floatArrayOf(73.1f, 78.0f, 83.0f),
        floatArrayOf(74.1f, 79.1f, 84.2f), floatArrayOf(75.0f, 80.2f, 85.4f), floatArrayOf(76.0f, 81.2f, 86.5f),
        floatArrayOf(76.9f, 82.3f, 87.7f), floatArrayOf(77.8f, 83.2f, 88.8f), floatArrayOf(78.6f, 84.2f, 89.8f),
        floatArrayOf(79.4f, 85.1f, 90.9f), floatArrayOf(80.2f, 86.0f, 91.9f), floatArrayOf(80.9f, 86.9f, 92.9f),
        floatArrayOf(81.7f, 87.8f, 94.0f)
    )

    // 头围 cm：女宝 / 男宝
    private val HEAD_GIRLS = rows(
        floatArrayOf(32.0f, 33.9f, 35.9f), floatArrayOf(33.8f, 35.8f, 37.9f), floatArrayOf(35.2f, 37.1f, 39.1f),
        floatArrayOf(36.3f, 38.1f, 40.1f), floatArrayOf(37.1f, 39.0f, 41.0f), floatArrayOf(37.8f, 39.7f, 41.7f),
        floatArrayOf(38.3f, 40.2f, 42.2f), floatArrayOf(38.7f, 40.7f, 42.7f), floatArrayOf(39.1f, 41.0f, 43.1f),
        floatArrayOf(39.4f, 41.3f, 43.4f), floatArrayOf(39.6f, 41.6f, 43.7f), floatArrayOf(39.9f, 41.9f, 44.0f),
        floatArrayOf(40.1f, 42.2f, 44.3f), floatArrayOf(40.3f, 42.4f, 44.6f), floatArrayOf(40.5f, 42.6f, 44.8f),
        floatArrayOf(40.7f, 42.8f, 45.1f), floatArrayOf(40.9f, 43.0f, 45.3f), floatArrayOf(41.0f, 43.2f, 45.5f),
        floatArrayOf(41.2f, 43.4f, 45.7f), floatArrayOf(41.3f, 43.5f, 45.9f), floatArrayOf(41.5f, 43.7f, 46.1f),
        floatArrayOf(41.6f, 43.9f, 46.3f), floatArrayOf(41.7f, 44.0f, 46.5f), floatArrayOf(41.9f, 44.2f, 46.7f),
        floatArrayOf(42.0f, 44.3f, 46.9f)
    )
    private val HEAD_BOYS = rows(
        floatArrayOf(32.4f, 34.5f, 36.6f), floatArrayOf(34.4f, 36.5f, 38.7f), floatArrayOf(35.8f, 37.9f, 40.1f),
        floatArrayOf(36.9f, 39.0f, 41.2f), floatArrayOf(37.7f, 39.9f, 42.1f), floatArrayOf(38.4f, 40.6f, 42.8f),
        floatArrayOf(38.9f, 41.0f, 43.3f), floatArrayOf(39.3f, 41.5f, 43.8f), floatArrayOf(39.6f, 41.9f, 44.2f),
        floatArrayOf(39.9f, 42.2f, 44.6f), floatArrayOf(40.1f, 42.5f, 44.9f), floatArrayOf(40.3f, 42.7f, 45.2f),
        floatArrayOf(40.5f, 42.9f, 45.4f), floatArrayOf(40.7f, 43.1f, 45.6f), floatArrayOf(40.8f, 43.3f, 45.8f),
        floatArrayOf(41.0f, 43.5f, 46.0f), floatArrayOf(41.1f, 43.7f, 46.3f), floatArrayOf(41.3f, 43.9f, 46.5f),
        floatArrayOf(41.4f, 44.1f, 46.8f), floatArrayOf(41.5f, 44.2f, 47.0f), floatArrayOf(41.6f, 44.4f, 47.2f),
        floatArrayOf(41.8f, 44.6f, 47.4f), floatArrayOf(41.9f, 44.7f, 47.6f), floatArrayOf(42.0f, 44.9f, 47.8f),
        floatArrayOf(42.1f, 45.0f, 48.0f)
    )

    fun forMetric(metric: MeasureType, gender: String): List<RefPoint> {
        val boy = gender == "boy"
        return when (metric) {
            MeasureType.WEIGHT -> if (boy) WEIGHT_BOYS else WEIGHT_GIRLS
            MeasureType.HEIGHT -> if (boy) HEIGHT_BOYS else HEIGHT_GIRLS
            MeasureType.HEAD -> if (boy) HEAD_BOYS else HEAD_GIRLS
        }
    }

    /** 在参考表的月龄点之间线性插值，返回 (下界, 中位, 上界) */
    fun sample(ref: List<RefPoint>, month: Float): Triple<Float, Float, Float>? {
        if (ref.isEmpty()) return null
        if (month <= ref.first().month) {
            val p = ref.first(); return Triple(p.low, p.median, p.high)
        }
        if (month >= ref.last().month) {
            val p = ref.last(); return Triple(p.low, p.median, p.high)
        }
        for (i in 0 until ref.size - 1) {
            val a = ref[i]
            val b = ref[i + 1]
            if (month >= a.month && month <= b.month) {
                val t = (month - a.month) / (b.month - a.month)
                fun lerp(x: Float, y: Float) = x + (y - x) * t
                return Triple(lerp(a.low, b.low), lerp(a.median, b.median), lerp(a.high, b.high))
            }
        }
        return null
    }
}
