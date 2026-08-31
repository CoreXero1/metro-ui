package com.codeancy.metroui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codeancy.metroui.common.utils.MetroConfig
import com.codeancy.metroui.firebase.MetroConfigKey
import indianmetro.metroui.generated.resources.Res
import indianmetro.metroui.generated.resources.ic_premium
import indianmetro.metroui.generated.resources.metro
import org.corexero.sutradhar.remoteConfig.FirebaseRemoteConfig
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun HomeScreenHeader(
    title: String,
    selectedCity: String = "",
    supportedCities: List<String> = emptyList(),
    onCitySelected: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    onPremiumClicked: () -> Unit = {}
) {
    var isCityMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary)
            .padding(
                vertical = 12.dp,
                horizontal = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.metro),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        if (supportedCities.isNotEmpty()) {
            OutlinedButton(
                onClick = { isCityMenuExpanded = true },
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.14f),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier
                    .height(36.dp)
                    .widthIn(max = 170.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationCity,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = selectedCity,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Change city",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = isCityMenuExpanded,
                onDismissRequest = { isCityMenuExpanded = false }
            ) {
                supportedCities.forEach { city ->
                    val isSelected = city == selectedCity
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = city,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            isCityMenuExpanded = false
                            onCitySelected(city)
                        },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }
        }

        if (MetroConfig.showPremium &&
            FirebaseRemoteConfig.getBoolean(MetroConfigKey.EnablePremium) &&
            !MetroConfig.isPremiumUser
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFD700).copy(alpha = 0.2f),
                border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.7f)),
                modifier = Modifier
                    .clickable { onPremiumClicked() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = vectorResource(Res.drawable.ic_premium),
                        contentDescription = "Upgrade to Pro",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "PRO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }

    }
}

@Preview
@Composable
fun HomeScreenHeaderPreview() {
    HomeScreenHeader(
        title = "Metro UI",
        selectedCity = "Bengaluru",
        supportedCities = listOf("Bengaluru", "Delhi"),
        onCitySelected = {}
    )
}
