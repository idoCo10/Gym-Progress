package live.icenet.gymprogress

// T 12-12-25 01:42

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
        composable("start_session") { NewSessionScreen(onBack = { navController.popBackStack() }) }

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
            EditSessionScreen(sessionId = sessionId, onBack = { navController.popBackStack() })
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
    var selectedMachine by remember { mutableStateOf<Machine?>(null) }

    LaunchedEffect(Unit) { scope.launch { machineList = db.machineDao().getAll() } }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Top) {
            item {
                Text("Add / Edit Machine", style = MaterialTheme.typography.titleLarge)
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

                Button(
                    onClick = {
                        if (machineName.isNotBlank()) {
                            scope.launch {
                                if (selectedMachine != null) {
                                    val oldName = selectedMachine!!.name
                                    val updatedMachine = selectedMachine!!.copy(name = machineName.trim())
                                    db.machineDao().update(updatedMachine)

                                    val allSessions = db.sessionDao().getAll()
                                    for (session in allSessions) {
                                        val exercises = db.exerciseDao().getBySession(session.id)
                                        exercises.filter { it.machineName == oldName }.forEach { ex ->
                                            db.exerciseDao().update(ex.copy(machineName = updatedMachine.name))
                                        }
                                    }

                                    message = "Machine updated!"
                                    selectedMachine = null
                                } else {
                                    db.machineDao().insert(Machine(name = machineName.trim()))
                                    message = "Machine saved!"
                                }

                                machineName = ""
                                machineList = db.machineDao().getAll()
                            }
                        } else message = "Please enter a name"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selectedMachine != null) "Update Machine" else "Save Machine")
                }

                Spacer(Modifier.height(10.dp))
                if (message.isNotEmpty()) Text(message)

                Spacer(Modifier.height(20.dp))
                Text("Existing Machines:", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
            }

            items(machineList) { machine ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    Text("- ${machine.name}", modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        selectedMachine = machine
                        machineName = machine.name
                        message = ""
                    }) { Icon(Icons.Default.Edit, contentDescription = "Edit Machine") }
                    IconButton(onClick = {
                        scope.launch {
                            db.machineDao().delete(machine)
                            val allSessions = db.sessionDao().getAll()
                            for (session in allSessions) {
                                val exercises = db.exerciseDao().getBySession(session.id)
                                exercises.filter { it.machineName == machine.name }.forEach {
                                    db.exerciseDao().delete(it)
                                }
                            }

                            machineList = db.machineDao().getAll()
                            if (selectedMachine?.id == machine.id) {
                                selectedMachine = null
                                machineName = ""
                            }
                        }
                    }) { Icon(Icons.Default.Delete, contentDescription = "Delete Machine") }
                }
            }
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().navigationBarsPadding()) { Text("Back") }
    }
}

@Composable
fun ExerciseEditorCard(
    machine: Machine,
    selectedExercise: Exercise?,
    weight: String,
    reps: String,
    sets: String,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onSetsChange: (String) -> Unit,
    onSave: () -> Unit,
    saveButtonText: String,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                if (selectedExercise != null) "Editing: ${machine.name}" else "Adding: ${machine.name}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = reps,
                onValueChange = onRepsChange,
                label = { Text("Reps") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = sets,
                onValueChange = onSetsChange,
                label = { Text("Sets") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) { Text(saveButtonText) }

            if (message.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                Text(message)
            }
        }
    }
}

