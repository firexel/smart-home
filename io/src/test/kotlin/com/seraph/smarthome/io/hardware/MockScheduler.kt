package com.seraph.smarthome.io.hardware

import org.junit.Assert
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

/**
 * Created by aleksandr.naumov on 07.05.18.
 */
class MockScheduler : Scheduler {

    private val mocks = mutableListOf<BusResponseMock>()
    private val posts = mutableListOf<PostRequest<*>>()

    val postsInQueue
        get() = posts.size

    fun mockResponse(request: ByteArray, response: ByteArray): MockHandle {
        val mock = BusResponseMock(request, response)
        mocks.add(mock)
        return MockHandleImpl(mock)
    }

    override fun <T> post(cmd: Bus.Command<T>, delay: Long, callback: (T) -> Unit) {
        posts.add(PostRequest(cmd, callback))
    }

    fun proceed() {
        if (posts.isEmpty()) {
            Assert.fail("No posts in queue to proceed")
        } else {
            posts.removeAt(posts.size - 1).proceed()
        }
    }

    private data class BusResponseMock(
            val request: ByteArray,
            val response: ByteArray
    )

    private inner class PostRequest<T>(
            val cmd: Bus.Command<T>,
            val callback: (T) -> Unit
    ) {
        fun proceed() {
            val request = ByteArrayOutputStream()
            cmd.writeRequest(request)
            val requestBytes = request.toByteArray()
            val mock = mocks.find { Arrays.equals(it.request, requestBytes) }
            if (mock == null) {
                Assert.fail("Cannot find mock for request ${requestBytes.asHexString()}")
            } else {
                val parseResult = cmd.readResponse(ByteArrayInputStream(mock.response))
                callback(parseResult)
            }
        }
    }

    private inner class MockHandleImpl(private val mock: BusResponseMock) : MockHandle {
        override fun discard() {
            mocks.remove(mock)
        }
    }

    interface MockHandle {
        fun discard()
    }
}