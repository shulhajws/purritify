package com.example.purrytify.ui.profile

import android.util.Log
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purrytify.repository.DailyListenData
import com.example.purrytify.repository.MonthYear
import com.example.purrytify.ui.theme.DarkBlack
import com.example.purrytify.ui.theme.SpotifyGreen

@Composable
fun ListenTimeSoundCapsuleScreen(
    monthYear: MonthYear,
    viewModel: SoundCapsuleViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(monthYear) {
        viewModel.selectMonth(monthYear)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBlack)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Time listened",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = SpotifyGreen,
                    strokeWidth = 3.dp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Month Info
                    Text(
                        text = monthYear.getDisplayName(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    state.analytics?.let { analytics ->
                        // Main listening time display
                        Column {
                            Text(
                                text = "You listened to music for",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White
                            )

                            Text(
                                text = "${viewModel.formatMinutesToReadable(analytics.totalListenTimeMinutes)} this month.",
                                style = MaterialTheme.typography.headlineMedium,
                                color = SpotifyGreen,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Daily average: ${String.format("%.0f", analytics.dailyAverageMinutes)} min",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    } ?: run {
                        Text(
                            text = "No data available for ${monthYear.getDisplayName()}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    DailyChartSection(
                        monthYear = monthYear,
                        analytics = state.analytics,
                        viewModel = viewModel
                    )
                }

                state.analytics?.let { analytics ->
                    item {
                        AdditionalListeningStats(
                            analytics = analytics,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyChartSection(
    monthYear: MonthYear,
    analytics: com.example.purrytify.repository.MonthlyAnalytics?,
    viewModel: SoundCapsuleViewModel
) {
    var dailyData by remember(monthYear) {
        mutableStateOf<List<DailyListenData>>(emptyList())
    }

    LaunchedEffect(monthYear) {
        viewModel.loadDailyDataForMonth(monthYear) { data ->
            dailyData = data
        }
    }

    Log.d("SoundCapsuleScreen", "Recomposing DailyChartSection for $monthYear, dailyData size: ${dailyData.size}")

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Daily Listening Activity",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (analytics != null && analytics.totalListenTimeMinutes > 0 && dailyData.isNotEmpty()) {
                Log.d("SoundCapsuleScreen", "Displaying chart for $monthYear")
                DailyListenLineChart(
                    dailyData = dailyData,
                    monthYear = monthYear,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Chart Statistics
                val maxDay = dailyData.maxByOrNull { it.listenTimeMinutes }
                val totalDaysWithActivity = dailyData.count { it.listenTimeMinutes > 0 }
                val averageMinutes = if (totalDaysWithActivity > 0) {
                    dailyData.sumOf { it.listenTimeMinutes } / totalDaysWithActivity
                } else 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ChartStatItem(
                        label = "Peak Day",
                        value = "${maxDay?.listenTimeMinutes ?: 0}m",
                        subtitle = maxDay?.let { "Day ${it.day}" } ?: ""
                    )

                    ChartStatItem(
                        label = "Active Days",
                        value = "$totalDaysWithActivity",
                        subtitle = "of ${dailyData.size}"
                    )

                    ChartStatItem(
                        label = "Daily Average",
                        value = "${averageMinutes}m",
                        subtitle = "when active"
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            Color.Black.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "No listening data to display",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DailyListenLineChart(
    dailyData: List<DailyListenData>,
    monthYear: MonthYear,
    modifier: Modifier = Modifier
) {
    val maxMinutes = dailyData.maxOfOrNull { it.listenTimeMinutes } ?: 1
    val minMinutes = dailyData.minOfOrNull { it.listenTimeMinutes } ?: 0
    val range = maxOf(1, maxMinutes - minMinutes)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val chartHeight = height - 40.dp.toPx()
        val chartWidth = width - 40.dp.toPx()
        val startX = 20.dp.toPx()
        val startY = 20.dp.toPx()

        // Grid Lines
        val gridLines = 5
        repeat(gridLines) { i ->
            val y = startY + (i * chartHeight / (gridLines - 1))
            drawLine(
                color = Color.White.copy(alpha = 0.1f),
                start = Offset(startX, y),
                end = Offset(startX + chartWidth, y),
                strokeWidth = 1.dp.toPx()
            )

            val value = maxMinutes - (i * maxMinutes / (gridLines - 1))
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 10.sp.toPx()
                    alpha = 128
                }
                drawText(
                    "${value}m",
                    4.dp.toPx(),
                    y + 4.dp.toPx(),
                    paint
                )
            }
        }

        // Path for Line
        val path = androidx.compose.ui.graphics.Path()
        val points = mutableListOf<Offset>()

        dailyData.forEachIndexed { index, dayData ->
            val x = startX + (index * chartWidth / (dailyData.size - 1).coerceAtLeast(1))
            val normalizedValue = if (range > 0) {
                (dayData.listenTimeMinutes - minMinutes).toFloat() / range
            } else 0f
            val y = startY + chartHeight - (normalizedValue * chartHeight)

            points.add(Offset(x, y))

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        // Draw Line
        drawPath(
            path = path,
            color = SpotifyGreen,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )

        // Points on Line
        points.forEachIndexed { index, point ->
            val dayData = dailyData[index]

            drawCircle(
                color = if (dayData.listenTimeMinutes > 0) {
                    SpotifyGreen
                } else {
                    Color.White.copy(alpha = 0.3f)
                },
                radius = if (dayData.listenTimeMinutes > maxMinutes * 0.8) 6.dp.toPx() else 4.dp.toPx(),
                center = point
            )

            if (dayData.listenTimeMinutes > 0) {
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = point
                )
            }
        }

        // X-axis labels
        dailyData.forEachIndexed { index, dayData ->
            if (dayData.day % 5 == 1 || dayData.day == dailyData.size) {
                val x = startX + (index * chartWidth / (dailyData.size - 1).coerceAtLeast(1))
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        alpha = 128
                    }
                    drawText(
                        dayData.day.toString(),
                        x,
                        height - 4.dp.toPx(),
                        paint
                    )
                }
            }
        }

        // Fill area under the curve for visual appeal
        val fillPath = androidx.compose.ui.graphics.Path()
        fillPath.moveTo(startX, startY + chartHeight)
        points.forEach { point ->
            fillPath.lineTo(point.x, point.y)
        }
        fillPath.lineTo(startX + chartWidth, startY + chartHeight)
        fillPath.close()

        drawPath(
            path = fillPath,
            color = SpotifyGreen.copy(alpha = 0.1f)
        )
    }
}

@Composable
fun ChartStatItem(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = SpotifyGreen,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )

        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun AdditionalListeningStats(
    analytics: com.example.purrytify.repository.MonthlyAnalytics,
    viewModel: SoundCapsuleViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Listening Stats",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = "Unique Artists",
                        value = "${analytics.uniqueArtistsCount}",
                        modifier = Modifier.weight(1f)
                    )

                    StatItem(
                        label = "Unique Songs",
                        value = "${analytics.uniqueSongsCount}",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        label = "Total Minutes",
                        value = "${analytics.totalListenTimeMinutes}m",
                        modifier = Modifier.weight(1f)
                    )

                    StatItem(
                        label = "Daily Average",
                        value = "${String.format("%.1f", analytics.dailyAverageMinutes)}m",
                        modifier = Modifier.weight(1f)
                    )
                }

                if (analytics.totalListenTimeMinutes > 0) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Additional Insights
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = SpotifyGreen.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = SpotifyGreen,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "Music Discovery",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "You discovered ${analytics.uniqueSongsCount} different songs from ${analytics.uniqueArtistsCount} artists this month!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = SpotifyGreen,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}