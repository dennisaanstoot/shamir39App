package com.example.test.ui.notifications

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
        calculateSeedButton.setOnClickListener { calculate_seed() }

        return this.root
    }

    // calculate BIP39 mnemonic seed based on Shamir parts
    private fun calculate_seed() {
        println("Calculate seed");
        val partsText : EditText = this.root.findViewById(R.id.partsText);
        println(partsText.text)

        // test Shamir combine function
        val shamir = Shamir()
        val support = ShamirSupport()
        shamir.init(support.initBitsValue)

        val seed : List<String> = shamir.combine(support.testData.parts, support.wordlist)
        println(seed)
        // original seed is supposed to be support.testData.seed: seek deposit organ vintage absurd daughter trip rabbit simple father effort welcome fashion bike venture

        // set result to view
        this.root.findViewById<EditText>(R.id.seedView).setText(support.convertListToString(seed))

    }

}
