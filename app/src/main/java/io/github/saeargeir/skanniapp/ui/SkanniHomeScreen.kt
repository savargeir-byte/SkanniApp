package io.github.saeargeir.skanniapp.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkanniHomeScreen(
    onOverview: () -> Unit,
    onNotes: () -> Unit,
    onScan: () -> Unit,
    onBatchScan: () -> Unit = {},
    onSendExcel: () -> Unit,
    onMenu: () -> Unit,
    onLogout: () -> Unit = {},
    // New callbacks
    onExportCsv: () -> Unit = {},
    onExportJson: () -> Unit = {},
    // Auto scan trigger
    autoStartScan: Boolean = false
) {
    // Settings menu state
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showFloatingMenu by remember { mutableStateOf(false) }
    
    // Background processing state
    var isBackgroundProcessing by remember { mutableStateOf(false) }
    var backgroundProcessingStage by remember { mutableStateOf("") }
    var showProcessingSuccess by remember { mutableStateOf(false) }
    
    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    
    // Button animation states
    var showButtons by remember { mutableStateOf(false) }
    var showMainActions by remember { mutableStateOf(false) }
    var showExportActions by remember { mutableStateOf(false) }
    
    // Floating animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "logo_float")
    val logoOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), 
        label = "logo_offset"
    )
    
    // Trigger entrance animations
    LaunchedEffect(Unit) {
        isVisible = true
        delay(200)
        showContent = true
        delay(300)
        showButtons = true
        delay(400)
        showMainActions = true
        delay(500)
        showExportActions = true
    }
    
    // Simulation of background processing when returning from camera
    LaunchedEffect(autoStartScan) {
        if (autoStartScan) {
            // Start background processing simulation
            isBackgroundProcessing = true
            backgroundProcessingStage = "Læsir texta úr mynd..."
            
            // Simulate OCR processing time
            kotlinx.coroutines.delay(2000)
            backgroundProcessingStage = "Vistar í skýið..."
            
            kotlinx.coroutines.delay(1500)
            backgroundProcessingStage = "Vistar reikninginn..."
            
            kotlinx.coroutines.delay(1000)
            
            // Show success indicator
            isBackgroundProcessing = false
            showProcessingSuccess = true
            backgroundProcessingStage = "✅ Afgreitt!"
            
            // Hide success after 2 seconds - EKKI byrja aftur sjálfkrafa
            kotlinx.coroutines.delay(2000)
            showProcessingSuccess = false
            // Fjarlægt: onScan() - Byrjar EKKI næsta skann sjálfkrafa
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.primary
                        ),
                        radius = 1200f
                    )
                )
        ) {
        // Settings menu button in top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = { showSettingsMenu = true },
                modifier = Modifier
                    .background(
                        Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = "Stillingar",
                    tint = Color.White
                )
            }

            // Settings dropdown menu
            DropdownMenu(
                expanded = showSettingsMenu,
                onDismissRequest = { showSettingsMenu = false },
                modifier = Modifier.background(Color.White)
            ) {
                DropdownMenuItem(
                    text = { Text("Stillingar") },
                    onClick = {
                        showSettingsMenu = false
                        onMenu()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Um forritið") },
                    onClick = {
                        showSettingsMenu = false
                        // Handle about screen
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Info, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Hjálp") },
                    onClick = {
                        showSettingsMenu = false
                        // Handle help screen
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null)
                    }
                )
                // New export actions
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Flytja út CSV") },
                    onClick = {
                        showSettingsMenu = false
                        onExportCsv()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Download, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Flytja út JSON") },
                    onClick = {
                        showSettingsMenu = false
                        onExportJson()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.DataObject, contentDescription = null)
                    }
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Útskrá", color = Color.Red) },
                    onClick = {
                        showSettingsMenu = false
                        onLogout()
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.Red)
                    }
                )
            }
        }

            // Main content area with improved responsive layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp) // Basic padding instead of responsivePadding
                    .padding(bottom = 80.dp), // Reduced padding for navigation bar
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App logo and intro with animation
                Spacer(modifier = Modifier.height(32.dp))

                AnimatedVisibility(
                    visible = showContent,
                    enter = slideInVertically(
                        initialOffsetY = { -300 },
                        animationSpec = tween(800, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(800))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = logoOffset.dp), // Floating animation
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Animated logo with pulse effect
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse_scale"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFF4CAF50),
                                                Color(0xFF2E7D32)
                                            )
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = "SkanniApp Logo",
                                    modifier = Modifier
                                        .size(40.dp)
                                        .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale),
                                    tint = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                "SkanniApp",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            
                            Text(
                                "Íslenski reikningaskannarinn",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF4CAF50),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Professional action buttons with animations
                AnimatedVisibility(
                    visible = showButtons,
                    enter = slideInVertically(
                        initialOffsetY = { 200 },
                        animationSpec = tween(600, delayMillis = 300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(600, delayMillis = 300))
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                "📷 Skanna reikninga",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Main scanning buttons with enhanced design
                            Button(
                                onClick = onScan,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Camera, 
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Skanna einn reikning",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedButton(
                                onClick = onBatchScan,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF4CAF50)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt, 
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Fjöldaskanning (Pro)")
                            }
                        }
                    }
                }

            Spacer(modifier = Modifier.height(16.dp))

            // Management buttons with enhanced animations and design
            AnimatedVisibility(
                visible = showMainActions,
                enter = slideInHorizontally(
                    initialOffsetX = { 400 },
                    animationSpec = tween(700, delayMillis = 400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(700, delayMillis = 400))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "📊 Reikninga Stjórnun",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // YFIRLIT button with enhanced design
                            Button(
                                onClick = onOverview,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2E7D32)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Assessment,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = Color.White
                                    )
                                    Text(
                                        "YFIRLIT", 
                                        fontSize = 13.sp, 
                                        color = Color.White, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // SKOÐA REIKNINGA button with enhanced design
                            Button(
                                onClick = onNotes,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF1976D2)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Receipt,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = Color.White
                                    )
                                    Text(
                                        "REIKNINGA", 
                                        fontSize = 12.sp, 
                                        color = Color.White, 
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Excel export button with animation
            AnimatedVisibility(
                visible = showExportActions,
                enter = slideInVertically(
                    initialOffsetY = { 300 },
                    animationSpec = tween(600, delayMillis = 500, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(600, delayMillis = 500))
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            "📤 Flytja út gögn",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        OutlinedButton(
                            onClick = onSendExcel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF2E7D32)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Senda Excel skrá", 
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Ice Veflausnir branding  
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Powered by Ice Veflausnir",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Menu button at bottom
            TextButton(
                onClick = onMenu,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White.copy(alpha = 0.8f)
                )
            ) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Valmynd", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
        
        // Simple floating action button for menu
        FloatingActionButton(
            onClick = onMenu,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu")
        }
        
        // Background processing indicator overlay
        if (isBackgroundProcessing || showProcessingSuccess) {
            BackgroundProcessingIndicator(
                isProcessing = isBackgroundProcessing,
                isSuccess = showProcessingSuccess,
                stage = backgroundProcessingStage,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun BackgroundProcessingIndicator(
    isProcessing: Boolean,
    isSuccess: Boolean,
    stage: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSuccess -> Color(0xFF4CAF50).copy(alpha = 0.95f) // Grænn fyrir success
                isProcessing -> Color.Black.copy(alpha = 0.85f) // Svartur fyrir processing
                else -> Color.Transparent
            }
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSuccess) {
                // Grænn hringur með checkmark
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Afgreitt",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            } else if (isProcessing) {
                // Spinning indicator fyrir processing
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            
            Text(
                text = stage,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
