package ru.karasevm.privatednstoggle.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.karasevm.privatednstoggle.R
import ru.karasevm.privatednstoggle.databinding.DialogOptionsBinding
import ru.karasevm.privatednstoggle.util.PreferenceHelper
import ru.karasevm.privatednstoggle.util.PreferenceHelper.autoMode
import ru.karasevm.privatednstoggle.util.PreferenceHelper.requireUnlock
import ru.karasevm.privatednstoggle.util.PrivateDNSUtils

class OptionsDialogFragment : DialogFragment() {
    private var _binding: DialogOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(
        savedInstanceState: Bundle?
    ): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            val inflater = requireActivity().layoutInflater
            _binding = DialogOptionsBinding.inflate(inflater)

            val view = binding.root
            builder.setTitle(R.string.options)
                .setView(view)
                .setPositiveButton(R.string.ok, null)
            builder.create()

        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onStart() {
        super.onStart()
        val sharedPrefs = PreferenceHelper.defaultPreference(requireContext())
        val autoModeOption = sharedPrefs.autoMode
        when (autoModeOption) {
            PrivateDNSUtils.AUTO_MODE_OPTION_OFF -> binding.autoOptionRadioGroup.check(R.id.autoOptionOff)
            PrivateDNSUtils.AUTO_MODE_OPTION_AUTO -> binding.autoOptionRadioGroup.check(R.id.autoOptionAuto)
            PrivateDNSUtils.AUTO_MODE_OPTION_OFF_AUTO -> binding.autoOptionRadioGroup.check(R.id.autoOptionOffAuto)
            PrivateDNSUtils.AUTO_MODE_OPTION_PRIVATE -> binding.autoOptionRadioGroup.check(R.id.autoOptionPrivate)
        }
        binding.autoOptionRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.autoOptionOff -> sharedPrefs.autoMode = PrivateDNSUtils.AUTO_MODE_OPTION_OFF
                R.id.autoOptionAuto -> sharedPrefs.autoMode = PrivateDNSUtils.AUTO_MODE_OPTION_AUTO
                R.id.autoOptionOffAuto -> sharedPrefs.autoMode =
                    PrivateDNSUtils.AUTO_MODE_OPTION_OFF_AUTO

                R.id.autoOptionPrivate -> sharedPrefs.autoMode =
                    PrivateDNSUtils.AUTO_MODE_OPTION_PRIVATE
            }
        }

        val requireUnlock = sharedPrefs.requireUnlock
        binding.requireUnlockSwitch.isChecked = requireUnlock
        binding.requireUnlockSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefs.requireUnlock = isChecked
        }
    }
}