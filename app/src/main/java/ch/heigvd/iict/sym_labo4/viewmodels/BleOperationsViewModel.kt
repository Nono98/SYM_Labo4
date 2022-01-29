package ch.heigvd.iict.sym_labo4.viewmodels

import android.app.Application
import android.bluetooth.*
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.util.*
import androidx.lifecycle.LiveData
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data
import java.sql.Array
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * Project: Labo4
 * Created by fabien.dutoit on 11.05.2019
 * Updated by Nicolas Viotti, Noémie Plancherel and Adrien Peguiron on 29.01.2022
 * (C) 2019 - HEIG-VD, IICT
 */
class BleOperationsViewModel(application: Application) : AndroidViewModel(application) {

    private var ble = SYMBleManager(application.applicationContext)
    private var mConnection: BluetoothGatt? = null

    //live data - observer
    val isConnected = MutableLiveData(false)

    private val temperature = MutableLiveData<Float>()
    fun getTemperature(): LiveData<Float>? {
        return temperature
    }
    private val dateAndTime = MutableLiveData<String>()
    fun getDateAndTime(): LiveData<String>? {
        return dateAndTime
    }
    private val nbClicks = MutableLiveData<Int>()
    fun getNbClicks(): LiveData<Int>? {
        return nbClicks
    }

