package com.shxhzhxx.manga

import android.Manifest
import android.app.Application
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shxhzhxx.imageloader.ImageLoader
import com.shxhzhxx.sdk.activity.BaseActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.directory.view.*
import java.util.*
import kotlin.collections.LinkedHashMap

class MainActivity : BaseActivity(), Observer<List<Path>> {
    private val mAdapter: MyAdapter = MyAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBarColor(Color.TRANSPARENT, true)
        setContentView(R.layout.activity_main)

        listView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        listView.adapter = mAdapter

        performRequestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), object : PermissionsResultListener() {
            override fun onPermissionGranted() {
                this@MainActivity.apply {
                    val viewModel = ViewModelProviders.of(this).get(PathViewModel::class.java)
                    viewModel.getPaths().observe(this, this)
                }
            }
        })
    }

    override fun onChanged(list: List<Path>) {
        mAdapter.setList(list)
    }

    inner class MyAdapter : RecyclerView.Adapter<MyAdapter.MyHolder>() {
        private var mList: List<Path> = ArrayList()
        fun setList(list: List<Path>) {
            mList = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
            return MyHolder(LayoutInflater.from(parent.context).inflate(R.layout.directory, parent, false))
        }

        override fun getItemCount(): Int {
            return mList.size
        }

        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            holder.itemView.displayName.text = mList[position].displayName
            holder.itemView.count.text = mList[position].count.toString()
            ImageLoader.getInstance().load(mList[position].cover).waitMeasure().centerCrop().tag(IDENTIFY).into(holder.itemView.preview)
        }

        inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            override fun onClick(v: View?) {
                launchGalleryActivity(applicationContext, mList[adapterPosition].path)
            }

            init {
                itemView.setOnClickListener(this)
            }
        }
    }
}

data class Path(val displayName: String, val path: String, val cover: String, var count: Int = 0)

class PathViewModel(application: Application) : AndroidViewModel(application) {
    private val paths = MutableLiveData<List<Path>>()

    init {
        Thread {
            application.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Images.Media.DATA),
                    "${MediaStore.Images.Media.MIME_TYPE}=? or ${MediaStore.Images.Media.MIME_TYPE}=?",
                    arrayOf("image/jpeg", "image/png"),
                    MediaStore.Images.Media.DATA
            )?.apply {
                val map: LinkedHashMap<String, Path> = LinkedHashMap()
                while (moveToNext()) {
                    getString(getColumnIndex(MediaStore.Images.Thumbnails.DATA)).let { data ->
                        data.dropLastWhile {
                            it != '/'
                        }.dropLast(1).let { path ->
                            if (path !in map) {
                                map[path] = Path(path.takeLastWhile {
                                    it != '/'
                                }, path, data)
                            }
                            map[path]!!.count += 1
                        }
                    }
                }
                close()
                paths.postValue(map.values.toList())
            }
        }.start()
    }

    fun getPaths(): LiveData<List<Path>> {
        return paths
    }
}