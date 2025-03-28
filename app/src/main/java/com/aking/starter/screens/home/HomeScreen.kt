package com.aking.starter.screens.home

import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import com.aking.starter.R
import com.aking.starter.ui.theme.Blue
import com.aking.starter.utils.FloatingPermissionHelper

/**
 * @author Ak
 * 2025/3/13  17:01
 */
object HomeScreen : Screen {
    private fun readResolve(): Any = HomeScreen

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { HomeScreenModel() }
        val context = LocalContext.current
        val state = screenModel.uiState

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { //处理权限请求结果
            val granted = FloatingPermissionHelper.checkFloatingPermission(context)
            Log.i("HomeScreen", "handlePermissionResult: $granted")
            screenModel.reducer(HomeIntent.HandlePermission(granted))
        }

        //Check permission on first composition
        LaunchedEffect(Unit) {
            val granted = FloatingPermissionHelper.checkFloatingPermission(context)
            screenModel.reducer(HomeIntent.HandlePermission(granted))
        }

        Column(modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Icon(painterResource(R.drawable.ic_safety), stringResource(R.string.text_permission))
                Text(stringResource(R.string.text_permission_request), style = MaterialTheme.typography.displayMedium)
                Text(stringResource(R.string.text_permission_des), style = MaterialTheme.typography.bodyMedium)
            }
            ItemPermission(state.floatingPermission, modifier = Modifier.clickable {
                // 请求悬浮窗权限
                FloatingPermissionHelper.launch(launcher, context)
            })
        }
    }
}

/**
 * 显示权限项。
 *
 * @param permission 包含要显示的权限数据，例如名称、描述、涉及的功能以及是否已授予权限。
 * @param modifier 可选的 [Modifier]，用于自定义可组合项的布局或外观，默认为 [Modifier]。
 */
@Composable
fun ItemPermission(permission: Permission, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(ImageVector.vectorResource(R.drawable.ic_float_window), stringResource(R.string.text_float_window))
            Text(
                stringResource(permission.name),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
            )
            Checkbox(permission.isGranted, null, enabled = false)
        }
        Text(
            stringResource(permission.description),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 2.dp)
        )
        Text(
            stringResource(permission.featuresInvolved), style = MaterialTheme.typography.bodyMedium, color = Blue
        )
    }
}

/**
 * 预览权限项。
 */
@Preview(device = Devices.PIXEL_7_PRO)
@Composable
fun ItemPermissionPreview() {
    HomeScreen.Content()
}