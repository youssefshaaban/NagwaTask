package com.example.assignmenttask.ui

import android.Manifest
import android.Manifest.permission
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.Factory
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.example.assignmenttask.R
import com.example.assignmenttask.data.Movie
import com.tbruyelle.rxpermissions2.RxPermissions
import dagger.android.AndroidInjection
import javax.inject.Inject
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.assignmenttask.data.PrecentageData
import com.example.assignmenttask.service.TITTLE
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.assignmenttask.BuildConfig
import com.example.assignmenttask.service.DownloadService
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var factory: ViewModelProvider.Factory
    lateinit var viewModel: MainViewModel
    lateinit var progress: ProgressBar
    lateinit var rv: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AndroidInjection.inject(this)
        setupViews()
        setupObserve()
    }

    private fun setupViews() {
        rv = findViewById(R.id.rvFiles)
        progress = findViewById(R.id.progress)
        rv.layoutManager= LinearLayoutManager(this)
    }


    private fun setupObserve() {
        viewModel = ViewModelProvider(this, factory).get(MainViewModel::class.java)
        observeData()
    }

    override fun onStart() {
        super.onStart()
        viewModel.getDataAttachmentFromFakeResponse()
        registerBroadCastRecevier()
    }

    override fun onStop() {
        super.onStop()
        unregisterBroadCastRecevier()
    }

    fun registerBroadCastRecevier(){
        val bManager = LocalBroadcastManager.getInstance(this)
        val filter = IntentFilter()
        filter.addAction("PROGRESS_DOWNLOAD")
        bManager.registerReceiver(recevierSuccess,filter)
        val filter2 = IntentFilter()
        filter.addAction("FAILURE_DOWNLOAD")
        bManager.registerReceiver(recevierFaliure,filter2)
    }

    fun unregisterBroadCastRecevier(){
        val bManager = LocalBroadcastManager.getInstance(this)
        bManager.unregisterReceiver(recevierFaliure)
        bManager.unregisterReceiver(recevierSuccess)
    }
    private fun observeData() {
        viewModel.loading.observe(this) {
            if (it) {
                progress.visibility = View.VISIBLE
            } else {
                progress.visibility = View.GONE
            }
        }
        // viewModel.notifyPosiotio.observe(this){
        //   val list=viewModel.listMovieLiveData.value
        //   Log.e("list",list.toString())
        //   rv.adapter?.notifyItemChanged(it)
        // }
        // viewModel.errrorMessage.observe(this){
        //   Toast.makeText(this,it,Toast.LENGTH_LONG).show()
        // }
        viewModel.listMovieLiveData.observe(this) {
            rv.adapter = AttachmentAdapter(it,::handleClickDownolad)
        }
    }


    private fun handleClickDownolad(movie: Movie, position:Int) {
        if (checkLocationPermission()){
            if (!movie.isCompleted){
                val mIntent = Intent(this, DownloadService::class.java)
                mIntent.putExtra("url", movie.url)
                mIntent.putExtra("NOTIFICATION_ID", movie.id)
                mIntent.putExtra("position",position)
                mIntent.putExtra(TITTLE,movie.name)
                startService(mIntent)
            }else{
                val uri: Uri = FileProvider.getUriForFile(
                    this@MainActivity,
                    BuildConfig.APPLICATION_ID + ".provider",
                    File(movie.pathFileLocal!!)
                )
                startActivity(Intent(Intent.ACTION_VIEW,uri))
            }

            //DownloadService.enqueueWork(this, mIntent,movie.id)
        }
    }

    private fun checkLocationPermission() :Boolean{
        if (Build.VERSION.SDK_INT >= 23 && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
                    != PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
                    != PackageManager.PERMISSION_GRANTED)
        ) {
            requestSotragePermission()
            return false
        } else {
            return true
        }
    }

    private fun requestSotragePermission() {
        viewModel.compositeDisposable.add(
            RxPermissions(this)
                .request(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .subscribe { granted: Boolean ->
                    if (granted) {
                        // start download
                        Toast.makeText(this,"permission granted",Toast.LENGTH_LONG).show()
                    } else {
                        askStoragePermissionPopUp()
                    }
                })
    }

    private fun askStoragePermissionPopUp() {
        val alertDialog= AlertDialog.Builder(this)
            .setTitle("Need permission")
            .setMessage("Must add permission storage to download attachment")
            .setPositiveButton("ok") { dialog, which ->
                requestSotragePermission()
                dialog.dismiss()
            }
            .setNegativeButton("cancel"){ dialog, which ->
                dialog.dismiss()
            }.create()
        alertDialog.show()
    }

    val recevierSuccess=object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            handleIntentSuccess(intent)
        }
    }
    val recevierFaliure=object :BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            handleIntentFaluire(intent)
        }
    }

    private fun handleIntentFaluire(intent: Intent?) {
        val position=intent?.getIntExtra("position",0)
        val movie=viewModel.listMovieLiveData.value?.get(position!!)
        movie?.isCompleted=false
        movie?.startDownload=false
        rv.adapter?.notifyItemChanged(position!!)
        Toast.makeText(this,"failed download ${movie?.name}", Toast.LENGTH_LONG).show()
    }



    private fun handleIntentSuccess(intent: Intent?) {
        val bundle=intent?.extras
        val position=bundle?.getInt("position",0)
        val precentage=bundle?.getParcelable("object") as? PrecentageData
        val movie=viewModel.listMovieLiveData.value?.get(position!!)
        val isComplated=bundle?.getBoolean("isCompleted",false)
        movie?.startDownload=!isComplated!!
        movie?.isCompleted=isComplated
        precentage?.let {
            movie?.totalFileSize=it.total
            movie?.currentDownload=it.currentPrecntage
        }
        rv.adapter?.notifyItemChanged(position!!)
    }
}