package com.aking.starter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.aking.starter.screens.floating.StarterFloatingComposeView
import com.aking.starter.screens.home.HomeScreen
import com.aking.starter.ui.theme.StarterTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StarterTheme {
                Host()
            }
        }
    }
}

@Composable
fun Host() {
    val context = LocalContext.current
    val floatingComposeView = remember { StarterFloatingComposeView(context) }
    Navigator(HomeScreen) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.text_start)) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.text_start)) },
                    onClick = {
                        if (floatingComposeView.isAttachedToWindow) {
                            floatingComposeView.removeFromWindowManager()
                        } else {
                            floatingComposeView.addToWindowManager()
                        }
                    })
            }) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                CurrentScreen()
            }
        }
    }
}

@Preview(device = Devices.PIXEL_7_PRO)
@Composable
fun PreviewHost() {
    Host()
}
