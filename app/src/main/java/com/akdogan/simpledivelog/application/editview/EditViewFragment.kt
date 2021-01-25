package com.akdogan.simpledivelog.application.editview
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.datalayer.database.DiveLogDatabase
import com.akdogan.simpledivelog.databinding.FragmentEditViewBinding
import com.akdogan.simpledivelog.datalayer.repository.RepositoryApiStatus
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import java.util.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class EditViewFragment : Fragment() {
    var t: Toast? = null
    lateinit var binding: FragmentEditViewBinding
    lateinit var editViewModel: EditViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_edit_view, container, false
        )
        val application = requireNotNull(this.activity).application
        val datasource = DiveLogDatabase.getInstance(application).diveLogDatabaseDao
        val viewModelFactory = EditViewModelFactory(
            datasource,
            application,
            EditViewFragmentArgs.fromBundle(requireArguments()).entryId
            //EditViewFragmentArgs.fromBundle(requireArguments()).createNewEntry
        )
        editViewModel = ViewModelProvider(this, viewModelFactory).get(EditViewModel::class.java)
        binding.lifecycleOwner = this
        binding.editViewModel = editViewModel


        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.diveDateEdit.inputType = InputType.TYPE_NULL
        binding.diveDateEdit.keyListener = null

        // Observe when to enable the save button
        editViewModel.enableSaveButton.observe(viewLifecycleOwner, Observer{
            binding.buttonSave.isEnabled = it == true
        })

        // Observe to navigate back
        editViewModel.navigateBack.observe(viewLifecycleOwner, Observer {
            if (it != null && it.act) {
                hideKeyboardFromView()
                if (it.param == null){
                    findNavController().navigate(EditViewFragmentDirections.actionDetailViewToListView())
                } else {
                    findNavController().navigate(EditViewFragmentDirections.actionEditViewFragmentToDetailViewFragment(it.param))
                }
                editViewModel.onNavigateBackFinished()
            }
        })

        // Observe when to present a message to the user
        editViewModel.makeToast.observe(viewLifecycleOwner, Observer{message: String? ->
            message?.let{
                makeToast(message)
                editViewModel.onMakeToastFinished()
            }
        })

        // Observe exceptions to display a message to the user
        editViewModel.apiError.observe(viewLifecycleOwner, Observer{ e: Exception? ->
            e?.let{
                makeToast(e.toString())
                editViewModel.onErrorDone()
            }
        })

        // Turns on the linear progress animation when uploading
        editViewModel.savingInProgress.observe(viewLifecycleOwner, Observer {
            if (it == true){
                binding.editViewUploadProgress.show()
            } else {
                binding.editViewUploadProgress.hide()
            }
        })

        // Observes the fetching status to hide the loading animatino and present the content
        editViewModel.repositoryApiStatus.observe(viewLifecycleOwner, Observer {
            Log.i("ApiStatus Tracing", "Api Status observer called with status: $it")
            if (it == RepositoryApiStatus.DONE || it == RepositoryApiStatus.ERROR){
                binding.editViewProgressCircular.hide()
                binding.editViewMainContent.visibility = View.VISIBLE
            }
        })

        binding.diveDateEdit.setOnFocusChangeListener{v, hasFocus ->
            if (hasFocus){
                openDatePicker(v)
            }
        }
        binding.diveDateEdit.setOnClickListener { v ->
            openDatePicker(v)
        }
    }

    private fun openDatePicker(v: View){
        hideKeyboardFromView()
        val myPickerCallback =
            DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(year, month, dayOfMonth)
                editViewModel.updateDate(cal.timeInMillis)
                binding.diveDurationContent.activateInput()
            }
        val cal = Calendar.getInstance()
        val today = System.currentTimeMillis()
        cal.timeInMillis = editViewModel.liveDate.value ?: today
        val picker = DatePickerDialog(
            v.context,
            myPickerCallback,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        picker.datePicker.maxDate = System.currentTimeMillis()
        picker.show()
    }


    private fun TextInputEditText.activateInput(){
        this.requestFocus()
        val inputManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.toggleSoftInput(0, 0)
    }

    private fun hideKeyboardFromView(){
        val v : View? = activity?.currentFocus
        if (v != null){
            val inputManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputManager.hideSoftInputFromWindow(v.windowToken, 0)
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


