package com.fox.mobile.ui.drawer

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fox.mobile.ui.theme.FoxColors

data class FoxContact(
    val name: String,
    val role: String,
    val sub: String,
    val phone: String
)

val DEFAULT_FOX_CONTACTS = listOf(
    FoxContact("Kavin", "Lab partner", "WhatsApp · 2h ago", "+91 98401 22104"),
    FoxContact("Mom", "Mobile", "Called · yesterday", "+91 98401 10082"),
    FoxContact("Arun", "Project group", "Messages · Tue", "+91 98401 55819"),
    FoxContact("Priya", "Study group", "WhatsApp · Mon", "+91 98401 77412"),
    FoxContact("Prof. Ramesh", "Advisor · CS", "Office 304", "+91 44 2257 4350")
)

/**
 * Implements the `Fox_Line_v30.html` 56% Spotlight Drawer + Refractive Chromatic Search Lens (`.dr-search.lens`)
 * + `DCONTACTS` Contact Search with morphing Call/Message pill buttons (`50% -> 16px` radius).
 */
@Composable
fun SpotlightSheet(
    query: String,
    onQueryChange: (String) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onContactAction: (FoxContact, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 14.dp else 22.dp,
        label = "lensCornerMorph"
    )

    val matchingContact = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) null
        else DEFAULT_FOX_CONTACTS.firstOrNull {
            it.name.lowercase().contains(q) || it.role.lowercase().contains(q)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Refractive Chromatic Lens Search Pill (.dr-search.lens)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .background(FoxColors.BgPill)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            FoxColors.CyanLens.copy(alpha = 0.42f),
                            Color(0x28F3EFE6),
                            FoxColors.EmberWarm.copy(alpha = 0.42f)
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                )
                .clickable { if (!isExpanded) onToggleExpand() }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    cursorBrush = SolidColor(FoxColors.Ember),
                    textStyle = TextStyle(
                        color = FoxColors.InkPrimary,
                        fontSize = 14.sp
                    ),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Apps, files, contacts, or =math...",
                                color = FoxColors.InkDim,
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "⌘K",
                    color = FoxColors.InkDim,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Contact Hit Card (.dr-contact) from Fox_Line_v30
        if (matchingContact != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(FoxColors.BgCard)
                    .border(1.dp, Color(0x1FF3EFE6), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x24F2B33D)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = matchingContact.name.take(1),
                            color = FoxColors.Ember,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${matchingContact.name} · ${matchingContact.role}",
                            color = FoxColors.InkPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = matchingContact.sub,
                            color = FoxColors.InkMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x247FD6A4))
                            .clickable { onContactAction(matchingContact, "call") }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Call", color = FoxColors.OkMint, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x24F2B33D))
                            .clickable { onContactAction(matchingContact, "msg") }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Msg", color = FoxColors.Ember, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
