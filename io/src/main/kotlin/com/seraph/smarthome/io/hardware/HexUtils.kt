package com.seraph.smarthome.io.hardware

fun ByteArray.asHexString(): String {
    var str = ""
    this.forEach {
        var string = Integer.toHexString(it + 0)
        if (string.length == 1) string = "0" + string
        else if (string.length > 2) string = string.takeLast(2)
        str += string + " "
    }
    return str
}