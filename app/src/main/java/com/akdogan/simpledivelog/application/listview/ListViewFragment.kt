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
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.databinding.FragmentListViewBinding
import com.akdogan.simpledivelog.datalayer.repository.RepositoryDownloadStatus

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class ListViewFragment : Fragment() {
    private var t: Toast? = null
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    lateinit var listViewModel: ListViewViewModel
    lateinit var adapter: ListViewAdapter
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val binding: FragmentListViewBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_list_view, container, false)
        val application = requireNotNull(this.activity).application
        val viewModelFactory = ListViewViewModelFactory(application)
        // TODO Use delegate instead of factory ?
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


        Log.i("MAIN THREAD", "End of onCreate Fragment")
        return binding.root
    }

    // Todo: Add search to actionbar as collapsible action view
    // https://developer.android.com/training/appbar/action-views


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listViewModel.listOfLogEntries.observe(viewLifecycleOwner, { list ->
            list?.let{
                adapter.dataSet = list
            }
        })

        // Observe makeToast to present a Toast to the Message. Triggers if the string is not null
        listViewModel.makeToast.observe(viewLifecycleOwner, { message: String? ->
            message?.let{
                makeToast(message)
                listViewModel.onMakeToastDone()
            }
        })
        // Observe Navigation to EditView (Creating new Entry)
        listViewModel.navigateToNewEntry.observe(viewLifecycleOwner, { act: Boolean? ->
            if (act == true){
                val action = ListViewFragmentDirections.actionListViewFragmentToEditViewFragment()
                findNavController().navigate(action)
                listViewModel.onNavigateToNewEntryDone()
            }
        })

        // Observe Navigation to Detailview. Triggered when the Id is not null
        listViewModel.navigateToDetailView.observe(viewLifecycleOwner, { diveId: String? ->
            if (diveId != null){
                val action = ListViewFragmentDirections.actionListViewFragmentToDetailViewFragment(diveId)
                findNavController().navigate(action)
                listViewModel.onNavigateToDetailViewFinished()
            }
        })

        // Observe the status of the API to switch of the pull to refresh animation
        listViewModel.repositoryApiStatus.observe(viewLifecycleOwner, { status: RepositoryDownloadStatus? ->
            Log.i("ApiStatus Tracing", "Api Status observer called with status: $status")
            if (mSwipeRefreshLayout.isRefreshing &&
                    (status == RepositoryDownloadStatus.DONE || status == RepositoryDownloadStatus.ERROR) ){
                mSwipeRefreshLayout.isRefreshing = false
            }
        })

        // Observe any Errors and Display the as Toast to the User
        listViewModel.apiError.observe(viewLifecycleOwner, { e: Exception? ->
            e?.let{
                makeToast("Error: $e")
                listViewModel.onErrorDone()
            }
        })

    }

    private fun makeToast(message: String){
        if (t != null){
            t?.cancel()
        }
        t = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        t?.show()
    }

}