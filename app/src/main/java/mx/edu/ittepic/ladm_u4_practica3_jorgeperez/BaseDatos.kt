package mx.edu.ittepic.ladm_u4_practica3_jorgeperez

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BaseDatos(context: Context?, name : String, factory: SQLiteDatabase.CursorFactory?, version: Int) :
    SQLiteOpenHelper(context, name, factory, version){
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE ENTRANTES(NUMERO VARCHAR(200), MENSAJE VARCHAR(2000), ESTADO CHAR(1))")
        db.execSQL("CREATE TABLE ALUMNO(ID INTEGER PRIMARY KEY AUTOINCREMENT, NUMEROCONTROL CHAR(8), NOMBRE VARCHAR(100), UNIDAD CHAR(2), CALIF CHAR(3))")
    }
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {}
}