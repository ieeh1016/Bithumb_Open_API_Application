package com.example.assignment

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.assignment.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import android.content.Intent as Intent


class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) } //by lazy를 사용해서 처음 호출될 때 초기화 되도록 설정한다. (by lazy = 처음 선언할때 바로 초기화(할당))

    private lateinit var db: AppDatabase

    private val adapter by lazy { MyAdapter() }

    override fun onCreate(savedInstanceState: Bundle?) { //CoroutineScope - 특정한 목적의 Dispatcher를 지정하여 제어 및 동작이 가능한 범위
        super.onCreate(savedInstanceState)
        setContentView(binding.root) //setContentView에는 binding.root 를 전달.

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "DataDB2").addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    //Dispatchers.IO 에서 동작
                    CoroutineScope(Dispatchers.IO).launch {
                        val data = runData()
                        this@MainActivity.db.DataDao().insertAll(*data.toTypedArray()) //items에들어있는 코인들을 가변인자로써 몽땅삽입
                        launch(Dispatchers.Main) {
                            adapter.submitList(data) //data로 교체
                        }
                    }
                }
            }).build()

        with(binding) { //Non-nullable 수신 객체이고 결과가 필요하지 않을때 with 을 사용함.
            recyclerviewMain.apply {
                layoutManager = LinearLayoutManager(this@MainActivity) // Recycler view layout manager 설정
                setHasFixedSize(true)
                adapter = this@MainActivity.adapter
            }
        // refresh 버튼 리스너 설정
        refleshButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                refresh(true)
            }
        }

        //에디터에 글을 하나씩입력할때마다 UI 갱신
        searchEditText.addTextChangedListener {
            CoroutineScope(Dispatchers.IO).launch {
                refresh()
            }
        }

        //에디터키 클릭시 자판을 내려가게 만듬.
       searchEditText.setOnKeyListener { view, i, keyEvent ->
            if (i == KeyEvent.KEYCODE_ENTER || i == KeyEvent.KEYCODE_SEARCH) {
                (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)
            }
            return@setOnKeyListener false
        }
    }

    CoroutineScope(Dispatchers.IO).launch {
        refresh()
    }
        adapter.setItemClickListener(object: MyAdapter.onItemClickListener{
            override fun onClick(v: View, position: Int) {
                val intent = Intent(this@MainActivity,DetailActivity::class.java)
                intent.putExtra("position","${position}")
                startActivity(intent)
            }
        })
}

    private suspend fun refresh(check: Boolean = false) = withContext(Dispatchers.IO) {
        val query = withContext(Dispatchers.Main) {
            binding.searchEditText.text.toString()
        }

        with(db.DataDao()) {
            if (check) {
                val newData = runData()
                insertAll(*newData.toTypedArray())
            }

            val data = getAll(query)

            withContext(Dispatchers.Main) {
                adapter.submitList(data)
            }
        }
    }


    private suspend fun runData(): List<Data> = withContext(Dispatchers.IO) {
        val site = "https://api.bithumb.com/public/ticker/ALL_KRW" // 빗썸API 정보를 가지고 있는 주소
        val url = URL(site)
        val conn = url.openConnection()
        val input = conn.getInputStream()
        val isr = InputStreamReader(input)
        val br = BufferedReader(isr)
        var str: String? = null
        val buf = StringBuffer()
        // Json 문서는 일단 문자열로 데이터를 모두 읽어온 후, Json에 관련된 객체를 만들어서 데이터를 가져옴
        do {
            str = br.readLine()
            if (str != null) {
                buf.append(str)
            }
        } while (str != null)


        val root = JSONObject(buf.toString())       // 객체로 가져옴
        val data = root.getJSONObject("data")  //data먼저 객체로가져옴
        val names = data.names() ?: JSONArray()     // json(코인들의 이름)이 들어있는 array를 만듬 , data의 코인이름을 전부 가져옴
        val date = data.getString("date")     // date는 먼저 다른 코인처럼 요소가 없기떄문에 따로 가져옴.
        val items = arrayListOf<Data>() // Data의 요소를 가지는 arrayList를 생성

        for (i in (0 until names.length())) { // name의 길이(비트코인데이터양)의 -1 번까지 반복
            val name = names.getString(i)       // data 의 요소 이름 (코인 이름)
            if (name.equals("date")) continue       // data 의 요소 이름이 date 이면(비트코인이 아니면) 건너뛴다.
            val element = data.getJSONObject(name)  // 요소 이름에 해당하는 JSON 객체 가져옴
            items.add(Data(name, date, element))    // 가져온 Data타입의 요소들을 포함하여 items(arrayList)로 담음
        }

        return@withContext items

    }
}