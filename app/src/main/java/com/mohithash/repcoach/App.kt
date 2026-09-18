package com.mohithash.repcoach

import android.app.Application
import androidx.room.Room
import com.mohithash.repcoach.ai.AiClient
import com.mohithash.repcoach.ai.CoachAi
import com.mohithash.repcoach.data.AppDb
import com.mohithash.repcoach.data.JsonStore

class App : Application() {
    lateinit var db: AppDb
    lateinit var store: JsonStore
    val client = AiClient()
    val coach by lazy { CoachAi(client) }
    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, AppDb::class.java, "repcoach.db").build()
        store = JsonStore(this)
    }
}
