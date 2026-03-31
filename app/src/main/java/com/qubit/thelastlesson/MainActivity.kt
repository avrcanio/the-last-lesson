package com.qubit.thelastlesson

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.qubit.thelastlesson.databinding.ActivityMainBinding
import com.qubit.thelastlesson.game.OutsideSchoolSceneView
import com.qubit.thelastlesson.navigation.AppScreen

class MainActivity : AppCompatActivity(), OutsideSchoolSceneView.SceneListener {
    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private var canEnterSchool = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.outsideScene.sceneListener = this

        showScreen(AppScreen.MainMenu)
        binding.primaryAction.setOnClickListener {
            if (binding.menuPanel.isShown) {
                Log.d(TAG, "Start clicked")
                showScreen(AppScreen.OutsideSchool)
                binding.menuPanel.isEnabled = false
                binding.menuPanel.alpha = 0f
                binding.menuPanel.visibility = android.view.View.GONE
                binding.scenePanel.visibility = android.view.View.VISIBLE
                binding.scenePanel.alpha = 0f
                binding.scenePanel.animate().alpha(1f).setDuration(350L).start()
            }
        }
        binding.enterDoorButton.setOnClickListener {
            if (!canEnterSchool) return@setOnClickListener
            enterSchool()
        }
    }

    private fun showScreen(screen: AppScreen) {
        binding.screenTitle.text = getString(screen.titleRes)
        binding.screenDescription.text = getString(screen.descriptionRes)
        binding.primaryAction.text = getString(screen.primaryActionRes)
    }

    override fun onDoorReached() {
        Toast.makeText(this, R.string.outside_scene_door_hint, Toast.LENGTH_SHORT).show()
    }

    override fun onDoorRangeChanged(inRange: Boolean) {
        canEnterSchool = inRange
        binding.enterDoorButton.visibility = if (inRange) android.view.View.VISIBLE else android.view.View.GONE
        binding.enterDoorButton.isEnabled = inRange
        binding.stormStatus.text = if (inRange) {
            getString(R.string.outside_scene_enter_prompt)
        } else {
            getString(R.string.outside_scene_status)
        }
    }

    override fun onStormLineChanged(line: String) {
        if (!canEnterSchool) {
            binding.stormStatus.text = line
        }
    }

    private fun enterSchool() {
        showScreen(AppScreen.SchoolEntrance)
        binding.scenePanel.animate()
            .alpha(0f)
            .setDuration(250L)
            .withEndAction {
                binding.scenePanel.visibility = android.view.View.GONE
                binding.interiorPanel.visibility = android.view.View.VISIBLE
                binding.interiorPanel.alpha = 0f
                binding.interiorPanel.animate().alpha(1f).setDuration(350L).start()
            }
            .start()
    }
}
