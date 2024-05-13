package com.ohuang.filemanager.ui.home

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView.OnQueryTextListener
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.kennyc.view.MultiStateView
import com.ohuang.filemanager.WebActivity
import com.ohuang.filemanager.bean.FileBean
import com.ohuang.filemanager.config.Http
import com.ohuang.filemanager.config.SpConfig
import com.ohuang.filemanager.databinding.FragmentHomeBinding
import com.ohuang.filemanager.util.ClipboardUtils
import com.ohuang.refresh.OnRefreshListener
import com.ohuang.refresh.Refresh

class HomeFragment : Fragment() {

    private lateinit var homeViewModel: HomeViewModel
    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var mAdapter: RvAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.rvHome.layoutManager = LinearLayoutManager(context)

        homeViewModel.state.observe(
            viewLifecycleOwner
        ) { t ->
            if (t != null) {
                binding.multiStateView.viewState = t
            }
        }
        binding.multiStateView.getView(MultiStateView.ViewState.ERROR)?.setOnClickListener {
            homeViewModel.loadData(homeViewModel.filePath)
        }


        homeViewModel.toastData.observe(
            viewLifecycleOwner
        ) { t -> Toast.makeText(context, t, Toast.LENGTH_SHORT).show() }

        homeViewModel.data.observe(
            viewLifecycleOwner
        ) { t ->
            if (mAdapter == null) {
                mAdapter = context?.let { RvAdapter(it, t) }!!
                binding.rvHome.adapter = mAdapter
                initAdapter()
            } else {
                mAdapter?.mData = t
            }
            binding.rvHome.scrollToPosition(0)
            binding.tvName.setText(homeViewModel.filePath)
            mAdapter?.notifyDataSetChanged()
        }
        binding.ohRefresh.isCanPullUp = false//禁用上拉加载
        binding.ohRefresh.onRefreshListener = object : OnRefreshListener {
            override fun onRefresh(refresh: Refresh?) {
                homeViewModel.loadData(homeViewModel.filePath, true)


            }

            override fun onBottomRefresh(refresh: Refresh?) {
                TODO("Not yet implemented")
            }

        }
        homeViewModel.refreshSate.observe(viewLifecycleOwner) {
            binding.ohRefresh.postDelayed({
                binding.ohRefresh.refreshComplete()
                Toast.makeText(context, "刷新完成", Toast.LENGTH_LONG).show()
            }, 1500)

        }

        homeViewModel.loadData()
        initSearch()

        return root
    }

    private fun initSearch() {
        binding.searchView.isSubmitButtonEnabled = true
        binding.searchView.isIconifiedByDefault = false
        binding.searchView.setOnQueryTextListener(object : OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                homeViewModel.setSearchData(query ?: "")
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (TextUtils.isEmpty(newText)){
                    homeViewModel.setSearchData("")
                }
                return false
            }

        })
    }

    private fun initAdapter() {
        mAdapter?.itemButtonListerner = object : RvAdapter.ItemButtonListerner {
            override fun onClick(position: Int) {
                var fileBean = mAdapter!!.mData[position]
                if (fileBean!!.isFolder) {
                    homeViewModel.loadData(fileBean.name)
                } else {
                    val s = getDownloadUrl(fileBean)
                    ClipboardUtils.copyText(s, context)
                    Toast.makeText(context, "链接已复制到粘贴板", Toast.LENGTH_SHORT).show()
                }

            }

        }
        mAdapter?.itemClickListerner = { position ->

            val fileBean = mAdapter!!.mData[position]
            if (fileBean!!.isFolder) {
                homeViewModel.loadData(fileBean.name)
            } else {
                val s = getDownloadUrl(fileBean)
                WebActivity.start(requireContext(), s)
            }
        }
    }

    private fun getDownloadUrl(fileBean: FileBean): String {
        if(SpConfig.getUseOldDownloadApi(requireContext())) {
            val replace = fileBean.name.replace("/", "%2f")
            val s = Http.Main.Get() + "/?name=$replace"
            return s
        }else{
            val name = if (fileBean.name.startsWith("/")) fileBean.name.substring(1) else fileBean.name
            return Http.Main.files()+"/"+name
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mAdapter = null
    }


    fun goBack(): Boolean {
        if (homeViewModel.filePath.isEmpty() || homeViewModel.filePath == "/") {
            return true
        } else {
            var split = homeViewModel.filePath.split("/")
            var string = StringBuffer()
            for (int in 0 until split.size - 1) {
                if (split[int].isNotEmpty()) {
                    string.append("/").append(split[int])
                }
            }
            homeViewModel.loadData(string.toString())
        }
        return false
    }
}


