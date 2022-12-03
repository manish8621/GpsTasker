package com.mk.gpstasker.view.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.mk.gpstasker.R
import com.mk.gpstasker.databinding.FragmentCreateMessageBinding
import com.mk.gpstasker.model.CREATE_MESSAGE_FRAGMENT_BUNDLE
import com.mk.gpstasker.model.CREATE_MESSAGE_FRAGMENT_RESULT
import com.mk.gpstasker.model.MESSAGE_KEY
import com.mk.gpstasker.model.MOBILE_NUMBER_KEY

class CreateMessageFragment : Fragment() {

    lateinit var binding :FragmentCreateMessageBinding
    private val args:CreateMessageFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCreateMessageBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDefaultValues()
        setOnclickListeners()
    }

    private fun setDefaultValues() {
        binding.mobileNumberEt.setText(args.mobileNumber)
        binding.messageEt.setText(args.message)
    }

    private fun setOnclickListeners() {
        binding.upBtn.setOnClickListener{
            findNavController().navigateUp()
        }
        binding.doneBtn.setOnClickListener{

            binding.mobileNumberEt.text?.let { mob->
                binding.messageEt.text?.let { msg->

                    if(mob.length<10) {
                        (Toast.makeText(context, "Enter a valid mobile number", Toast.LENGTH_SHORT)
                            .show())
                        return@setOnClickListener
                    }
                    if(msg.isEmpty()) {
                        (Toast.makeText(context, "Enter a message", Toast.LENGTH_SHORT)
                            .show())
                        return@setOnClickListener
                    }
                    setFragmentResult(
                            CREATE_MESSAGE_FRAGMENT_RESULT, bundleOf(
                                MOBILE_NUMBER_KEY to mob.toString(), MESSAGE_KEY to msg.toString()
                            )
                        )
                        findNavController().navigateUp()
                }
            }
        }
    }


}