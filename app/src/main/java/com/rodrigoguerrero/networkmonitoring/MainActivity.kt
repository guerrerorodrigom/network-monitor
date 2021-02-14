package com.rodrigoguerrero.networkmonitoring

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.rodrigoguerrero.networkmonitoring.databinding.ActivityMainBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect

class MainActivity : AppCompatActivity() {

    private val networkMonitor = NetworkMonitor(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycle.addObserver(networkMonitor)

        networkMonitor.networkAvailableStateFlow.asLiveData().observe(this) {
            val message = when (it) {
                NetworkState.Available -> "available"
                NetworkState.Unavailable -> "unavailable"
            }
            Snackbar.make(binding.root, "Network is $message.", Snackbar.LENGTH_SHORT).show()
        }
    }
}