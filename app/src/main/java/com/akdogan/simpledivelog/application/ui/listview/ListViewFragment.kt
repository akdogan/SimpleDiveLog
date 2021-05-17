package com.akdogan.simpledivelog.application.ui.listview

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.application.ServiceLocator
import com.akdogan.simpledivelog.application.mainactivity.MainActivity
import com.akdogan.simpledivelog.databinding.FragmentListViewBinding
import com.akdogan.simpledivelog.datalayer.ErrorCases
import com.akdogan.simpledivelog.datalayer.repository.RepositoryDownloadStatus

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class ListViewFragment : Fragment() {
    private var t: Toast? = null
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout
    private lateinit var adapter: ListViewAdapter
    private val listViewModel: ListViewModel by viewModels {
        ListViewModelFactory(ServiceLocator.repo)
    }

    private val args: ListViewFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val binding: FragmentListViewBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_list_view, container, false
        )

        binding.lifecycleOwner = this // Not sure why this is used
        binding.listViewViewModel = listViewModel

        // Setup Recyclerview and its adapter
        setupRecyclerView(binding.diveLogList)

        // Setup Pull to Refresh
        mSwipeRefreshLayout = binding.swipeRefreshLayout
        mSwipeRefreshLayout.setOnRefreshListener { listViewModel.onRefresh() }

        // Create Dummy Data if account was just created
        if (args.createSampleData){
            createSampleData()
        }

        Log.i("LIST_VIEW_LIFECYCLE", "On Create called")

        return binding.root
    }

    private fun createSampleData(){
        makeToast(getString(R.string.message_creating_sample_data), true)
        listViewModel.createMultipleDummyEntries(5)
    }

    // Todo: Add search to actionbar as collapsible action view
    // https://developer.android.com/training/appbar/action-views

    private fun setupRecyclerView(recyclerView: RecyclerView){
        adapter = ListViewAdapter(listViewModel::onListItemClicked)
        recyclerView.adapter = adapter
        // Change Recyclerview to Gridlayout if in Landscape mode
        val currentOrientation = resources.configuration.orientation
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            recyclerView.layoutManager = GridLayoutManager(context, 2)
        }
        // Setup ItemTouchHelper for Swipe to delete of entries
        setupItemTouchHelper(recyclerView)
    }

    private fun setupItemTouchHelper(recyclerView: RecyclerView){
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT
        ) {
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
        }).attachToRecyclerView(recyclerView)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listViewModel.listOfLogEntries.observe(viewLifecycleOwner, { list ->
            list?.let {
                Log.i("Refresh Tracing", "listOfLogentries called")
                adapter.dataSet = list
            }
        })

        // Observe makeToast to present a Toast to the Message. Triggers if the string is not null
        listViewModel.makeToast.observe(viewLifecycleOwner, { code: Int? ->
            code?.let {
                makeToast(ErrorCases.getMessage(resources, it))
                listViewModel.onMakeToastDone()
            }
        })
        // Observe Navigation to EditView (Creating new Entry)
        listViewModel.navigateToNewEntry.observe(viewLifecycleOwner, { act: Boolean? ->
            if (act == true) {
                val action = ListViewFragmentDirections.actionListViewFragmentToEditViewFragment()
                findNavController().navigate(action)
                listViewModel.onNavigateToNewEntryDone()
            }
        })

        // Observe Navigation to Detailview. Triggered when the Id is not null
        listViewModel.navigateToDetailView.observe(viewLifecycleOwner, { diveId: String? ->
            if (diveId != null) {
                val action =
                    ListViewFragmentDirections.actionListViewFragmentToDetailViewFragment(diveId)
                findNavController().navigate(action)
                listViewModel.onNavigateToDetailViewFinished()
            }
        })

        // Observe the status of the API to switch of the pull to refresh animation
        listViewModel.repositoryApiStatus.observe(
            viewLifecycleOwner,
            { status: RepositoryDownloadStatus? ->
                Log.i("ApiStatus Tracing", "Api Status observer called with status: $status")
                if (mSwipeRefreshLayout.isRefreshing &&
                    (status == RepositoryDownloadStatus.DONE || status == RepositoryDownloadStatus.ERROR)
                ) {
                    mSwipeRefreshLayout.isRefreshing = false
                }
            })

        // Observe the trigger for unauthorized Access
        listViewModel.unauthorizedAccess.observe(viewLifecycleOwner, {
            if (it == true) {
                try {
                    (requireActivity() as MainActivity).authExpired()
                } catch (e: ClassCastException){
                    makeToast("Cast to MainActivity failed")
                }
            }
        })

    }

    override fun onDestroy() {
        Log.i("LIST_VIEW_LIFECYCLE", "On Destroy called")
        super.onDestroy()
    }

    private fun makeToast(message: String, showLonger: Boolean = false) {
        if (t != null) {
            t?.cancel()
        }
        val length = if (showLonger){
            Toast.LENGTH_LONG
        } else {
            Toast.LENGTH_SHORT
        }
        t = Toast.makeText(context, message, length)
        t?.show()
    }

}