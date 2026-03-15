package app.romanmarinov.dochintkmp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.romanmarinov.dochintkmp.ui.theme.DocHintTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import app.romanmarinov.dochintkmp.di.dataModule
import app.romanmarinov.dochintkmp.di.domainModule
import app.romanmarinov.dochintkmp.di.viewModelModule
import app.romanmarinov.dochintkmp.presentation.navigation.AppNavigation
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MainActivity)
            modules(dataModule, domainModule, viewModelModule)
        }

        setContent {
            DocHintTheme {
                AppNavigation()
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    DocHintTheme {
        AppNavigation()
    }
}