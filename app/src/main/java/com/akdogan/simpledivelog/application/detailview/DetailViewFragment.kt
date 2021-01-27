package com.akdogan.simpledivelog.application.detailview

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
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.databinding.FragmentDetailViewBinding
import com.akdogan.simpledivelog.datalayer.repository.RepositoryDownloadStatus

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class DetailViewFragment : Fragment() {
    private var t: Toast? = null
    private lateinit var binding : FragmentDetailViewBinding
    private lateinit var detailViewModel: DetailViewModel

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_detail_view, container, false)
        val application = requireNotNull(this.activity).application
        val viewModelFactory = DetailViewModelFactory(application, DetailViewFragmentArgs.fromBundle(requireArguments()).diveLogId)
        detailViewModel = ViewModelProvider(this, viewModelFactory).get(DetailViewModel::class.java)
        binding.lifecycleOwner = this
        binding.detailViewModel = detailViewModel

        binding.editButton.setOnClickListener {
            val action = DetailViewFragmentDirections.actionDetailViewFragmentToEditViewFragment(detailViewModel.diveLogId)
            findNavController().navigate(action)
        }

        detailViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Observe makeToast to display messages to the user
        detailViewModel.makeToast.observe(viewLifecycleOwner, { message: String? ->
            message?.let{
                makeToast(message)
                detailViewModel.onMakeToastDone()
            }
        })

        //Observe the API Status to hide loading animation and display content
        detailViewModel.repositoryApiStatus.observe(viewLifecycleOwner, {
            Log.i("ApiStatus Tracing", "Api Status observer called with status: $it")
            if (it == RepositoryDownloadStatus.DONE){
                binding.detailViewProgressCircular.hide()
                binding.detailViewMainContent.visibility = View.VISIBLE
            }
        })

        // Observe any Errors and Display the as Toast to the User
        detailViewModel.apiError.observe(viewLifecycleOwner, { e: Exception? ->
            e?.let{
                makeToast( "Error: $e")
                detailViewModel.onErrorDone()
            }
        })

        // Observe Navigation back to the List (e.g. if the element could not be found)
        detailViewModel.navigateBack.observe(viewLifecycleOwner, { act: Boolean? ->
            if (act == true){
                val action = DetailViewFragmentDirections.actionDetailViewFragmentToListViewFragment()
                findNavController().navigate(action)
                detailViewModel.onNavigateBackDone()
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