@Composable
fun SelectedExercisesList(
    sessionExercises: List<Exercise>,
    onEdit: (Exercise) -> Unit,
    onDelete: (Exercise) -> Unit
) {
    if (sessionExercises.isNotEmpty()) {
        Text("Selected Machines", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(5.dp))
        Column {
            sessionExercises.forEach { ex ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ex.machineName, style = MaterialTheme.typography.titleMedium)
                            Text("Weight: ${ex.weight} kg | Reps: ${ex.reps} | Sets: ${ex.sets}")
                        }
                        Row {
                            IconButton(onClick = { onEdit(ex) }) { Icon(Icons.Default.Edit, contentDescription = "Edit Machine") }
                            IconButton(onClick = { onDelete(ex) }) { Icon(Icons.Default.Delete, contentDescription = "Delete Machine") }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun MachinePickerList(
    machineList: List<Machine>,
    sessionExercises: List<Exercise>,
    onSelectMachine: (Machine) -> Unit
) {
    Text("Add Machine:", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(5.dp))
    Column {
        machineList.filter { machine -> sessionExercises.none { it.machineName == machine.name } }
            .forEach { machine ->
                Button(
                    onClick = { onSelectMachine(machine) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) { Text(machine.name) }
            }
    }
}

@Composable
fun NewSessionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()

    var sessionName by remember { mutableStateOf("") }
    var sessionMessage by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf<Int?>(null) }

    var machineList by remember { mutableStateOf(listOf<Machine>()) }
    var sessionExercises by remember { mutableStateOf(listOf<Exercise>()) }

    var selectedMachine by remember { mutableStateOf<Machine?>(null) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }
    var exerciseMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { scope.launch { machineList = db.machineDao().getAll() } }
    LaunchedEffect(sessionId) { if (sessionId != null) scope.launch { sessionExercises = db.exerciseDao().getBySession(sessionId!!) } }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Create Session", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Top,
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (sessionId == null) {
                item {
                    OutlinedTextField(
                        value = sessionName,
                        onValueChange = { sessionName = it },
                        label = { Text("Session Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (sessionName.isNotBlank()) {
                                scope.launch {
                                    val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yy"))
                                    val fullName = "$sessionName ($dateStr)"
                                    sessionId = db.sessionDao().insert(Session(name = fullName)).toInt()
                                    sessionMessage = "Session '$fullName' created!"
                                }
                            } else sessionMessage = "Enter a session name"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Create Session") }
                    Spacer(Modifier.height(10.dp))
                    if (sessionMessage.isNotEmpty()) Text(sessionMessage)
                    Spacer(Modifier.height(20.dp))
                }
            }

            if (sessionId != null) {
                selectedMachine?.let { machine ->
                    item {
                        ExerciseEditorCard(
                            machine = machine,
                            selectedExercise = selectedExercise,
                            weight = weight,
                            reps = reps,
                            sets = sets,
                            onWeightChange = { weight = it },
                            onRepsChange = { reps = it },
                            onSetsChange = { sets = it },
                            saveButtonText = if (selectedExercise != null) "Save Exercise" else "Add Exercise",
                            message = exerciseMessage,
                            onSave = {
                                if (weight.isNotBlank() && reps.isNotBlank() && sets.isNotBlank()) {
                                    scope.launch {
                                        val exToSave = selectedExercise?.copy(
                                            sessionId = sessionId!!,
                                            machineName = machine.name,
                                            weight = weight.toIntOrNull() ?: 0,
                                            reps = reps.toIntOrNull() ?: 0,
                                            sets = sets.toIntOrNull() ?: 0
                                        ) ?: Exercise(
                                            sessionId = sessionId!!,
                                            machineName = machine.name,
                                            weight = weight.toIntOrNull() ?: 0,
                                            reps = reps.toIntOrNull() ?: 0,
                                            sets = sets.toIntOrNull() ?: 0
                                        )
                                        if (selectedExercise != null) db.exerciseDao().update(exToSave) else db.exerciseDao().insert(exToSave)

                                        sessionExercises = db.exerciseDao().getBySession(sessionId!!)
                                        selectedMachine = null
                                        selectedExercise = null
                                        weight = ""
                                        reps = ""
                                        sets = ""
                                        exerciseMessage = "Saved!"
                                    }
                                } else exerciseMessage = "Fill all fields"
                            }
                        )
                        Spacer(Modifier.height(20.dp))
                    }
                }

                item {
                    SelectedExercisesList(
                        sessionExercises = sessionExercises,
                        onEdit = { ex ->
                            selectedMachine = machineList.find { it.name == ex.machineName }
                            selectedExercise = ex
                            weight = ex.weight.toString()
                            reps = ex.reps.toString()
                            sets = ex.sets.toString()
                            exerciseMessage = ""
                        },
                        onDelete = { ex ->
                            scope.launch {
                                db.exerciseDao().delete(ex)
                                sessionExercises = db.exerciseDao().getBySession(sessionId!!)
                                if (selectedExercise?.id == ex.id) {
                                    selectedMachine = null
                                    selectedExercise = null
                                    weight = ""
                                    reps = ""
                                    sets = ""
                                }
                            }
                        }
                    )
                }

                item {
                    MachinePickerList(
                        machineList = machineList,
                        sessionExercises = sessionExercises,
                        onSelectMachine = { machine ->
                            selectedMachine = machine
                            selectedExercise = null
                            weight = ""
                            reps = ""
                            sets = ""
                            exerciseMessage = ""
                        }
                    )
                }
            }
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().navigationBarsPadding()) { Text("Back") }
    }
}

@Composable
fun ViewSessionsScreen(onBack: () -> Unit, onEditSession: (Int) -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()

    var sessions by remember { mutableStateOf(listOf<Session>()) }
    var exercisesMap by remember { mutableStateOf(mapOf<Int, List<Exercise>>()) }

    fun loadSessions() {
        scope.launch {
            val allSessions = db.sessionDao().getAll()
            val map = mutableMapOf<Int, List<Exercise>>()
            for (s in allSessions) map[s.id] = db.exerciseDao().getBySession(s.id)
            sessions = allSessions
            exercisesMap = map
        }
    }

    LaunchedEffect(Unit) { loadSessions() }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {

        Text("Sessions history", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Top) {
            items(sessions) { session ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("${session.name}:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onEditSession(session.id) }) { Icon(Icons.Default.Edit, contentDescription = "Edit Session") }
                    IconButton(onClick = { scope.launch { db.sessionDao().delete(session); loadSessions() } }) { Icon(Icons.Default.Delete, contentDescription = "Delete Session") }
                }
                exercisesMap[session.id]?.forEach { ex ->
                    Text("- ${ex.machineName}: Weight: ${ex.weight} kg | ${ex.reps} Reps | ${ex.sets} Sets", modifier = Modifier.padding(start = 10.dp))
                }
                Spacer(Modifier.height(15.dp))
            }
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().navigationBarsPadding()) { Text("Back") }
    }
}

@Composable
fun EditSessionScreen(sessionId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()

    var machineList by remember { mutableStateOf(listOf<Machine>()) }
    var sessionExercises by remember { mutableStateOf(listOf<Exercise>()) }

    var selectedMachine by remember { mutableStateOf<Machine?>(null) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }
    var exerciseMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scope.launch {
            machineList = db.machineDao().getAll()
            sessionExercises = db.exerciseDao().getBySession(sessionId)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text("Edit Session", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(20.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Top, contentPadding = PaddingValues(bottom = 80.dp)) {
            selectedMachine?.let { machine ->
                item {
                    ExerciseEditorCard(
                        machine = machine,
                        selectedExercise = selectedExercise,
                        weight = weight,
                        reps = reps,
                        sets = sets,
                        onWeightChange = { weight = it },
                        onRepsChange = { reps = it },
                        onSetsChange = { sets = it },
                        saveButtonText = if (selectedExercise != null) "Save Exercise" else "Add Exercise",
                        message = exerciseMessage,
                        onSave = {
                            if (weight.isNotBlank() && reps.isNotBlank() && sets.isNotBlank()) {
                                scope.launch {
                                    val exToSave = selectedExercise?.copy(
                                        weight = weight.toIntOrNull() ?: 0,
                                        reps = reps.toIntOrNull() ?: 0,
                                        sets = sets.toIntOrNull() ?: 0
                                    ) ?: Exercise(
                                        sessionId = sessionId,
                                        machineName = machine.name,
                                        weight = weight.toIntOrNull() ?: 0,
                                        reps = reps.toIntOrNull() ?: 0,
                                        sets = sets.toIntOrNull() ?: 0
                                    )
                                    if (selectedExercise != null) db.exerciseDao().update(exToSave) else db.exerciseDao().insert(exToSave)

                                    sessionExercises = db.exerciseDao().getBySession(sessionId)
                                    selectedMachine = null
                                    selectedExercise = null
                                    weight = ""
                                    reps = ""
                                    sets = ""
                                    exerciseMessage = "Saved!"
                                }
                            } else exerciseMessage = "Fill all fields"
                        }
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            item {
                SelectedExercisesList(
                    sessionExercises = sessionExercises,
                    onEdit = { ex ->
                        selectedMachine = machineList.find { it.name == ex.machineName }
                        selectedExercise = ex
                        weight = ex.weight.toString()
                        reps = ex.reps.toString()
                        sets = ex.sets.toString()
                        exerciseMessage = ""
                    },
                    onDelete = { ex ->
                        scope.launch {
                            db.exerciseDao().delete(ex)
                            sessionExercises = db.exerciseDao().getBySession(sessionId)
                            if (selectedExercise?.id == ex.id) {
                                selectedMachine = null
                                selectedExercise = null
                                weight = ""
                                reps = ""
                                sets = ""
                            }
                        }
                    }
                )
            }

            item {
                MachinePickerList(
                    machineList = machineList,
                    sessionExercises = sessionExercises,
                    onSelectMachine = { machine ->
                        selectedMachine = machine
                        selectedExercise = null
                        weight = ""
                        reps = ""
                        sets = ""
                        exerciseMessage = ""
                    }
                )
            }
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().navigationBarsPadding()) { Text("Back") }
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    MainMenuScreen(onAddMachine = {}, onStartSession = {}, onViewSessions = {})
}
