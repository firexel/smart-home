/***********************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */
package com.seraph.smarthome.io.hardware.dmx.ola

import com.google.protobuf.RpcCallback
import com.google.protobuf.RpcController

/**
 * Simple Rpc Controller implementation.
 */
class SimpleRpcController : RpcController {

    private var failed = false
    private var cancelled = false
    private var error: String? = null
    private var callback: RpcCallback<Any>? = null

    /* (non-Javadoc)
     * @see com.google.protobuf.RpcController#errorText()
     */
    override fun errorText(): String? {
        return error
    }

    /* (non-Javadoc)
     * @see com.google.protobuf.RpcController#failed()
     */
    override fun failed(): Boolean {
        return failed
    }

    /* (non-Javadoc)
     * @see com.google.protobuf.RpcController#isCanceled()
     */
    override fun isCanceled(): Boolean {
        return cancelled
    }

    /* (non-Javadoc)
     * @see com.google.protobuf.RpcController#notifyOnCancel(com.google.protobuf.RpcCallback)
     */
    override fun notifyOnCancel(notifyCallback: RpcCallback<Any>) {
        callback = notifyCallback
    }

    /* (non-Javadoc)
     * @see com.google.protobuf.RpcController#reset()
     */
    override fun reset() {
        failed = false
        cancelled = false
        error = null
        callback = null
    }

    /* (non-Javadoc)
     * @see com.google.protobuf.RpcController#setFailed(java.lang.String)
     */
    override fun setFailed(reason: String) {
        failed = true
        error = reason
    }

    /* (non-Javadoc)
     * @see com.google.protobuf.RpcController#startCancel()
     */
    override fun startCancel() {
        cancelled = true
        callback!!.run(null)
    }
}