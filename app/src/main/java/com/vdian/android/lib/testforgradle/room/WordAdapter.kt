package com.vdian.android.lib.testforgradle.room

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vdian.android.lib.testforgradle.R

/**
 * @author yulun
 * @since 2022-07-25 16:44
 */
class WordAdapter(context: Context, list: MutableList<WordEntity>): RecyclerView.Adapter<WordAdapter.ViewHolder>() {

    private var mContext: Context = context
    private var mList: MutableList<WordEntity> = list
    private lateinit var wordDeleteListener:WordDeleteListener
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(R.layout.room_recyclerview_item,parent,false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mList[position]
        holder.tvWord.text = "${item.word}===${item.content ?: "content empty"}"
        holder.llItem.setOnClickListener {
            wordDeleteListener.delete(item.id)
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvWord: TextView = itemView.findViewById(R.id.tv_word)
        var llItem: LinearLayout = itemView.findViewById(R.id.ll_item)
    }

    fun setWordDeleteListener(wordDeleteListener: WordDeleteListener){
        this.wordDeleteListener = wordDeleteListener
    }

    interface WordDeleteListener{
        fun delete(id: Long)
    }
}