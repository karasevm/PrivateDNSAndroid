package ru.karasevm.privatednstoggle

import android.app.Dialog
import android.content.Intent
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

class DNSServerDialogFragment : DialogFragment() {

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
            if (items[0] == "") {
                items.removeAt(0)
                items.add("dns.google")
            }

            items.add(0, resources.getString(R.string.dns_auto))
            items.add(0, resources.getString(R.string.dns_off))

            adapter = RecyclerAdapter(items, false) {}
            binding.recyclerView.adapter = adapter


            val startIntent = Intent(context, MainActivity::class.java)

            builder.setTitle(R.string.select_server)
                .setView(binding.root)
                .setPositiveButton(
                    R.string.done
                ) { _, _ ->
                    dialog?.dismiss()
                }
                .setNeutralButton(R.string.open_app) { _, _ -> context?.startActivity(startIntent) }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    override fun onStart() {
        super.onStart()

        adapter.onItemClick = { position ->
            when (position) {
                0 -> {
                    PrivateDNSUtils.setPrivateMode(
                        requireActivity().contentResolver,
                        PrivateDNSUtils.DNS_MODE_OFF
                    )
                    Toast.makeText(context, R.string.set_to_off_toast, Toast.LENGTH_SHORT).show()
                }

                1 -> {
                    PrivateDNSUtils.setPrivateMode(
                        requireActivity().contentResolver,
                        PrivateDNSUtils.DNS_MODE_AUTO
                    )
                    Toast.makeText(context, R.string.set_to_auto_toast, Toast.LENGTH_SHORT).show()
                }

                else -> {
                    val server = items[position].split(" : ").last()
                    PrivateDNSUtils.setPrivateMode(
                        requireActivity().contentResolver,
                        PrivateDNSUtils.DNS_MODE_PRIVATE
                    )
                    PrivateDNSUtils.setPrivateProvider(
                        requireActivity().contentResolver,
                        server
                    )
                    Toast.makeText(
                        context,
                        getString(R.string.set_to_provider_toast, server),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            dialog?.dismiss()
            requireContext().sendBroadcast(Intent("refresh_tile").setPackage(requireContext().packageName))
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