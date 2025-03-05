package com.uranahjus.sananmuunnos

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.uranahjus.sananmuunnos.databinding.ActivityWordDetailsBinding
import org.json.JSONArray
import org.json.JSONObject

class WordDetails : AppCompatActivity() {
    private lateinit var binding: ActivityWordDetailsBinding
    private lateinit var dbHelper: SanastoDbHelper
    private val TAG = "databaseHelper"
    private var word: String = ""
    private var wordConjugations: Array<String> = emptyList<String>().toTypedArray()
    private var wordWDeclensions: Array<String> = emptyList<String>().toTypedArray()
    private val apikey = BuildConfig.API_KEY

    companion object {
        const val EXTRA_WORD = "word"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWordDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = SanastoDbHelper(applicationContext)

        setupToolbar()
        loadWordDetails()

        // ai fab
        val fab: View = findViewById(R.id.floatingActionButton)
        fab.setOnClickListener { view ->
            CoroutineScope(Dispatchers.Main).launch {
                Snackbar.make(
                    view,
                    "Loihditaan tekoälyllä esimerkki sanasta $word",
                    Snackbar.LENGTH_LONG
                )
                    .setAction("Action", null).show()
                generateExamples(word)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private suspend fun generateExamples(inputWord: String) {
        Log.d(TAG, "Generating examples for $inputWord")

        val generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apikey
        )

        val prompt: StringBuilder = StringBuilder()
        prompt.append("Tarvitsen taivutusmuotoja tälle sanalle: ")
        prompt.append(inputWord)
        prompt.append("\n")
        prompt.append("Se taipuu samalla tavalla kuin sana ")
        prompt.append(wordConjugations.first())
        prompt.append("\n")
        prompt.append("")

        val response = generativeModel.generateContent(prompt.toString())
        print(response.text)
        val examplesView = findViewById<TextView>(R.id.textViewWordExamples)
        examplesView.append("\n\nTekoälyn esimerkit voi olla epätarkkoja:\n${response.text}")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun loadWordDetails() {
        // Safely get the word from the intent
        word = intent.getStringExtra(EXTRA_WORD) ?: run {
            Log.e(TAG, "No word provided in intent.")
            finish() // Close the activity if no word is provided
            return
        }

        Log.d(TAG, "Word from intent: $word")

        // Set the title of the activity and the TextView
        binding.textViewWord.text = word.replaceFirstChar { it.uppercase() }
        binding.toolbar.title = word

        // Open the database
        val db = dbHelper.openDatabase()

        // Retrieve word details from the database
        val wordDetails = dbHelper.getValueFromDatabase(word, db)
        println("Word details: $wordDetails")

        displayClassification(word, wordDetails.classification)
        displayExamples(wordDetails.example)
    }

    private fun displayClassification(word: String, classification: String) {
        if (classification.isNotEmpty()) {
            binding.textViewWordClassification.text = getString(R.string.word_classification_format, word.replaceFirstChar { it.uppercase() }, classification)
        }
    }

    private fun displayExamples(examplesCount: Int) {
        if (examplesCount > 0) {
            val conjugation: Map<Int, Array<String>> = loadConjugations()

            Log.d(TAG, "Examples: ${conjugation[examplesCount]}")
            wordConjugations = conjugation[examplesCount] ?: run {
                Log.w(TAG, "No conjugations found for example count: $examplesCount")
                return
            }

            Log.d(TAG, "Word conjugations: ${wordConjugations.contentToString()}")
            wordWDeclensions = appendDeclension(wordConjugations, examplesCount)
            Log.d(TAG, "Word with declensions: $wordWDeclensions")

            if (wordWDeclensions.isNotEmpty()) {
                binding.textViewWordExamples.text = wordWDeclensions.joinToString("\n")
            }
            binding.textViewReferenceWord.text = getString(
                R.string.taipuu_samalla_tavalla_kuin_sana,
                wordConjugations.first().replaceFirstChar { it.uppercase() })
        }
    }

    private fun loadConjugations(): Map<Int, Array<String>> {
        val inputStream = resources.openRawResource(R.raw.taivutukset)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)

        return jsonObject.keys().asSequence().associate { key ->
            key.toInt() to JSONArray(jsonObject.getString(key)).let { jsonArray ->
                Array(jsonArray.length()) { i -> jsonArray.getString(i) }
            }
        }
    }

    private fun appendDeclension(word: Array<String>?, declension: Int): Array<String> {
        Log.d(TAG, "Etsitaan nominien jotkut ihme nimet: $word")
        val singleDeclension = getSingleDeclension(declension)
        val combined: Array<String> = Array(singleDeclension.size) { "" }

        for (i in singleDeclension.indices) {
            Log.d(TAG, "yhdistetaan $word[i] ja ${singleDeclension[i]}")
            combined[i] = "${singleDeclension[i]}: ${word?.get(i)}"
        }
        return combined
    }

    private fun getSingleDeclension(declension: Int): Array<String> {
        Log.d(TAG, "Minkalainen lista: $declension")
        when (declension) {
            // nominit
            in 1..49 -> return arrayOf("yksikön nominatiivi",
                "genetiivi",
                "partitiivi",
                "illatiivi",
                "monikon nominatiivi",
                "genetiivi",
                "partitiivi",
                "illatiivi")
            // yhdysnominit
            in 50..51 -> return arrayOf("yksikön nominatiivi",
                "genetiivi",
                "partitiivi",
                "illatiivi",
                "monikon nominatiivi",
                "genetiivi",
                "partitiivi",
                "illatiivi")
            // verbit
            in 52..76 -> return arrayOf("1. infinitiivi (eli A-infinitiivi)",
                "aktiivin indikatiivin preesensin yksikön ensimmäinen persoona",
                "aktiivin indikatiivin imperfektin kolmas persoona",
                "aktiivin konditionaalin preesensin yksikön kolmas persoona",
                "potentiaalin preesensin yksikön kolmas persoona",
                "imperatiivin preesensin yksikön kolmas persoona",
                "aktiivin 2. partisiippi (eli NUT-partisiippi)",
                "passiivin imperfekti")
        }
        return emptyArray<String>()
    }
}