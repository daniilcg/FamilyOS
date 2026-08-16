package com.familyos.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.widget.ScrollView
import android.widget.TextView

/**
 * Shows the last uncaught exception so a crash is visible instead of a silent close.
 */
class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = TextView(this).apply {
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.BLACK)
            setTextIsSelectable(true)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(32, 48, 32, 48)
            this.text = buildString {
                appendLine("FamilyOS crashed. Screenshot this and send it.")
                appendLine()
                append(intent.getStringExtra(EXTRA_CRASH).orEmpty().ifBlank { "No stack trace" })
            }
        }
        setContentView(ScrollView(this).apply { addView(text) })
    }

    companion object {
        const val EXTRA_CRASH = "crash"
    }
}
