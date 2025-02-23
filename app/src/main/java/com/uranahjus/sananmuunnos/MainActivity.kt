package com.uranahjus.sananmuunnos

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import android.database.Cursor
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import com.uranahjus.sananmuunnos.SanastoDbHelper.Companion.COLUMN_TEXT
import com.uranahjus.sananmuunnos.SanastoDbHelper.Companion.COLUMN_VALUE
import com.uranahjus.sananmuunnos.SanastoDbHelper.Companion.TABLE_NAME

class MainActivity : AppCompatActivity() {
    lateinit var l: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        l = findViewById(R.id.listView)
        var wordArray: ArrayAdapter<String>

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val dbHelper = SanastoDbHelper(applicationContext)
        val db = dbHelper.openDatabase()

        // text input field
        val editText = findViewById<EditText>(R.id.editText)
        editText.requestFocus()

        // add onclick listener to text input field to listview l
        l.setOnItemClickListener { parent, view, position, id ->
            val textView = view as TextView
            println("Clicked on item: ${textView.text}")

            // send intent to open word details activity
            val intent = Intent(this, WordDetails::class.java)
            intent.putExtra("word", textView.text.toString())
            startActivity(intent)
        }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // This method is called before the text is changed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // This method is called when the text is being changed
                handleModifiedText(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                // This method is called after the text has been changed.
                val result = getValuesFromDatabase(s.toString(), db)
                println("Result from database: $result")

                wordArray = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, result)
                l.adapter = wordArray
            }
        })

    }
    private fun handleModifiedText(text: String) {
        println("Modified text: $text")
    }

    // return array of values from database
    private fun getValuesFromDatabase(text: String, db: SQLiteDatabase): List<String> {
        val selectQuery = "SELECT $COLUMN_VALUE FROM $TABLE_NAME WHERE $COLUMN_TEXT like ? AND taivutustiedot IS NOT ''"
        val selectionArgs = arrayOf("$text%")
        val cursor: Cursor = db.rawQuery(selectQuery, selectionArgs)
        val values = mutableListOf<String>()
        if (cursor.moveToFirst()) {
            // print how many rows were returned
            println("Number of rows found: ${cursor.count}")
            // get the value from the first column
            values.add(cursor.getString(0))
            while (cursor.moveToNext()) {
                values.add(cursor.getString(0))
            }
        }
        cursor.close()
        return values
    }
}