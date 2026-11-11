package com.auramusic.offline.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auramusic.offline.model.AudioTrack
import com.auramusic.offline.theme.AppleCrimson
import com.auramusic.offline.util.HapticHelper
import kotlinx.coroutines.launch

@Composable
fun AlphabetFastScroller(
    tracks: List<AudioTrack>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    headerOffsetCount: Int = 0 // Account for items before the tracks in LazyColumn
) {
    val context = LocalContext.current
    val alphabet = remember {
        listOf("#") + ('A'..'Z').map { it.toString() }
    }

    val coroutineScope = rememberCoroutineScope()
    var activeLetter by remember { mutableStateOf<String?>(null) }

    fun scrollToLetter(letter: String) {
        val targetIndex = if (letter == "#") {
            0
        } else {
            tracks.indexOfFirst {
                it.title.trim().startsWith(letter, ignoreCase = true)
            }
        }

        if (targetIndex != -1) {
            coroutineScope.launch {
                listState.scrollToItem(targetIndex + headerOffsetCount)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(28.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(vertical = 4.dp, horizontal = 2.dp)
                .pointerInput(tracks) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            val itemHeight = size.height.toFloat() / alphabet.size
                            val index = (offset.y / itemHeight).toInt().coerceIn(0, alphabet.lastIndex)
                            activeLetter = alphabet[index]
                            HapticHelper.lightTick(context)
                            scrollToLetter(alphabet[index])
                        },
                        onDragEnd = {
                            activeLetter = null
                        },
                        onDragCancel = {
                            activeLetter = null
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val itemHeight = size.height.toFloat() / alphabet.size
                            val index = (change.position.y / itemHeight).toInt().coerceIn(0, alphabet.lastIndex)
                            val letter = alphabet[index]
                            if (activeLetter != letter) {
                                activeLetter = letter
                                HapticHelper.lightTick(context)
                                scrollToLetter(letter)
                            }
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            alphabet.forEach { char ->
                Text(
                    text = char,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (activeLetter == char) AppleCrimson else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 0.5.dp)
                )
            }
        }

        // Floating Apple magnifier preview bubble with spring pop-in
        AnimatedVisibility(
            visible = activeLetter != null,
            enter = fadeIn() + scaleIn(spring(dampingRatio = 0.6f)),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-48).dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .shadow(10.dp, CircleShape, spotColor = AppleCrimson.copy(alpha = 0.5f))
                    .clip(CircleShape)
                    .background(AppleCrimson),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLetter ?: "",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
