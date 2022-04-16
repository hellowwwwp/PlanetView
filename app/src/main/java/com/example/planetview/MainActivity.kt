package com.example.planetview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.planetview.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val viewBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val planetView: PlanetView
        get() = viewBinding.planetView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        with(viewBinding) {
            root.post {
                planetView.start()
            }
            startBtn.setOnClickListener {
                planetView.start()
            }
            pauseBtn.setOnClickListener {
                planetView.pause()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        planetView.resume()
    }

    override fun onPause() {
        super.onPause()
        planetView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        planetView.stop()
    }
}