package live.icenet.gymprogress

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import live.icenet.gymprogress.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Master! 11/12/25 02:58

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

        composable("view_sessions") {
            ViewSessionsScreen(
                onBack = { navController.popBackStack() },
                onEditSession = { sessionId ->
                    navController.navigate("edit_session/$sessionId")
                }
            )
        }

        composable(
            "edit_session/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.IntType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getInt("sessionId")!!
            EditSessionScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }
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

    var sessionExercises by remember { mutableStateOf(listOf<Exercise>()) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }

    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }
    var exerciseMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch { machineList = db.machineDao().getAll() }
    }

    // Load existing exercises if session already exists
    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            scope.launch {
                sessionExercises = db.exerciseDao().getBySession(sessionId!!)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            if (sessionId == null) "Create Session" else "Edit Session",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(20.dp))

        if (sessionId == null) {
            // Session creation UI
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
        }

        // Add new exercise section
        if (sessionId != null) {
            Spacer(Modifier.height(20.dp))
            Text("Add / Edit Exercise", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))

            Text("Select Machine:")
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
            ) {
                items(machineList) { machine ->
                    val existingExercise = sessionExercises.find { it.machineName == machine.name }
                    Button(
                        onClick = {
                            selectedMachine = machine
                            if (existingExercise != null) {
                                selectedExercise = existingExercise
                                weight = existingExercise.weight.toString()
                                reps = existingExercise.reps.toString()
                                sets = existingExercise.sets.toString()
                                exerciseMessage = ""
                            } else {
                                selectedExercise = null
                                weight = ""
                                reps = ""
                                sets = ""
                                exerciseMessage = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(machine.name + if (existingExercise != null) " (Edit)" else "")
                    }
                }
            }

            if (selectedMachine != null) {
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
                            val exToSave = selectedExercise?.copy(
                                sessionId = sessionId!!,
                                machineName = selectedMachine!!.name,
                                weight = weight.toIntOrNull() ?: 0,
                                reps = reps.toIntOrNull() ?: 0,
                                sets = sets.toIntOrNull() ?: 0
                            ) ?: Exercise(
                                sessionId = sessionId!!,
                                machineName = selectedMachine!!.name,
                                weight = weight.toIntOrNull() ?: 0,
                                reps = reps.toIntOrNull() ?: 0,
                                sets = sets.toIntOrNull() ?: 0
                            )
                            if (exToSave.id == 0) db.exerciseDao().insert(exToSave)
                            else db.exerciseDao().update(exToSave)

                            sessionExercises = db.exerciseDao().getBySession(sessionId!!)
                            selectedExercise = null
                            selectedMachine = null
                            weight = ""
                            reps = ""
                            sets = ""
                            exerciseMessage = "Saved!"
                        }
                    } else exerciseMessage = "Fill all fields"
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Save Exercise")
                }

                if (exerciseMessage.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Text(exerciseMessage)
                }
            }

            // Show existing exercises with edit/delete
            if (sessionExercises.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("Existing Exercises", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))

                sessionExercises.forEach { ex ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text(
                            "- ${ex.machineName}: ${ex.weight} kg | ${ex.reps} Reps | ${ex.sets} Sets",
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            selectedMachine = machineList.find { it.name == ex.machineName }
                            selectedExercise = ex
                            weight = ex.weight.toString()
                            reps = ex.reps.toString()
                            sets = ex.sets.toString()
                            exerciseMessage = ""
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Exercise")
                        }
                        IconButton(onClick = {
                            scope.launch {
                                db.exerciseDao().delete(ex)
                                sessionExercises = db.exerciseDao().getBySession(sessionId!!)
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Exercise")
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) { Text("Back") }
        }
    }
}




