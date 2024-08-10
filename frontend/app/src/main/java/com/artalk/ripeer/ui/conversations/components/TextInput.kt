package com.artalk.ripeer.ui.conversations.components

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artalk.ripeer.ui.conversations.ConversationViewModel
import kotlinx.coroutines.launch
import android.widget.Toast

@Composable
fun TextInput(
    conversationViewModel: ConversationViewModel = hiltViewModel()
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    TextInputIn(
        sendMessage = { text ->
            coroutineScope.launch {
                conversationViewModel.sendMessageToCustomApi(text)
            }
        },
        startVoiceInput = { onVoiceInputFinished ->
            startVoiceRecognitionActivity(context, onVoiceInputFinished)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextInputIn(
    sendMessage: (String) -> Unit,
    startVoiceInput: (onVoiceInputFinished: (String) -> Unit) -> Unit
) {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var isListening by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column {
            Divider(Modifier.height(0.2.dp))
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .padding(top = 6.dp, bottom = 10.dp)
            ) {
                Row {
                    TextField(
                        value = text,
                        onValueChange = {
                            text = it
                        },
                        label = null,
                        placeholder = { Text("Ask me anything", fontSize = 12.sp) },
                        shape = RoundedCornerShape(25.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .weight(1f),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                        ),
                    )
                    IconButton(onClick = {
                        scope.launch {
                            val textClone = text.text.toString()
                            text = TextFieldValue("")
                            sendMessage(textClone)
                        }
                    }) {
                        Icon(
                            Icons.Filled.Send,
                            "sendMessage",
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = {
                        isListening = true // Start listening
                        startVoiceInput { voiceInputText ->
                            scope.launch {
                                sendMessage(voiceInputText)
                                isListening = false // Stop listening
                            }
                        }
                    }) {
                        if (isListening) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Filled.Mic,
                                "voiceInput",
                                modifier = Modifier.size(26.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun startVoiceRecognitionActivity(context: Context, onVoiceInputFinished: (String) -> Unit) {
    val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
    speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
    speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")

    val speechRecognizerListener = object : android.speech.RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onError(error: Int) {
            Toast.makeText(context, "Voice recognition error: $error", Toast.LENGTH_SHORT).show()
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches != null && matches.isNotEmpty()) {
                onVoiceInputFinished(matches[0])
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    speechRecognizer.setRecognitionListener(speechRecognizerListener)
    speechRecognizer.startListening(speechIntent)
}

@Preview()
@Composable
fun PreviewTextInput() {
    TextInputIn(
        sendMessage = {},
        startVoiceInput = {}
    )
}
