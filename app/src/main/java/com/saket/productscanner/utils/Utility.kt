package com.saket.productscanner.utils

import android.content.Context
import android.text.Html
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

object Utility {
    private var utility: Utility? = null
    private var mDirectory: File? = null
    private var mDateFormat: SimpleDateFormat? = null
    val instance: Utility?
        get() = if (utility == null) Utility.also { utility = it } else utility

    fun AssignDirectory(cxt: Context) {
        mDirectory = cxt.getExternalFilesDir("")
        mDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
    }

    fun removeJunkValues(message: String): String {
        var formattedtext = ""
        try {
            formattedtext = removeJunk(message)
            formattedtext = Html.fromHtml(formattedtext).toString()
            formattedtext = formattedtext.replace("[^\\u0000-\\uFFFF]", "")
            formattedtext = formattedtext.replace("[^\\x00-\\x7F]", "")
            formattedtext = formattedtext.replace("%EF%BF%BC", "")
            formattedtext = formattedtext.replace("[^\\p{ASCII}]^\\r^\\n".toRegex(), " ")
        } catch (e: Exception) {
            // TODO: handle exception
        }
        return formattedtext
    }

    private fun removeJunk(userText: String): String {
        var userText = userText
        val outputEncoding = "UTF-8"
        val charsetOutput = Charset.forName(outputEncoding)
        val encoder = charsetOutput.newEncoder()
        val bufferToConvert = userText.toByteArray()
        val decoder = charsetOutput.newDecoder()
        try {
            val cbuf = decoder.decode(ByteBuffer.wrap(bufferToConvert))
            val bbuf = encoder.encode(CharBuffer.wrap(cbuf))
            userText = decoder.decode(bbuf).toString()
        } catch (e: CharacterCodingException) {
            e.printStackTrace()
        }
        return userText.trim { it <= ' ' }
    }

    fun writeLogSpp(content: String, event: String) {
        Log.d(content, event)
        if (mDirectory == null) return
        try {
            val file = File(mDirectory, "monitor_log.log")
            val date = Date()
            println(mDateFormat!!.format(date))
            val fw = FileWriter(file.absoluteFile, true)
            val bw = BufferedWriter(fw)
            bw.write(
                "${mDateFormat!!.format(date)} : $content : $event"
            )
            bw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun writeLogError(content: String, event: String) {
        Log.d(content, event)
        if (mDirectory == null) return
        try {
            val file = File(mDirectory, "Exception.log")
            val date = Date()
            println(mDateFormat!!.format(date))
            val fw = FileWriter(file.absoluteFile, true)
            val bw = BufferedWriter(fw)
            bw.write(
                "${mDateFormat!!.format(date)} : $content : $event"
            )
            bw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}