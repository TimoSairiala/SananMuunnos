package com.uranahjus.sananmuunnos

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import java.io.File
import java.io.FileOutputStream

class SanastoDbHelper (val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        const val ASSET_PATH = "databases"
        const val DATABASE_NAME = "sanasto.db"
        const val DATABASE_VERSION = 1
        val TABLE_NAME = "sanasto"
        val COLUMN_TEXT = "sana"
        val COLUMN_VALUE = "sana"
        private const val TAG = "DatabaseHelper"
    }

    private val databasePath: String = context.getDatabasePath(DATABASE_NAME).path

    private fun createDatabaseIfNotExists() {
        if (!checkDatabaseExists()) {
            println("copy db from assets")
            copyDatabaseFromAssets()
        }
    }

    private fun checkDatabaseExists(): Boolean {
        return File(databasePath).exists()
    }

    private fun copyDatabaseFromAssets() {
        try {
            context.assets.open("$ASSET_PATH/$DATABASE_NAME").use { inputStream ->
                FileOutputStream(databasePath).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        TODO("Not yet implemented")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }

    fun openDatabase(): SQLiteDatabase {
        createDatabaseIfNotExists()
        return SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READWRITE)
    }

    fun getValueFromDatabase(text: String, db: SQLiteDatabase): Word {
        val selectQuery = "SELECT homonymia, sanaluokka, taivutustiedot FROM $TABLE_NAME WHERE $COLUMN_TEXT = ?"
        val cursor: Cursor = db.rawQuery(selectQuery, arrayOf(text))

        if (cursor.moveToFirst()) {
            val homonymiaIndex = cursor.getColumnIndexOrThrow("homonymia")
            val sanaluokkaIndex = cursor.getColumnIndexOrThrow("sanaluokka")
            val taivutusIndex = cursor.getColumnIndexOrThrow("taivutustiedot")

            val wordClass: Word = Word(
                cursor.getString(homonymiaIndex),
                cursor.getString(sanaluokkaIndex),
                cursor.getInt(taivutusIndex)
            )
            val wordDetails: Map<String, String> = mapOf(
                "homonymia" to cursor.getString(homonymiaIndex),
                "sanaluokka" to cursor.getString(sanaluokkaIndex),
                "taivutus" to cursor.getString(taivutusIndex)
            )

            // Log using Log.d with a tag for better filtering
            Log.d(Companion.TAG, "homonymia: ${cursor.getString(homonymiaIndex)}")
            Log.d(Companion.TAG, "sanaluokka: ${cursor.getString(sanaluokkaIndex)}")
            Log.d(Companion.TAG, "taivutus: ${cursor.getString(taivutusIndex)}")
            cursor.close()
            return wordClass
        }
        cursor.close()
        return Word("","", 0)
    }
}