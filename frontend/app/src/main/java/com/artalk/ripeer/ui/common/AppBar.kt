package com.artalk.ripeer.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artalk.ripeer.R
import com.artalk.ripeer.ui.conversations.ui.theme.ArtalkTheme
import com.artalk.ripeer.ui.theme.BackGroundColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBar(onClickMenu: () -> Unit, userName: String) {
    ArtalkTheme {
        Surface(
            shadowElevation = 4.dp,
            tonalElevation = 0.dp,
        ) {
            CenterAlignedTopAppBar(
                title = {
                    val imageSizeModifier = Modifier
                        .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
                        .size(40.dp)
                    Box {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = R.drawable.logo),
                                    modifier = imageSizeModifier,
                                    contentScale = ContentScale.Crop,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Artalk",
                                        textAlign = TextAlign.Center,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "Welcome $userName",
                                        textAlign = TextAlign.Center,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onClickMenu()
                        },
                    ) {
                        Icon(
                            Icons.Filled.Menu,
                            "backIcon",
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = BackGroundColor,
                    titleContentColor = Color.White,
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppBarPreview() {
    AppBar(
        onClickMenu = {  },
        userName = "John Doe"
    )
}
