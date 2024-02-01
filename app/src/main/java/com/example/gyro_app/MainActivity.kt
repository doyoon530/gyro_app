package com.example.gyro_app

import MyGLRenderer
import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import jxl.Workbook
import jxl.write.Label
import jxl.write.WritableSheet
import jxl.write.WritableWorkbook
import jxl.write.WriteException
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.HashMap
import java.util.Random
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.widget.EditText
import java.text.ParseException
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : AppCompatActivity(), DataClient.OnDataChangedListener {
    private lateinit var gyroXTextView: TextView
    private lateinit var gyroYTextView: TextView
    private lateinit var gyroZTextView: TextView
    private lateinit var gyroDataListLengthTextView: TextView

    private val dataClient: DataClient by lazy {
        Wearable.getDataClient(this)
    }

    private lateinit var checkConnectionButton: Button
    private lateinit var stopButton: Button
    private lateinit var selectLocationButton: Button
    private lateinit var selectLabelButton: Button
    private lateinit var startButton: Button
    private val colorRange = 200

    private var currentLabel: String? = null

    private var soundPool: SoundPool? = null
    private var soundMap: HashMap<String, Int>? = null
    private lateinit var vibrator: Vibrator

    private var isRecording = false
    private var gyroDataList: ArrayList<String> = ArrayList()

    private val PERMISSION_REQUEST_CODE = 12345

    private lateinit var savePathTextView: TextView
    private lateinit var labelTextView: TextView


    private var currentColor = Color.parseColor("#7895CB")
    private var targetColor = Color.parseColor("#A04EF6")
    private var currentStep = 0

    // MyGLRenderer 인스턴스
    //private lateinit var renderer: MyGLRenderer
    //private lateinit var glSurfaceView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // SoundPool 초기화
        initSoundPool()

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // GLSurfaceView 및 Renderer 초기화
        //glSurfaceView = findViewById(R.id.glSurfaceView)
        //renderer = MyGLRenderer()
        //glSurfaceView.setEGLContextClientVersion(2)
        //glSurfaceView.setRenderer(renderer)

        gyroXTextView = findViewById(R.id.gyroXTextView)
        gyroYTextView = findViewById(R.id.gyroYTextView)
        gyroZTextView = findViewById(R.id.gyroZTextView)
        labelTextView = findViewById(R.id.labelTextView)

        checkConnectionButton = findViewById(R.id.checkConnectionButton)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        selectLocationButton = findViewById(R.id.selectLocationButton)
        selectLabelButton = findViewById(R.id.selectLabelButton)

        // Add this line to initialize savePathTextView
        savePathTextView = findViewById(R.id.savePathTextView)

        gyroDataListLengthTextView = findViewById(R.id.gyroDataListLengthTextView)

        selectLocationButton.setOnClickListener {
            // 저장 위치 선택 다이얼로그 표시
            showLocationSelectionDialog()
        }

        startButton.setOnClickListener {
            // Start 버튼 클릭 시 녹음 시작
            startRecording()
        }

        stopButton.setOnClickListener {
            // Stop 버튼 클릭 시 녹음 정지 및 데이터 저장
            stopRecordingAndSave()
        }

        checkConnectionButton.setOnClickListener {
            // 연결 상태 확인
            checkWearableConnection()
        }

        selectLabelButton.setOnClickListener {
            showLabelInputDialog()
        }

        // 필요한 권한 체크 및 요청
        checkAndRequestPermissions()
    }

    private fun startRecording() {
        if(isRecording == false){
            startButton.isEnabled = false
            stopButton.isEnabled = true

            val backgroundColor = ContextCompat.getColor(this, R.color.colorButtonEnable)
            startButton.setBackgroundColor(backgroundColor)
            val backgroundColor2 = ContextCompat.getColor(this, R.color.colorButtonBackground2)
            stopButton.setBackgroundColor(backgroundColor2)

            isRecording = true
            val message = "기록을 시작합니다."
            showToast(message)
            updateGyroDataListLengthText()
            playSound("start")
        }
        else{
            val message = "이미 기록중입니다."
            showToast(message)
        }
    }

    private fun stopRecording() {
        startButton.isEnabled = true
        stopButton.isEnabled = false
        val backgroundColor = ContextCompat.getColor(this, R.color.colorButtonBackground2)
        startButton.setBackgroundColor(backgroundColor)
        val backgroundColor2 = ContextCompat.getColor(this, R.color.colorButtonEnable)
        stopButton.setBackgroundColor(backgroundColor2)
        isRecording = false
        updateGyroDataListLengthText()
        playSound("stop")
    }

    private fun stopRecordingAndSave() {
        if (isRecording) {
            stopRecording()

            if (gyroDataList.isNotEmpty()) {
                saveGyroDataToExcel(gyroDataList, isInternalStorageSelected)
                gyroDataList.clear()
            } else {
                val message = "기록된 데이터가 없습니다"
                showToast(message)
                Log.d("AndroidApp", "저장된 데이터가 없습니다")
            }
        }
        else{
            val message = "기록중이 아닙니다."
            showToast(message)
        }
    }

    private var isInternalStorageSelected = true

    private fun showLabelInputDialog() {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("레이블 설정")
        val input = EditText(this)
        input.hint = "레이블을 입력하세요" // 입력 필드 안내 메시지
        alertDialog.setView(input)

        alertDialog.setPositiveButton("확인") { dialog, _ ->
            val label = input.text.toString()
            if (label.isNotEmpty()) {
                // 사용자가 레이블을 입력한 경우
                currentLabel = label
                showToast("레이블이 설정되었습니다.")

                labelTextView.text = "레이블: $currentLabel"
            } else {
                showToast("레이블을 입력하세요.")
            }
            dialog.dismiss()
        }

        alertDialog.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss()
        }

        alertDialog.show()
    }

    // gyroDataList에 저장된 데이터를 Excel 파일에 저장
    private fun saveGyroDataToExcel(dataList: ArrayList<String>, isInternalStorage: Boolean) {
        val timestampFormat = SimpleDateFormat("yyMMdd_HHmmssSSS")
        val timestamp = timestampFormat.format(Date())

        val folderName = "GyroData"
        val fileName = "$timestamp.xls"

        val storageType = if (isInternalStorage) {
            Environment.DIRECTORY_DOCUMENTS
        } else {
            Environment.DIRECTORY_DOWNLOADS
        }

        val storageDir = if (isInternalStorage) {
            getExternalFilesDir(storageType)
        } else {
            Environment.getExternalStoragePublicDirectory(storageType)
        }

        val folder = File(storageDir, folderName)

        if (!folder.exists()) {
            folder.mkdirs()
        }

        val filePath = File(folder, fileName)

        try {
            val writableWorkbook: WritableWorkbook = Workbook.createWorkbook(filePath)
            val sheet: WritableSheet = writableWorkbook.createSheet("자이로스코프 데이터", 0)

            sheet.addCell(Label(0, 0, "Time"))
            sheet.addCell(Label(1, 0, "GyroX"))
            sheet.addCell(Label(2, 0, "GyroY"))
            sheet.addCell(Label(3, 0, "GyroZ"))

            // 레이블이 설정되어 있다면 레이블을 추가합니다.
            if (currentLabel != null) {
                sheet.addCell(Label(4, 0, "Label"))
            }

            for (i in 0 until dataList.size) {
                val gyroData = dataList[i].split(" ")
                for (j in gyroData.indices) {
                    try {
                        if (j == 0) {
                            sheet.addCell(Label(j, i + 1, gyroData[j]))
                        } else if (1 <= j && j <= 3) {
                            sheet.addCell(jxl.write.Number(j, i + 1, gyroData[j].toDouble()))
                        } else if (j == 4 && currentLabel != null) {
                            Log.d("ExcelSave", currentLabel.toString())
                            sheet.addCell(Label(j, i + 1, currentLabel.toString()))
                        }

                    } catch (e: WriteException) {
                        e.printStackTrace()
                    }
                }
            }

            writableWorkbook.write()
            writableWorkbook.close()
            val message = "데이터가 Excel 파일에 저장되었습니다: ${filePath.absolutePath}"
            showToast(message)
            Log.d("ExcelSave", message)
        } catch (e: IOException) {
            e.printStackTrace()
            val errorMessage = "Excel 파일에 데이터를 저장하는 중 오류가 발생했습니다."
            showToast(errorMessage)
            Log.e("ExcelSave", errorMessage)
        }
    }



    private fun showLocationSelectionDialog() {
        val options = arrayOf("내부 저장소", "외부 저장소")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("저장 위치 선택")
        builder.setItems(options) { dialog: DialogInterface, which: Int ->
            when (which) {
                0 -> {
                    isInternalStorageSelected = true
                }
                1 -> {
                    isInternalStorageSelected = false
                }
            }
            updateSavePathText()
            dialog.dismiss()
        }
        builder.show()
    }

    private fun checkAndRequestPermissions() {
        val bluetoothPermission = Manifest.permission.BLUETOOTH
        val bluetoothAdminPermission = Manifest.permission.BLUETOOTH_ADMIN
        val wakeLockPermission = Manifest.permission.WAKE_LOCK
        val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE

        val permissionsToRequest = ArrayList<String>()

        if (ContextCompat.checkSelfPermission(this, storagePermission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(storagePermission), PERMISSION_REQUEST_CODE)
        }

        if (ContextCompat.checkSelfPermission(this, bluetoothPermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(bluetoothPermission)
        }

        if (ContextCompat.checkSelfPermission(this, bluetoothAdminPermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(bluetoothAdminPermission)
        }

        if (ContextCompat.checkSelfPermission(this, wakeLockPermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(wakeLockPermission)
        }

        val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, locationPermission) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(locationPermission)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun checkWearableConnection() {
        Wearable.getNodeClient(this).connectedNodes.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val nodes: List<Node> = task.result!!
                if (nodes.isNotEmpty()) {
                    Log.d("WearableApp", "Wearable device is connected.")
                    showToast("워치와 연결되어 있습니다.")
                } else {
                    Log.d("WearableApp", "Wearable device is not connected.")
                    showToast("워치와 연결되어 있지 않습니다.")
                }
            } else {
                val errorMessage = task.exception?.message ?: "Unknown error"
                Log.e("WearableApp", "Error checking wearable connection: $errorMessage")
                showToast("Error checking wearable connection: $errorMessage")
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isInternalStorageSelected", isInternalStorageSelected)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        dataClient.addListener(this)
    }

    override fun onPause() {
        super.onPause()
        dataClient.removeListener(this)
    }

    override fun onDataChanged(dataEventBuffer: DataEventBuffer) {
        for (event in dataEventBuffer) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/gyro_data") {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val dataMap = dataMapItem.dataMap
                val gyroX = dataMap.getFloat("gyro_x")
                val gyroY = dataMap.getFloat("gyro_y")
                val gyroZ = dataMap.getFloat("gyro_z")

                runOnUiThread {
                    gyroXTextView.text = "Gyro X: $gyroX"
                    gyroYTextView.text = "Gyro Y: $gyroY"
                    gyroZTextView.text = "Gyro Z: $gyroZ"
                }
                //Log.d("AndroidApp", "Received gyro data: X=$gyroX, Y=$gyroY, Z=$gyroZ")
                //renderer.setRotationAngles(gyroX, gyroY, gyroZ)

                if (isRecording) {
                    val timestampFormat = SimpleDateFormat("HH:mm:ss.SSS")
                    val currentTime = timestampFormat.format(Date())
                    val gyroData = "$currentTime $gyroX $gyroY $gyroZ"
                    gyroDataList.add(gyroData)
                    updateGyroDataListLengthText()
                }
            }
        }
    }

    private fun updateGyroDataListLengthText() {
        val gyroDataListLength = gyroDataList.size
        val recordingStatus = if (isRecording) "기록 중" else "기록 완료"

        // 변화할 단계 수 (예: 100개당 한 번 색상 변경)
        val numSteps = 100

        if (currentStep < numSteps) {
            // 현재 단계에서의 R, G, B 값을 계산
            val currentR = Color.red(currentColor) + (Color.red(targetColor) - Color.red(currentColor)) * currentStep / numSteps
            val currentG = Color.green(currentColor) + (Color.green(targetColor) - Color.green(currentColor)) * currentStep / numSteps
            val currentB = Color.blue(currentColor) + (Color.blue(targetColor) - Color.blue(currentColor)) * currentStep / numSteps

            // 현재 색상 설정
            currentColor = Color.rgb(currentR.toInt(), currentG.toInt(), currentB.toInt())
            gyroDataListLengthTextView.setTextColor(currentColor)

            // 데이터 포인트 간의 시간 간격 계산 (단위: 초)
            //val timeInterval = calculateTimeInterval()

            // 주파수 계산 (주파수 = 데이터 포인트 개수 / 시간 간격)
            //val frequency = gyroDataListLength.toDouble() / timeInterval
            //gyroDataListLengthTextView.text = "$gyroDataListLength 번째 줄 $recordingStatus\n주파수: ${"%.2f".format(frequency)} Hz"

            gyroDataListLengthTextView.text = "$gyroDataListLength 번째 줄 $recordingStatus"


            currentStep++
        } else {
            // 목표 색상에 도달하면 시작 색상을 목표 색상으로 변경
            currentColor = targetColor

            // 목표 색상을 랜덤한 색상으로 변경
            targetColor = getRandomColor(currentColor)

            // 다음 단계를 위해 currentStep 초기화
            currentStep = 0
        }
    }

    private fun getRandomColor(currentColor: Int): Int {
        val random = Random()
        val red = Color.red(currentColor)
        val green = Color.green(currentColor)
        val blue = Color.blue(currentColor)

        val newRed = (red + random.nextInt(2 * colorRange + 1) - colorRange).coerceIn(0, 255)
        val newGreen = (green + random.nextInt(2 * colorRange + 1) - colorRange).coerceIn(0, 255)
        val newBlue = (blue + random.nextInt(2 * colorRange + 1) - colorRange).coerceIn(0, 255)

        return Color.rgb(newRed, newGreen, newBlue)
    }

    private fun calculateTimeInterval(): Double {
        // 데이터 포인트 간의 시간 간격 계산 (단위: 밀리초)
        if (gyroDataList.size >= 2) {
            val dateFormat = SimpleDateFormat("HH:mm:ss.SSS")
            val firstDataTimeString = gyroDataList.first().split(" ")[0]
            val lastDataTimeString = gyroDataList.last().split(" ")[0]
            try {
                val firstTime = dateFormat.parse(firstDataTimeString)
                val lastTime = dateFormat.parse(lastDataTimeString)
                val timeDiff = lastTime.time - firstTime.time
                // Convert milliseconds to seconds
                return (timeDiff / 1000.0)
            } catch (e: ParseException) {
                e.printStackTrace()
            }
        }
        return 0.0
    }



    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundMap = HashMap()
        soundMap?.put("start", soundPool?.load(this, R.raw.start, 1)!!)
        soundMap?.put("stop", soundPool?.load(this, R.raw.stop, 1)!!)
    }

    private fun playSound(soundFileName: String) {
        // 진동 모드인지 확인
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            // 휴대폰이 진동 모드일 때 진동 실행
            vibrator.vibrate(VibrationEffect.createOneShot(500, 100))
        } else {
            val soundId = soundMap?.get(soundFileName)
            soundId?.let {
                soundPool?.play(it, 1.0f, 1.0f, 1, 0, 1.0f)
            }
            vibrator.vibrate(VibrationEffect.createOneShot(100, 100))
        }
    }

    private fun updateSavePathText() {
        val storageTypeText = if (isInternalStorageSelected) {
            "내부 저장소"
        } else {
            "외부 저장소"
        }

        val folderName = "GyroData"
        val fileName = "gyro_data.xls"

        val storageType = if (isInternalStorageSelected) {
            Environment.DIRECTORY_DOCUMENTS // 내부 저장소
        } else {
            Environment.DIRECTORY_DOWNLOADS // 외부 저장소
        }

        val storageDir = if (isInternalStorageSelected) {
            getExternalFilesDir(storageType)
        } else {
            Environment.getExternalStoragePublicDirectory(storageType)
        }

        val folder = File(storageDir, folderName)
        val filePath = File(folder, fileName)

        val selectedLocationText = filePath.absolutePath

        // savePathTextView의 텍스트를 파일 경로로 업데이트
        savePathTextView.text = "저장 경로: $selectedLocationText"
    }
}