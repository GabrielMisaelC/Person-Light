package br.com.misael.software.personlight

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.misael.software.personlight.Utils.HouseViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

class MapActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp {
                MapWithDrawerScreen(applicationContext)
            }
        }
    }

}

@Composable
fun MyApp(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapWithDrawerScreen(context: Context) {
    val houseViewModel: HouseViewModel = HouseViewModel()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ButtonOption(text = "House", context, houseViewModel)
                    Spacer(modifier = Modifier.height(20.dp))
                    ButtonOption(text = "Business", context, houseViewModel)
                    Spacer(modifier = Modifier.height(20.dp))
                    ButtonOption(text = "Leisure", context, houseViewModel)

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Misael Software @ V1.0.0",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        content = {
            Scaffold(
                topBar = {
                    SmallTopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Black
                        ),
                        title = { Text("Person Light", color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    MapScreen(houseViewModel)
                }
            }
        }
    )
}

@Composable
fun MapScreen(viewModel: HouseViewModel) {
    val saoPaulo = LatLng(-23.550520, -46.633308)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(saoPaulo, 10f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        viewModel.houses.forEach { position ->
            Marker(
                state = MarkerState(position.location!!),
                title = "Marker at ${position.peopleCount}",
                snippet = "This is a snippet"
            )
        }
    }

}

@Composable
fun ButtonOption(text: String, context: Context,viewModel: HouseViewModel){
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog){
        RegisterHouseDialog(
            onDismiss = { showDialog = false },
            onSubmit = { houseData ->
                viewModel.houses.add(houseData)
                println("House data submitted: $houseData")
            },
            context = context )
    }

    Button(
        onClick = {showDialog = true},
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Black,
            contentColor = Color.White
        ),
        ) {
        Text(text = text)
    }
}


//-------------------

@Composable
fun RegisterHouseDialog(onDismiss: () -> Unit, onSubmit: (HouseData) -> Unit, context: Context) {
    var openDialog by remember { mutableStateOf(true) }
    var peopleCount by remember { mutableStateOf("") }
    var roomCount by remember { mutableStateOf("") }
    var location by remember { mutableStateOf<LatLng?>(null) }
    val photoUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLocationPermissionGranted by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        isLocationPermissionGranted = isGranted
        if (isGranted) {
            getCurrentLocation({ latLng -> location = latLng }, context)
        }
    }

    if (openDialog) {
        AlertDialog(
            onDismissRequest = { openDialog = false },
            title = { Text(text = "Register House") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = peopleCount,
                        onValueChange = { peopleCount = it },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        label = { Text("Number of People") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = roomCount,
                        onValueChange = { roomCount = it },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        label = { Text("Number of Rooms") }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Photos: ${photoUris.size} selected")

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                "android.permission.ACCESS_FINE_LOCATION"
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            getCurrentLocation({ latLng -> location = latLng }, context)
                        } else {
                            locationPermissionLauncher.launch("android.permission.ACCESS_FINE_LOCATION")
                        }
                    },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White
                        )
                    )
                    {
                        Text("Get Current Location")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    location?.let {
                        Text(text = "Location: ${it.latitude}, ${it.longitude}")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                            if (peopleCount.isNotEmpty() && roomCount.isNotEmpty()) {
                                val houseData = HouseData(
                                    peopleCount = peopleCount.toInt(),
                                    roomCount = roomCount.toInt(),
                                    location = location,
                                    photos = photoUris
                                )
                                onSubmit(houseData)
                                openDialog = false
                            }
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                Button(
                    onClick = { openDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@SuppressLint("MissingPermission")
private fun getCurrentLocation(onLocationRetrieved: (LatLng) -> Unit, context: Context) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    if (ActivityCompat.checkSelfPermission(
            context,
            "android.permission.ACCESS_FINE_LOCATION"
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        return
    }

    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        location?.let {
            onLocationRetrieved(LatLng(it.latitude, it.longitude))
        }
    }
}

data class HouseData(
    val peopleCount: Int,
    val roomCount: Int,
    val location: LatLng?,
    val photos: List<String>
)

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MyApp {
//        MapWithDrawerScreen()
    }
}