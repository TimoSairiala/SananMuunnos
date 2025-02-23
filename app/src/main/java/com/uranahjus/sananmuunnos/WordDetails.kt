package com.uranahjus.sananmuunnos

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.uranahjus.sananmuunnos.databinding.ActivityWordDetailsBinding
import org.json.JSONArray
import org.json.JSONObject

class WordDetails : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityWordDetailsBinding
    private val TAG = "DatabaseHelper"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWordDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        // display back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val dbHelper = SanastoDbHelper(applicationContext)
        val db = dbHelper.openDatabase()

        // get intent extra
        val word = intent.getStringExtra("word")
        Log.d(TAG, "Word from intent: $word")

        // set title of activity to word
        val title =  findViewById<TextView>(R.id.textViewWord)
        title.setText(word)
        toolbar.setTitle(word)

        val sanastoa = dbHelper.getValueFromDatabase(word!!, db)
        println("Sanastoa: $sanastoa")

        // display word details
        val classification = sanastoa.classification
        if ( classification.isNotEmpty()) {
            val details = findViewById<TextView>(R.id.textViewWordClassification)
            details.setText("$word on ${classification}")
        }

        val examples = sanastoa.example
        if ( examples > 0) {
            val examplesView = findViewById<TextView>(R.id.textViewWordExamples)
            val wordReferenceView = findViewById<TextView>(R.id.textViewReferenceWord)
            val conjugation: Map<Int, Array<String>> = loadConjugations()

            Log.d(TAG, "Examples: $conjugation.get(examples.toInt())".toString())
            val wordConjugations = conjugation.get(examples.toInt()) ?: return
            wordReferenceView.append(wordConjugations.get(0))

            Log.d(TAG, "Word conjugations: $wordConjugations")
            val wordWDeclensions = appendDeclension(wordConjugations, examples.toInt())
            Log.d(TAG, "Word with declensions: $wordWDeclensions")

            if (wordWDeclensions.isNotEmpty()) {
                var exampleDetails: StringBuilder = StringBuilder()
                exampleDetails.append(wordWDeclensions.joinToString("\n"))
                examplesView.setText(exampleDetails)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    fun loadConjugations(): Map<Int, Array<String>> {
        val inputStream = resources.openRawResource(R.raw.taivutukset)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)

        return jsonObject.keys().asSequence().associate { key ->
            key.toInt() to JSONArray(jsonObject.getString(key)).let { jsonArray ->
                Array(jsonArray.length()) { i -> jsonArray.getString(i) }
            }
        }
    }

    fun appendDeclension(word: Array<String>?, declension: Int): Array<String> {
        Log.d(TAG, "Etsitaan nominien jotkut ihme nimet: $word")
        val singleDeclension = getSingleDeclension(declension)
        var combined: Array<String> = Array(singleDeclension.size) { "" }

        for (i in singleDeclension.indices) {
            Log.d(TAG, "yhdistetaan $word[i] ja ${singleDeclension[i]}")
            combined[i] = "${singleDeclension[i]}: ${word?.get(i)}"
        }
        return combined
    }

    fun getSingleDeclension(declension: Int): Array<String> {
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