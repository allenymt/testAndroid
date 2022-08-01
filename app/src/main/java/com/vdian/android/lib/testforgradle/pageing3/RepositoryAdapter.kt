package com.vdian.android.lib.testforgradle.pageing3

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.vdian.android.lib.testforgradle.R

/**
 * @author yulun
 * @since 2022-07-26 20:02
 */
class RepositoryAdapter(private val context: Context): PagingDataAdapter<PagingRepositoryItem, RepositoryAdapter.ViewHolder>(COMPARATOR) {

    companion object{
        private val COMPARATOR = object : DiffUtil.ItemCallback<PagingRepositoryItem>() {
            override fun areItemsTheSame(oldItem: PagingRepositoryItem, newItem: PagingRepositoryItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: PagingRepositoryItem, newItem: PagingRepositoryItem): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        item?.let {
            holder.tvName.text = it.name
            holder.tvDesc.text = it.description
            holder.tvStar.text = it.stargazersCount.toString()
        }

        holder.llItem.setOnClickListener {

//            val intent = Intent(context,CommonWebActivity::class.java)
//            intent.putExtra("url",item?.htmlUrl)
//            context.startActivity(intent)

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.paging_item_repository, parent, false)
        return ViewHolder(view)
    }


    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val tvName: TextView = itemView.findViewById(R.id.tv_name)
        val tvDesc: TextView = itemView.findViewById(R.id.tv_desc)
        val tvStar: TextView = itemView.findViewById(R.id.tv_star)
        val llItem: LinearLayout = itemView.findViewById(R.id.ll_item)
    }
}