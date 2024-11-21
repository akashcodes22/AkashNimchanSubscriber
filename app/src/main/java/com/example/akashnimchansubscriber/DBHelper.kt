package com.example.akashnimchansubscriber

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.akashnimchansubscriber.models.LocationModel

// Constants for database configuration
private const val DB_NAME = "database.sql"
private const val DB_VERSION = 1
private const val TABLE_LOCATION = "Location"

// Column constants
private const val COL_ID = "id"
private const val COL_LATITUDE = "latitude"
private const val COL_LONGITUDE = "longitude"
private const val COL_STUDENT_ID = "studentID"
private const val COL_SPEED = "speed"
private const val COL_TIMESTAMP = "timestamp"

private const val CREATE_TABLE_LOCATION_QUERY = """
    CREATE TABLE $TABLE_LOCATION (
        $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $COL_LATITUDE DOUBLE,
        $COL_LONGITUDE DOUBLE,
        $COL_STUDENT_ID TEXT,
        $COL_SPEED DOUBLE,
        $COL_TIMESTAMP INTEGER
    )
"""

class DatabaseHelper(context: Context, factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, DB_NAME, factory, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_LOCATION_QUERY)
        println("Database Created")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle non-destructive updates when upgrading DB versions
    }

    fun createLocation(
        latitude: Double,
        longitude: Double,
        id: String,
        timestamp: Int,
        speed: Double
    ) {
        val values = ContentValues().apply {
            put(COL_LATITUDE, latitude)
            put(COL_LONGITUDE, longitude)
            put(COL_STUDENT_ID, id)
            put(COL_TIMESTAMP, timestamp)
            put(COL_SPEED, speed)
        }

        writableDatabase.use { db ->
            db.insert(TABLE_LOCATION, null, values)
            println("Location inserted successfully")
        }
    }

    fun getAllLocationsGroupedByStudent(): Map<String, List<LocationModel>> {
        val studentLocationMap = mutableMapOf<String, MutableList<LocationModel>>()

        readableDatabase.use { db ->
            val cursor = db.rawQuery("SELECT * FROM $TABLE_LOCATION", null)
            cursor.use {
                if (it.moveToFirst()) {
                    do {
                        val locationData = parseLocationFromCursor(it)
                        val locationList = studentLocationMap.getOrPut(locationData.id) { mutableListOf() }
                        locationList.add(locationData)
                    } while (it.moveToNext())
                }
            }
        }
        return studentLocationMap
    }

    fun getAllLocations(): List<LocationModel> {
        val result = mutableListOf<LocationModel>()

        readableDatabase.use { db ->
            val cursor = db.rawQuery("SELECT * FROM $TABLE_LOCATION", null)
            cursor.use {
                while (it.moveToNext()) {
                    result.add(parseLocationFromCursor(it))
                }
            }
        }
        return result
    }

    fun getAllLocations(id: String): List<LocationModel> {
        val result = mutableListOf<LocationModel>()

        readableDatabase.use { db ->
            val cursor = db.rawQuery("SELECT * FROM $TABLE_LOCATION WHERE $COL_STUDENT_ID = ?", arrayOf(id))
            cursor.use {
                while (it.moveToNext()) {
                    result.add(parseLocationFromCursor(it))
                }
            }
        }
        return result
    }

    fun reset() {
        writableDatabase.use { db ->
            db.execSQL("DROP TABLE IF EXISTS $TABLE_LOCATION")
            db.execSQL(CREATE_TABLE_LOCATION_QUERY)
            println("Database reset successfully")
        }
    }

    private fun parseLocationFromCursor(cursor: android.database.Cursor): LocationModel {
        val id = cursor.getString(cursor.getColumnIndexOrThrow(COL_STUDENT_ID))
        val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LATITUDE))
        val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LONGITUDE))
        val timestamp = cursor.getInt(cursor.getColumnIndexOrThrow(COL_TIMESTAMP))
        val speed = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_SPEED))
        return LocationModel(id, latitude, longitude, timestamp, speed)
    }
}