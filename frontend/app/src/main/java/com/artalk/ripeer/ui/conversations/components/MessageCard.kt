package com.artalk.ripeer.ui.conversations.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artalk.ripeer.models.MessageModel
import com.artalk.ripeer.ui.conversations.ConversationViewModel
import com.artalk.ripeer.ui.theme.*
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.ui.CodeBlockStyle
import com.halilibo.richtext.ui.RichText
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material3.SetupMaterial3RichText

@Composable
fun MessageCard(message: MessageModel, isHuman: Boolean = false, isLast: Boolean = false, apiKey: String, voiceId: String) {
    Column(
        horizontalAlignment = if (isHuman) Alignment.End else Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .padding(top = if (isLast) 120.dp else 0.dp)
    ) {
        Box(
            modifier = Modifier
                .widthIn(0.dp, 260.dp)
                .background(
                    if (isHuman) BackGroundMessageHuman else BackGroundMessageGPT,
                    shape = RoundedCornerShape(12.dp)
                ),
        ) {
            if (isHuman) {
                HumanMessageCard(message = message)
            } else {
                BotMessageCard(message = message, apiKey = apiKey, voiceId = voiceId)
            }
        }
    }
}

@Composable
fun HumanMessageCard(message: MessageModel) {
    Text(
        text = message.question,
        fontSize = 14.sp,
        color = ColorTextHuman,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
        textAlign = TextAlign.Justify,
    )
}

@Composable
fun BotMessageCard(message: MessageModel, apiKey: String, voiceId: String) {
    val context = LocalContext.current
    val conversationViewModel: ConversationViewModel = hiltViewModel()

    Column(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .clickable {
                Log.d("BotMessageCard", "Message clicked: ${message.answer}")
                conversationViewModel.toggleAudioPlayback(apiKey, voiceId, message.id, message.answer)
            }
    ) {
        SetupMaterial3RichText {
            RichText(
                style = RichTextStyle(
                    codeBlockStyle = CodeBlockStyle(
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = ColorTextGPT,
                        ),
                        wordWrap = true,
                        modifier = Modifier.background(
                            color = Color.Black,
                            shape = RoundedCornerShape(6.dp)
                        )
                    )
                )
            ) {
                Markdown(
                    message.answer.trimIndent()
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Speaker Icon",
                tint = Color.Gray,
                modifier = Modifier.clickable {
                    Log.d("BotMessageCard", "Speaker icon clicked: ${message.answer}")
                    conversationViewModel.toggleAudioPlayback(apiKey, voiceId, message.id, message.answer)
                }
            )
        }
    }
}