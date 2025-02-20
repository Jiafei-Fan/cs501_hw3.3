package com.example.hw33

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.hw33.ui.theme.Hw33Theme
import kotlinx.coroutines.delay
import org.xmlpull.v1.XmlPullParser

// Data class that stores a word along with its appearance time
data class DisplayedWord(
    val text: String,
    val appearTime: Long
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Hw33Theme {
                TypingSpeedTestApp()
            }
        }
    }
}

// Parses the XML file (res/xml/typingwords.xml) and returns a list of words
fun parseTypingWordsXml(context: Context): List<String> {
    val words = mutableListOf<String>()
    try {
        val parser = context.resources.getXml(R.xml.typingwords)
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "word") {
                val wordText = parser.nextText()
                if (!wordText.isNullOrEmpty()) {
                    words.add(wordText)
                }
            }
            eventType = parser.next()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return words
}

@Composable
fun TypingSpeedTestApp() {
    val context = LocalContext.current
    val allWords = remember { parseTypingWordsXml(context) }

    // Current displayed list of words (each with its appearance time)
    var displayedWords by remember {
        mutableStateOf(
            allWords.shuffled().take(10).map {
                DisplayedWord(text = it, appearTime = System.currentTimeMillis())
            }
        )
    }

    // User's current input in the TextField
    var userInput by remember { mutableStateOf("") }

    // Count of words that have been correctly typed
    var typedCount by remember { mutableStateOf(0) }

    // Start time for typing, used to calculate WPM
    val startTime = remember { System.currentTimeMillis() }

    // State that holds the current time for dynamic WPM calculation; updated every second
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Coroutine that runs every second to update currentTime and check for words that have been displayed for more than 5 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // Check every second
            val now = System.currentTimeMillis()
            currentTime = now

            displayedWords = displayedWords.map { oldWord ->
                // If the word has been displayed for over 5 seconds, replace it with a new random word and update its appearance time
                if (now - oldWord.appearTime >= 5000) {
                    oldWord.copy(
                        text = allWords.random(),
                        appearTime = now
                    )
                } else {
                    oldWord
                }
            }
        }
    }

    // Calculate elapsed time and compute WPM (words per minute)
    val elapsedMillis = currentTime - startTime
    val elapsedMinutes = elapsedMillis / 1000f / 60f
    val wpm = if (elapsedMinutes > 0) typedCount / elapsedMinutes else 0f

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            // Display the dynamically updated WPM
            Text(text = "WPM: %.1f".format(wpm))
            // Display the current count of correctly typed words
            Text(text = "Typed Count: $typedCount")

            Spacer(modifier = Modifier.height(8.dp))

            // Display the list of words using LazyColumn
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                itemsIndexed(displayedWords) { index, displayedWord ->
                    Text(text = "${index + 1}. ${displayedWord.text}")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // TextField for user input
            OutlinedTextField(
                value = userInput,
                onValueChange = { newText ->
                    userInput = newText
                    val typed = newText.trim()

                    // Find the word that exactly matches the user's input (ignoring case)
                    val matchIndex = displayedWords.indexOfFirst {
                        it.text.equals(typed, ignoreCase = true)
                    }

                    if (matchIndex >= 0) {
                        // Remove the correctly typed word and add a new one with the current time
                        val currentList = displayedWords.toMutableList()
                        currentList.removeAt(matchIndex)
                        currentList.add(
                            DisplayedWord(
                                text = allWords.random(),
                                appearTime = System.currentTimeMillis()
                            )
                        )
                        displayedWords = currentList

                        // Increment the count of correctly typed words
                        typedCount++
                        // Clear the input field
                        userInput = ""
                    }
                },
                label = { Text("Type the word here") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
