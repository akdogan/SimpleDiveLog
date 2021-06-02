package com.akdogan.simpledivelog.application.ui.detailview

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.application.ServiceLocator
import com.akdogan.simpledivelog.application.mainactivity.MainActivity
import com.akdogan.simpledivelog.application.ui.pictureview.PictureFragment
import com.akdogan.simpledivelog.databinding.FragmentDetailViewBinding
import com.akdogan.simpledivelog.datalayer.DiveLogEntry
import com.akdogan.simpledivelog.datalayer.ErrorCases
import com.akdogan.simpledivelog.datalayer.repository.RepositoryDownloadStatus
import com.akdogan.simpledivelog.diveutil.Constants.SHARED_VIEW_MODEL_TAG

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class DetailViewFragment : Fragment() {
    private var t: Toast? = null
    private lateinit var binding : FragmentDetailViewBinding
    private val args: DetailViewFragmentArgs by navArgs()
    private lateinit var detailViewModel: DetailViewModel
    private lateinit var recViewAdapter: DetailViewListAdapter

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_detail_view, container, false)

        val viewModelFactory = DetailViewModelFactory(
            ServiceLocator.repo,
            args.diveLogId
        )
        detailViewModel = ViewModelProvider(this, viewModelFactory).get(
            SHARED_VIEW_MODEL_TAG,
            DetailViewModel::class.java
        )


        binding.lifecycleOwner = this
        binding.detailViewModel = detailViewModel

        // Join into the options menu
        setHasOptionsMenu(true)

        // Setup the picture child fragment
        childFragmentManager.commit {
            add(R.id.detail_view_picture_container, PictureFragment::class.java, null)
        }

        setupListView()

        return binding.root
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_settings).isVisible = false
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.detail_view_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId){
            R.id.detail_view_edit_button -> {
                val action = DetailViewFragmentDirections.actionDetailViewFragmentToEditViewFragment(detailViewModel.diveLogId)
                findNavController().navigate(action)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observe if this fragment was navigated back to and call refresh in the viewModel accordingly
        val navController = findNavController()
        // We use a String here, but any type that can be put in a Bundle is supported
        navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>(getString(R.string.navigated_back_key))?.observe(
            viewLifecycleOwner) { result ->
            // Do something with the result.
            if (result != null && result == true){
                detailViewModel.refresh()
                navController.currentBackStackEntry?.savedStateHandle?.remove<Boolean>(getString(R.string.navigated_back_key))
            }
        }


        // Observe makeToast to display messages to the user
        detailViewModel.makeToast.observe(viewLifecycleOwner, { code: Int? ->
            code?.let{
                makeToast(ErrorCases.getMessage(resources, it))
                detailViewModel.onMakeToastDone()
            }
        })

        //Observe the API Status to hide loading animation and display content
        detailViewModel.repositoryApiStatus.observe(viewLifecycleOwner, {
            Log.i("ApiStatus Tracing", "Api Status observer called with status: $it")
            // TODO Loading probably needs to get updated
            if (it == RepositoryDownloadStatus.DONE){
                binding.detailViewProgressCircular.hide()
                //binding.detailViewMainContent.visibility = View.VISIBLE

            }
        })

        detailViewModel.diveLogEntry.observe(viewLifecycleOwner, { item: DiveLogEntry? ->
            Log.i("DETAIL_VIEW", "observe dle called with $item")
            item?.let{
                addDataSet(it)
            }
        })

        // Observe the trigger for unauthorized Access
        detailViewModel.unauthorizedAccess.observe(viewLifecycleOwner, {
            if (it == true) {
                try {
                    (requireActivity() as MainActivity).authExpired()
                } catch (e: ClassCastException){
                    makeToast("Cast to MainActivity failed")
                }
            }
        })

        // Observe Navigation back to the List (e.g. if the element could not be found)
        detailViewModel.navigateBack.observe(viewLifecycleOwner, { act: Boolean? ->
            if (act == true){
                findNavController().navigateUp()
                detailViewModel.onNavigateBackDone()
            }
        })

    }

    private fun setupListView(){
        recViewAdapter = DetailViewListAdapter()
        binding.detailItemList.apply{
            this.adapter = recViewAdapter
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        }

    }

    private fun addDataSet(item: DiveLogEntry){
        Log.i("DETAIL_VIEW", "Add Dataset $item")
        recViewAdapter.dataSet = item.toDetailItemsList(resources)
    }

    private fun makeToast(message: String){
        if (t != null){
            t?.cancel()
        }
        t = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        t?.show()
    }

}