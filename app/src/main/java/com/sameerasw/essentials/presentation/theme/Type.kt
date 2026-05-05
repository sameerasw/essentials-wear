package com.sameerasw.essentials.presentation.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sameerasw.essentials.R

@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexRounded = FontFamily(
    Font(
        R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("ROND", 100f)
        )
    )
)

// Wider variant for prominent titles on the home screen
@OptIn(ExperimentalTextApi::class)
val GoogleSansFlexRoundedWide = FontFamily(
    Font(
        R.font.google_sans_flex,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("ROND", 100f),
            FontVariation.Setting("wdth", 125f)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val WearTypography = androidx.wear.compose.material.Typography(
    display1 = TextStyle(
        fontFamily = GoogleSansFlexRounded,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp
    ),
    display2 = TextStyle(
        fontFamily = GoogleSansFlexRounded,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp
    ),
    display3 = TextStyle(
        fontFamily = GoogleSansFlexRounded,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp
    ),
    title1 = TextStyle(
        fontFamily = GoogleSansFlexRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp
    ),
    title2 = TextStyle(
        fontFamily = GoogleSansFlexRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp
    ),
    title3 = TextStyle(
        fontFamily = GoogleSansFlexRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    body1 = TextStyle(
        fontFamily = GoogleSansFlexRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    body2 = TextStyle(
        fontFamily = GoogleSansFlexRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    button = TextStyle(
        fontFamily = GoogleSansFlexRounded,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp
    ),
    caption1 = TextStyle(
        fontFamily = GoogleSansFlexRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    caption2 = TextStyle(
        fontFamily = GoogleSansFlexRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),
    caption3 = TextStyle(
        fontFamily = GoogleSansFlexRounded,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp
    )
)
