package com.hul.skb.supervisor.ui.form

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hul.R

class SKBSupervisorFormFragment : Fragment() {

    companion object {
        fun newInstance() = SKBSupervisorFormFragment()
    }

    private val viewModel: SKBSupervisorFormViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_s_k_b_supervisor_form, container, false)
    }
}