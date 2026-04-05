package com.example.way.ui.auth

import android.app.Activity
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.way.R
import com.example.way.databinding.FragmentSignupBinding
import com.example.way.util.Result
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import android.util.Log
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

/**
 * Signup screen — create account with email/password or Google.
 */
@AndroidEntryPoint
class SignupFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { authViewModel.handleGoogleSignInResult(it) }
                    ?: showError("Google Sign-In failed: no ID token")
            } catch (e: ApiException) {
                showError("Google Sign-In failed: ${e.localizedMessage}")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hasNavigated = false
        authViewModel.resetAuthState()
        setupGoogleSignIn()
        observeAuthState()

        binding.tvSignIn.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSignUp.setOnClickListener {
            if (validateFields()) {
                val name = binding.etName.text.toString().trim()
                val email = binding.etEmail.text.toString().trim()
                val password = binding.etPassword.text.toString()
                authViewModel.signup(name, email, password)
            }
        }

        binding.btnGoogleSignUp.setOnClickListener {
            if (::googleSignInClient.isInitialized) {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            } else {
                showError("Google Sign-In is not configured yet")
            }
        }
    }

    private var hasNavigated = false

    private fun setupGoogleSignIn() {
        try {
            val webClientId = getString(R.string.default_web_client_id)
            if (webClientId.isBlank() || webClientId.startsWith("PLACEHOLDER")) {
                binding.btnGoogleSignUp.isEnabled = false
                binding.btnGoogleSignUp.alpha = 0.5f
                return
            }
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
        } catch (e: Exception) {
            binding.btnGoogleSignUp.isEnabled = false
            binding.btnGoogleSignUp.alpha = 0.5f
        }
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { result ->
            if (result == null || hasNavigated) return@observe
            Log.d("SignupFragment", "Auth state: $result")
            when (result) {
                is Result.Loading -> {
                    Log.d("SignupFragment", "Loading...")
                    setLoading(true)
                }
                is Result.Success -> {
                    Log.d("SignupFragment", "Success! Navigating to setup")
                    setLoading(false)
                    hasNavigated = true
                    findNavController().navigate(R.id.action_signup_to_setup)
                    authViewModel.resetAuthState()
                }
                is Result.Error -> {
                    Log.e("SignupFragment", "Error: ${result.message}")
                    setLoading(false)
                    showError(result.message)
                    authViewModel.resetAuthState()
                }
            }
        }
    }

    private fun validateFields(): Boolean {
        var valid = true
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()

        if (name.isEmpty()) {
            binding.tilName.error = "Name is required"
            valid = false
        } else {
            binding.tilName.error = null
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email format"
            valid = false
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            valid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = "Minimum 6 characters"
            valid = false
        } else {
            binding.tilPassword.error = null
        }

        if (confirm.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm password"
            valid = false
        } else if (confirm != password) {
            binding.tilConfirmPassword.error = "Passwords don't match"
            valid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return valid
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSignUp.isEnabled = !loading
        binding.btnGoogleSignUp.isEnabled = !loading
        binding.loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
