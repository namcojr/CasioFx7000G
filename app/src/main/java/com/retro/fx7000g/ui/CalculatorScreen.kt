package com.retro.fx7000g.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.retro.fx7000g.calc.CalculatorState

@Composable
fun CalculatorScreen(modifier: Modifier = Modifier) {
    val state = remember { CalculatorState() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Fx7000gColors.BodyEdge)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Fx7000gColors.Body)
                .padding(14.dp)
        ) {
            BrandingHeader()
            Spacer(Modifier.height(10.dp))
            LcdDisplay(
                entry = state.entry,
                result = state.result,
                modeLabel = state.modeLabel,
                memorySet = state.memorySet,
                cursor = state.cursor,
                showCursor = state.showCursor,
                graph = state.graphBuffer,
                rangeLines = state.rangeLines,
                rangeCursorRow = state.rangeCursorRow,
                rangeCursorCol = state.rangeCursorCol
            )
            Spacer(Modifier.height(16.dp))
            Keypad(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun BrandingHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CASIO",
            color = Fx7000gColors.Branding,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "fx-7000G",
            color = Fx7000gColors.Branding,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(Fx7000gColors.ModelPlate)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "GRAPHICS",
                color = Fx7000gColors.Branding,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
