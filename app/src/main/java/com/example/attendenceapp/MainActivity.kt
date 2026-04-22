package com.example.attendenceapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

data class Subject(
    val name: String,
    val attended: Float,
    val total: Float
)

@SuppressLint("UseKtx")
fun saveSubjects(context: Context, subjects: List<Subject>) {
    val prefs = context.getSharedPreferences("attendance", Context.MODE_PRIVATE)
    val data = subjects.joinToString("|") {
        "${it.name},${it.attended},${it.total}"
    }
    prefs.edit().putString("data", data).apply()
}

fun loadSubjects(context: Context): MutableList<Subject> {
    val prefs = context.getSharedPreferences("attendance", Context.MODE_PRIVATE)
    val data = prefs.getString("data", "") ?: ""
    if (data.isEmpty()) return mutableListOf()

    return data.split("|").map {
        val parts = it.split(",")
        Subject(parts[0], parts[1].toFloat(), parts[2].toFloat())
    }.toMutableList()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AttendanceApp()
        }
    }
}

@Composable
fun AttendanceApp() {
    val navController = rememberNavController()

    NavHost(navController, startDestination = "input") {
        composable("input") { InputScreen(navController) }
        composable("result/{percent}") { backStack ->
            val percent = backStack.arguments?.getString("percent")?.toFloat() ?: 0f
            ResultScreen(percent, navController)
        }
    }
}

@Composable
fun InputScreen(navController: NavHostController) {

    val context = LocalContext.current
    val subjects = remember { mutableStateListOf<Subject>().apply {
        addAll(loadSubjects(context))
    }}

    var name by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var attended by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text("Smart Attendance Buddy", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        TextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        TextField(branch, { branch = it }, label = { Text("Branch") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        TextField(subject, { subject = it }, label = { Text("Subject") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))

        TextField(
            attended,
            { attended = it },
            label = { Text("Classes Attended") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        TextField(
            total,
            { total = it },
            label = { Text("Total Classes") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        if (error.isNotEmpty()) {
            Text(error, color = Color.Red)
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

            Button(onClick = {
                val attendedNum = attended.toFloatOrNull()
                val totalNum = total.toFloatOrNull()

                if (subject.isBlank() || attendedNum == null || totalNum == null || totalNum == 0f) {
                    error = "Enter valid subject data"
                    return@Button
                }

                subjects.add(Subject(subject, attendedNum, totalNum))
                saveSubjects(context, subjects)

                subject = ""
                attended = ""
                total = ""
                error = ""
            }) {
                Text("Add Subject")
            }

            Button(onClick = {

                if (name.isBlank() || branch.isBlank()) {
                    error = "Please enter Name and Branch"
                    return@Button
                }

                val totalAttended = subjects.sumOf { it.attended.toDouble() }
                val totalClasses = subjects.sumOf { it.total.toDouble() }

                if (totalClasses == 0.0) {
                    error = "Add subjects first"
                    return@Button
                }

                val percent = (totalAttended / totalClasses) * 100
                error = ""
                navController.navigate("result/$percent")
            }) {
                Text("Calculate")
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Subjects:", fontWeight = Bold)

        LazyColumn {
            items(subjects) {
                val percent = (it.attended / it.total) * 100
                val color = when {
                    percent >= 75 -> Color(0xFF4CAF50)
                    percent >= 60 -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }

                Text(
                    "${it.name} → %.1f%%".format(percent),
                    color = color,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun ResultScreen(percent: Float, navController: NavHostController) {

    val (message, color) = when {
        percent >= 75 -> "Safe Zone ✅" to Color(0xFF4CAF50)
        percent >= 60 -> "Warning ⚠️" to Color(0xFFFFC107)
        else -> "Danger ❌" to Color(0xFFF44336)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color.copy(alpha = 0.1f)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Overall Attendance: %.2f%%".format(percent),
            style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        Text(message, color = color, fontWeight = Bold)

        Spacer(Modifier.height(24.dp))

        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }
}