package com.example.test.ui.notifications

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.test.R
import com.example.test.Shamir
import com.example.test.ShamirSupport

class NotificationsFragment : Fragment() {

    private lateinit var notificationsViewModel: NotificationsViewModel
    private lateinit var root : View

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        notificationsViewModel =
                ViewModelProviders.of(this).get(NotificationsViewModel::class.java)
        this.root = inflater.inflate(R.layout.fragment_notifications, container, false)

        // bind calculate_seeds() method to button click event
        val calculateSeedButton : Button = this.root.findViewById(R.id.calculate_seed_button)
        calculateSeedButton.setOnClickListener { calculateSeed() }

        return this.root
    }

    // calculate BIP39 mnemonic seed based on Shamir parts
    private fun calculateSeed() {
        // init classes
        val shamir = Shamir()

        // result containers
        var result : List<String>? = null
        var error: String? = null
        val resultsContainer: EditText = this.root.findViewById(R.id.seedView);

        // get user-provided input
        val partsText : EditText = this.root.findViewById(R.id.partsText);
        val parts : List<List<String>> = ShamirSupport.convertPartsStringToList(partsText.text.toString())

        // combine shamir parts
        if (error == null) {
            try {
                shamir.init(ShamirSupport.initBitsValue)
                result = shamir.combine(parts, ShamirSupport.wordlist)
            } catch(e: IllegalStateException) {
                error = e.message
            }
        }

        // display results
        if (result != null && error == null) {
            println(result)
            resultsContainer.setText(ShamirSupport.convertListToString(result))
            resultsContainer.setTextColor(Color.parseColor("#000000")) // regular color

        // display error
        } else {
            resultsContainer.setText("Error: " + error)
            resultsContainer.setTextColor(Color.parseColor("#FF0000")) // error color
        }

    }

}
