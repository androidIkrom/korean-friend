package uz.hangulfriend

import android.app.Application

class HangulFriendApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
