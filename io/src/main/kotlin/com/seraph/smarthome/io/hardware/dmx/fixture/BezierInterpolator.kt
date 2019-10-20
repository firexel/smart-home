package com.seraph.smarthome.io.hardware.dmx.fixture

import com.seraph.smarthome.util.Log
import kotlin.math.pow

class BezierInterpolator(
        initialValue: Double,
        private val maxAnimTimeNs: Long = 1_100_000_000,
        private val intermediateStepsCount: Int = 100,
        private val steepness: Double = 0.2,
        private val log: Log
) : StandaloneLightFixture.Interpolator {

    private var current = Point(0L, initialValue)
    private var previous = current.copy(t = current.t - 100)
    private var points: List<Point> = arrayListOf()

    override fun setTarget(target: Float) {
        log.v("Request to go from ${current.v} to $target in $maxAnimTimeNs nanos")

        val temporalOffset = (maxAnimTimeNs * steepness).toLong()

        val p0 = Point(0L, current.v)
        val p1 = Point(temporalOffset, current.v)
        val p2 = Point(maxAnimTimeNs - temporalOffset, target.toDouble())
        val p3 = Point(maxAnimTimeNs, target.toDouble())

        val points = ArrayList<Point>(intermediateStepsCount + 1)
        for (i in 0..intermediateStepsCount) {
            val step = i.toDouble() / intermediateStepsCount
            val t = bezierCubic(step, p0.t.toDouble(), p1.t.toDouble(), p2.t.toDouble(), p3.t.toDouble())
                    .toLong()
            val v = bezierCubic(step, p0.v, p1.v, p2.v, p3.v)
            points.add(Point(t, v))
        }
        points.sortBy { it.t }
        log.v("Sorted points: \n${points.joinToString("\n")}")
        this.points = points
        this.current = p0
    }

    private fun bezierCubic(t: Double, p0: Double, p1: Double, p2: Double, p3: Double) =
            (1 - t).pow(3) * p0 + 3 * (1 - t).pow(2) * t * p1 + 3 * (1 - t) * t.pow(2) * p2 + t.pow(3) * p3

    override fun progress(nanosPassed: Long): Float {
        if (!isStable) {
            previous = current
            val currentT = current.t + nanosPassed
            val nearestRightIndex = points.indexOfFirst { it.t >= currentT }
            if (nearestRightIndex < 0) {
                log.v("Last point of sequence currentT=$currentT. Finishing.")
                current = points.last().copy(t = 0)
                previous = current.copy(t = -100)
                points = emptyList()
            } else {
                current = Point(currentT, points[nearestRightIndex].v)
                log.v("T+$currentT Selected V: ${current.v}")
            }
        }
        return current.v.toFloat()
    }

    override val isStable: Boolean
        get() {
            return points.isEmpty()
        }

    private data class Point(val t: Long, val v: Double) {
        override fun toString(): String {
            return "$t - $v"
        }
    }
}