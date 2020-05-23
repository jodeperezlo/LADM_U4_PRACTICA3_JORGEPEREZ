package mx.edu.ittepic.ladm_u4_practica3_jorgeperez

import android.content.pm.PackageManager
import android.database.sqlite.SQLiteException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    //Variables globales necesarias.
    var dataLista = ArrayList<String>()
    var listaID = ArrayList<String>()
    var nombreBD = "registros"
    var hilo : Hilo?=null

    val REQUEST_PERMISOS = 111

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Colocar el ícono en la barra de título
        getSupportActionBar()?.setDisplayShowHomeEnabled(true)
        getSupportActionBar()?.setIcon(R.mipmap.ic_launcher)


        // Solicitar los permisos, en caso de que no hayan sido otorgados.
        solicitarPermisos()

        //Actualizar Lista Calificaciones
        obtenerLista()

        //Boton agregar Calificación
        btnGuardar.setOnClickListener {
            // VALIDAR QUE LOS CAMPOS NO ESTÉN VACÍOS
            if(txtNombre.text.toString().isEmpty()||
                txtNC.text.toString().isEmpty()||
                txtCalificacion.text.toString().isEmpty()||
                txtUnidad.text.toString().isEmpty()){
                mensaje("TODOS LOS CAMPOS DEBEN ESTAR LLENOS")
                return@setOnClickListener
            }

            // VALIDAR EL NÚMERO DE CONTROL
            if(txtNC.text.toString().length != 8){
                mensaje("EL NÚMERO DE CONTROL DEBE TENER 8 DÍGITOS")
                return@setOnClickListener
            }
            // VALIDAR LA CALIFICACIÓN ASIGNADA
            if(txtCalificacion.text.toString().toFloat()<0 || txtCalificacion.text.toString().toFloat()>100){
                mensaje("DEBE PONER UNA CALIFICACIÓN ENTRE 0-100")
                return@setOnClickListener
            }
            // VALIDAR LA UNIDAD
            if(txtUnidad.text.toString().toInt() < 1 || txtUnidad.text.toString().toInt() > 8){
                mensaje("LA UNIDAD SÓLO PUEDE SER UN NÚMERO ENTERO ENTRE 1-8")
                return@setOnClickListener
            }

            // UNA VEZ VALIDADO TODO, PROCEDEMOS A GUARDAR.
            guardarRegistro()

            txtUnidad.requestFocus()
        }

        // BOTÓN PARA BORRAR LOS DATOS INGRESADOS.
        btnBorrar.setOnClickListener{
            txtNC.setText("")
            txtNombre.setText("")
            txtUnidad.setText("")
            txtCalificacion.setText("")
            txtNC.requestFocus()
        }

        // AL DAR CLIC AL TEXTVIEW SE MUESTRA EL ÚLTIMO MENSAJE
        lblUltimo.setOnClickListener{
            consultaUltMsj()
        }

        // EJECUTAMOS EL HILO
        hilo = Hilo(this)
        hilo!!.start()

    }

    // FUNCIÓN PARA GUARDAR LOS REGISTROS
    private fun guardarRegistro() {
        var nombre = txtNombre.text.toString()
        var NC = txtNC.text.toString()
        var calificacion = txtCalificacion.text.toString()
        var unidad = txtUnidad.text.toString()

        try {
            var baseDatos = BaseDatos(this,nombreBD,null,1)
            var insertar = baseDatos.writableDatabase
            var SQL = "INSERT INTO ALUMNO VALUES(NULL,'${NC}','${nombre}','U${unidad}','${calificacion}')"
            insertar.execSQL(SQL)
            baseDatos.close()
        }catch (e: SQLiteException){
            mensaje(e.message!!)
        }
        // Actualizar lista
        obtenerLista()
        mensaje("SE GUARDÓ EL REGISTRO")
    }

    // FUNCIÓN PARA CONSULTAR EL ÚLTIMO MENSAJE RECIBIDO.
    fun consultaUltMsj() {
         try{
             val cursor = BaseDatos(this, nombreBD, null, 1).readableDatabase
                 .rawQuery("SELECT * FROM ENTRANTES", null)

             var ultimo = ""
             // SI EL CURSOR MUESTRA RESULTADOS, LLENAMOS LA VARIABLE PARA MOSTRAR EL ÚLTIMO MENSAJE.
             if(cursor.moveToFirst()){
                 var estado = ""

                 // IF PARA SABER SI EL MENSAJE YA FUE CONTESTADO.
                 if(cursor.getString(2).equals("1")){
                     estado = "CONTESTADO"
                 }else{
                     estado = "NO CONTESTADO AÚN"
                 }
                 do{
                     ultimo = "ÚLTIMO MENSAJE RECIBIDO\nCELULAR ORIGEN: " +
                             cursor.getString(0)+"\nMENSAJE SMS: "+
                             cursor.getString(1)+"\nESTADO: "+ estado
                 }while(cursor.moveToNext())
             }else{
                 // SI LA VARIABLE CURSOR ESTÁ VACÍA, NO HAY DATOS.
                 ultimo = "SIN MENSAJES AÚN. TABLA VACÍA."
             }
             lblUltimo.setText(ultimo)
         }catch (err:SQLiteException){
             Toast.makeText(this,err.message,Toast.LENGTH_LONG).show()
         }
    }

    // FUNCIÓN PARA SOLICITAR LOS PERMISOS, EN CASO DE QUE NO ESTÉN OTORGADOS.
    private fun solicitarPermisos() {
        var permisoSendSMS = ActivityCompat.checkSelfPermission(this,android.Manifest.permission.SEND_SMS)
        var permisoReadSMS = ActivityCompat.checkSelfPermission(this,android.Manifest.permission.READ_SMS)
        var permisoReceiveSMS = ActivityCompat.checkSelfPermission(this,android.Manifest.permission.RECEIVE_SMS)

        if(permisoSendSMS != PackageManager.PERMISSION_GRANTED || permisoReadSMS != PackageManager.PERMISSION_GRANTED ||
            permisoReceiveSMS != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.SEND_SMS,android.Manifest.permission.READ_SMS,
                    android.Manifest.permission.RECEIVE_SMS),REQUEST_PERMISOS)
        }
    }
    // VER EL RESULTADO DE LA SOLICITUD DE PERMISOS
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_PERMISOS){
            consultaUltMsj()
        }
    }

    // FUNCIÓN PARA ELIMINAR REGISTROS DE CALIFICACIONES.
    private fun eliminaRegistro(id: String) {
        try {
            var baseDatos = BaseDatos(this,nombreBD,null,1)
            var eliminar = baseDatos.writableDatabase
            var SQL = "DELETE FROM ALUMNO WHERE ID = ?"
            var parametros = arrayOf(id)
            eliminar.execSQL(SQL,parametros)
            baseDatos.close()

            mensaje("SE ELIMINÓ CORRECTAMENTE")
        }catch (e:SQLiteException){
            mensaje(e.message!!)
        }
        // ACTUALIZAR LA LISTA
        obtenerLista()
    }

    // LLENAR LA LISTA DE REGISTROS.
    private fun obtenerLista() {
        this.dataLista.clear()
        this.listaID.clear()
        try{
            val cursor = BaseDatos(this,nombreBD,null,1).readableDatabase
                .rawQuery("SELECT * FROM ALUMNO",null)
            var resultado = ""

            if(cursor.moveToFirst()){
                do{
                    resultado ="\nNo Control: "+cursor.getString(1)+"\n"+
                            "Nombre: "+cursor.getString(2)+"\n"+
                            "Unidad: "+cursor.getString(3)+"\n"+
                            "Calificación: "+cursor.getString(4)
                    cursor.getString(1)
                    dataLista.add(resultado)
                    listaID.add(cursor.getString(0))
                }while(cursor.moveToNext())
            }else{
                // NO TIENE NADA EL CURSOR
                dataLista.add("NO SE ENCONTRARON DATOS")
            }
            // LLENAMOS LA LISTA DE REGISTROS.
            var adaptador = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataLista)
            listaRegistros.adapter = adaptador

            listaRegistros.setOnItemClickListener { parent, view, position, id ->
                AlertDialog.Builder(this)
                    .setTitle("ATENCIÓN")
                    .setMessage("¿DESEA REALMENTE ELIMINAR EL REGISTRO?\n"+dataLista[position])
                    .setPositiveButton("Eliminar") {d, i ->
                        eliminaRegistro(listaID[position])
                    }
                    .setNegativeButton("Cancelar") {d, i -> }
                    .show()
            }
        }catch (err: SQLiteException){
            Toast.makeText(this,err.message, Toast.LENGTH_LONG)
                .show()
        }
    }

    // FUNCIÓN PARA MANDAR MENSAJES DE AVISO CON TOAST.
    private fun mensaje(m: String) {
        Toast.makeText(this,m,Toast.LENGTH_LONG)
            .show()
    }

    // VALIDAR EL MENSAJE, QUE TENGA LA SINTAXIS CORRECTA
    fun validaMensaje(m:String):Boolean{
        try{
            // DIVIDE EL MENSAJE EN PARTES, PARA COMPARAR
            var partes = m.split("-")
            var parte1 = partes[0]
            var nc = partes[1].length
            var unidad = partes[2].length
            var primeraLetraUnidad = partes[2].substring(0,1)

            if(parte1.equals("CALIFICACION") && nc == 8  && unidad == 2 && primeraLetraUnidad.equals("U")){
                return true
            }
        }catch (e:IndexOutOfBoundsException){ return false }
        return false
    }

    // BUSCAR AL ALUMNO Y RETORNAR EL MENSAJE A ENVIAR, YA SEA LA CALIFICACIÓN, O QUE NO ENCONTRÓ CALIFICACIÓN.
    fun buscarAlumno(nc:String,unidad:String):String{
        var resultado = ""
        try {
            var baseDatos = BaseDatos(this,nombreBD,null,1)
            var select = baseDatos.readableDatabase
            var SQL = "SELECT NOMBRE,CALIF FROM ALUMNO WHERE NUMEROCONTROL = ? AND UNIDAD = ?"
            var parametros = arrayOf(nc,unidad)
            var cursor = select.rawQuery(SQL,parametros)

            if(cursor.moveToFirst()){
                // SI HAY RESULTADO.
                resultado = "ALUMNO: " + cursor.getString(0) + " | CALIFICACIÓN: " + cursor.getString(1)
            }
            else{
                // NO ENCONTRÓ DATOS PARA ESE NÚMERO DE CONTROL Y UNIDAD
                resultado = "NO HAY CALIFICACIÓN PARA ESE NÚMERO DE CONTROL Y UNIDAD"
            }
            select.close()
            baseDatos.close()
        }catch (error:SQLiteException){ }

        return resultado
    }

    // ENVIAR EL MENSAJE
    fun enviarSMS() {
        var nc = ""
        var unidad = ""
        var mensaje =""

        try {
            val cursor = BaseDatos(this,nombreBD,null,1).readableDatabase
                .rawQuery("SELECT * FROM ENTRANTES",null)

            var ultimoNum = ""
            var ultimoMsj = ""
            var estado = ""

            if(cursor.moveToFirst()) {
                do {
                    ultimoNum = cursor.getString(0)
                    ultimoMsj = cursor.getString(1)
                    estado = cursor.getString(2)

                    if (validaMensaje(ultimoMsj)) {
                        // SI ENTRA AQUÍ ES PORQUE LA SINTEXIS DEL MENSAJE SÍ ES CORRECTA.
                        var partes = ultimoMsj.split("-")
                        nc = partes[1]
                        unidad = partes[2]
                        mensaje = buscarAlumno(nc,unidad)

                        if (estado.equals("0")) {
                            // SI ENTRA AQUÍ ES PORQUE EL MENSAJE NO HA SIDO RESPONDIDO.
                            SmsManager.getDefault().sendTextMessage(ultimoNum, null, mensaje, null, null)
                            actualizaEstado(ultimoNum)
                        }
                    } else {
                        if (estado.equals("0")) {
                            // SI ENTRA AQUÍ ES PORQUE LA SINTAXIS DEL MENSAJE NO ES CORRECTA, Y NO HA RESPONDIDO EL MENSAJE.
                            mensaje = "ERROR: LA SINTAXIS DEBE SER; CALIFICACION-16400973-U1 , POR EJEMPLO"
                            SmsManager.getDefault().sendTextMessage(ultimoNum, null, mensaje, null, null)
                            actualizaEstado(ultimoNum)
                        }
                    }
                }while(cursor.moveToNext())
            }
            cursor.close()
        }catch (error: SQLiteException){
            Toast.makeText(this,error.message,Toast.LENGTH_LONG).show()
        }
    }

    // CAMBIAR EL ESTADO A CONTESTADO, UNA VEZ ENVIADO EL MENSAJE
    fun actualizaEstado (numero:String){
        // SI TIENE UN 0 AÚN NO SE HA RESPONDIDO
        // SI TIENE UN 1 YA FUE RESPONDIDO EL MENSAJE
        try{
            var baseDatos = BaseDatos(this,nombreBD,null,1)
            var insertar = baseDatos.writableDatabase
            var SQL = "UPDATE ENTRANTES SET ESTADO ='1' WHERE NUMERO = ?"
            var parametros = arrayOf(numero)
            insertar.execSQL(SQL,parametros)
            insertar.close()
            baseDatos.close()
        }catch (error:SQLiteException){
            Toast.makeText(this,error.message,Toast.LENGTH_LONG).show()
        }
    }
}