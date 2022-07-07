package com.example.assignment


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.recyclerview.widget.RecyclerView
import com.example.assignment.databinding.CardviewLayoutBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter


class MyAdapter() : ListAdapter<Data, MyAdapter.MyViewHolder>(object : DiffUtil.ItemCallback<Data>() {
    override fun areItemsTheSame(oldItem: Data, newItem: Data): Boolean {
        return oldItem.cointitle == newItem.cointitle
    }

    override fun areContentsTheSame(oldItem: Data, newItem: Data): Boolean {
        return oldItem.date == newItem.date
    }
})

    {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return MyViewHolder(CardviewLayoutBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.setOnClickListener {
            itemClickListener.onClick(it,position)
        }

        with(holder.binding) {
            itemCointitle.text = "${item.cointitle}"
            itemOpeningPrice.text = "시가: ${item.opening_price} 원"
            itemClosingPrice.text = "현재가: ${item.closing_price} 원"
            itemUnitsTraded24H.text = "거래량: ${item.units_traded_24H}"
        }
    }

    interface onItemClickListener{
        fun onClick(v: View, position: Int)
    }
    fun setItemClickListener(onItemClickListener: onItemClickListener){
        this.itemClickListener = onItemClickListener
    }
    private lateinit var itemClickListener: onItemClickListener
    class MyViewHolder(val binding: CardviewLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    // ViewHolder 의 생성자 매개변수로 binding 을 받도록 함
    // RecyclerView.ViewHolder 의 생성자 매개변수는 View 이므로 Binding 의 root 를 넘겨줌

}
