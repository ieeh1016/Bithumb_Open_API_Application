package com.example.assignment


import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.example.assignment.databinding.FragmentListBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat


class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    private var _item: Data? = arguments?.getParcelable("item")
    private val item get() = _item!!

    private val adapter by lazy {
        MyListAdapter().apply {
            setItemClickListener(object : MyListAdapter.onItemClickListener {
                override fun onClick(item: Data) {
                    val fragment3 = DetailFragment()
                    val bundle = Bundle()
                    bundle.putParcelable("item",item)
                    fragment3.arguments = bundle
                    val transaction: FragmentTransaction = requireActivity().supportFragmentManager.beginTransaction().replace(R.id.fragment_main, fragment3)
                    transaction.addToBackStack(null).commit()


                }
            })
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment

        _binding = FragmentListBinding.inflate(inflater,container,false)
        _item = arguments?.getParcelable("item")
        return binding.root
    }

    private fun backFragment(){
        activity?.supportFragmentManager!!.beginTransaction().remove(this).commit()
        activity?.supportFragmentManager!!.popBackStack()

    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val cointitle_bundle = item.cointitle
        binding.listCointitle.text = "$cointitle_bundle"
        binding.backButton.setOnClickListener {
            backFragment()
        }
        db = Room.databaseBuilder(
            requireActivity().applicationContext,
            AppDatabase::class.java,
            "DataDB104"
        ).build()
        with(binding) {
            recyclerviewList.apply {
                layoutManager = LinearLayoutManager(this@ListFragment.context) // Recycler view layout manager ??????
                setHasFixedSize(true)
                adapter = this@ListFragment.adapter
            }

            // refresh ?????? ????????? ??????
            refleshButton.setOnClickListener {
                lifecycleScope.launch {
                    //???????????? ????????? ????????? ???????????? Insert?????? UI??? ????????? ?????? ?????? chart??? ????????????.
                    insertRefresh()
                    chart()
                }
            }
        }
        lifecycleScope.launch {
            uiRefresh() // room??? ????????? ???????????? edit??? ????????? query??? ????????? ?????????????????? UI??? ???????????????.
            chart()
        }
    }


    private fun covertTimestampToDate(timestamp: String): String {
        val sdf = SimpleDateFormat("yyyy.MM.dd / hh:mm:ss")
        val date = sdf.format(timestamp.toLong())
        return date.toString()
    }


    private suspend fun insertRefresh() = withContext(Dispatchers.IO) {
        with(db.DataDao()) {
            val newData = runData()
            insertAll(*newData.toTypedArray())
            val data2 = item.cointitle
            val data = getQueryAll(data2)
            withContext(Dispatchers.Main) {
                adapter.submitList(data)
            }
        }
    }


    private suspend fun uiRefresh() = withContext(Dispatchers.IO) {
        with(db.DataDao()) {
            val data2 = item.cointitle
            val data = getQueryAll(data2)
            withContext(Dispatchers.Main) {
                adapter.submitList(data)
            }
        }
    }


    private suspend fun chart() = withContext(Dispatchers.Main) {

        with(db.DataDao()) {

            val datelist = mutableListOf<String>()
            val pricelist = mutableListOf<String>()

            val data2 = item.cointitle
            val Data = getItem(data2) // List ??????

            for (data in Data) {
                datelist.add(data.date)
            }
            for (data in Data) {
                pricelist.add(data.closing_price)
            }
            Log.d("?????????","$data2")
            val entries = ArrayList<Entry>()

            for (i in 0 until pricelist.size) {
                entries.add(Entry(i.toFloat(), pricelist[i].toFloat()))
            }

            val labels = ArrayList<String>()

            for (i in 0 until datelist.size) {
                labels.add(covertTimestampToDate(datelist[i]))
            }

            val dataset = LineDataSet(entries, "?????? ??????")
            dataset.valueTextSize = 20F
            dataset.color = Color.BLACK
            dataset.setCircleColor(Color.RED)
            dataset.setCircleHoleColor(Color.RED)
            with(binding.linechart){
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            getTransformer(YAxis.AxisDependency.LEFT)
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.textSize = 12.5f
            description.isEnabled = false

            val data = LineData(dataset)
            this.data = data
            legend.textSize = 15F
            legend.verticalAlignment = Legend.LegendVerticalAlignment.TOP
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            setVisibleXRangeMaximum(2f)
            setVisibleXRangeMinimum(2f)
            xAxis.granularity = 1f
            invalidate()
            }
        }
    }

    private suspend fun runData(): List<Data> = withContext(Dispatchers.IO) {
        val site = "https://api.bithumb.com/public/ticker/ALL_KRW" // ??????API ????????? ????????? ?????? ??????
        val url = URL(site)
        val conn = url.openConnection()
        val input = conn.getInputStream()
        val isr = InputStreamReader(input)
        val br = BufferedReader(isr)
        var str: String? = null
        val buf = StringBuffer()
        // Json ????????? ?????? ???????????? ???????????? ?????? ????????? ???, Json??? ????????? ????????? ???????????? ???????????? ?????????
        do {
            str = br.readLine()
            if (str != null) {
                buf.append(str)
            }
        } while (str != null)


        val root = JSONObject(buf.toString())       // ????????? ?????????
        val data = root.getJSONObject("data")  //data?????? ??????????????????
        val names =
            data.names() ?: JSONArray()     // json(???????????? ??????)??? ???????????? array??? ?????? , data??? ??????????????? ?????? ?????????
        val date = data.getString("date")     // date??? ?????? ?????? ???????????? ????????? ??????????????? ?????? ?????????.
        val items = arrayListOf<Data>() // Data??? ????????? ????????? arrayList??? ??????

        for (i in (0 until names.length())) { // name??? ??????(????????????????????????)??? -1 ????????? ??????
            val name = names.getString(i)       // data ??? ?????? ?????? (?????? ??????)
            if (name.equals("date")) continue       // data ??? ?????? ????????? date ??????(??????????????? ?????????) ????????????.
            val element = data.getJSONObject(name)  // ?????? ????????? ???????????? JSON ?????? ?????????
            items.add(Data(name, date, element))    // ????????? Data????????? ???????????? ???????????? items(arrayList)??? ??????
        }
        return@withContext items
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
