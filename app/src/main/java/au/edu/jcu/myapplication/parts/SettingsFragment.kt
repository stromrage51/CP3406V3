package au.edu.jcu.myapplication.parts

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import au.edu.jcu.myapplication.LoginScreen
import au.edu.jcu.myapplication.R
import androidx.navigation.fragment.findNavController

import au.edu.jcu.myapplication.databinding.FragmentProfileBinding
import au.edu.jcu.myapplication.ui.AppColors


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val colorMap = mapOf(
        "White" to Color.WHITE,
        "Red" to Color.RED,
        "Green" to Color.GREEN,
        "Blue" to Color.BLUE,
        "Yellow" to Color.YELLOW
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val view = binding.root

        val sharedPreferences = requireContext().
        getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        val fontSizeSlider = binding.fontSizeSlider
        val bgSpinner = binding.bgColorSpinner
        val fontColorSpinner = binding.fontColorSpinner

        //load saved settings
        val savedFontSize = sharedPreferences.getInt("fontSize", 16)
        val savedBgColor = sharedPreferences.getString("bgColor", "White") ?: "White"
        val savedFontColor = sharedPreferences.getString("fontColor", "Black") ?: "Black"


        // set initial UI
        binding.fontSizeSlider.progress = savedFontSize
        binding.bgColorSpinner.setSelection(getSpinnerIndex(binding.bgColorSpinner, savedBgColor))
        binding.fontColorSpinner.setSelection(getSpinnerIndex(binding.fontColorSpinner, savedFontColor))
        applySettings(savedFontSize, savedBgColor, savedFontColor)


        //apply saved settings
        applySettings(savedFontSize, savedBgColor, savedFontColor)

        //live updates
        fontSizeSlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applySettings(progress, bgSpinner.selectedItem.toString(), fontColorSpinner.selectedItem.toString())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val onColorChange = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applySettings(
                    binding.fontSizeSlider.progress,
                    binding.bgColorSpinner.selectedItem.toString(),
                    binding.fontColorSpinner.selectedItem.toString()
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        bgSpinner.onItemSelectedListener = onColorChange
        fontColorSpinner.onItemSelectedListener = onColorChange

        binding.saveSettingsButton.setOnClickListener {
            sharedPreferences.edit().apply {
                putInt("fontSize", fontSizeSlider.progress)
                putString("bgColor", bgSpinner.selectedItem.toString())
                putString("fontColor", fontColorSpinner.selectedItem.toString())
                apply()
            }
            Toast.makeText(requireContext(), "Settings Saved", Toast.LENGTH_SHORT).show()
        }

        binding.logButtonButton.setOnClickListener {
            startActivity(Intent(requireContext(), LoginScreen::class.java))
            requireActivity().finish()
        }

        return view
    }



    private fun applySettings(fontSize: Int, bgColor: String, fontColor: String) {
        val root = binding.root
        root.setBackgroundColor(AppColors.map[bgColor] ?: Color.WHITE)
        setTextViewAppearance(root, fontSize, fontColor)
    }

    private fun setTextViewAppearance(view: View, size: Int, colorName: String) {
        val color = AppColors.map[colorName] ?: Color.BLACK
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setTextViewAppearance(view.getChildAt(i), size, colorName)
            }
        } else if (view is TextView) {
            view.textSize = size.toFloat()
            view.setTextColor(color)
        }
    }


    private fun getSpinnerIndex(spinner: Spinner, value: String): Int {
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString().equals(value, ignoreCase = true)) {
                return i
            }
        }
        return 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
