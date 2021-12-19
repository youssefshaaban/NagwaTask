package com.example.assignmenttask.data

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
@Parcelize
data class Movie(
  val id: Int,
  val name: String,
  val type: String,
  val url: String,
  var pathFileLocal:String?=null,
  var startDownload: Boolean = false,
  var isCompleted: Boolean = false,
  var currentDownload: Long = 0,
  var totalFileSize: Long = Long.MAX_VALUE
):Parcelable