    //Services and Characteristics of the SYM Pixl
    private var timeService: BluetoothGattService? = null
    private var symService: BluetoothGattService? = null
    private var currentTimeChar: BluetoothGattCharacteristic? = null
    private var integerChar: BluetoothGattCharacteristic? = null
    private var temperatureChar: BluetoothGattCharacteristic? = null
    private var buttonClickChar: BluetoothGattCharacteristic? = null

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared")
        ble.disconnect()
    }

    fun connect(device: BluetoothDevice) {
        Log.d(TAG, "User request connection to: $device")
        if (!isConnected.value!!) {
            ble.connect(device)
                .retry(1, 100)
                .useAutoConnect(false)
                .enqueue()
        }
    }

    fun disconnect() {
        Log.d(TAG, "User request disconnection")
        ble.disconnect()
        mConnection?.disconnect()
    }

    /* TODO
        vous pouvez placer ici les différentes méthodes permettant à l'utilisateur
        d'interagir avec le périphérique depuis l'activité
     */


    fun readTemperature(): Boolean {
        if (!isConnected.value!! || temperatureChar == null)
            return false
        else
            return ble.readTemperature()
    }

    fun sendInt(value: Int){
        if (isConnected.value!! && integerChar != null)
            ble.setInt(value)
    }

    fun sendTime(){
        if (isConnected.value!! && currentTimeChar != null)
            ble.setTime()
    }

    private val bleConnectionObserver: ConnectionObserver = object : ConnectionObserver {
        override fun onDeviceConnecting(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceConnecting")
            isConnected.value = false
        }

        override fun onDeviceConnected(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceConnected")
            isConnected.value = true
        }

        override fun onDeviceDisconnecting(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceDisconnecting")
            isConnected.value = false
        }

        override fun onDeviceReady(device: BluetoothDevice) {
            Log.d(TAG, "onDeviceReady")
        }

        override fun onDeviceFailedToConnect(device: BluetoothDevice, reason: Int) {
            Log.d(TAG, "onDeviceFailedToConnect")
        }

        override fun onDeviceDisconnected(device: BluetoothDevice, reason: Int) {
            if (reason == ConnectionObserver.REASON_NOT_SUPPORTED) {
                Log.d(TAG, "onDeviceDisconnected - not supported")
                Toast.makeText(
                    getApplication(),
                    "Device not supported - implement method isRequiredServiceSupported()",
                    Toast.LENGTH_LONG
                ).show()
            } else
                Log.d(TAG, "onDeviceDisconnected")
            isConnected.value = false
        }

    }

    private inner class SYMBleManager(applicationContext: Context) :
        BleManager(applicationContext) {
        /**
         * BluetoothGatt callbacks object.
         */
        private var mGattCallback: BleManagerGattCallback? = null
        // UUIDs des services et caractéristiques
        final val uuidSymService = "3c0a1000-281d-4b48-b2a7-f15579a1c38f"
        final val uuidTimeService = "00001805-0000-1000-8000-00805f9b34fb"
        final val uuidCurrentTimeChar = "00002A2B-0000-1000-8000-00805f9b34fb"
        final val uuidIntegerChar = "3c0a1001-281d-4b48-b2a7-f15579a1c38f"
        final val uuidTemperatureChar = "3c0a1002-281d-4b48-b2a7-f15579a1c38f"
        final val uuidButtonClickChar = "3c0a1003-281d-4b48-b2a7-f15579a1c38f"

        public override fun getGattCallback(): BleManagerGattCallback {


            //we initiate the mGattCallback on first call, singleton
            if (mGattCallback == null) {
                mGattCallback = object : BleManagerGattCallback() {

                    public override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
                        mConnection = gatt //trick to force disconnection

                        Log.d(TAG, "isRequiredServiceSupported - TODO")
                        symService = gatt.getService(UUID.fromString(uuidSymService))
                        timeService = gatt.getService(UUID.fromString(uuidTimeService))
                        if (symService != null && timeService != null) {
                            currentTimeChar =
                                timeService!!.getCharacteristic((UUID.fromString(uuidCurrentTimeChar)))
                            integerChar =
                                symService!!.getCharacteristic((UUID.fromString(uuidIntegerChar)))
                            temperatureChar =
                                symService!!.getCharacteristic((UUID.fromString(uuidTemperatureChar)))
                            buttonClickChar =
                                symService!!.getCharacteristic((UUID.fromString(uuidButtonClickChar)))

                            if (currentTimeChar != null && integerChar != null && temperatureChar != null && buttonClickChar != null) {
                                return true
                            }
                        }
                        /* TODO
                        - Nous devons vérifier ici que le périphérique auquel on vient de se connecter possède
                          bien tous les services et les caractéristiques attendues, on vérifiera aussi que les
                          caractéristiques présentent bien les opérations attendues
                        - On en profitera aussi pour garder les références vers les différents services et
                          caractéristiques (déclarés en lignes 39 à 44)
                        */



                        return false //FIXME si tout est OK, on retourne true, sinon la librairie appelera la méthode onDeviceDisconnected() avec le flag REASON_NOT_SUPPORTED
                    }

                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun initialize() {
                        /*  TODO
                            Ici nous somme sûr que le périphérique possède bien tous les services et caractéristiques
                            attendus et que nous y sommes connectés. Nous pouvous effectuer les premiers échanges BLE:
                            Dans notre cas il s'agit de s'enregistrer pour recevoir les notifications proposées par certaines
                            caractéristiques, on en profitera aussi pour mettre en place les callbacks correspondants.
                         */
                        setNotificationCallback(currentTimeChar).with { _, data ->
                            var date = data.getIntValue(Data.FORMAT_UINT16, 0).toString() + "-"
                            for (i in 2..6) {
                                val value = data.getIntValue(Data.FORMAT_UINT8, i).toString()
                                if (value < 10.toString()){
                                    date += 0
                                }
                                date += value
                                //pour la date
                                if (i < 3){
                                    date += "-"
                                }
                                else if (i == 3){
                                    date += " "
                                }
                                //pour l'heure
                                else if(i < 6){
                                    date += ":"
                                }
                            }
                            dateAndTime.setValue(date)
                        }
                        enableNotifications(currentTimeChar).enqueue()

                        setNotificationCallback(buttonClickChar).with { _, data ->
                            nbClicks.setValue(data.getIntValue(Data.FORMAT_UINT8, 0))

                        }
                        enableNotifications(buttonClickChar).enqueue()
                    }

                    override fun onServicesInvalidated() {
                        //we reset services and characteristics
                        timeService = null
                        currentTimeChar = null
                        symService = null
                        integerChar = null
                        temperatureChar = null
                        buttonClickChar = null
                    }
                }
            }
            return mGattCallback!!
        }

        // Lecture de la température sur l'appareil
        fun readTemperature(): Boolean {
            /*  TODO
                on peut effectuer ici la lecture de la caractéristique température
                la valeur récupérée sera envoyée à l'activité en utilisant le mécanisme
                des MutableLiveData
                On placera des méthodes similaires pour les autres opérations
            */
            readCharacteristic(temperatureChar).with { _: BluetoothDevice?, data: Data ->
                //la valeur trouvée est divisée par 10 pour avoir une valeur en celsius
                temperature.setValue(data.getIntValue(Data.FORMAT_UINT16, 0)!! / 10f)
            }.enqueue()

            return true //FIXME
        }

        // Envoi d'un entier sur l'appareil
        fun setInt(value: Int){
            val buffer : ByteArray = ByteArray(4)
            //4 shifts à droite pour convertir la valeur en uint32
            for (i in 0..3) buffer[i] = (value shr (i * 8)).toByte()
            writeCharacteristic(integerChar, buffer, WRITE_TYPE_DEFAULT).enqueue()
        }


        // Envoie de l'heure actuelle sur l'appareil
        fun setTime(){
            val date = Calendar.getInstance()
            val year = date.get(Calendar.YEAR)
            val month = date.get(Calendar.MONTH) + 1 //Calendar.Month commence à 0
            val day = date.get(Calendar.DAY_OF_MONTH)
            val hour = date.get(Calendar.HOUR_OF_DAY)
            val minute = date.get(Calendar.MINUTE)
            val second = date.get(Calendar.SECOND)
            val dayOfWeek = date.get(Calendar.DAY_OF_WEEK)

            val buffer : ByteArray = ByteArray(10)
            //shift à droite pour convertir l'année en uint16
            buffer[0] = (year shr 0).toByte()
            buffer[1] = (year shr 8).toByte()
            buffer[2] = month.toByte()
            buffer[3] = day.toByte()
            buffer[4] = hour.toByte()
            buffer[5] = minute.toByte()
            buffer[6] = second.toByte()
            buffer[7] = dayOfWeek.toByte()
            writeCharacteristic(currentTimeChar, buffer, WRITE_TYPE_DEFAULT).enqueue()
        }
    }

    companion object {
        private val TAG = BleOperationsViewModel::class.java.simpleName
    }

    init {
        ble.setConnectionObserver(bleConnectionObserver)
    }

}