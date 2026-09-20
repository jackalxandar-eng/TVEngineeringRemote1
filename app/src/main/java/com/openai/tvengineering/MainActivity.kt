package com.openai.tvengineering

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.openai.tvengineering.ui.TvEngineerApp
import com.openai.tvengineering.ui.TvRemoteViewModel

class MainActivity : ComponentActivity() {
    private val vm: TvRemoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TvEngineerApp(vm) }
    }
}
