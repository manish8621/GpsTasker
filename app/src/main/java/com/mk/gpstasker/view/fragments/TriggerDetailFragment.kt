package com.mk.gpstasker.view.fragments

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.text.PrecomputedText.Params
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout.LayoutParams
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.marginStart
import androidx.core.view.setMargins
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mk.gpstasker.App
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.FragmentTriggerDetailBinding
import com.mk.gpstasker.model.CREATE_MESSAGE_FRAGMENT_RESULT
import com.mk.gpstasker.model.MESSAGE_KEY
import com.mk.gpstasker.model.MOBILE_NUMBER_KEY
import com.mk.gpstasker.model.room.Trigger
import com.mk.gpstasker.viewmodel.TriggerDetailViewModel
import com.mk.gpstasker.viewmodel.TriggersDetailViewModelFactory

//TODO:add more actions
class TriggerDetailFragment : Fragment() {
    val args:TriggerDetailFragmentArgs by navArgs()

    lateinit var binding: FragmentTriggerDetailBinding
    lateinit var viewModel:TriggerDetailViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTriggerDetailBinding.inflate(inflater,container,false)
        //get repo ref from application
        val factory = TriggersDetailViewModelFactory((requireActivity().application as App).triggersRepository)
        viewModel = ViewModelProvider(this,factory)[TriggerDetailViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setClickListeners()
        setOnFragmentResult()
    }

    private fun setClickListeners() {
        binding.upBtn.setOnClickListener{
            findNavController().navigateUp()
        }
        binding.alertIv.setOnClickListener{
            viewModel.triggerAction.value = Trigger.ACTION_ALERT
            selectOption(it as ImageView , binding.alertTv)
            unSelectOption(binding.silentIv, binding.silentTv)
            unSelectOption(binding.messageIv, binding.messageTv)
        }
        binding.silentIv.setOnClickListener{
            Toast.makeText(context, "Puts phone into silent mode", Toast.LENGTH_SHORT).show()
            viewModel.triggerAction.value = Trigger.ACTION_SILENCE
            selectOption(it as ImageView , binding.silentTv)
            unSelectOption(binding.alertIv, binding.alertTv)
        }

        binding.messageIv.setOnClickListener{
            gotoCreateMessageScreen()
        }
        binding.addBtn.setOnClickListener{
            if(binding.labelEt.toString().isNotEmpty()) {
                viewModel.addTrigger(args.location)
                Toast.makeText(context, "trigger added", Toast.LENGTH_SHORT).show()
                goToTriggersListScreen()
            }
            else
                Toast.makeText(context, "label is empty", Toast.LENGTH_SHORT).show()
        }
    }



    private fun gotoCreateMessageScreen() {
        findNavController().navigate(TriggerDetailFragmentDirections.actionTriggerDetailFragmentToCreateMessageFragment(viewModel.mobileNumber,viewModel.message))
    }

    private fun goToTriggersListScreen() {
        findNavController().popBackStack(R.id.triggersFragment,false)
    }

    private fun unSelectOption(imageView: ImageView, textView: TextView) {
        imageView.tag =""
        imageView.background = null
        imageView.setColorFilter(requireContext().getColor(R.color.black))
        textView.setTextColor(requireContext().getColor(R.color.black))
    }

    private fun selectOption(imageView: ImageView, textView: TextView) {
        imageView.tag ="selected"
        imageView.setColorFilter(requireContext().getColor(R.color.primary_sat_highlight))
        imageView.background = AppCompatResources.getDrawable(requireContext(),R.drawable.highlight_bg_a)
        textView.setTextColor(requireContext().getColor(R.color.primary_sat_highlight))
    }
    private fun setOnFragmentResult() {
        setFragmentResultListener(CREATE_MESSAGE_FRAGMENT_RESULT) { s: String, bundle: Bundle ->
            viewModel.mobileNumber = bundle.getString(MOBILE_NUMBER_KEY) ?: return@setFragmentResultListener
            viewModel.message = bundle.getString(MESSAGE_KEY) ?: return@setFragmentResultListener
            with(binding) {
                this@TriggerDetailFragment.viewModel.triggerAction.value = Trigger.ACTION_MESSAGE
                selectOption(messageIv, messageTv)
                //show short message
                binding.messageTv.text = this@TriggerDetailFragment.viewModel.getMessageInfo()
                unSelectOption(alertIv, alertTv)
                unSelectOption(silentIv, silentTv)
            }
        }
    }
}