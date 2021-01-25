package com.akdogan.simpledivelog.application.listview

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.datalayer.database.DiveLogDatabase
import com.akdogan.simpledivelog.databinding.FragmentListViewBinding
import com.akdogan.simpledivelog.datalayer.repository.RepositoryApiStatus
import com.akdogan.simpledivelog.diveutil.ActWithString
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class ListViewFragment : Fragment() {
    var t: Toast? = null
    lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    lateinit var listViewModel: ListViewViewModel
    lateinit var adapter: ListViewAdapter
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding: FragmentListViewBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_list_view, container, false)
        val application = requireNotNull(this.activity).application
        val datasource = DiveLogDatabase.getInstance(application).diveLogDatabaseDao
        val viewModelFactory = ListViewViewModelFactory(datasource, application)
        listViewModel = ViewModelProvider(this, viewModelFactory).get(ListViewViewModel::class.java)
        binding.lifecycleOwner = this
        binding.listViewViewModel = listViewModel

        // Setup Recyclerview and its adapter
        adapter = ListViewAdapter(listViewModel::onListItemClicked)
        binding.diveLogList.adapter = adapter
        // Change Recyclerview to Gridlayout if in Landscape mode
        val currentOrientation = resources.configuration.orientation
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.diveLogList.layoutManager = GridLayoutManager(context, 2)
        }
        // Setup ItemTouchHelper for Swipe to delete of entries
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ){
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }
            // TODO ?? Item gets removed in UI when there is not internet connection -> How to handle?
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                listViewModel.deleteRemoteItem(adapter.getDatabaseIdAtPosition(viewHolder.adapterPosition))
            }
        }).attachToRecyclerView(binding.diveLogList)

        // Setup Pull to Refresh
        mSwipeRefreshLayout = binding.swipeRefreshLayout
        mSwipeRefreshLayout.setOnRefreshListener { listViewModel.onRefresh() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //  TODO
        listViewModel.listOfLogEntries.observe(viewLifecycleOwner, Observer { list ->
            list?.let{
                adapter.dataSet = list
            }
        })

        // Observe makeToast to present a Toast to the Message. Triggers if the string is not null
        listViewModel.makeToast.observe(viewLifecycleOwner, Observer { message: String? ->
            message?.let{
                makeToast(message)
                listViewModel.onMakeToastDone()
            }
        })
        // Observe Navigation to EditView (Creating new Entry)
        listViewModel.navigateToNewEntry.observe(viewLifecycleOwner, Observer{ act: Boolean? ->
            if (act == true){
                val action = ListViewFragmentDirections.actionListViewFragmentToEditViewFragment()
                findNavController().navigate(action)
                listViewModel.onNavigateToNewEntryDone()
            }
        })

        // Observe Navigation to Detailview. Triggered when the Id is not null
        listViewModel.navigateToDetailView.observe(viewLifecycleOwner, Observer{ diveId: String? ->
            if (diveId != null){
                val action = ListViewFragmentDirections.actionListViewFragmentToDetailViewFragment(diveId)
                findNavController().navigate(action)
                listViewModel.onNavigateToDetailViewFinished()
            }
        })

        // Observe the status of the API to switch of the pull to refresh animation
        listViewModel.repositoryApiStatus.observe(viewLifecycleOwner, Observer { status: RepositoryApiStatus? ->
            Log.i("ApiStatus Tracing", "Api Status observer called with status: $status")
            if (mSwipeRefreshLayout.isRefreshing &&
                    (status == RepositoryApiStatus.DONE || status == RepositoryApiStatus.ERROR) ){
                mSwipeRefreshLayout.isRefreshing = false
            }
        })

        // Observe any Errors and Display the as Toast to the User
        listViewModel.apiError.observe(viewLifecycleOwner, Observer { e: Exception? ->
            e?.let{
                makeToast("Error: $e")
                listViewModel.onErrorDone()
            }
        })

        //Kotlin Flow TestObserver // TODO einlesen https://www.google.com/search?client=firefox-b-d&q=launch+when+created+vs.+launch+when+started
        lifecycleScope.launchWhenResumed{
            listViewModel.errors
                .onEach {
                    makeToast(it.toString())
                }.launchIn(this)
        }

    }

    private fun makeToast(message: String){
        if (t != null){
            t?.cancel()
        }
        t = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        t?.show()
    }

}