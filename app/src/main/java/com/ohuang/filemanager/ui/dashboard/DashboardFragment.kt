package com.ohuang.filemanager.ui.dashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

import com.ohuang.filemanager.MainActivity
import com.ohuang.filemanager.TextActivity
import com.ohuang.filemanager.config.Http
import com.ohuang.filemanager.config.SpConfig
import com.ohuang.filemanager.databinding.FragmentDashboardBinding
import com.ohuang.filemanager.util.NetWorkUtil
import com.ohuang.filemanager.util.SPUtil

class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.btnTest.setOnClickListener {
            binding.tvTest.text = "测试:测试中..."
            dashboardViewModel.TestUrl()
        }
        binding.editUrl.setText(Http.getBaseUrl())
        binding.btnDefault.setOnClickListener {

            binding.editUrl.setText(Http.BaseDefaultUrl)
        }
        dashboardViewModel.text.observe(viewLifecycleOwner,
            { t -> binding.tvTest.text = t })


        binding.btnChange.setOnClickListener {
            val s: String = binding.editUrl.text.toString()
            SpConfig.setUrl(requireContext(), s)
            Http.setBaseUrl(s)
        }
        binding.btnStartMain.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
        }
        binding.btnStartText.setOnClickListener {
            startActivity(Intent(context, TextActivity::class.java))
        }
        binding.btnHtml.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(Http.Main.index())
            startActivity(intent)
        }
        binding.btnUseOldApi.setOnClickListener {
            val useOldApi = !SpConfig.getUseOldDownloadApi(requireContext())
            binding.btnUseOldApi.text = if (useOldApi) "当前为旧api" else "当前为新api"
            SpConfig.setUseOldDownloadApi(requireContext(), useOldApi)
        }
        val useOldDownloadApi = SpConfig.getUseOldDownloadApi(requireContext())
        binding.btnUseOldApi.text = if (useOldDownloadApi) "当前为旧api" else "当前为新api"




        return root
    }

    override fun onResume() {
        super.onResume()
        when (NetWorkUtil.getAPNType(context)) {
            NetWorkUtil.NetType.WIFI -> {
                val ip = NetWorkUtil.getWifiIP(context)
                binding.tvNetwork.text = "当前网络为wifi: ip=$ip"
            }

            NetWorkUtil.NetType.NoneNet -> {
                binding.tvNetwork.text = "当前无网络"
            }

            else -> {
                binding.tvNetwork.text = "当前为数据网络"
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}