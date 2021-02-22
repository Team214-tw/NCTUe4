package com.team214.nycue4.course

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.team214.nycue4.R
import com.team214.nycue4.client.E3Client
import com.team214.nycue4.model.CourseItem
import com.team214.nycue4.model.ScoreItem
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_score.*
import kotlinx.android.synthetic.main.progress_bar.*
import kotlinx.android.synthetic.main.status_empty.*
import kotlinx.android.synthetic.main.status_error.*

class ScoreFragment : Fragment() {
    lateinit var client: E3Client
    lateinit var courseItem: CourseItem
    private var disposable: Disposable? = null
    override fun onDestroy() {
        disposable?.dispose()
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_score, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        courseItem = (activity as CourseActivity).courseItem
        client = (activity as CourseActivity).client
        getData()
    }

    private fun getData() {
        disposable?.dispose()
        error_request.visibility = View.GONE
        progress_bar.visibility = View.VISIBLE
        disposable = client.getScore(courseItem)
            .observeOn(AndroidSchedulers.mainThread())
            .collectInto(mutableListOf<ScoreItem>()) { scoreItems, scoreItem -> scoreItems.add(scoreItem) }
            .doFinally { progress_bar?.visibility = View.GONE }
            .subscribeBy(
                onSuccess = { displayData(it) },
                onError = {
                    error_request.visibility = View.VISIBLE
                    error_request_retry.setOnClickListener { getData() }
                }
            )
    }

    private fun displayData(scoreItems: MutableList<ScoreItem>) {
        if (scoreItems.isEmpty()) {
            empty_request.visibility = View.VISIBLE
        } else {
            course_score_recycler_view?.layoutManager = LinearLayoutManager(context)
            course_score_recycler_view?.addItemDecoration(
                DividerItemDecoration(
                    context,
                    LinearLayoutManager.VERTICAL
                )
            )
            course_score_recycler_view?.adapter = ScoreAdapter(scoreItems)
        }
    }

}