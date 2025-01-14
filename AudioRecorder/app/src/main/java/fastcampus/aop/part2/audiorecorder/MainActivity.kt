 package fastcampus.aop.part2.audiorecorder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AlertDialog

 class MainActivity : AppCompatActivity() {

     private val soundVisualizerView: SoundVisualizerView by lazy {
         findViewById(R.id.soundVisualizerView)
     }
     private val recordTimeTextView: CountUpView by lazy {
         findViewById(R.id.recordTimeTextView)
     }

     private val resetButton: Button by lazy {
         findViewById(R.id.resetButton)
     }
     private val recordButton: RecordButton by lazy {
        findViewById(R.id.recordButton)
     }

     private val requiredPermissions = arrayOf(Manifest.permission.RECORD_AUDIO)
     private val recordingFilePath: String by lazy {
        "${externalCacheDir?.absolutePath}/recording.3gp"
     }
     private var recorder: MediaRecorder? = null
     private var player: MediaPlayer? = null
     private var state = State.BEFORE_RECORDING
        set(value) {
            field = value
            resetButton.isEnabled = (value == State.AFTER_RECORDING || value == State.ON_PLAYING)
            recordButton.updateIconWithState(value)
        }

     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         setContentView(R.layout.activity_main)

         requestAudioPermission()

         initViews()
         bindViews()
         initVariables()
     }

     override fun onRequestPermissionsResult(
         requestCode: Int,
         permissions: Array<out String>,
         grantResults: IntArray
     ) {
         super.onRequestPermissionsResult(requestCode, permissions, grantResults)

         val audioRecordPermissionGranted = requestCode == REQUEST_RECORD_AUDIO_PERMISSION && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED

         if (!audioRecordPermissionGranted) {
             if (!shouldShowRequestPermissionRationale(permissions.first())) {
                 showPermissionExplanationDialog()
             } else {
                 finish()
             }
         }
     }

     private fun requestAudioPermission() {
        requestPermissions(requiredPermissions, REQUEST_RECORD_AUDIO_PERMISSION)
     }

     private fun initViews() {
        recordButton.updateIconWithState(state)
     }

     private fun bindViews() {
         soundVisualizerView.onRequestCurrentAmplitude = {
             recorder?.maxAmplitude ?: 0
         }

         resetButton.setOnClickListener {
             stopPlaying()
             soundVisualizerView.clearVisualization()
             recordTimeTextView.clearCountTime()
             state = State.BEFORE_RECORDING
         }

         recordButton.setOnClickListener {
             when(state) {
                 State.BEFORE_RECORDING -> {
                     startRecording()
                 }
                 State.ON_RECORDING -> {
                     stopRecording()
                 }
                 State.AFTER_RECORDING -> {
                     startPlaying()
                 }
                 State.ON_PLAYING -> {
                     stopPlaying()
                 }
             }
         }
     }

     private fun initVariables() {
         state = State.BEFORE_RECORDING
     }

     private fun startRecording() {
         recorder = MediaRecorder()
             .apply {
                 setAudioSource(MediaRecorder.AudioSource.MIC)
                 setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                 setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                 setOutputFile(recordingFilePath)
                 prepare()
             }
         recorder?.start()
         soundVisualizerView.startVisualizing(false)
         recordTimeTextView.startCountUp()
         state = State.ON_RECORDING
     }

     private fun stopRecording() {
         recorder?.run {
             stop()
             release()
         }
         recorder = null
         soundVisualizerView.stopVisualizing()
         recordTimeTextView.stopCountUp()
         state = State.AFTER_RECORDING
     }

     private fun startPlaying() {
         player = MediaPlayer().apply {
             setDataSource(recordingFilePath)
             prepare()
         }
         player?.setOnCompletionListener {
             stopPlaying()
             state = State.AFTER_RECORDING
         }
         player?.start()
         soundVisualizerView.startVisualizing(true)
         recordTimeTextView.startCountUp()
         state = State.ON_PLAYING
     }

     private fun stopPlaying() {
         player?.release()
         player = null
         soundVisualizerView.stopVisualizing()
         recordTimeTextView.stopCountUp()
         state = State.AFTER_RECORDING
     }

     private fun showPermissionExplanationDialog() {
         AlertDialog.Builder(this)
             .setMessage("녹음 권한을 켜주셔야지 앱을 정상적으로 사용할 수 있습니다. 앱 설정 화면으로 진입하셔서 권한을 켜주세요.")
             .setPositiveButton("권한 변경하러 가기") { _, _ -> navigateToAppSetting() }
             .setNegativeButton("앱 종료하기") { _, _ -> finish() }
             .show()
     }

     private fun navigateToAppSetting() {
         val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
         val uri: Uri = Uri.fromParts("package", packageName, null)
         intent.data = uri
         startActivity(intent)
     }

     companion object {
         private const val REQUEST_RECORD_AUDIO_PERMISSION = 201
      }
}