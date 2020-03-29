package com.example.test.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.test.R
import com.example.test.Shamir
import com.example.test.ShamirSupport
import kotlinx.android.synthetic.main.fragment_dashboard.*
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var root : View

    private val defaultPartsNumberValue: String = "5"
    private val defaultReconstructionPartsNumberValue: String = "3"

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        dashboardViewModel =
                ViewModelProviders.of(this).get(DashboardViewModel::class.java)
        this.root = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // bind calculate_parts() method to button click event
        val calculatePartsButton : Button = this.root.findViewById(R.id.calculate_parts_button)
        calculatePartsButton.setOnClickListener { calculate_parts() }

        // dropdown spinner: total number of parts
        val numberPartsSpinner : Spinner = this.root.findViewById(R.id.spinner_number_parts)
        val spinnerAdapter = ArrayAdapter.createFromResource(
            this.activity!!,
            R.array.parts_number_selection,
            android.R.layout.simple_spinner_dropdown_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            numberPartsSpinner.adapter = adapter
        }
        // set default value
        numberPartsSpinner.setSelection(spinnerAdapter.getPosition(this.defaultPartsNumberValue))

        // dropdown spinner: parts necessary for reconstruction
        val numberReconstructionPartsSpinner : Spinner = this.root.findViewById(R.id.spinner_number_reconstruct_parts)
        val reconstructionSpinnerAdapter = ArrayAdapter.createFromResource(
            this.activity!!,
            R.array.reconstruct_parts_number_selection,
            android.R.layout.simple_spinner_dropdown_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            numberReconstructionPartsSpinner.adapter = adapter
        }
        // set default value
        numberReconstructionPartsSpinner.setSelection(reconstructionSpinnerAdapter.getPosition(this.defaultReconstructionPartsNumberValue))

        return this.root
    }

    // calculate Shamir parts based on seed
    private fun calculate_parts() {
        println("Calculate parts");
        val seedText : EditText = this.root.findViewById(R.id.seedText);
        println(seedText.text)

        // attempt to generate Shamir parts
        // example to be verified at https://iancoleman.io/shamir39/#english
        val shamir = Shamir()
        val support = ShamirSupport()
        shamir.init(support.initBitsValue)
        val parts: List<Array<String>> = shamir.split(support.testData.seed, support.wordlist, support.testData.partsRequiredForReconstruction, support.testData.numberOfParts)
//        var parts : List<String> = shamir.share("seek deposit organ vintage absurd daughter trip rabbit simple father effort welcome fashion bike venture", 5, 3, 0, false)

        for (part in parts)
            println(part.joinToString(separator = " "))

        val partsView : EditText = this.root.findViewById(R.id.partsView);

        partsView.setText(parts.map { p -> p.joinToString(separator = " ") }.joinToString(separator = "\n"))

    }
}
