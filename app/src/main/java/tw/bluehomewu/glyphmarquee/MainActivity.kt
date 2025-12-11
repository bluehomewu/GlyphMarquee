package tw.bluehomewu.glyphmarquee

import android.content.ComponentName // Add this
import android.content.Intent      // Add this
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 這會打開 Nothing 的玩具管理介面
        try {
            val intent = Intent()
            intent.component = ComponentName(
                "com.nothing.thirdparty",
                "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity"
            )
            startActivity(intent)
        } catch (e: Exception) {
            // 如果不是 Nothing Phone (3) 或沒安裝相關服務，可能會報錯
            e.printStackTrace()
        }
    }
}
