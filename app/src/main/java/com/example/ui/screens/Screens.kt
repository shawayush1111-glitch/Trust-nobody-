package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.GameViewModel
import com.example.ScreenState
import com.example.SoundManager
import com.example.data.LevelsDataSource
import com.example.data.Platform
import com.example.data.TrapType
import com.example.ui.theme.*
import com.example.R
import kotlinx.coroutines.delay
import java.util.Random

// --- SPLASH SCREEN ---
@Composable
fun SplashScreen(viewModel: GameViewModel) {
    var isCracked by remember { mutableStateOf(false) }
    val crackProgress by animateFloatAsState(
        targetValue = if (isCracked) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    LaunchedEffect(Unit) {
        delay(800)
        isCracked = true
        SoundManager.playFloorCrumble()
        delay(1400)
        viewModel.screenState = ScreenState.MAIN_MENU
        SoundManager.startBackgroundMusic()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Elegant, custom-rendered "TRUST NOBODY" typographic logo from reference
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.trust_nobody_logo),
                contentDescription = "Trust Nobody Logo",
                modifier = Modifier
                    .size(230.dp)
                    .scale(if (isCracked) 1.05f else 0.95f)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Every level is a lie... 💀",
                color = CrimsonPrimary.copy(alpha = 0.85f),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Animated Crack Overlay Visual
        if (isCracked) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(w / 2, 0f)
                    lineTo(w / 2 - 20 * crackProgress, h * 0.3f)
                    lineTo(w / 2 + 30 * crackProgress, h * 0.5f)
                    lineTo(w / 2 - 40 * crackProgress, h * 0.7f)
                    lineTo(w / 2 + 10 * crackProgress, h)
                }
                drawPath(path, color = CrimsonPrimary, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f * crackProgress))
            }
        }
    }
}

