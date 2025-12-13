package live.icenet.gymprogress

// T 13-12-25 15:46

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
        modifier = modifier.fillMaxSize().statusBarsPadding().padding(20.dp),
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

    val machineNameFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var machineName by remember { mutableStateOf(TextFieldValue("")) }
    var message by remember { mutableStateOf("") }
    var machineList by remember { mutableStateOf(listOf<Machine>()) }
    var selectedMachine by remember { mutableStateOf<Machine?>(null) }

    LaunchedEffect(message) {
        if (message.isNotEmpty()) {
            kotlinx.coroutines.delay(2000)
            message = ""
        }
    }

    LaunchedEffect(Unit) {
        machineList = db.machineDao().getAll()
    }

    Column(modifier = Modifier.fillMaxSize().padding(25.dp)) {

        // 🔒 FIXED HEADER
        Text("Add Machine", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = machineName,
            onValueChange = { machineName = it },
            label = { Text("Machine Name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(machineNameFocusRequester)
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                val name = machineName.text.trim()
                if (name.isNotBlank()) {
                    scope.launch {
                        if (selectedMachine != null) {
                            val oldName = selectedMachine!!.name
                            val updatedMachine = selectedMachine!!.copy(name = name)

                            // 🔁 Update machine
                            db.machineDao().update(updatedMachine)

                            // 🔁 Update machine name in ALL history
                            val allSessions = db.sessionDao().getAll()
                            for (session in allSessions) {
                                val exercises =
                                    db.exerciseDao().getBySession(session.id)
                                exercises
                                    .filter { it.machineName == oldName }
                                    .forEach {
                                        db.exerciseDao()
                                            .update(it.copy(machineName = name))
                                    }
                            }

                            message = "Machine updated!"
                            selectedMachine = null
                        } else {
                            db.machineDao().insert(Machine(name = name))
                            message = "Machine saved!"
                        }

                        machineName = TextFieldValue("")
                        machineList = db.machineDao().getAll()

                        keyboardController?.hide()
                        focusManager.clearFocus()
                    }
                } else {
                    message = "Please enter a name"
                }
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

        // 🟢 SCROLLABLE LIST
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(machineList) { machine ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = machine.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )

                        // ✏️ EDIT
                        IconButton(
                            onClick = {
                                val text = machine.name
                                machineName = TextFieldValue(
                                    text = text,
                                    selection = TextRange(text.length)
                                )
                                selectedMachine = machine
                                message = ""

                                scope.launch {
                                    machineNameFocusRequester.requestFocus()
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color(0xFF1976D2)
                            )
                        }

                        // 🗑️ DELETE (CASCADE DELETE HISTORY)
                        IconButton(
                            onClick = {
                                scope.launch {
                                    // 🔥 DELETE ALL EXERCISES USING THIS MACHINE
                                    val allSessions = db.sessionDao().getAll()
                                    for (session in allSessions) {
                                        val exercises =
                                            db.exerciseDao().getBySession(session.id)
                                        exercises
                                            .filter { it.machineName == machine.name }
                                            .forEach {
                                                db.exerciseDao().delete(it)
                                            }
                                    }

                                    // 🔥 DELETE MACHINE
                                    db.machineDao().delete(machine)

                                    // 🧹 RESET UI IF EDITING THIS MACHINE
                                    if (selectedMachine?.id == machine.id) {
                                        selectedMachine = null
                                        machineName = TextFieldValue("")
                                    }

                                    machineList = db.machineDao().getAll()
                                    message = "Machine deleted!"
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
        ) {
            Text("Back")
        }
    }
}

@Composable
fun ExerciseEditorCard(
    machine: Machine,
    selectedExercise: Exercise?,
    weight: TextFieldValue,
    reps: String,
    sets: String,
    onWeightChange: (TextFieldValue) -> Unit,
    onRepsChange: (String) -> Unit,
    onSetsChange: (String) -> Unit,
    onSave: () -> Unit,
    saveButtonText: String,
    message: String,
    weightFocusRequester: FocusRequester
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                if (selectedExercise != null)
                    "Editing: ${machine.name}"
                else
                    "Adding: ${machine.name}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(weightFocusRequester)
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

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(saveButtonText)
            }

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
                            IconButton(onClick = { onEdit(ex) }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Machine",
                                    tint = Color(0xFF1976D2)  // 🔵 Blue
                                )
                            }

                            IconButton(onClick = { onDelete(ex) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Machine",
                                    tint = Color.Red         // 🔴 Red
                                )
                            }

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
        machineList
            .filter { machine ->
                sessionExercises.none { it.machineName == machine.name }
            }
            .forEach { machine ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {

                        Text(
                            text = machine.name,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )

                        // --- ADD ICON (single bold circle, no ripple) ---
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val isHovered by interactionSource.collectIsHoveredAsState()

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isPressed ->
                                            Color(0x332E7D32)
                                        isHovered ->
                                            Color(0x1A2E7D32)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null // ✅ allowed here
                                ) {
                                    onSelectMachine(machine)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Machine",
                                tint = Color(0xFF2E7D32), // 🟢 green
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
    }
}

@Composable
fun NewSessionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    val weightFocusRequester = remember { FocusRequester() }

    var sessionName by remember { mutableStateOf("") }
    var sessionMessage by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf<Int?>(null) }

    var machineList by remember { mutableStateOf(listOf<Machine>()) }
    var sessionExercises by remember { mutableStateOf(listOf<Exercise>()) }

    var selectedMachine by remember { mutableStateOf<Machine?>(null) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }

    var weight by remember { mutableStateOf(TextFieldValue("")) }
    var reps by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }
    var exerciseMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        machineList = db.machineDao().getAll()
    }

    LaunchedEffect(sessionId) {
        if (sessionId != null) {
            sessionExercises = db.exerciseDao().getBySession(sessionId!!)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(25.dp)) {

        Text("Create Session", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(5.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
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
                                    val dateStr = LocalDate.now()
                                        .format(DateTimeFormatter.ofPattern("dd/MM/yy"))
                                    val fullName = "$sessionName ($dateStr)"
                                    sessionId =
                                        db.sessionDao().insert(Session(name = fullName)).toInt()
                                    sessionMessage = "Session '$fullName' created!"
                                }
                            } else sessionMessage = "Enter a session name"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Create Session")
                    }

                    if (sessionMessage.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(sessionMessage)
                    }

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
                            saveButtonText =
                            if (selectedExercise != null) "Save Exercise" else "Add Exercise",
                            message = exerciseMessage,
                            weightFocusRequester = weightFocusRequester,
                            onSave = {
                                scope.launch {
                                    val weightValue = weight.text.toIntOrNull() ?: 0
                                    val repsValue = reps.toIntOrNull() ?: 0
                                    val setsValue = sets.toIntOrNull() ?: 0

                                    val ex =
                                        selectedExercise?.copy(
                                            weight = weightValue,
                                            reps = repsValue,
                                            sets = setsValue
                                        ) ?: Exercise(
                                            sessionId = sessionId!!,
                                            machineName = machine.name,
                                            weight = weightValue,
                                            reps = repsValue,
                                            sets = setsValue
                                        )

                                    if (selectedExercise != null)
                                        db.exerciseDao().update(ex)
                                    else
                                        db.exerciseDao().insert(ex)

                                    sessionExercises =
                                        db.exerciseDao().getBySession(sessionId!!)
                                    selectedMachine = null
                                    selectedExercise = null
                                    weight = TextFieldValue("")
                                    reps = ""
                                    sets = ""
                                    exerciseMessage = "Saved!"
                                }
                            }
                        )

                        Spacer(Modifier.height(20.dp))
                    }
                }

                item {
                    SelectedExercisesList(
                        sessionExercises = sessionExercises,
                        onEdit = { ex ->
                            val text = ex.weight.toString()
                            weight = TextFieldValue(
                                text = text,
                                selection = TextRange(text.length)
                            )
                            reps = ex.reps.toString()
                            sets = ex.sets.toString()
                            selectedMachine =
                                machineList.find { it.name == ex.machineName }
                            selectedExercise = ex
                            exerciseMessage = ""

                            scope.launch {
                                listState.animateScrollToItem(0)
                                weightFocusRequester.requestFocus()
                            }
                        },
                        onDelete = { ex ->
                            scope.launch {
                                db.exerciseDao().delete(ex)
                                sessionExercises =
                                    db.exerciseDao().getBySession(sessionId!!)
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
                            weight = TextFieldValue("")
                            reps = ""
                            sets = ""
                            exerciseMessage = ""

                            scope.launch {
                                listState.animateScrollToItem(0)
                                weightFocusRequester.requestFocus()
                            }
                        }
                    )
                }
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
        ) {
            Text("Back")
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

    Column(modifier = Modifier.fillMaxSize().padding(25.dp)) {

        Text("Sessions history", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(5.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Top
        ) {
            items(sessions) { session ->

                // ⭐ Card ⭐
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 15.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {

                        // Top Row: Title + Edit + Delete
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = session.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )

                            IconButton(onClick = { onEditSession(session.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF1976D2))
                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        db.sessionDao().delete(session)
                                        loadSessions()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Red
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Exercises inside card
                        exercisesMap[session.id]?.forEach { ex ->
                            Text(
                                "- ${ex.machineName}: ${ex.weight} kg | ${ex.reps} reps | ${ex.sets} sets",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
        ) {
            Text("Back")
        }
    }
}

@Composable
fun EditSessionScreen(sessionId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    val db = AppDatabase.getDatabase(context)
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    val weightFocusRequester = remember { FocusRequester() }

    var machineList by remember { mutableStateOf(listOf<Machine>()) }
    var sessionExercises by remember { mutableStateOf(listOf<Exercise>()) }

    var selectedMachine by remember { mutableStateOf<Machine?>(null) }
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }

    var weight by remember { mutableStateOf(TextFieldValue("")) }
    var reps by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("") }
    var exerciseMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        machineList = db.machineDao().getAll()
        sessionExercises = db.exerciseDao().getBySession(sessionId)
    }

    Column(modifier = Modifier.fillMaxSize().padding(25.dp)) {

        Text("Edit Session", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(5.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

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
                        saveButtonText =
                        if (selectedExercise != null) "Save Exercise" else "Add Exercise",
                        message = exerciseMessage,
                        weightFocusRequester = weightFocusRequester,
                        onSave = {
                            scope.launch {
                                val weightValue = weight.text.toIntOrNull() ?: 0
                                val repsValue = reps.toIntOrNull() ?: 0
                                val setsValue = sets.toIntOrNull() ?: 0

                                val ex =
                                    selectedExercise?.copy(
                                        weight = weightValue,
                                        reps = repsValue,
                                        sets = setsValue
                                    ) ?: Exercise(
                                        sessionId = sessionId,
                                        machineName = machine.name,
                                        weight = weightValue,
                                        reps = repsValue,
                                        sets = setsValue
                                    )

                                if (selectedExercise != null)
                                    db.exerciseDao().update(ex)
                                else
                                    db.exerciseDao().insert(ex)

                                sessionExercises =
                                    db.exerciseDao().getBySession(sessionId)
                                selectedMachine = null
                                selectedExercise = null
                                weight = TextFieldValue("")
                                reps = ""
                                sets = ""
                                exerciseMessage = "Saved!"
                            }
                        }
                    )

                    Spacer(Modifier.height(20.dp))
                }
            }

            item {
                SelectedExercisesList(
                    sessionExercises = sessionExercises,
                    onEdit = { ex ->
                        val text = ex.weight.toString()
                        weight = TextFieldValue(
                            text = text,
                            selection = TextRange(text.length)
                        )
                        reps = ex.reps.toString()
                        sets = ex.sets.toString()
                        selectedMachine =
                            machineList.find { it.name == ex.machineName }
                        selectedExercise = ex
                        exerciseMessage = ""

                        scope.launch {
                            listState.animateScrollToItem(0)
                            weightFocusRequester.requestFocus()
                        }
                    },
                    onDelete = { ex ->
                        scope.launch {
                            db.exerciseDao().delete(ex)
                            sessionExercises =
                                db.exerciseDao().getBySession(sessionId)
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
                        weight = TextFieldValue("")
                        reps = ""
                        sets = ""
                        exerciseMessage = ""

                        scope.launch {
                            listState.animateScrollToItem(0)
                            weightFocusRequester.requestFocus()
                        }
                    }
                )
            }
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
        ) {
            Text("Back")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainMenuPreview() {
    MainMenuScreen(onAddMachine = {}, onStartSession = {}, onViewSessions = {})
}
