package com.example.test.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import com.example.test.R
import com.example.test.Shamir
import com.example.test.ShamirSupport
import java.lang.IllegalStateException

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
        val support = ShamirSupport()
        val shamir = Shamir()
        var parts: List<Array<String>>? = null
        var error: String? = null
        val resultsContainer: EditText = this.root.findViewById(R.id.partsView);

        // get user-provided input
        val seedView: EditText = this.root.findViewById(R.id.seedText);
        val seed: List<String> = support.convertStringToList(seedView.text.toString());

        val numberPartsSpinner: Spinner = this.root.findViewById(R.id.spinner_number_parts)
        val numberReconstructionPartsSpinner: Spinner =
            this.root.findViewById(R.id.spinner_number_reconstruct_parts)
        val numberParts: Int = numberPartsSpinner.selectedItem.toString().toInt()
        val numberReconstructionParts: Int =
            numberReconstructionPartsSpinner.selectedItem.toString().toInt()

        // verify values
        if (numberReconstructionParts > numberParts) {
            error =
                "The amount of parts necessary for reconstruction cannot be higher then the total number of parts"
        }

        // attempt to generate Shamir parts
        if (error == null) {
            try {
                shamir.init(support.initBitsValue)
                parts = shamir.split(seed, support.wordlist, numberReconstructionParts, numberParts)
            } catch (e: IllegalStateException) {
                error = e.message
            }
        }

        // display results
        if (parts != null && error == null) {
            for (part in parts)
                println(part.joinToString(separator = " "))

            resultsContainer.setText(parts.map { p -> p.joinToString(separator = " ") }.joinToString(separator = "\n"))
            resultsContainer.setTextColor(Color.parseColor("#000000")) // regular color

        // error handling
        } else {
            resultsContainer.setText("Error: " + error)
            resultsContainer.setTextColor(Color.parseColor("#FF0000")) // error color
        }

    }

}
