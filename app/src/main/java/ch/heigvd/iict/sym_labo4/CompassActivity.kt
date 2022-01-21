package ch.heigvd.iict.sym_labo4

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import ch.heigvd.iict.sym_labo4.gl.OpenGLRenderer

/**
 * Project: Labo4
 * Created by fabien.dutoit on 21.11.2016
 * Updated by fabien.dutoit on 06.11.2020
 * Updated by Adrien Peguiron, Noémie Plancherel, Nicolas Viotti on 29.11.2021
 * (C) 2016 - HEIG-VD, IICT
 */
class CompassActivity : AppCompatActivity() , SensorEventListener {

    //opengl
    private lateinit var opglr: OpenGLRenderer
    private lateinit var m3DView: GLSurfaceView
    // Sensors
    private lateinit var sensorManager: SensorManager
    private lateinit var magnetometer: Sensor
    private lateinit var accelerator: Sensor

    // Sensors data
    private var accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)
    private var rotationMatrix = FloatArray(16)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // we need fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)

        // we initiate the view
        setContentView(R.layout.activity_compass)

        // we create the renderer
        opglr = OpenGLRenderer(applicationContext)

        // link to GUI
        m3DView = findViewById(R.id.compass_opengl)

        // init opengl surface view
        m3DView.setRenderer(opglr)

        // Get sensors of the current system
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        accelerator = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        // Register listener of sensors when activity is on resume
        sensorManager.registerListener(this, accelerator, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        // Unregistered listener of sensors when activity is on pause
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // Get event when there are changes (new data that the sensor recorded)
        when (event?.sensor?.type) {
            Sensor.TYPE_ACCELEROMETER -> accelerometerReading = event.values
            Sensor.TYPE_MAGNETIC_FIELD -> magnetometerReading = event.values
        }
        // Get rotation matrix
        SensorManager.getRotationMatrix(rotationMatrix,
            null, accelerometerReading, magnetometerReading)

        // Swap rotation matrix
        rotationMatrix = opglr.swapRotMatrix(rotationMatrix)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        when (accuracy) {
            0 -> Log.i("CompassActivity", "Accurancy of sensor $sensor status changed to Unreliable")
            1 -> Log.i("CompassActivity", "Accurancy of sensor $sensor status changed to Low Accuracy")
            2 -> Log.i("CompassActivity", "Accurancy of sensor $sensor status changed to Medium Accuracy")
            3 -> Log.i("CompassActivity", "Accurancy of sensor $sensor status changed to High Accuracy")
        }
    }
}