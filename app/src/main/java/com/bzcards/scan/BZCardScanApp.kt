package com.bzcards.scan

import android.app.Application
import com.bzcards.scan.data.AppDatabase

class BZCardScanApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}
