package uz.hangulfriend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import uz.hangulfriend.ui.HangulFriendNav
import uz.hangulfriend.ui.theme.HangulFriendTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as HangulFriendApp).container
        setContent {
            HangulFriendTheme {
                HangulFriendNav(container)
            }
        }
    }
}
