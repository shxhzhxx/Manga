package com.shxhzhxx.manga

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.shxhzhxx.imageloader.ImageLoader
import com.shxhzhxx.sdk.activity.BaseActivity
import kotlinx.android.synthetic.main.activity_gallery.*
import kotlinx.android.synthetic.main.dialog_settings.*
import kotlinx.android.synthetic.main.gallery.view.*

fun launchGalleryActivity(context: Context, path: String) {
    context.startActivity(Intent(context, GalleryActivity::class.java).putExtra("path", path))
}

class GalleryActivity : BaseActivity(), OnViewTapListener {
    private val mSnapHelper = PagerSnapHelper()
    private val mAdapter = GalleryAdapter()
    private val mPath: String by lazy {
        intent.getStringExtra("path") ?: ""
    }
    private lateinit var mSettings: PathSettings
    private lateinit var mLayoutManager: LinearLayoutManager
    private lateinit var mListView: RecyclerView
    private var mDialog: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStatusBarColor(Color.TRANSPARENT, true)
        setContentView(R.layout.activity_gallery)

        mListView = listView

        performRequestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), object : PermissionsResultListener() {
            override fun onPermissionGranted() {
                Thread(Runnable {
                    var settings = App.db.dao().load(mPath)
                    if (settings == null) {
                        settings = PathSettings(mPath)
                        App.db.dao().save(settings)
                    }
                    mSettings = settings
                    mLayoutManager = LinearLayoutManager(this@GalleryActivity,
                            if (mSettings.vertical) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL, mSettings.reverse)

                    val list = ArrayList<String>()
                    var index = 0
                    contentResolver.query(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            arrayOf(MediaStore.Images.Media.DATA),
                            "${MediaStore.Images.Media.DATA} REGEXP ? AND (${MediaStore.Images.Media.MIME_TYPE}=? OR ${MediaStore.Images.Media.MIME_TYPE}=?)",
                            arrayOf("$mPath/[^/]*", "image/jpeg", "image/png"),
                            MediaStore.Images.Media.DISPLAY_NAME
                    )?.apply {
                        while (moveToNext()) {
                            val data = getString(getColumnIndex(MediaStore.Images.Thumbnails.DATA))
                            if (mSettings.lastTime == data) {
                                index = position
                            }
                            list.add(data)
                        }
                        close()
                    }

                    runOnUiThread {
                        tapLayout.setOnViewTapListener(this@GalleryActivity)
                        if (mSettings.snap) {
                            mSnapHelper.attachToRecyclerView(mListView)
                        }
                        mListView.layoutManager = mLayoutManager
                        mListView.adapter = mAdapter
                        mAdapter.list = list
                        mAdapter.notifyDataSetChanged()
                        mListView.scrollToPosition(index)
                    }
                }).start()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        mDialog?.dismiss()
    }

    override fun onStop() {
        super.onStop()

        mSettings.lastTime = mAdapter.list[mLayoutManager.findFirstVisibleItemPosition()]
        Thread(Runnable {
            App.db.dao().save(mSettings)
        }).start()
    }

    inner class GalleryAdapter : RecyclerView.Adapter<GalleryAdapter.GalleryHolder>() {
        var list: List<String> = ArrayList()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryHolder {
            return GalleryHolder(LayoutInflater.from(parent.context).inflate(R.layout.gallery, parent, false))
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: GalleryHolder, position: Int) {
            ImageLoader.getInstance().load(list[position]).waitMeasure().tag(IDENTIFY).into(holder.itemView.preview)
        }

        inner class GalleryHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }

    private fun next() {
        val position = mLayoutManager.findFirstVisibleItemPosition() + 1
        if (position < mAdapter.list.size) {
            mListView.smoothScrollToPosition(position)
        }
    }

    private fun prev() {
        val position = mLayoutManager.findLastVisibleItemPosition() - 1
        if (position >= 0) {
            mListView.smoothScrollToPosition(position)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                next()
                return true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                prev()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onViewTap(v: View, x: Float, y: Float) {
        val p = x / v.width
        when {
            mSettings.snap && p < 0.2 -> {
                if (mLayoutManager.reverseLayout) {
                    next()
                } else {
                    prev()
                }
            }
            mSettings.snap && p > 0.8 -> {
                if (mLayoutManager.reverseLayout) {
                    prev()
                } else {
                    next()
                }
            }
            else -> {
                mDialog?.dismiss()
                mDialog = Dialog(this@GalleryActivity, R.style.MyDialog).apply {
                    setContentView(R.layout.dialog_settings)
                    window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    reverse.isChecked = mSettings.reverse
                    vertical.isChecked = mSettings.vertical
                    snap.isChecked = mSettings.snap
                    setOnDismissListener {
                        mSettings.reverse = reverse.isChecked
                        mSettings.vertical = vertical.isChecked
                        mSettings.snap = snap.isChecked

                        mLayoutManager.reverseLayout = mSettings.reverse
                        mLayoutManager.orientation = if (mSettings.vertical) RecyclerView.VERTICAL else RecyclerView.HORIZONTAL
                        mSnapHelper.attachToRecyclerView(if (mSettings.snap) mListView else null)
                        mDialog = null
                    }
                    show()
                }
            }
        }
    }
}