@Composable
fun ViewSessionsScreen(onBack: () -> Unit, onEditSession: (Int) -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()

    var sessions by remember { mutableStateOf(listOf<Session>()) }
    var exercisesMap by remember { mutableStateOf(mapOf<Int, List<Exercise>>()) }

    // Load all sessions and exercises
    fun loadSessions() {
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

    LaunchedEffect(Unit) { loadSessions() }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("All Sessions", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sessions) { session ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${session.name}:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { onEditSession(session.id) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Session")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            db.sessionDao().delete(session) // delete session
                            loadSessions() // reload sessions
                        }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Session")
                    }
                }

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



@Composable
fun EditSessionScreen(sessionId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()

    var machineList by remember { mutableStateOf(listOf<Machine>()) }
    var exercises by remember { mutableStateOf(listOf<Exercise>()) }

    var selectedMachine by remember { mutableStateOf<Machine?>(null) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(sessionId) {
        scope.launch {
            machineList = db.machineDao().getAll()
            exercises = db.exerciseDao().getBySession(sessionId)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Text("Editing Session", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(20.dp))

            // Existing Exercises with Edit/Delete icons
            Text("Existing Exercises", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
        }

        items(exercises) { ex ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    "- ${ex.machineName}: ${ex.weight} kg | ${ex.reps} reps | ${ex.sets} sets",
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {
                    // Edit existing exercise
                    selectedMachine = machineList.find { it.name == ex.machineName }
                    selectedExercise = ex
                    weight = ex.weight.toString()
                    reps = ex.reps.toString()
                    sets = ex.sets.toString()
                    message = ""
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Exercise")
                }
                IconButton(onClick = {
                    scope.launch {
                        db.exerciseDao().delete(ex)
                        exercises = db.exerciseDao().getBySession(sessionId)
                        if (selectedExercise?.id == ex.id) {
                            selectedExercise = null
                            selectedMachine = null
                            weight = ""
                            reps = ""
                            sets = ""
                        }
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Exercise")
                }
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
            Text(
                if (selectedExercise != null) "Edit Exercise" else "Add New Exercise",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(10.dp))
        }

        // Machines that don't have an exercise yet
        val availableMachines = machineList.filter { machine ->
            exercises.none { it.machineName == machine.name }
        }

        items(availableMachines) { machine ->
            Button(
                onClick = {
                    selectedMachine = machine
                    weight = ""
                    reps = ""
                    sets = ""
                    selectedExercise = null
                    message = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Text(machine.name)
            }
        }

        item {
            Spacer(Modifier.height(10.dp))

            // Show input fields when a machine is selected
            if (selectedMachine != null) {
                Text(
                    if (selectedExercise != null) "Editing: ${selectedMachine!!.name}"
                    else "Adding: ${selectedMachine!!.name}"
                )
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reps,
                    onValueChange = { reps = it },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Sets") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (weight.isNotBlank() && reps.isNotBlank() && sets.isNotBlank()) {
                            scope.launch {
                                if (selectedExercise != null) {
                                    // Save edited exercise
                                    val updated = selectedExercise!!.copy(
                                        weight = weight.toIntOrNull() ?: 0,
                                        reps = reps.toIntOrNull() ?: 0,
                                        sets = sets.toIntOrNull() ?: 0
                                    )
                                    db.exerciseDao().update(updated)
                                    selectedExercise = null
                                } else {
                                    // Add new exercise
                                    db.exerciseDao().insert(
                                        Exercise(
                                            sessionId = sessionId,
                                            machineName = selectedMachine!!.name,
                                            weight = weight.toIntOrNull() ?: 0,
                                            reps = reps.toIntOrNull() ?: 0,
                                            sets = sets.toIntOrNull() ?: 0
                                        )
                                    )
                                }
                                exercises = db.exerciseDao().getBySession(sessionId)
                                selectedMachine = null
                                weight = ""
                                reps = ""
                                sets = ""
                                message = "Saved!"
                            }
                        } else {
                            message = "Fill all fields"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selectedExercise != null) "Save Exercise" else "Add Exercise")
                }

                if (message.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Text(message)
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
            ) {
                Text("Back")
            }
        }
    }
}





@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    MainMenuScreen(onAddMachine = {}, onStartSession = {}, onViewSessions = {})
}
