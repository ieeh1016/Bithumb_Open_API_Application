package com.example.assignment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.assignment.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL


class MainActivity : AppCompatActivity() {

    //by lazy를 사용해서 처음 호출될 때 초기화 되도록 설정한다. (by lazy - 처음 선언할때 바로 초기화(할당))
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private lateinit var db: AppDatabase

    //Main화면에 보여지는 코인 목록을 클릭했을때 data(item)를 넘겨준다.
    private val adapter by lazy {
        MyAdapter().apply {
            setItemClickListener(object : MyAdapter.onItemClickListener {
                override fun onClick(item: Data) {
                    // 데이터 자체를 ListActivity 로 넘겨줌
                    val intent = Intent(this@MainActivity, ListActivity::class.java).apply {
                        putExtra("item", item)
                    }
                    startActivity(intent)
                }
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) { //CoroutineScope - 특정한 목적의 Dispatcher를 지정하여 제어 및 동작이 가능한 범위
        super.onCreate(savedInstanceState)

        setContentView(binding.root) //setContentView에는 binding.root 를 전달.
        val button = findViewById<Button>(R.id.tvChangeTheme)
        button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }

            }
        })
        //ROOM DB를 만들고 실행시킨다.
        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "DataDB104")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    lifecycleScope.launch {
                        insertRefresh() //(데이터를 다시 읽어와서 room에 삽입하고 Edit에 쓰여진 query를 포함한 코인데이터로 UI를  갱신)
                    }
                }
            }).build()

        with(binding) { //Non-nullable 수신 객체이고 결과가 필요하지 않을때 with 을 사용함.
            recyclerviewMain.apply {
                layoutManager =
                    LinearLayoutManager(this@MainActivity) // Recycler view layout manager 설정
                setHasFixedSize(true)
                adapter = this@MainActivity.adapter
            }

            //refresh 버튼 리스너 설정 - 새로고침 버튼을 누를때마다 refresh함수 실행
            refleshButton.setOnClickListener {
                lifecycleScope.launch {
                    insertRefresh()
                }
            }

            //에디터에 글을 하나씩입력할때마다 room에 저장된 데이터에서 에디터에서 쓴 query를 포함한 코인데이터로 UI를 갱신
            searchEditText.addTextChangedListener {
                lifecycleScope.launch {
                    uiRefresh()
                }
            }

            //에디터에서 엔터, SEARCH를 클릭시 자판을 내려가게 만듬
            searchEditText.setOnKeyListener { view, i, keyEvent ->
                if (i == KeyEvent.KEYCODE_ENTER || i == KeyEvent.KEYCODE_SEARCH) {
                    (getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
                        view.windowToken,
                        0
                    )
                }
                return@setOnKeyListener false
            }
        }
    }

    // 상세 화면에서 새로고침을 하고 다시 Main 화면으로 돌아온 경우 새로고침된 데이터를 보여줄 수 있도록 onResume 에서 firstRefresh 함수 호출한다
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            uiRefresh() // room에 저장된 데이터를 edit에 쓰여진 query가 포함된 코인데이터로 UI를 갱신시킨다.
        }
    }

    //API에서 읽어온 데이터를 ROOM에 삽입하고 EDIT에서 작성한 string이 이름으로 포함된 코인으로 UI를 갱신한다.
    private suspend fun insertRefresh() = withContext(Dispatchers.IO) {
        val query = withContext(Dispatchers.Main) {
            binding.searchEditText.text.toString()
        }
        with(db.DataDao()) {
            //데이터를 runData()를 이용하여 api로 부터 데이터를 다시 읽어온다
            val newData = runData()
            insertAll(*newData.toTypedArray())
            //edit에 쓰여진 string을 포함한 코인을 select한다.
            val data = getAll(query)
            //ROOM에 저장된 데이터 UI로 갱신시킨다. - UI갱신이므로 Dispatchers.Main에서 작업
            withContext(Dispatchers.Main) {
                adapter.submitList(data)
            }
        }
    }

    private suspend fun uiRefresh() = withContext(Dispatchers.IO) {
        val query = withContext(Dispatchers.Main) {
            binding.searchEditText.text.toString()
        }
        with(db.DataDao()) {
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
        val names =
            data.names() ?: JSONArray()     // json(코인들의 이름)이 들어있는 array를 만듬 , data의 코인이름을 전부 가져옴
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