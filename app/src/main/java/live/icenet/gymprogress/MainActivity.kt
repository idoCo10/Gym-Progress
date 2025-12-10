package live.icenet.gymprogress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import live.icenet.gymprogress.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// temp!


// MASTER!

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                NavApp()
            }
        }
    }
}




@Composable
fun NavApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "main_menu") {
        composable("main_menu") {
            MainMenuScreen(
                onAddMachine = { navController.navigate("add_machine") },
                onStartSession = { navController.navigate("start_session") },
                onViewSessions = { navController.navigate("view_sessions") }
            )
        }

        composable("add_machine") { AddMachineScreen(onBack = { navController.popBackStack() }) }
        composable("start_session") { StartSessionScreen(onBack = { navController.popBackStack() }) }
        composable("view_sessions") { ViewSessionsScreen(onBack = { navController.popBackStack() }) }
    }
}

@Composable
fun MainMenuScreen(
    onAddMachine: () -> Unit,
    onStartSession: () -> Unit,
    onViewSessions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = onAddMachine) { Text("Add Machine") }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onStartSession) { Text("Add New Session") }
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onViewSessions) { Text("View Sessions") }
    }
}

@Composable
fun AddMachineScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()

    var machineName by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var machineList by remember { mutableStateOf(listOf<Machine>()) }

    LaunchedEffect(Unit) {
        scope.launch { machineList = db.machineDao().getAll() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add Machine", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = machineName,
            onValueChange = { machineName = it },
            label = { Text("Machine Name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))
        Button(onClick = {
            if (machineName.isNotBlank()) {
                scope.launch {
                    db.machineDao().insert(Machine(name = machineName.trim()))
                    machineName = ""
                    message = "Machine saved!"
                    machineList = db.machineDao().getAll()
                }
            } else message = "Please enter a name"
        }) { Text("Save Machine") }

        Spacer(Modifier.height(10.dp))
        if (message.isNotEmpty()) Text(message)

        Spacer(Modifier.height(20.dp))
        Text("Existing Machines:", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(machineList) { machine -> Text("- ${machine.name}") }
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
fun StartSessionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()

    var sessionName by remember { mutableStateOf("") }
    var sessionMessage by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf<Int?>(null) }

    var machineList by remember { mutableStateOf(listOf<Machine>()) }
    var selectedMachine by remember { mutableStateOf<Machine?>(null) }
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") } // Reps first
    var sets by remember { mutableStateOf("") } // Then sets
    var exerciseMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch { machineList = db.machineDao().getAll() }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Add New Session", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))

        if (sessionId == null) {
            OutlinedTextField(
                value = sessionName,
                onValueChange = { sessionName = it },
                label = { Text("Session Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                if (sessionName.isNotBlank()) {
                    scope.launch {
                        val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yy"))
                        val fullName = "$sessionName ($dateStr)"
                        val id = db.sessionDao().insert(Session(name = fullName)).toInt()
                        sessionId = id
                        sessionMessage = "Session '$fullName' created!"
                    }
                } else sessionMessage = "Enter a session name"
            }) { Text("Create Session") }

            Spacer(Modifier.height(10.dp))
            if (sessionMessage.isNotEmpty()) Text(sessionMessage)
        } else {
            Text("Adding exercises to session", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            Text("Select Machine:")
            LazyColumn(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                items(machineList) { machine ->
                    Button(
                        onClick = {
                            selectedMachine = machine
                            weight = ""
                            reps = ""
                            sets = ""
                            exerciseMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) { Text(machine.name) }
                }
            }

            Spacer(Modifier.height(20.dp))
            if (selectedMachine != null) {
                Text("Selected: ${selectedMachine!!.name}")
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Sets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Button(onClick = {
                    if (weight.isNotBlank() && reps.isNotBlank() && sets.isNotBlank() && sessionId != null && selectedMachine != null) {
                        scope.launch {
                            db.exerciseDao().insert(
                                Exercise(
                                    sessionId = sessionId!!,
                                    machineName = selectedMachine!!.name,
                                    weight = weight.toInt(),
                                    reps = reps.toInt(),
                                    sets = sets.toInt()
                                )
                            )
                            exerciseMessage = "Saved ${selectedMachine!!.name} - $weight kg | $reps Reps | $sets Sets"
                            weight = ""
                            reps = ""
                            sets = ""
                        }
                    } else exerciseMessage = "Fill all fields"
                }) { Text("Add Exercise") }

                Spacer(Modifier.height(10.dp))
                if (exerciseMessage.isNotEmpty()) Text(exerciseMessage)
            }
        }

        Spacer(Modifier.height(30.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}

@Composable
fun ViewSessionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()

    var sessions by remember { mutableStateOf(listOf<Session>()) }
    var exercisesMap by remember { mutableStateOf(mapOf<Int, List<Exercise>>()) }

    LaunchedEffect(Unit) {
        scope.launch {
            val allSessions = db.sessionDao().getAll()
            val map = mutableMapOf<Int, List<Exercise>>()
            for (s in allSessions) {
                map[s.id] = db.exerciseDao().getBySession(s.id)
            }
            sessions = allSessions
            exercisesMap = map
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("All Sessions", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sessions) { session ->
                Text("${session.name}:", style = MaterialTheme.typography.titleMedium)
                exercisesMap[session.id]?.forEach { ex ->
                    Text(
                        "- ${ex.machineName}: Weight: ${ex.weight} kg | ${ex.reps} Reps | ${ex.sets} Sets",
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                Spacer(Modifier.height(15.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    MainMenuScreen(onAddMachine = {}, onStartSession = {}, onViewSessions = {})
}
