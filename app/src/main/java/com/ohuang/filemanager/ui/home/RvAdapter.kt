package com.ohuang.filemanager.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.ohuang.filemanager.R
import com.ohuang.filemanager.bean.FileBean
import com.ohuang.filemanager.config.Http
import com.ohuang.filemanager.databinding.ItemFileListBinding
import com.ohuang.filemanager.util.ClipboardUtils


class RvAdapter(mcontext: Context, mdata: List<FileBean?>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val context = mcontext
    var mData = mdata
    var itemButtonListerner:ItemButtonListerner?=null




    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return MyHolder(LayoutInflater.from(context).inflate(R.layout.item_file_list,parent,false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
         if (holder is MyHolder){
             holder.item.data=mData[position]
             if (mData[position]!!.isFolder){
                 holder.item.ivList.setImageDrawable(context.getDrawable(R.mipmap.icon_folder))
             }else{
                 holder.item.ivList.setImageDrawable(context.getDrawable(R.mipmap.icon_copy))
             }
             holder.item.ivList.setOnClickListener {
                 itemButtonListerner?.onClick(position)
             }

         }
    }

    override fun getItemCount(): Int {
        return mData.size
    }

    class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var item: ItemFileListBinding = ItemFileListBinding.bind(itemView)

    }

    interface ItemButtonListerner{
        fun onClick(position: Int)
    }
}