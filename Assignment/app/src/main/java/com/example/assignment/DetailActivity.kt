package com.example.assignment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.assignment.databinding.ActivityDetailBinding
import android.app.Activity
import android.media.CamcorderProfile.getAll
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL


class DetailActivity : AppCompatActivity() {

    private var _item: Data? = null
    private val item get() = _item!!


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        _item = intent?.getParcelableExtra("item")

        if (savedInstanceState != null) {
            _item = savedInstanceState.getParcelable("item")
        }
        val cointitle_intent = item.cointitle
        val cointitle: TextView = findViewById(R.id.detail_cointitle)
        cointitle.text = "$cointitle_intent 상세"
        val button = findViewById<Button>(R.id.back_button)
        button.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                onBackPressed()
            }
        })

        val detail_opening_price:TextView = findViewById<TextView>(R.id.detail_item_opening_price)
        detail_opening_price.text = "시가: "+ item.opening_price

       val detail_closing_price:TextView = findViewById<TextView>(R.id.detail_item_closing_price)
        detail_closing_price.text = "현재가: "+ item.closing_price

        val detail_min_price:TextView = findViewById<TextView>(R.id.detail_item_min_price)
        detail_min_price.text = "현재시간 기준 저가: "+ item.min_price

        val detail_max_price:TextView = findViewById<TextView>(R.id.detail_item_max_price)
        detail_max_price.text = "현재시간 기준 고가: "+ item.max_price

        val detail_units_traded:TextView = findViewById<TextView>(R.id.detail_item_units_traded)
        detail_units_traded.text = "현재시간 기준 거래량: "+ item.units_traded

        val detail_acc_trade_value:TextView = findViewById<TextView>(R.id.detail_item_acc_trade_value)
        detail_acc_trade_value.text = "현재시간 기준 거래금액: "+ item.acc_trade_value

        val detail_prev_closing_price:TextView = findViewById<TextView>(R.id.detail_item_prev_closing_price)
        detail_prev_closing_price.text = "전일종가: "+ item.closing_price

        val detail_units_traded_24H:TextView = findViewById<TextView>(R.id.detail_item_units_traded_24H)
        detail_units_traded_24H.text = "최근 24시간 거래량: "+ item.units_traded_24H

        val detail_acc_trade_value_24H:TextView = findViewById<TextView>(R.id.detail_item_acc_trade_value_24H)
        detail_acc_trade_value_24H.text = "최근 24시간 거래금액: "+ item.acc_trade_value_24H

        val detail_fluctate_24H:TextView = findViewById<TextView>(R.id.detail_item_fluctate_24H)
        detail_fluctate_24H.text = "최근 24시간 변동가: "+ item.fluctate_24H

        val detail_fluctate_rate_24H:TextView = findViewById<TextView>(R.id.detail_item_fluctate_rate_24H)
        detail_fluctate_rate_24H.text = "최근24시간 변동률: "+ item.fluctate_rate_24H

        _item = intent?.getParcelableExtra("item")
        if (savedInstanceState != null) {
            _item = savedInstanceState.getParcelable("item")
        }


    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("item", item)
    }
}

