package ru.karasevm.privatednstoggle

import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.karasevm.privatednstoggle.databinding.SheetDnsSelectorBinding
import ru.karasevm.privatednstoggle.utils.PreferenceHelper.defaultPreference
import ru.karasevm.privatednstoggle.utils.PreferenceHelper.dns_servers
import ru.karasevm.privatednstoggle.utils.PrivateDNSUtils

class DNSServerDialogFragment: DialogFragment() {

    private var _binding: SheetDnsSelectorBinding? = null
    private val binding get() = _binding!!

    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: RecyclerAdapter
    private var items = mutableListOf<String>()
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreateDialog(
        savedInstanceState: Bundle?
    ): Dialog {
        return activity?.let {
            val builder = MaterialAlertDialogBuilder(it)
            val inflater = requireActivity().layoutInflater
            _binding = SheetDnsSelectorBinding.inflate(inflater)

            linearLayoutManager = LinearLayoutManager(context)
            binding.recyclerView.layoutManager = linearLayoutManager

            sharedPrefs = defaultPreference(requireContext())
            items = sharedPrefs.dns_servers
            if(items[0] == "") {
                items.removeAt(0)
                items.add("dns.google")
            }

            adapter = RecyclerAdapter(items)
            binding.recyclerView.adapter = adapter

            builder.setTitle(R.string.select_server)
                .setView(binding.root)
                .setPositiveButton(R.string.done
                ) { _, _ ->
                    dialog?.dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onStart() {
        super.onStart()
        val dnsMode = PrivateDNSUtils.getPrivateMode(requireActivity().contentResolver)
        binding.autoSwitch.isChecked = dnsMode.lowercase() == "opportunistic"

        adapter.onItemClick = { position ->
            binding.autoSwitch.isChecked = false
            val server = items[position]
            PrivateDNSUtils.setPrivateMode(requireActivity().contentResolver, PrivateDNSUtils.DNS_MODE_PRIVATE)
            PrivateDNSUtils.setPrivateProvider(requireActivity().contentResolver, server)
            Toast.makeText(context, "DNS Server Set", Toast.LENGTH_SHORT).show()
        }

        binding.autoSwitch.setOnClickListener {
            if(binding.autoSwitch.isChecked) {
                PrivateDNSUtils.setPrivateMode(requireActivity().contentResolver, PrivateDNSUtils.DNS_MODE_AUTO)
            } else {
                PrivateDNSUtils.setPrivateMode(requireActivity().contentResolver, PrivateDNSUtils.DNS_MODE_PRIVATE)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activity?.finish()
    }

    companion object {
        const val TAG = "DNSServerDialogFragment"
    }
}