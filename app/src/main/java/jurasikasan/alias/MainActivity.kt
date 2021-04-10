package jurasikasan.alias

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import jurasikasan.alias.ui.main.MainFragment
import jurasikasan.alias.ui.main.MainViewModel

class MainActivity : AppCompatActivityStreamMusic() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        val viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewModel.teamNames = intent.getStringArrayListExtra(EXTRA_TEAMS_NAMES)
        viewModel.teamList = ArrayList(viewModel.teamNames!!.size)
        for (name in viewModel.teamNames!!) {
            viewModel.teamList!!.add(Team(name))
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commitNow()
        }
    }
}