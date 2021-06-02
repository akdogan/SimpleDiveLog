package com.akdogan.simpledivelog.application.ui.editview
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
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.work.WorkManager
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.application.ServiceLocator
import com.akdogan.simpledivelog.application.mainactivity.MainActivity
import com.akdogan.simpledivelog.application.ui.pictureview.PictureFragment
import com.akdogan.simpledivelog.databinding.FragmentEditViewBinding
import com.akdogan.simpledivelog.datalayer.ErrorCases
import com.akdogan.simpledivelog.datalayer.repository.RepositoryDownloadStatus
import com.akdogan.simpledivelog.datalayer.repository.RepositoryUploadStatus
import com.akdogan.simpledivelog.diveutil.Constants.SHARED_VIEW_MODEL_TAG
import com.google.android.material.textfield.TextInputEditText
import java.util.*

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class EditViewFragment : Fragment() {
    private var t: Toast? = null
    private lateinit var binding: FragmentEditViewBinding
    private val args: EditViewFragmentArgs by navArgs()
    private lateinit var editViewModel: EditViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_edit_view, container, false
        )

        val viewModelFactory = EditViewModelFactory(
            WorkManager.getInstance(requireContext()),
            ServiceLocator.repo,
            args.entryId
        )
        editViewModel = ViewModelProvider(this, viewModelFactory).get(
            SHARED_VIEW_MODEL_TAG,
            EditViewModel::class.java
        )
        binding.lifecycleOwner = this
        binding.editViewModel = editViewModel

        // Add Picture Fragment
        childFragmentManager.commit {
            add(R.id.edit_view_picture_container, PictureFragment::class.java, null)
        }

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
            if (act == true) {
                hideKeyboardFromView()
                val navCon = findNavController()
                navCon.previousBackStackEntry?.savedStateHandle?.set(getString(R.string.navigated_back_key), true)
                navCon.navigateUp()
                editViewModel.onNavigateBackFinished()
            }
        })

        // Observe when to present a message to the user
        editViewModel.makeToast.observe(viewLifecycleOwner, { code: Int? ->
            code?.let{
                makeToast(ErrorCases.getMessage(resources, code))
                editViewModel.onMakeToastFinished()
            }
        })

        // Observe unauthorized access and trigger logout if true
        editViewModel.unauthorizedAccess.observe(viewLifecycleOwner, {
            if (it == true) {
                try {
                    (requireActivity() as MainActivity).authExpired()
                } catch (e: ClassCastException){
                    makeToast("Cast to MainActivity failed")
                }
            }
        })

        editViewModel.networkAvailable.observe(viewLifecycleOwner, {
            Log.i("EditViewFragment Lifecycle", "Network changed to: $it")
        })

        //  Turns on the linear progress animation when uploading
        editViewModel.uploadStatus.observe(viewLifecycleOwner, {
            // TODO Create Espresso Test
            // Start Progress indeterminate, verifiy progress bar is shown indeterminate
            // Switch to determinate progress, verify progress is shown determinate
            // Increment progress, verify progress displayed matches
            // Switch to indeterminate, verify progress bar is shown indeterminate
            // Switch off upload, verify progress bar is not shown anymore
            // Maybe create fragment and activity in test? then control from fragment check in activity
            it?.let{
                Log.i("OFFLINE_UPLOAD", "upload status observer called with ${it.status}")
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
        picker.datePicker.maxDate = today
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


