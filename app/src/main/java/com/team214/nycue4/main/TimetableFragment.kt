package com.team214.nycue4.main

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.github.tlaabs.timetableview.Schedule
import com.github.tlaabs.timetableview.Time
import com.team214.nycue4.R
import com.team214.nycue4.client.E3Type
import com.team214.nycue4.model.CourseDBHelper
import com.team214.nycue4.model.CourseItem
import kotlinx.android.synthetic.main.fragment_timetable.*


class TimetableFragment : Fragment() {
    private lateinit var courseDBHelper: CourseDBHelper
    private var courseItems = mutableListOf<CourseItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        requireActivity().setTitle(R.string.timetable)
//        setHasOptionsMenu(true)
        courseDBHelper = CourseDBHelper(requireContext())
        val cur = System.currentTimeMillis() / 1000
        courseItems = courseDBHelper.readCourses(E3Type.NEW)
        courseItems.retainAll { cur <= it.sortKey }
        return inflater.inflate(R.layout.fragment_timetable, container, false)
    }

    private val daycodes: HashMap<Char, Int> = hashMapOf(
        'M' to 0,
        'T' to 1,
        'W' to 2,
        'R' to 3,
        'F' to 4,
        'S' to 5,
        'U' to 6
    )

    private val timecodes: HashMap<Char, Pair<Pair<Int, Int>, Pair<Int, Int>>> = hashMapOf(
        'y' to Pair(Pair(6, 0), Pair(6, 50)),
        'z' to Pair(Pair(7, 0), Pair(7, 50)),
        '1' to Pair(Pair(8, 0), Pair(8, 50)),
        '2' to Pair(Pair(9, 0), Pair(9, 50)),
        '3' to Pair(Pair(10, 10), Pair(11, 0)),
        '4' to Pair(Pair(11, 10), Pair(12, 0)),
        'n' to Pair(Pair(12, 20), Pair(13, 10)),
        '5' to Pair(Pair(13, 20), Pair(14, 10)),
        '6' to Pair(Pair(14, 20), Pair(15, 10)),
        '7' to Pair(Pair(15, 30), Pair(16, 20)),
        '8' to Pair(Pair(16, 30), Pair(17, 20)),
        '9' to Pair(Pair(17, 30), Pair(18, 20)),
        'a' to Pair(Pair(18, 30), Pair(19, 20)),
        'b' to Pair(Pair(19, 30), Pair(20, 20)),
        'c' to Pair(Pair(20, 30), Pair(21, 20)),
        'd' to Pair(Pair(21, 30), Pair(22, 20))
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val schedules = ArrayList<Schedule>()
        courseItems.forEach {
            if (it.time != null) {
                val regex = Regex("[A-Z][0-9a-z]*")
                regex.findAll(it.time!!).forEach { t ->
                    val schedule = Schedule()
                    val dayTimecode = t.value
                    val day = daycodes[dayTimecode[0]]!!
                    val startTime = timecodes[dayTimecode[1]]?.first!!
                    val endTime = timecodes[dayTimecode.last()]?.second!!
                    schedule.classTitle = it.courseName// sets subject
                    schedule.professorName = it.additionalInfo  // sets professor
                    schedule.day = day
                    schedule.startTime = Time(
                        startTime.first,
                        startTime.second
                    ) // sets the beginning of class time (hour,minute)
                    schedule.endTime = Time(
                        endTime.first,
                        endTime.second
                    ) // sets the end of class time (hour,minute)
                    schedules.add(schedule)
                }
            }
        }

        timetable.add(schedules)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.timetable_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.timetable_menu_show_weekend -> {
                true
            }
            R.id.timetable_menu_edit -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}