package com.example.testservicecomponentandroid

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.testservicecomponentandroid.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var focusService: FocusService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as FocusService.LocalBinder
            focusService = binder.getService()
            isBound = true
            observeServiceState()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            focusService = null
            isBound = false
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Permissão de notificação concedida!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Sem permissão, você não verá o cronômetro na barra de status.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkNotificationPermission()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, FocusService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    private fun setupClickListeners() {
        binding.btnStart.setOnClickListener {
            sendCommandToService(FocusService.ACTION_START)
        }

        binding.btnPause.setOnClickListener {
            sendCommandToService(FocusService.ACTION_PAUSE)
        }

        binding.btnStop.setOnClickListener {
            sendCommandToService(FocusService.ACTION_STOP)
        }
    }

    private fun sendCommandToService(action: String) {
        val intent = Intent(this, FocusService::class.java).apply {
            this.action = action
        }
        startService(intent)
    }

    private fun observeServiceState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    focusService?.secondsRemaining?.collect { seconds ->
                        updateTimerText(seconds)
                    }
                }

                launch {
                    focusService?.timerState?.collect { state ->
                        updateUiState(state)
                    }
                }
            }
        }
    }

    private fun updateTimerText(totalSeconds: Int) {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        binding.tvTimer.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateUiState(state: FocusService.TimerState) {
        when (state) {
            FocusService.TimerState.STOPPED -> {
                binding.tvState.text = "Pronto para focar"
                binding.btnStart.isEnabled = true
                binding.btnPause.isEnabled = false
                binding.btnStop.isEnabled = false
                updateTimerText(FocusService.INITIAL_TIME)
            }
            FocusService.TimerState.RUNNING -> {
                binding.tvState.text = "Foco ativo!"
                binding.btnStart.isEnabled = false
                binding.btnPause.isEnabled = true
                binding.btnStop.isEnabled = true
            }
            FocusService.TimerState.PAUSED -> {
                binding.tvState.text = "Pausado"
                binding.btnStart.isEnabled = true
                binding.btnPause.isEnabled = false
                binding.btnStop.isEnabled = true
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}