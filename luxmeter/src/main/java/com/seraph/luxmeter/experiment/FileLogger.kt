package com.seraph.luxmeter.experiment

import android.util.Log
import java.io.File
import java.io.FileWriter

class FileLogger(directory: File, name: String) : ResultLogger {

    private val writer: FileWriter
    private val file: File

    init {
        file = findNextFile(directory, name)
        writer = FileWriter(file, false)
        writer.write("power\tluminance\n")
    }

    private fun findNextFile(directory: File, name: String): File {
        return (0..200)
                .map { File(directory, "${name}_$it.csv") }
                .first { !it.exists() }
    }

    override fun logParameters(powerLevel: Float, luminance: Float) {
        val string = "$powerLevel\t$luminance\n"
        Log.i("Experiment", string)
        writer.write(string)
        writer.flush()
    }

    override fun close() {
        writer.flush()
        writer.close()
    }

    override fun getStorageInfo(): String {
        return file.name
    }
}