package com.github.drewstephensdesigns.amxsfamilyzone.ui.settings

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.github.drewstephensdesigns.amxsfamilyzone.LoginActivity
import com.github.drewstephensdesigns.amxsfamilyzone.R
import com.github.drewstephensdesigns.amxsfamilyzone.databinding.FragmentSettingsBinding
import com.github.drewstephensdesigns.amxsfamilyzone.utils.FirebaseUtils
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.maxkeppeler.sheets.core.SheetStyle
import com.maxkeppeler.sheets.option.DisplayMode
import com.maxkeppeler.sheets.option.Option
import com.maxkeppeler.sheets.option.OptionSheet

class SettingsFragment : Fragment() {

    private var _binding : FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        initCardViews()

        // Gets version info from Gradle/Manifest
        val versionHeader = resources.getString(R.string.version_header)
        val versionName = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0).versionName
        val formattedVersionText = resources.getString(R.string.version_text, versionHeader, versionName)

        // Displays build version
        binding.appVersionTV.text = formattedVersionText

        return binding.root
    }

    private fun initCardViews(){
        // Change Theme
        binding.theme.setOnClickListener{ changeTheme() }

        // Display Open Source Build Info
        binding.openSource.setOnClickListener {
            startActivity(Intent(requireActivity(), OssLicensesMenuActivity::class.java))
            OssLicensesMenuActivity.setActivityTitle(getString(
                    R.string.open_source))
        }

        // Feedback
        binding.feedback.setOnClickListener { feedBack() }

        // Social Media
        binding.connect.setOnClickListener { socialMedia() }

        // Log user out
        binding.logout.setOnClickListener { logout() }
    }

    private fun feedBack(){
        val versionHeader = resources.getString(R.string.version_header)
        val versionName = requireActivity().packageManager.getPackageInfo(requireActivity().packageName, 0).versionName
        val formattedVersionText = resources.getString(R.string.version_text, versionHeader, versionName)

        val send = Intent(Intent.ACTION_SENDTO)

        // Email subject (App Name and Version Code for troubleshooting)
        val uriText = "mailto:" + Uri.encode("drewstephensdesigns@gmail.com") +
                "?subject=" + Uri.encode("App Feedback: ") + resources.getString(R.string.app_name) + " " + formattedVersionText
        val uri = Uri.parse(uriText)
        send.data = uri

        // Displays apps that are able to handle email
        startActivity(Intent.createChooser(send, "Send feedback..."))
    }

    private fun logout() {
        FirebaseUtils.firebaseAuth.signOut()
        val intent = Intent(
            requireActivity(),
            LoginActivity::class.java
        )
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        startActivity(intent)
        requireActivity().finish()
    }

    private fun changeTheme(){
        OptionSheet().show(requireContext()){
            title("Set Theme")
            displayToolbar(true)
            style(SheetStyle.BOTTOM_SHEET)
            displayMode(DisplayMode.LIST)
            with(
                Option(R.drawable.ic_light_mode, R.string.light_mode),
                Option(R.drawable.ic_change_theme, R.string.dark_mode),
                Option(R.drawable.ic_system_follow, R.string.follow_system)
            )
            onPositive { index: Int, _: Option ->
                when(index){
                    0 -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        sharedPreferences.edit().putInt(
                            getString(R.string.pref_key_mode_night),
                            AppCompatDelegate.MODE_NIGHT_NO
                        ).apply()

                    }
                    1 -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        sharedPreferences.edit().putInt(
                            getString(R.string.pref_key_mode_night),
                            AppCompatDelegate.MODE_NIGHT_YES
                        ).apply()

                    }
                    2 -> {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                        sharedPreferences.edit().putInt(
                            getString(R.string.pref_key_mode_night),
                            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        ).apply()
                    }
                }
            }
        }
    }

    private fun socialMedia(){
        OptionSheet().show(requireContext()){
            title("Let's Connect!")
            displayToolbar(true)
            style(SheetStyle.BOTTOM_SHEET)
            displayMode(DisplayMode.LIST)
            with(
                Option(R.drawable.ic_person, "LinkedIn"),
                Option(R.drawable.snoo, "Reddit"),
                Option(R.drawable.ic_camera, "Instagram")
            )
            onPositive { index: Int, _: Option ->
                when(index){
                    0 -> {openLink(resources.getString(R.string.linkedIn))}
                    1 -> {openLink(resources.getString(R.string.reddit))}
                    2 -> {openLink(resources.getString(R.string.instagram))}
                }
            }
        }
    }

    private fun openLink(link: String){
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = (Uri.parse(link))
        startActivity(intent)
    }
}