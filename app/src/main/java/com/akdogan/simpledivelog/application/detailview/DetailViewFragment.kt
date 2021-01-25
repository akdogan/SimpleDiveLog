package com.akdogan.simpledivelog.application.detailview

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.akdogan.simpledivelog.R
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.akdogan.simpledivelog.datalayer.database.DiveLogDatabase
import com.akdogan.simpledivelog.databinding.FragmentDetailViewBinding
import com.akdogan.simpledivelog.datalayer.repository.RepositoryApiStatus

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class DetailViewFragment : Fragment() {
    var t: Toast? = null
    lateinit var binding : FragmentDetailViewBinding
    lateinit var detailViewModel: DetailViewModel

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_detail_view, container, false)
        val application = requireNotNull(this.activity).application
        val datasource = DiveLogDatabase.getInstance(application).diveLogDatabaseDao
        val viewModelFactory = DetailViewModelFactory(datasource, application, DetailViewFragmentArgs.fromBundle(requireArguments()).diveLogId)
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
        detailViewModel.makeToast.observe(viewLifecycleOwner, Observer { message: String? ->
            message?.let{
                makeToast(message)
                detailViewModel.onMakeToastDone()
            }
        })

        //Observe the API Status to hide loading animation and display content
        detailViewModel.repositoryApiStatus.observe(viewLifecycleOwner, Observer {
            Log.i("ApiStatus Tracing", "Api Status observer called with status: $it")
            if (it == RepositoryApiStatus.DONE){
                binding.detailViewProgressCircular.hide()
                binding.detailViewMainContent.visibility = View.VISIBLE
            }
        })

        // Observe any Errors and Display the as Toast to the User
        detailViewModel.apiError.observe(viewLifecycleOwner, Observer { e: Exception? ->
            e?.let{
                makeToast( "Error: $e")
                detailViewModel.onErrorDone()
            }
        })

        // Observe Navigation back to the List (e.g. if the element could not be found)
        detailViewModel.navigateBack.observe(viewLifecycleOwner, Observer { act: Boolean? ->
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