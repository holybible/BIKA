package com.shizq.bika.ui.settings

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.DropDownPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.shizq.bika.MyApp
import com.shizq.bika.R
import com.shizq.bika.network.RetrofitUtil
import com.shizq.bika.network.base.BaseHeaders
import com.shizq.bika.network.base.BaseResponse
import com.shizq.bika.ui.account.AccountActivity
import com.shizq.bika.utils.AppVersion
import com.shizq.bika.utils.GlideCacheUtil
import com.shizq.bika.utils.SPUtil
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.observers.DefaultObserver
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.HttpException

// TODO 没有bug 以后再优化 懒
class SettingsPreferenceFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceChangeListener,
    Preference.OnPreferenceClickListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)
        val setting_close: Preference? = findPreference("setting_close")//清理缓存
        val setting_addresses: Preference? = findPreference("setting_addresses")//分流节点
        val setting_punch: Preference? = findPreference("setting_punch")//自动打卡
        val setting_night: Preference? = findPreference("setting_night")//夜间模式
        val setting_app_ver: Preference? = findPreference("setting_app_ver")//应用版本
        val setting_change_password: Preference? =
            findPreference("setting_change_password")//修改密码
        val setting_exit: Preference? = findPreference("setting_exit")//账号退出

        setting_close?.onPreferenceClickListener = this
        setting_addresses?.onPreferenceChangeListener = this
        setting_punch?.onPreferenceChangeListener = this
        setting_night?.onPreferenceChangeListener = this
        setting_app_ver?.onPreferenceClickListener = this
        setting_change_password?.onPreferenceClickListener = this
        setting_exit?.onPreferenceClickListener = this

        //自动打卡
        setting_punch as SwitchPreferenceCompat
        setting_punch.summary = if (setting_punch.isChecked) "开启" else "关闭"

        //夜间模式
        setting_night as DropDownPreference
        setting_night.summary = setting_night.value

        //当前版本
        setting_app_ver?.summary = "当前版本：${AppVersion().name()}(${AppVersion().code()})"

        //节点分流
        setting_addresses as DropDownPreference
        val addresses = SPUtil.get(context, "addresses", "")as String
        setting_addresses.summary= if (addresses=="addresses2") "分流二" else "分流一"//显示当前的分流
        val addressesList: ArrayList<String> = ArrayList()
        addressesList.add("分流一")
        addressesList.add("分流二")
        setting_addresses.entries=addressesList.toTypedArray()//展示当前所有分流
        setting_addresses.entryValues=addressesList.toTypedArray()

        //清理图片
        setting_close?.summary=GlideCacheUtil.getInstance().getCacheSize(context)


    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            "setting_close" -> {
                GlideCacheUtil.getInstance().clearImageAllCache(context)
                preference.summary="0.0Byte"
                Toast.makeText(activity, "清理完成", Toast.LENGTH_SHORT).show()
                return true
            }


            "setting_change_password" -> {

                activity?.let {
                    val dia = MaterialAlertDialogBuilder(it)
                        .setTitle("修改密码")
                        .setView(R.layout.view_dialog_edit_text_change_password)
                        .setCancelable(false)
                        .setPositiveButton("修改", null)
                        .setNegativeButton("取消", null)
                        .show();//在按键响应事件中显示此对话框 }
                    dia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        dia as AlertDialog
                        val newPasswordLayout: TextInputLayout? =
                            dia.findViewById(android.R.id.icon1)
                        val confirmPasswordLayout: TextInputLayout? =
                            dia.findViewById(android.R.id.icon2)
                        val newPassword: TextInputEditText? = dia.findViewById(android.R.id.text1)
                        val confirmPassword: TextInputEditText? =
                            dia.findViewById(android.R.id.text2)

                        newPassword?.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(
                                s: CharSequence,
                                start: Int,
                                count: Int,
                                after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s: CharSequence,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                            }

                            override fun afterTextChanged(s: Editable) {
                                newPasswordLayout?.isErrorEnabled = false
                            }
                        })
                        confirmPassword?.addTextChangedListener(object : TextWatcher {
                            override fun beforeTextChanged(
                                s: CharSequence,
                                start: Int,
                                count: Int,
                                after: Int
                            ) {
                            }

                            override fun onTextChanged(
                                s: CharSequence,
                                start: Int,
                                before: Int,
                                count: Int
                            ) {
                            }

                            override fun afterTextChanged(s: Editable) {
                                confirmPasswordLayout?.isErrorEnabled = false
                            }
                        })
                        if (newPassword?.text.toString().trim().isEmpty()) {
                            newPasswordLayout?.isErrorEnabled = true
                            newPasswordLayout?.error = "新密码不能为空！"
                        } else if (newPassword?.text.toString().trim().length < 8) {
                            newPasswordLayout?.isErrorEnabled = true
                            newPasswordLayout?.error = "密码不能小于8字！"
                        }
                        if (confirmPassword?.text.toString().trim().isEmpty()) {
                            confirmPasswordLayout?.isErrorEnabled = true
                            confirmPasswordLayout?.error = "确认密码不能为空！"
                        } else if (confirmPassword?.text.toString()
                                .trim() != newPassword?.text.toString().trim()
                        ) {
                            confirmPasswordLayout?.isErrorEnabled = true
                            confirmPasswordLayout?.error = "确认密码与新密码不符！"
                        }
                        if (confirmPassword?.text.toString().trim().isNotEmpty()
                            && newPassword?.text.toString().trim().isNotEmpty()
                            && newPassword?.text.toString().trim().length >= 8
                            && (confirmPassword?.text.toString().trim()
                                    == newPassword?.text.toString().trim())
                        ) {
                            changePassword("", newPassword?.text.toString().trim())
                            // TODO 添加加载进度条
                            dia.dismiss()
                        }
                    }

                }
                return false
            }

            "setting_exit" -> {
                activity?.let {
                    MaterialAlertDialogBuilder(it)
                        .setTitle("你确定要退出登录吗")
                        .setPositiveButton("确定") { dialog, which ->
                            SPUtil.remove(MyApp.contextBase, "token")
                            startActivity(Intent(activity, AccountActivity::class.java))
                            activity?.finish()
                        }
                        .setNegativeButton("取消", null)
                        .show()

                }
                return false
            }

        }
        return false
    }

    override fun onPreferenceChange(preference: Preference, value: Any?): Boolean {
        when (preference.key) {
            "setting_punch" -> {
                // TODO 一个小bug 开关会影响toolbar颜色变化
                preference as SwitchPreferenceCompat
                preference.summary = if (value as Boolean) "开启" else "关闭"
                return true
            }
            "setting_addresses" -> {
                value as String
                preference as DropDownPreference
                preference.value = value
                preference.summary = value

                SPUtil.put(context, "addresses", if(value=="分流二") "addresses2" else "addresses1")
                return true
            }
            "setting_night" -> {
                value as String
                preference as DropDownPreference
                preference.summary = value
                preference.value = value
                AppCompatDelegate.setDefaultNightMode(
                    when (value) {
                        "开启"->AppCompatDelegate.MODE_NIGHT_YES
                        "关闭"->AppCompatDelegate.MODE_NIGHT_NO
                        "跟随系统"->AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        else->AppCompatDelegate.MODE_NIGHT_NO
                    }
                )
                return true
            }
        }
        return true
    }

    private fun changePassword(oldpassword: String, password: String) {
        var old = oldpassword
        if (oldpassword == "") {
            old = SPUtil.get(MyApp.contextBase, "password", "") as String
        }

        val body = RequestBody.create(
            MediaType.parse("application/json; charset=UTF-8"),
            JsonObject().apply {
                addProperty("new_password", password)
                addProperty("old_password", old)
            }.asJsonObject.toString()
        )
        val headers = BaseHeaders("users/password", "PUT").getHeaderMapAndToken()

        RetrofitUtil.service.changePasswordPUT(body, headers)
            .compose { upstream ->
                upstream.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
            }
            .subscribe(object : DefaultObserver<BaseResponse<*>>() {

                override fun onNext(baseResponse: BaseResponse<*>) {
                    if (baseResponse.code == 200) {

                        //保存密码
                        SPUtil.put(MyApp.contextBase, "password", password)
                        Toast.makeText(activity, "修改密码成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            activity,
                            "修改密码失败，网络错误code=${baseResponse.code} error=${baseResponse.error} message=${baseResponse.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onError(e: Throwable) {
                    if (e is HttpException) {
                        val responseBody = e.response()!!.errorBody()
                        if (responseBody != null) {
                            val type = object : TypeToken<BaseResponse<*>>() {}.type
                            val baseResponse: BaseResponse<*> =
                                Gson().fromJson(responseBody.string(), type)
                            if (baseResponse.code == 400 && baseResponse.error == "1010") {
                                showAlertDialog()
                            }
                        } else {
                            Toast.makeText(activity, "修改密码失败，网络错误", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(activity, "修改密码失败，网络错误", Toast.LENGTH_SHORT).show()

                    }
                }

                override fun onComplete() {}
            })
    }

    fun showAlertDialog() {
        activity?.let {
            val dia: AlertDialog = MaterialAlertDialogBuilder(it)
                .setTitle("修改密码失败，请重试")
                .setView(R.layout.view_dialog_edit_text_change_password_old)
                .setCancelable(false)
                .setPositiveButton("修改", null)
                .setNegativeButton("取消", null)
                .show();//在按键响应事件中显示此对话框 }
            dia.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val oldPasswordLayout: TextInputLayout? = dia.findViewById(R.id.old_password_layout)
                val oldPassword: TextInputEditText? = dia.findViewById(R.id.new_password)
                val newPasswordLayout: TextInputLayout? = dia.findViewById(R.id.old_password)
                val newPassword: TextInputEditText? = dia.findViewById(R.id.confirm_password_layout)
                val confirmPasswordLayout: TextInputLayout? = dia.findViewById(R.id.new_password_layout)
                val confirmPassword: TextInputEditText? = dia.findViewById(R.id.confirm_password)

                oldPassword?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable) { oldPasswordLayout?.isErrorEnabled = false }
                })

                newPassword?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable) { newPasswordLayout?.isErrorEnabled = false }
                })

                confirmPassword?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable) { confirmPasswordLayout?.isErrorEnabled = false }
                })


                if (oldPassword?.text.toString().trim().isEmpty()) {
                    oldPasswordLayout?.isErrorEnabled = true
                    oldPasswordLayout?.error = "旧密码不能为空！"
                } else if (oldPassword?.text.toString().trim().length < 8) {
                    oldPasswordLayout?.isErrorEnabled = true
                    oldPasswordLayout?.error = "密码不能小于8字！"
                }

                if (newPassword?.text.toString().trim().isEmpty()) {
                    newPasswordLayout?.isErrorEnabled = true
                    newPasswordLayout?.error = "新密码不能为空！"
                } else if (newPassword?.text.toString().trim().length < 8) {
                    newPasswordLayout?.isErrorEnabled = true
                    newPasswordLayout?.error = "密码不能小于8字！"
                }

                if (confirmPassword?.text.toString().trim().isEmpty()) {
                    confirmPasswordLayout?.isErrorEnabled = true
                    confirmPasswordLayout?.error = "确认密码不能为空！"
                } else if (confirmPassword?.text.toString().trim() != newPassword?.text.toString().trim()) {
                    confirmPasswordLayout?.isErrorEnabled = true
                    confirmPasswordLayout?.error = "确认密码与新密码不符！"
                }

                if (oldPassword?.text.toString().trim().isNotEmpty()
                    && oldPassword?.text.toString().trim().length >= 8
                    && confirmPassword?.text.toString().trim().isNotEmpty()
                    && newPassword?.text.toString().trim().isNotEmpty()
                    && newPassword?.text.toString().trim().length >= 8
                    && (confirmPassword?.text.toString().trim() == newPassword?.text.toString().trim())
                ) {
                    changePassword(
                        oldPassword?.text.toString().trim(),
                        newPassword?.text.toString().trim()
                    )
                    // TODO 添加加载进度条
                    dia.dismiss()
                }
            }

        }
    }
}