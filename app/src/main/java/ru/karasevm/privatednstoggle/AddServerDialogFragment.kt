package ru.karasevm.privatednstoggle

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.common.net.InternetDomainName
import ru.karasevm.privatednstoggle.databinding.DialogAddBinding


class AddServerDialogFragment : DialogFragment() {
    // Use this instance of the interface to deliver action events
    private lateinit var listener: NoticeDialogListener

    private var _binding: DialogAddBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface NoticeDialogListener {
        fun onDialogPositiveClick(label: String? ,server: String)
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as NoticeDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException(
                (context.toString() +
                        " must implement NoticeDialogListener")
            )
        }
    }

    override fun onCreateDialog(
        savedInstanceState: Bundle?
    ): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater
            _binding = DialogAddBinding.inflate(inflater)

            val view = binding.root
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            builder.setTitle(R.string.add_server)
                .setView(view)
                // Add action buttons
                .setPositiveButton(
                    R.string.menu_add
                ) { _, _ ->
                    listener.onDialogPositiveClick(
                        binding.editTextServerHint.text.toString().trim(),
                        binding.editTextServerAddr.text.toString().trim()
                    )
                }
                .setNegativeButton(
                    R.string.cancel
                ) { _, _ ->
                    dialog?.cancel()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onStart() {
        super.onStart()
        val button = ((dialog) as AlertDialog).getButton(DialogInterface.BUTTON_POSITIVE)
        binding.editTextServerAddr.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val server = binding.editTextServerAddr.text.toString().trim()
                if (TextUtils.isEmpty(server) || !isValidServer(server)) {
                    button.isEnabled = false
                } else {
                    binding.editTextServerAddr.error = null
                    button.isEnabled = true
                }
            }
        })
    }

    private fun isValidServer(str: String): Boolean {
        return InternetDomainName.isValid(str)
    }

}