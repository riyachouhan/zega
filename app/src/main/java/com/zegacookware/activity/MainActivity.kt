package com.zegacookware.activity

import android.app.AlertDialog
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.zegacookware.R
import com.zegacookware.activity.bl.DigitalTimerActivity
import com.zegacookware.activity.setting.SettingActivity
import com.zegacookware.model.AddDeviceRequest
import com.zegacookware.model.UserDevices
import com.zegacookware.model.user.UserModel
import com.zegacookware.model.user.UserResult
import com.zegacookware.network.Constant
import com.zegacookware.util.CommonUtility
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : BaseActivity()/*, View.OnClickListener*/ {
    private lateinit var userData: UserResult
    private lateinit var mContext: Context
    private var isFromTimer: Boolean? = null
    lateinit var languageCh: String

    override fun recreate() {
        CommonUtility.setLocale(CommonUtility.loadLanguage(this).toString(),this)
        super.recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        languageCh = CommonUtility.loadLanguage(this)
        CommonUtility.setLocale(CommonUtility.loadLanguage(this),this)
        setContentView(R.layout.activity_main)
        overridePendingTransition(0, 0)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        mContext = this
        isFromTimer = intent.getBooleanExtra("isFromTimer", false)
        val btnRecipes: RelativeLayout = findViewById(R.id.btnRecipes)
        val btnFAQs: RelativeLayout = findViewById(R.id.btnFAQs)
        val btnQuickCook: RelativeLayout = findViewById(R.id.btnQuickCook)
        val btnSetting: RelativeLayout = findViewById(R.id.btnSetting)

        btnRecipes.setOnClickListener(View.OnClickListener {
            Constant.selfcook=false
            Constant.popupint=false
            startActivity(Intent(this@MainActivity, RecipesActivity::class.java))
            //throw RuntimeException("Test Crash")
        })

        btnFAQs.setOnClickListener(View.OnClickListener {
            Constant.selfcook=false
            Constant.popupint=false
            startActivity(Intent(this@MainActivity, FAQActivity::class.java))
        })

        btnQuickCook.setOnClickListener(View.OnClickListener {
            Constant.selfcook=false
            Constant.popupint=false
            startActivity(Intent(this@MainActivity, QuickCookActivity::class.java))
        })

        btnSetting.setOnClickListener(View.OnClickListener {
            Constant.selfcook=false
            Constant.popupint=false
            startActivity(Intent(this@MainActivity, SettingActivity::class.java))
        })

        userData = CommonUtility.getUserData(Constant.userInfo, this@MainActivity)
        tvUserName.text = "${userData.name}'S ZEGA"
        getDevices()

        if(Constant.completecook_boolean==true)
        {
            Constant.completecook_boolean=false
            showDialog(resources.getString(R.string.cooking_complete))
        }
    }


    private fun connectDevice() {
        if (CommonUtility.getBooleanPreference(
                "isDeviceAvailable",
                mContext
            ) && CommonUtility.getBooleanPreference(
                Constant.isDigital, mContext
            )
        ) {
            if (!CommonUtility.getBooleanPreference(
                    Constant.cookingIsRunning,
                    mContext
                )
            ) {
//                UserModel.isHardwareOutOfRange = false
                connectHardware(this@MainActivity)
            }

        }
    }

    private fun getDevices()
    {
        if(userData.userId.toString()!=null && userData.userId.toString()!="")
        {
            Constant.service.getUserDevices(AddDeviceRequest(user_id = "" + userData.userId ,lang = CommonUtility.loadLanguage(this))).apply {
                enqueue(object : Callback<UserDevices> {
                    override fun onFailure(call: Call<UserDevices>, t: Throwable) {
                    }

                    override fun onResponse(
                        call: Call<UserDevices>,
                        response: Response<UserDevices>
                    ) {
                        if (response.isSuccessful && response.body()?.status == 1) {
                            CommonUtility.setBooleanPreference(
                                true,
                                "isDeviceAvailable",
                                this@MainActivity
                            )
                            val diId = CommonUtility.getStringPreference(
                                "deviceId",
                                mContext
                            )
                            if (diId.isNullOrEmpty()) {
                                CommonUtility.setStringPreference(
                                    response.body()?.userDeviceList!![0].macId!!,
                                    "deviceId",
                                    mContext
                                )
                                if (response.body()?.userDeviceList!![0].deviceName.equals(
                                        "digital",
                                        true
                                    )
                                ) {
                                    CommonUtility.setBooleanPreference(
                                        true,
                                        Constant.isDigital,
                                        mContext
                                    )
                                } else {
                                    CommonUtility.setBooleanPreference(
                                        false,
                                        Constant.isDigital,
                                        mContext
                                    )
                                }
                            }
                            connectDevice()
                        } else {
                            CommonUtility.setBooleanPreference(
                                false,
                                "isDeviceAvailable",
                                this@MainActivity
                            )
                        }
                    }
                })
            }
        }else
        {
            Toast.makeText(applicationContext,"user is wrong "+userData.userId.toString(),Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (CommonUtility.getBooleanPreference(Constant.cookingIsRunning, mContext)) {
            Handler().postDelayed({
                LocalBroadcastManager.getInstance(mContext)
                    .sendBroadcast(Intent("AppIsBackground").putExtra("isBackground", false))
            }, 600)
        }
        if (!languageCh.equals(CommonUtility.loadLanguage(this))){
            recreate()
        }
    }

    private fun showDialog(title: String) {
        //Inflate the dialog with custom view
        val mDialogView = LayoutInflater.from(this).inflate(R.layout.complete_cooking_popup, null)
        //AlertDialogBuilder
        val mBuilder = AlertDialog.Builder(this)
            .setView(mDialogView)
            .setTitle("")
        //show dialog
        val  mAlertDialog = mBuilder.show()
        mAlertDialog.setCanceledOnTouchOutside(false);
        //login button click of custom layout
        val body = mDialogView.findViewById(R.id.tvHeaderMsg) as AppCompatTextView
        body.text = title
        val btnDone = mDialogView.findViewById(R.id.btnDone) as AppCompatTextView
        btnDone.setOnClickListener {
            UserModel.isCallFromDevice = true
            if (false)
            {
                CommonUtility.setBooleanPreference(
                    false,
                    Constant.isShowTemperatureView,
                    this
                )
                CommonUtility.setStringPreference("0", "count", this)
                LocalBroadcastManager.getInstance(this).sendBroadcast(
                    Intent("isFromServiceForTimer11").putExtra(
                        "isFromServiceForTimer",
                        true
                    )
                )
                val intent = Intent(
                    this,
                    DigitalTimerActivity::class.java
                ).putExtra("isFromService", true)
                    .putExtra("isFromServiceForTimer", true)
                intent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            } else
            {
                LocalBroadcastManager.getInstance(this).sendBroadcast(
                    Intent("isFromServiceForTimer11").putExtra(
                        "isFromServiceForTimer",
                        false
                    )
                )
                disconnect()
            }

            //disconnect()
            close()
            connectHardware(this@MainActivity)
        }
    }

    var isCallMethod: Boolean = false
    private var mBluetoothGatt: BluetoothGatt? = null
    private var mBluetoothDeviceAddress: String? = null
    fun disconnect() {
        if (mBluetoothGatt == null) {
            Log.w("", "BluetoothAdapter not initialized")
            return
        }
        mBluetoothGatt!!.disconnect()
    }

    fun close() {
        if (mBluetoothGatt == null) {
            return
        }
        mBluetoothGatt!!.close()
        mBluetoothGatt = null
    }

   /* fun openDialogComplete(
        msgString: String,
        buttonText: String,
        ids: Drawable,
        mContext: Context,
        isTemp: Boolean
    ) {
        CompleteCookingPopup.Builder<CompleteCookingPopup>(
            mContext as Activity,
            msgString,
            buttonText,
            "",
            ids, object : ClickOnFavouritePopup {
                override fun onItemClick(position: Int, cusineType: String) {
                    UserModel.isCallFromDevice = true
                    isCallMethod = false
                    *//* if (mp != null) {
                         mp?.stop()
                         mp?.release()
                     }*//*
                    if (isTemp) {
                        CommonUtility.setBooleanPreference(
                            false,
                            Constant.isShowTemperatureView,
                            mContext
                        )
                        CommonUtility.setStringPreference("0", "count", mContext)
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(
                            Intent("isFromServiceForTimer11").putExtra(
                                "isFromServiceForTimer",
                                true
                            )
                        )
                        val intent = Intent(
                            mContext,
                            DigitalTimerActivity::class.java
                        ).putExtra("isFromService", true)
                            .putExtra("isFromServiceForTimer", true)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        mContext.startActivity(intent)
                    } else {
                        LocalBroadcastManager.getInstance(mContext).sendBroadcast(
                            Intent("isFromServiceForTimer11").putExtra(
                                "isFromServiceForTimer",
                                false
                            )
                        )

                        disconnect()
                        close()

                    }
                }
            }
        ).setContentView(R.layout.dialog_validation)
            .setGravity(Gravity.CENTER)
            .setScaleRatio(0.1f)
            .setBlurRadius(Constant.blurRadius)
            .setDismissOnClickBack(false)
            .setDismissOnTouchBackground(false)
            .build()
            .show()
    }
   */
/*
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnRecipes -> {
                Constant.selfcook=false
                Constant.popupint=false
                startActivity(Intent(this@MainActivity, RecipesActivity::class.java))
            }
            R.id.btnQuickCook -> {
                Constant.selfcook=false
                Constant.popupint=false
                startActivity(Intent(this@MainActivity, QuickCookActivity::class.java))
            }
            R.id.btnFAQs -> {
                Constant.selfcook=false
                Constant.popupint=false
                startActivity(Intent(this@MainActivity, FAQActivity::class.java))
            }
            R.id.btnSetting -> {
                Constant.selfcook=false
                Constant.popupint=false
                startActivity(Intent(this@MainActivity, SettingActivity::class.java))
            }
        }
    }
*/
}
