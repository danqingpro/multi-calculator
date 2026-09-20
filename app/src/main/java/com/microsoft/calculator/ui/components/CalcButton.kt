package com.microsoft.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.microsoft.calculator.ui.theme.*

/** 通用计算器按钮,对标 Windows 计算器按钮外观 */
@Composable
fun CalcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bg: Color = NumberBtnBg,
    fg: Color = TextPrimary,
    fontSize: TextUnit = 20.sp,
    fontWeight: FontWeight = FontWeight.Medium,
    subText: String? = null
) {
    Box(
        modifier = modifier
            .background(color = bg, shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text, color = fg, fontSize = fontSize, fontWeight = fontWeight, textAlign = TextAlign.Center)
            if (subText != null) {
                Text(subText, color = TextMuted, fontSize = 10.sp)
            }
        }
    }
}

/** 数字/运算符按钮网格 */
@Composable
fun CalcGrid(
    rows: Int,
    columns: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 简单包装,实际布局由调用者用 Row/Column 控制
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}
