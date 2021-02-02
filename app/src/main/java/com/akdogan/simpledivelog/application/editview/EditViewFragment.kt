package com.akdogan.simpledivelog.application.editview
import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.databinding.FragmentEditViewBinding
import com.akdogan.simpledivelog.datalayer.repository.RepositoryDownloadStatus
import com.akdogan.simpledivelog.datalayer.repository.RepositoryUploadStatus
import com.google.android.material.textfield.TextInputEditText
import java.util.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class EditViewFragment : Fragment() {
    private var t: Toast? = null
    private lateinit var binding: FragmentEditViewBinding
    private lateinit var editViewModel: EditViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_edit_view, container, false
        )
        val application = requireNotNull(this.activity).application
        val viewModelFactory = EditViewModelFactory(
            application,
            EditViewFragmentArgs.fromBundle(requireArguments()).entryId
        )
        editViewModel = ViewModelProvider(this, viewModelFactory).get(EditViewModel::class.java)
        binding.lifecycleOwner = this
        binding.editViewModel = editViewModel

        // Join into the options menu
        setHasOptionsMenu(true)

        return binding.root
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_settings).isVisible = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.diveDateEdit.inputType = InputType.TYPE_NULL
        binding.diveDateEdit.keyListener = null

        // Observe when to enable the save button
        editViewModel.enableSaveButton.observe(viewLifecycleOwner, {
            binding.buttonSave.isEnabled = it == true
        })

        // Observe to navigate back
        editViewModel.navigateBack.observe(viewLifecycleOwner, { act: Boolean? ->
            Log.i("CREATE ENTRY TRACING", "navigate Back Observer with $act")
            if (act == true) {
                hideKeyboardFromView()
                val navCon = findNavController()
                navCon.previousBackStackEntry?.savedStateHandle?.set(getString(R.string.navigated_back_key), true)
                navCon.navigateUp()
                editViewModel.onNavigateBackFinished()
            }
        })

        // Observe when to present a message to the user
        editViewModel.makeToast.observe(viewLifecycleOwner, { message: String? ->
            message?.let{
                makeToast(message)
                editViewModel.onMakeToastFinished()
            }
        })

        // Observe exceptions to display a message to the user
        editViewModel.apiError.observe(viewLifecycleOwner, { e: Exception? ->
            e?.let{
                makeToast(e.toString())
                editViewModel.onErrorDone()
            }
        })

        editViewModel.networkAvailable.observe(viewLifecycleOwner, {
            Log.i("EditViewFragment Lifecycle", "Network changed to: $it")
        })

        //  Turns on the linear progress animation when uploading
        editViewModel.uploadStatus.observe(viewLifecycleOwner, {
            it?.let{
                when (it.status){
                    RepositoryUploadStatus.INDETERMINATE_UPLOAD -> {
                        binding.editViewUploadProgress.apply {
                            if (!this.isIndeterminate){
                                visibility = View.INVISIBLE
                                this.isIndeterminate = true
                                this.progress = 70
                                this.visibility = View.VISIBLE
                            } else {
                                this.visibility = View.VISIBLE
                            }
                        }
                    }
                    RepositoryUploadStatus.PROGRESS_UPLOAD -> {
                        binding.editViewUploadProgress.apply{
                            if (this.isIndeterminate){
                                this.setProgressCompat(it.percentage, true)
                            } else {
                                this.progress = it.percentage
                            }
                        }
                    }
                    RepositoryUploadStatus.DONE -> {
                        binding.editViewUploadProgress.apply {
                            this.visibility = View.INVISIBLE
                        }
                    }
                }
            }
        })

        // Observes the fetching status to hide the loading animation and present the content
        editViewModel.downloadStatus.observe(viewLifecycleOwner, {
            Log.i("ApiStatus Tracing", "Api Status observer called with status: $it")
            if (it == RepositoryDownloadStatus.DONE || it == RepositoryDownloadStatus.ERROR){
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


