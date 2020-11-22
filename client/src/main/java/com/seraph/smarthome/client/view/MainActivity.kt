package com.seraph.smarthome.client.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ScrollableColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.setContent

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScrollableColumn(Modifier.fillMaxSize()) {
                (1..10).forEach {
                    Card("Item #$it", "Some body text #$it")
                }
            }
        }
    }

    @Composable
    fun Card(header: String, body: String) {
        MaterialTheme {
            val typo = MaterialTheme.typography
            androidx.compose.material.Card {
                Column {
                    Text(header, style = typo.h6)
                    Text(body, style = typo.body2)
                }
            }
        }
    }
}