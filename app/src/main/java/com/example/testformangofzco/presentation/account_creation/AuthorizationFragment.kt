package com.example.testformangofzco.presentation.account_creation

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import by.kirich1409.viewbindingdelegate.viewBinding
import com.example.domain.models.CheckAuth
import com.example.domain.models.Phone
import com.example.testformangofzco.App
import com.example.testformangofzco.R
import com.example.testformangofzco.databinding.FragmentAuthorizationBinding
import com.example.testformangofzco.presentation.user_profile.UserProfileActivity
import com.example.testformangofzco.presentation.account_creation.PhoneCodesBottomSheet.Companion.COUNTRY_CODE_KEY
import com.example.testformangofzco.presentation.account_creation.PhoneCodesBottomSheet.Companion.REQUEST_ACTION_KEY
import com.example.testformangofzco.presentation.account_creation.RegistrationFragment.Companion.PHONE_KEY
import com.example.testformangofzco.utils.AuthSharedPrefs
import com.example.testformangofzco.utils.MaskTextWatcher
import com.example.testformangofzco.utils.safelyNavigate
import com.example.testformangofzco.utils.setCountryCode
import com.example.testformangofzco.utils.showToast
import com.example.testformangofzco.viewmodels.account_creation.AuthorizationViewModel
import com.example.testformangofzco.viewmodels.account_creation.AuthorizationViewModelFactory
import javax.inject.Inject

// code: 133337

class AuthorizationFragment : Fragment(R.layout.fragment_authorization) {

    @Inject
    lateinit var authorizationViewModelFactory: AuthorizationViewModelFactory

    private val viewModel by lazy {
        ViewModelProvider(this, authorizationViewModelFactory)[AuthorizationViewModel::class.java]
    }

    private val binding by viewBinding(FragmentAuthorizationBinding::bind)

    private val sharedPreferences by lazy { AuthSharedPrefs(requireActivity()) }

    private var countryCode = DEFAULT_COUNTRY_CODE

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences.checkAuth(UserProfileActivity())

        inject()

        auth()

        checkAuth()

        setCountryCode()

        binding.etPhoneNumber.addTextChangedListener(MaskTextWatcher)
    }

    private fun inject() {
        App.component.injectAuthorizationFragment(authorizationFragment = this)
    }

    private fun auth() {
        binding.btnGetCode.setOnClickListener {
            if (binding.etPhoneNumber.text?.length != 13) showToast(getString(R.string.incorrect_phone))
            else {
                val phoneNumber = binding.etPhoneNumber.text?.replace("-".toRegex(), "").toString()
                val phone = Phone(phone = countryCode + phoneNumber)

                viewModel.auth(phone = phone)
                lifecycleScope.launchWhenCreated {
                    viewModel.success.collect { state ->
                        state.on(
                            error = {
                                showToast(message = getString(R.string.error, it.message))
                                return@on
                            },
                            loading = {
                                binding.clAuthorization.isGone = true
                                binding.pbAuthorization.isGone = false
                            },
                            success = { success ->
                                if (success.isSuccess) {
                                    binding.clAuthorization.isGone = false
                                    binding.pbAuthorization.isGone = true
                                    binding.etCode.isGone = false
                                    binding.rlPhone.isGone = true
                                    binding.btnGetCode.isGone = true
                                    binding.btnLogIn.isGone = false
                                } else {
                                    binding.clAuthorization.isGone = false
                                    binding.pbAuthorization.isGone = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkAuth() {
        binding.btnLogIn.setOnClickListener {
            val phoneNumber = binding.etPhoneNumber.text?.replace("-".toRegex(), "").toString()
            val phone = countryCode + phoneNumber
            val code = binding.etCode.text.toString()
            val checkAuth = CheckAuth(phoneNumber = phone, code = code)
            viewModel.checkAuth(checkAuth = checkAuth)
            lifecycleScope.launchWhenCreated {
                viewModel.auth.collect { state ->
                    state.on(
                        error = {
                            showToast(message = getString(R.string.error, it.message))
                            binding.clAuthorization.isGone = false
                            binding.pbAuthorization.isGone = true
                        },
                        loading = {
                            binding.clAuthorization.isGone = true
                            binding.pbAuthorization.isGone = false
                        },
                        success = { auth ->
                            if (auth.isUserExists) sharedPreferences.logIn(UserProfileActivity())
                            else findNavController().safelyNavigate(
                                R.id.action_authorizationFragment_to_registrationFragment,
                                bundleOf(PHONE_KEY to phone)
                            )
                        }
                    )
                }
            }
        }
    }

    private fun setCountryCode() {
        binding.tvCountryCode.setOnClickListener {
            findNavController().navigate(R.id.action_authorizationFragment_to_phoneCodesBottomSheet)
        }
        setFragmentResultListener(REQUEST_ACTION_KEY) { _, bundle ->
            bundle.getString(COUNTRY_CODE_KEY)?.let { countryCode ->
                this.countryCode = countryCode
                binding.tvCountryCode.text = countryCode
                binding.tvCountryCode.setCountryCode(countryCode)
            }
        }
    }

    private companion object {
        private const val DEFAULT_COUNTRY_CODE = "+7"
    }
}