// --- MAIN MENU SCREEN ---
@Composable
fun MainMenuScreen(viewModel: GameViewModel) {
    var showLevelSelect by remember { mutableStateOf(false) }
    var currentRandomJoke by remember { mutableStateOf("Don't trust the floor.") }
    val jokesList = listOf(
        "Everything here is a lie.",
        "That safe button is definitely safe. Promise.",
        "Your jump buttons might not save you.",
        "Even the exit door is running away.",
        "The spikes are very friendly once you know them.",
        "Double jumping into pits is a pro strat.",
        "Gravity is merely a suggested rule."
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            currentRandomJoke = jokesList[Random().nextInt(jokesList.size)]
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 500.dp)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            // Genuine, glowing/pulsating "TRUST NOBODY" logo from reference
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.trust_nobody_logo),
                contentDescription = "Trust Nobody Logo",
                modifier = Modifier
                    .size(190.dp)
                    .scale(scale)
                    .padding(bottom = 4.dp)
            )

            Text(
                text = currentRandomJoke,
                color = WarningAmber,
                fontSize = 16.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Main menu buttons
            Button(
                onClick = { viewModel.startGame() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .border(2.dp, CrimsonPrimary, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PLAY GAME", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showLevelSelect = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("LEVELS", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = { viewModel.screenState = ScreenState.LEADERBOARD },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("TOP SCORES", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.screenState = ScreenState.SETTINGS },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (viewModel.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Muted State"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SETTINGS", fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Footer stats
            Text(
                text = "Total Deaths: ${viewModel.deathCount} 💀",
                color = Color.LightGray,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }

    if (showLevelSelect) {
        Dialog(onDismissRequest = { showLevelSelect = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .border(2.dp, CrimsonPrimary, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CHOOSE YOUR PAIN",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = CrimsonPrimary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(50) { index ->
                            val idx = index + 1
                            val levelInfo = LevelsDataSource.levels[idx - 1]
                            var difficultyLabel = "Easy"
                            var diffColor = NeonTeal
                            if (idx > 10) { difficultyLabel = "Medium"; diffColor = WarningAmber }
                            if (idx > 25) { difficultyLabel = "Hard"; diffColor = CrimsonPrimary }
                            if (idx > 40) { difficultyLabel = "CRUEL"; diffColor = Color.Magenta }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color.Black, RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                                    .clickable {
                                        showLevelSelect = false
                                        viewModel.skipToLevel(idx)
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Level $idx: ${levelInfo.name}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    Text(levelInfo.description, color = Color.Gray, fontSize = 12.sp)
                                }
                                Text(
                                    difficultyLabel,
                                    color = diffColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showLevelSelect = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CANCEL", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- GAME SURFACE AND CANVAS CANVAS SCENE ---
@Composable
fun GameLevelScreen(viewModel: GameViewModel) {
    val level = viewModel.currentLevelData ?: return

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val viewW = maxWidth
        val viewH = maxHeight

        // Dynamic coordinate translations (Original 1000f x 600f coordinates scaled to fit viewport aspect ratio)
        val scaleX = viewW.value / 1000f
        val scaleY = viewH.value / 600f
        val scaleFactor = minOf(scaleX, scaleY)

        val worldWPixels = 1000f * scaleFactor
        val worldHPixels = 600f * scaleFactor
        val startOffsetX = (viewW.value - worldWPixels) / 2
        val startOffsetY = (viewH.value - worldHPixels) / 2 // PERFECT CENTERING!

        // Physics game updates
        LaunchedEffect(Unit) {
            var lastTime = System.currentTimeMillis()
            while (true) {
                val now = System.currentTimeMillis()
                val dt = (now - lastTime) / 1000f
                lastTime = now
                // Bound max updates so we don't get huge jumps on slow frames
                viewModel.updatePhysics(dt.coerceAtMost(0.05f))
                delay(12) // Aim ~60fps checking
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Gameplay viewport Box with background covering full screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF070606))
            ) {
                // Main game engine rendering space
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    // Can use general stage tap gestures if needed
                                }
                            )
                        }
                ) {
                    val scopeOffsetX = startOffsetX * density
                    val scopeOffsetY = startOffsetY * density
                    val pxScale = scaleFactor * density

                    // Render game environment within scaled screen frames
                    clipRect(
                        left = scopeOffsetX,
                        top = scopeOffsetY,
                        right = scopeOffsetX + worldWPixels * density,
                        bottom = scopeOffsetY + worldHPixels * density
                    ) {
                        // Background Grid Aesthetic lines
                        val bgBrush = Brush.linearGradient(
                            colors = listOf(Color(0xFF0F0E0D), Color(0xFF030303))
                        )
                        drawRect(
                            brush = bgBrush,
                            topLeft = Offset(scopeOffsetX, scopeOffsetY),
                            size = Size(worldWPixels * density, worldHPixels * density)
                        )

                        // Draw level goal door portal
                        val portalX = scopeOffsetX + level.exitX * pxScale
                        val portalY = scopeOffsetY + level.exitY * pxScale
                        val portalW = 40f * pxScale
                        val portalH = 50f * pxScale
                        drawRect(
                            color = NeonTeal,
                            topLeft = Offset(portalX, portalY),
                            size = Size(portalW, portalH),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f * density)
                        )
                        drawRect(
                            color = NeonTeal.copy(alpha = 0.25f),
                            topLeft = Offset(portalX + 4f, portalY + 4f),
                            size = Size(portalW - 8f, portalH - 8f)
                        )
                        // Label exit door
                        drawContext.canvas.nativeCanvas.apply {
                            val paint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 11f * density
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                            drawText("EXIT", portalX + portalW / 2, portalY - 6f * density, paint)
                        }

                        // Draw Platforms
                        viewModel.activePlatforms.forEach { p ->
                            if (!p.isVanished) {
                                val x = scopeOffsetX + p.x * pxScale
                                val y = scopeOffsetY + p.y * pxScale
                                val w = p.width * pxScale
                                val h = p.height * pxScale

                                rotate(p.angle, pivot = Offset(x + w / 2, y + h / 2)) {
                                    // Custom visual designs based on platform types
                                    val platColor = when (p.type) {
                                        TrapType.DISAPPEARING_FLOOR -> CrimsonDark.copy(alpha = (1f - p.progress * 2f).coerceIn(0.1f, 1f))
                                        TrapType.SPEED_FLOOR -> WarningAmber
                                        TrapType.SHRINKING_PLATFORM -> Color(0xFF7C3AED)
                                        TrapType.FAKE_WALL -> Color.DarkGray
                                        else -> ObsidianSurface
                                    }

                                    // Outer borders glow
                                    val borderStroke = if (p.type == TrapType.DISAPPEARING_FLOOR) CrimsonPrimary else Color.Gray

                                    drawRect(
                                        color = platColor,
                                        topLeft = Offset(x, y),
                                        size = Size(w, h)
                                    )
                                    drawRect(
                                        color = borderStroke,
                                        topLeft = Offset(x, y),
                                        size = Size(w, h),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f * density)
                                    )

                                    // Special Warning symbols on tricky blocks
                                    if (p.type == TrapType.DISAPPEARING_FLOOR) {
                                        drawLine(
                                            color = CrimsonPrimary,
                                            start = Offset(x, y),
                                            end = Offset(x + w, y + h),
                                            strokeWidth = 1f * density
                                        )
                                    }
                                }
                            }
                        }

                        // Draw Hazardo Objects (spikes, flags, traps)
                        viewModel.activeObjects.forEach { obj ->
                            val x = scopeOffsetX + obj.x * pxScale
                            val y = scopeOffsetY + obj.y * pxScale
                            val w = obj.width * pxScale
                            val h = obj.height * pxScale

                            when (obj.type) {
                                TrapType.SPIKES -> {
                                    // Draw series of lethal sharp hazard triangles
                                    val numTriangles = (obj.width / 20f).toInt().coerceAtLeast(1)
                                    val triW = w / numTriangles
                                    for (i in 0 until numTriangles) {
                                        val startX = x + i * triW
                                        val path = Path().apply {
                                            moveTo(startX, y + h)
                                            lineTo(startX + triW / 2, y)
                                            lineTo(startX + triW, y + h)
                                            close()
                                        }
                                        drawPath(path, color = CrimsonPrimary)
                                    }
                                }
                                TrapType.LYING_BUTTON -> {
                                    // Round green button base labeled SAFE
                                    drawArc(
                                        color = if (obj.isActivated) Color.Gray else NeonTeal,
                                        startAngle = 180f,
                                        sweepAngle = 180f,
                                        useCenter = true,
                                        topLeft = Offset(x, y),
                                        size = Size(w, h * 2)
                                    )
                                    drawContext.canvas.nativeCanvas.apply {
                                        val paint = android.graphics.Paint().apply {
                                            color = android.graphics.Color.BLACK
                                            textSize = 8f * density
                                            isFakeBoldText = true
                                            textAlign = android.graphics.Paint.Align.CENTER
                                        }
                                        drawText(obj.message, x + w / 2, y + h * 1.1f, paint)
                                    }
                                    Unit
                                }
                                TrapType.FAKE_CHECKPOINT -> {
                                    // Drawing checkpoint flagpole
                                    val flagColor = if (obj.isActivated) NeonTeal else Color.LightGray
                                    drawLine(
                                        color = Color.DarkGray,
                                        start = Offset(x + w / 2, y + h),
                                        end = Offset(x + w / 2, y),
                                        strokeWidth = 3f * density
                                    )
                                    val flagPath = Path().apply {
                                        moveTo(x + w / 2, y)
                                        lineTo(x + w, y + h / 3)
                                        lineTo(x + w / 2, y + h / 2)
                                        close()
                                    }
                                    drawPath(flagPath, color = flagColor)
                                    drawContext.canvas.nativeCanvas.apply {
                                        val paint = android.graphics.Paint().apply {
                                            color = android.graphics.Color.WHITE
                                            textSize = 9f * density
                                            textAlign = android.graphics.Paint.Align.CENTER
                                        }
                                        drawText(obj.message, x + w / 2, y - 4f * density, paint)
                                    }
                                    Unit
                                }
                                TrapType.WRONG_ARROW -> {
                                    // Pointer arrow helper
                                    val path = Path().apply {
                                        if (obj.extraData == "RIGHT") {
                                            moveTo(x, y + h / 3)
                                            lineTo(x + w * 0.6f, y + h / 3)
                                            lineTo(x + w * 0.6f, y)
                                            lineTo(x + w, y + h / 2)
                                            lineTo(x + w * 0.6f, y + h)
                                            lineTo(x + w * 0.6f, y + h * 0.66f)
                                            lineTo(x, y + h * 0.66f)
                                            close()
                                        } else {
                                            moveTo(x + w, y + h / 3)
                                            lineTo(x + w * 0.4f, y + h / 3)
                                            lineTo(x + w * 0.4f, y)
                                            lineTo(x, y + h / 2)
                                            lineTo(x + w * 0.4f, y + h)
                                            lineTo(x + w * 0.4f, y + h * 0.66f)
                                            lineTo(x + w, y + h * 0.66f)
                                            close()
                                        }
                                    }
                                    drawPath(path, color = WarningAmber)
                                }
                                else -> {}
                            }
                        }

                        // Draw Character Player Avatar (Glowing custom cube)
                        val px = scopeOffsetX + viewModel.playerX * pxScale
                        val py = scopeOffsetY + viewModel.playerY * pxScale
                        val pw = viewModel.playerWidth * pxScale
                        val ph = viewModel.playerHeight * pxScale

                        // Rotate upside down if gravity is inverted
                        val charAngle = if (viewModel.gravityFlipped) 180f else 0f
                        rotate(charAngle, Offset(px + pw / 2, py + ph / 2)) {
                            // Draw yellow face block
                            drawRect(
                                color = Color.Yellow,
                                topLeft = Offset(px, py),
                                size = Size(pw, ph)
                            )
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(px, py),
                                size = Size(pw, ph),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f * density)
                            )

                            // Eyes - look suspicious
                            val eyeOffset = if (viewModel.vx >= 0) 4f * density else 0f
                            drawCircle(
                                color = Color.Black,
                                radius = 2.5f * density,
                                center = Offset(px + pw / 4 + eyeOffset, py + ph / 3)
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = 2.5f * density,
                                center = Offset(px + pw * 0.6f + eyeOffset, py + ph / 3)
                            )
                            // Funny tiny dynamic mouth
                            drawLine(
                                color = Color.Black,
                                start = Offset(px + pw / 3, py + ph * 0.6f),
                                end = Offset(px + pw * 0.66f, py + ph * 0.6f),
                                strokeWidth = 2f * density
                            )
                        }
                    }
                }

                // AI SYSTEM LEVEL INTRO MESSAGE ANNOUNCEMENTS Overlays
                if (viewModel.showIntroductionToast) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CrimsonDark.copy(alpha = 0.9f)),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                            .border(2.dp, CrimsonPrimary, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "SYSTEM ANNOUNCEMENT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                viewModel.levelAnnouncementText,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // SCREEN CRACKS VISUAL OVERLAYS ON FAKE COMPLETE
                if (viewModel.showFakeWinCrack) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(CrimsonDark.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "JUST KIDDING 😈",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Red,
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }
                }
            }

            // 2. HUD TOP BAR OVERLAY
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LEVEL ${level.id}",
                            color = CrimsonPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.width(1.dp).height(14.dp).background(Color.DarkGray))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = level.name,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Deaths symbol",
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${viewModel.levelDeaths} Deaths",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    IconButton(
                        onClick = { viewModel.showPauseDialog = true },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .size(38.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause button", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 3. SEMI-TRANSPARENT GAMEPAD TOUCH OVERLAYS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.BottomStart)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        viewModel.keyMoveLeft = true
                                        tryAwaitRelease()
                                        viewModel.keyMoveLeft = false
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Move Left arrow", tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        viewModel.keyMoveRight = true
                                        tryAwaitRelease()
                                        viewModel.keyMoveRight = false
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Move Right arrow", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }

                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 54.dp)
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CrimsonDark.copy(alpha = 0.65f))
                        .border(2.dp, CrimsonPrimary.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    viewModel.pressJump()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JUMP",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }

    if (viewModel.showPauseDialog) {
        Dialog(onDismissRequest = { viewModel.showPauseDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .border(2.dp, CrimsonPrimary, RoundedCornerShape(16.dp))
                    .padding(4.dp)
                    .widthIn(max = 280.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("GAME PAUSED", fontSize = 18.sp, fontWeight = FontWeight.Black, color = CrimsonPrimary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.showPauseDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("RESUME", color = Color.White, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.showPauseDialog = false
                            viewModel.restartCurrentLevel()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                    ) {
                        Text("RESTART LEVEL", color = Color.White, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { viewModel.goToMenu() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                    ) {
                        Text("QUIT TO MENU", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// --- DEATH SCREEN ---
@Composable
fun DeathScreen(viewModel: GameViewModel) {
    var animatedFlash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(150)
        animatedFlash = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (animatedFlash) CrimsonDark else Color.Black)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 500.dp)
        ) {
            Text(
                "YOU DIED",
                color = CrimsonPrimary,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Text(
                "DEATH COUNT: ${viewModel.deathCount} 💀",
                color = Color.LightGray,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Gemini Narrative mock card
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CrimsonPrimary, RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "THE NARRATOR SAYS:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (viewModel.isGeneratingTaunt) {
                        CircularProgressIndicator(color = CrimsonPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "\"${viewModel.activeTaunt}\"",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { viewModel.restartCurrentLevel() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .border(2.dp, CrimsonPrimary, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("TRY AGAIN", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.goToMenu() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("MAIN MENU", fontSize = 15.sp, color = Color.LightGray)
            }
        }
    }
}

// --- LEVEL COMPLETE SCREEN ---
@Composable
fun LevelCompleteScreen(viewModel: GameViewModel) {
    val level = viewModel.currentLevelData ?: return

    // Calculate stars: 0 deaths = 3 stars, 1-3 deaths = 2 stars, 4+ deaths = 1 star
    val completionDeaths = viewModel.levelDeaths
    val stars = when {
        completionDeaths == 0 -> 3
        completionDeaths in 1..3 -> 2
        else -> 1
    }

    var showHintDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 500.dp)
        ) {
            Text(
                "LEVEL COMPLETE 🎉",
                color = NeonTeal,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Star representations
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 1..3) {
                    val isLit = i <= stars
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = if (isLit) WarningAmber else Color.DarkGray,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Text(
                "Completed in $completionDeaths deaths!",
                color = Color.Gray,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Next Level trigger
            Button(
                onClick = { viewModel.proceedToNextLevel() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .border(2.dp, NeonTeal, RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("PROCEED TO NEXT LAYOUT", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rewarded ad simulation trigger
            Button(
                onClick = {
                    showHintDialog = true
                    // Call the AI with a random chance to lie! Sarcasm rule: 50% chance hint is wrong!
                    viewModel.requestRealAILobbyHint(isFake = Random().nextBoolean())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Movie, contentDescription = "Watch AD", tint = WarningAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WATCH SHORT AD FOR FAKE HINT", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.goToMenu() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("MAIN MENU", color = Color.Gray)
            }
        }
    }

    if (showHintDialog) {
        Dialog(onDismissRequest = { showHintDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                modifier = Modifier
                    .border(2.dp, WarningAmber, RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("REWARD REGISTERED! 📺", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Ad complete. The AI generated a helpful guide for level ${level.id + 1}:",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (viewModel.isGeneratingHint) {
                        CircularProgressIndicator(color = WarningAmber, modifier = Modifier.size(28.dp))
                    } else {
                        Text(
                            text = "\"${viewModel.aiHintText}\"",
                            color = Color.White,
                            fontStyle = FontStyle.Italic,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Serif
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { showHintDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("AWESOME", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- LEADERBOARD HALL SCREEN ---
@Composable
fun LeaderboardScreen(viewModel: GameViewModel) {
    val entries by viewModel.leaderboardEntries.collectAsState()

    // Divide entries into Hall of Fame (fewest deaths) versus Hall of Shame (most deaths)
    val hallOfFame = entries.sortedBy { it.totalDeaths }.take(5)
    val hallOfShame = entries.sortedByDescending { it.totalDeaths }.take(5)

    var currentTabIsFame by remember { mutableStateOf(true) }
    var inputName by remember { mutableStateOf(viewModel.playerName) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 500.dp)
        ) {
            Text(
                "GLOBAL RANKS",
                color = CrimsonPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Submit Name widget
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Your Sarcastic Tag") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = CrimsonPrimary
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = {
                        viewModel.savePlayerName(inputName)
                        viewModel.submitLeaderboardScore()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("SUBMIT")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Tabs toggle headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianSurface, RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (currentTabIsFame) CrimsonDark else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { currentTabIsFame = true }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("HALL OF FAME 👑", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (!currentTabIsFame) CrimsonDark else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { currentTabIsFame = false }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("HALL OF SHAME 💀", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List rankings
            LazyColumn(modifier = Modifier.weight(1f)) {
                val currentList = if (currentTabIsFame) hallOfFame else hallOfShame
                itemsIndexed(currentList) { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(if (entry.name == viewModel.playerName) CrimsonDark.copy(alpha = 0.3f) else ObsidianSurface, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#${index + 1}",
                                color = WarningAmber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.width(36.dp)
                            )
                            Column {
                                Text(entry.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                Text("Cleared ${entry.levelsCompleted} levels", color = Color.Gray, fontSize = 12.sp)
                            }
                        }

                        Text(
                            "${entry.totalDeaths} Deaths",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.goToMenu() },
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("RETURN", color = Color.LightGray)
            }
        }
    }
}

// --- SETTINGS SCREEN ---
@Composable
fun SettingsScreen(viewModel: GameViewModel) {
    var inputName by remember { mutableStateOf(viewModel.playerName) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 500.dp)
        ) {
            Text(
                "SETTINGS",
                color = CrimsonPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Name section
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("PLAYER REGISTRY", fontSize = 12.sp, color = WarningAmber, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputName,
                        onValueChange = {
                            inputName = it
                            viewModel.savePlayerName(it)
                        },
                        label = { Text("Configure Display Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray,
                            focusedBorderColor = CrimsonPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio settings
            Card(
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SOUND AND SYNTH EFFECTS", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Mute all real-time 8-bit sound chimes", color = Color.Gray, fontSize = 11.sp)
                    }

                    Switch(
                        checked = !viewModel.isMuted,
                        onCheckedChange = { viewModel.toggleMute() },
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonTeal, checkedTrackColor = Color(0xFF0F766E))
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { viewModel.goToMenu() },
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("DONE", color = Color.LightGray)
            }
        }
    }
}
