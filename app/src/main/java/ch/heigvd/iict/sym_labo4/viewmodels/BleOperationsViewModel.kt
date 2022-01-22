package ch.heigvd.iict.sym_labo4.viewmodels

import android.app.Application
import android.bluetooth.*
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.observer.ConnectionObserver
import java.util.*
import androidx.lifecycle.LiveData
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data


/**
 * Project: Labo4
 * Created by fabien.dutoit on 11.05.2019
 * Updated by fabien.dutoit on 18.10.2021
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
                                val timePerm = currentTimeChar!!.permissions
                                val integerPerm = integerChar!!.permissions
                                val temperaturePerm = temperatureChar!!.permissions
                                val buttonClickPerm = buttonClickChar!!.permissions

                                if (temperaturePerm != BluetoothGattDescriptor.PERMISSION_READ) {
                                    return false;
                                } else {
                                    Log.d(TAG, "It works");
                                }


                            } else {
                                return false;
                            }
                        } else {
                            return false;
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

                    override fun initialize() {
                        /*  TODO
                            Ici nous somme sûr que le périphérique possède bien tous les services et caractéristiques
                            attendus et que nous y sommes connectés. Nous pouvous effectuer les premiers échanges BLE:
                            Dans notre cas il s'agit de s'enregistrer pour recevoir les notifications proposées par certaines
                            caractéristiques, on en profitera aussi pour mettre en place les callbacks correspondants.
                         */


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

        fun readTemperature(): Boolean {
            /*  TODO
                on peut effectuer ici la lecture de la caractéristique température
                la valeur récupérée sera envoyée à l'activité en utilisant le mécanisme
                des MutableLiveData
                On placera des méthodes similaires pour les autres opérations
            */
            readCharacteristic(temperatureChar).with { device: BluetoothDevice?, data: Data ->
                temperature.setValue(
                    data.getIntValue(Data.FORMAT_UINT16, 0)!! / 10f
                )
            }.enqueue()

            return true //FIXME
        }
        fun sendValue(value: Int): Boolean {
            val tab = ByteArray(4);
            tab[3]=value.toByte()
            writeCharacteristic(temperatureChar,tab,WRITE_TYPE_DEFAULT)
            return true;
        }

        fun sendCurrentTime(): Boolean {
            //writeCharacteristic(currentTimeChar)
            return false;
        }
    }

    companion object {
        private val TAG = BleOperationsViewModel::class.java.simpleName
    }

    init {
        ble.setConnectionObserver(bleConnectionObserver)
    }

}