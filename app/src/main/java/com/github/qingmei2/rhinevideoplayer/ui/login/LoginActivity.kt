package com.github.qingmei2.rhinevideoplayer.ui.login

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import com.github.qingmei2.rhinevideoplayer.R
import com.github.qingmei2.rhinevideoplayer.base.BaseActivity
import com.github.qingmei2.rhinevideoplayer.databinding.ActivityLoginBinding
import org.kodein.di.Kodein
import org.kodein.di.android.retainedKodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance

class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    override val kodein: Kodein by retainedKodein {
        extend(parentKodein)
        import(loginKodeinModule)
        bind<LoginActivity>() with instance(this@LoginActivity)
    }

    private val delegate: LoginViewDelegate by instance()

    override val layoutId: Int = R.layout.activity_login

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.delegate = delegate
    }

    companion object {

        fun launch(context: FragmentActivity) =
            context.apply {
                startActivity(Intent(this, LoginActivity::class.java))
            }
    }
}
