import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.telephony.SmsManager
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.Aplication.abi.databinding.ActivityDashBoardBinding
import com.example.yourpackage.databinding.ActivityDashBoardBinding
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.sqrt

class DashBoard : AppCompatActivity(), SensorEventListener {
    private var mAuth: FirebaseAuth? = null
    private lateinit var binding: ActivityDashBoardBinding

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val deviceMacAddress = "0C:B8:15:F2:D0:BE" // Inserta aquí la dirección MAC del dispositivo BLE que deseas conectar
    private val serviceUuid = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E" // UUID del servicio BLE
    private val characteristicUuid = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E" // UUID de la característica BLE

    private lateinit var bluetoothGatt: BluetoothGatt

    private val handler = Handler()
    private val scanTimeout = 10000L // 10 segundos

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device: BluetoothDevice = result.device
            if (device.address == deviceMacAddress) {
                connectToDevice(device)
                stopScan()
            }
        }
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val accelerationThreshold = 9.8 // Valor de aceleración umbral (aquí se utiliza 9.8 m/s^2, que es la gravedad terrestre)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashBoardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()

        val user = mAuth!!.currentUser
        val email = user?.email

        if (email != null) {
            binding.ASHBOARD.text = email
        }

        binding.ASHBOARD.setOnClickListener {
            salir()
        }

        binding.sendMessageButton.setOnClickListener {
            sendMessage()
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (!isBluetoothEnabled()) {
            // El Bluetooth no está habilitado, puedes solicitar al usuario que lo habilite aquí
        }

        if (!hasLocationPermission()) {
            requestLocationPermission()
        } else {
            connectToDeviceByMac()
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No se utiliza en este ejemplo
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val accelerationX = event.values[0]
            val accelerationY = event.values[1]
            val accelerationZ = event.values[2]

            val accelerationMagnitude = sqrt(
                accelerationX.toDouble().pow(2.0) +
                        accelerationY.toDouble().pow(2.0) +
                        accelerationZ.toDouble().pow(2.0)
            )

            if (accelerationMagnitude >= accelerationThreshold) {
                // Envía el mensaje de texto cuando se excede el umbral de aceleración
                sendMessage()
            }
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter.isEnabled
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun connectToDeviceByMac() {
        val device = bluetoothAdapter.getRemoteDevice(deviceMacAddress)
        connectToDevice(device)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                // El dispositivo BLE se ha conectado exitosamente
                // Aquí puedes realizar las operaciones que desees con el dispositivo BLE
                discoverServices(gatt)
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                // El dispositivo BLE se ha desconectado
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service: BluetoothGattService? = gatt.getService(serviceUuid.toUUID())
                if (service != null) {
                    val characteristic: BluetoothGattCharacteristic? =
                        service.getCharacteristic(characteristicUuid.toUUID())
                    if (characteristic != null) {
                        // Leer el valor de la característica
                        gatt.readCharacteristic(characteristic)
                    }
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value: String = characteristic.getStringValue(0)
                // Hacer algo con el valor leído
            }
        }
    }

    private fun discoverServices(gatt: BluetoothGatt) {
        gatt.discoverServices()
    }

    private fun startScan() {
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        bluetoothAdapter.bluetoothLeScanner.startScan(null, scanSettings, scanCallback)

        handler.postDelayed({
            stopScan()
        }, scanTimeout)
    }

    private fun stopScan() {
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
    }

    private fun salir() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun sendMessage() {
        val phoneNumber = binding.phoneEdt.text.toString()
        val message = binding.messageEdt.text.toString()

        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(phoneNumber, null, message, null, null)

            Toast.makeText(applicationContext, "Message Sent", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Please enter all the data.."+e.message.toString(), Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
    }
}