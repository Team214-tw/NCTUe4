package com.team214.nctue4.course

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.analytics.FirebaseAnalytics
import com.team214.nctue4.R
import com.team214.nctue4.client.E3Client
import com.team214.nctue4.model.CourseItem
import kotlinx.android.synthetic.main.activity_course.*

class CourseActivity : AppCompatActivity() {
    private var firebaseAnalytics: FirebaseAnalytics? = null
    lateinit var client: E3Client
    lateinit var courseItem: CourseItem

    private var currentFragment = -1

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt("currentFragment", currentFragment)
    }

    private val mOnNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            switchFragment(item.itemId)
            return@OnNavigationItemSelectedListener true
        }


    private fun switchFragment(itemId: Int) {
//        currentFragment = itemId
//        val fragment = when (itemId) {
//            R.id.course_nav_ann -> {
//                firebaseAnalytics!!.setCurrentScreen(
//                    this,
//                    "CourseAnnFragment",
//                    CourseAnnFragment::class.java.simpleName
//                )
//                CourseAnnFragment()
//            }
//            R.id.course_nav_doc -> {
//                firebaseAnalytics!!.setCurrentScreen(
//                    this,
//                    "CourseDocListFragment",
//                    CourseDocListFragment::class.java.simpleName
//                )
//                CourseDocListFragment()
//            }
//            R.id.course_nav_assignment -> {
//                firebaseAnalytics!!.setCurrentScreen(this, "AssignFragment", AssignFragment::class.java.simpleName)
//                AssignFragment()
//            }
//            R.id.course_nav_score -> {
//                firebaseAnalytics!!.setCurrentScreen(this, "ScoreFragment", ScoreFragment::class.java.simpleName)
//                ScoreFragment()
//            }
//            R.id.course_nav_members -> {
//                firebaseAnalytics!!.setCurrentScreen(this, "MembersFragment", MembersFragment::class.java.simpleName)
//                MembersFragment()
//            }
//            else -> {
//                firebaseAnalytics!!.setCurrentScreen(
//                    this,
//                    "CourseAnnFragment",
//                    CourseAnnFragment::class.java.simpleName
//                )
//                CourseAnnFragment()
//            }
//        }
//        fragment.arguments = intent.extras
//        supportFragmentManager.beginTransaction().replace(R.id.course_container, fragment).commit()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        setContentView(R.layout.activity_course)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        courseItem = intent.extras!!.getParcelable("courseItem")!!
        this.title = courseItem.courseName

        if (savedInstanceState?.getInt("currentFragment") == null)
            switchFragment(-1)

        course_bottom_nav?.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

}
