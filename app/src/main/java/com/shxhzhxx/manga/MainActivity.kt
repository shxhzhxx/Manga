package com.shxhzhxx.manga

import android.Manifest
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.shxhzhxx.imageloader.ImageLoader
import com.shxhzhxx.sdk.activity.BaseActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.directory.view.*
import java.util.*
import kotlin.collections.LinkedHashMap

class MainActivity : BaseActivity() {
    private val mAdapter: MyAdapter = MyAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBarColor(Color.TRANSPARENT, true)
        setContentView(R.layout.activity_main)

        listView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        listView.adapter = mAdapter

        performRequestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), object : PermissionsResultListener() {
            override fun onPermissionGranted() {
                Thread(Runnable {
                    val map: LinkedHashMap<String, Path> = LinkedHashMap()
                    contentResolver.query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            arrayOf(MediaStore.Images.Media.DATA),
                            "${MediaStore.Images.Media.MIME_TYPE}=? or ${MediaStore.Images.Media.MIME_TYPE}=?",
                            arrayOf("image/jpeg", "image/png"),
                            MediaStore.Images.Media.DATA
                    )?.apply {
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
                                    map[path]?.apply {
                                        count += 1
                                    }
                                }
                            }
                        }
                        close()
                    }
                    runOnUiThread {
                        mAdapter.list = map.values.toList()
                        mAdapter.notifyDataSetChanged()
                    }
                }).start()
            }
        })
    }


    data class Path(val displayName: String, val path: String, val cover: String, var count: Int = 0)

    inner class MyAdapter : RecyclerView.Adapter<MyAdapter.MyHolder>() {
        var list: List<Path> = ArrayList()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
            return MyHolder(LayoutInflater.from(parent.context).inflate(R.layout.directory, parent, false))
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: MyHolder, position: Int) {
            holder.itemView.displayName.text = list[position].displayName
            holder.itemView.count.text = list[position].count.toString()
            ImageLoader.getInstance().load(list[position].cover).waitMeasure().centerCrop().tag(IDENTIFY).into(holder.itemView.preview)
        }

        inner class MyHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
            override fun onClick(v: View?) {
                launchGalleryActivity(applicationContext, list[adapterPosition].path)
            }

            init {
                itemView.setOnClickListener(this)
            }
        }
    }
}
