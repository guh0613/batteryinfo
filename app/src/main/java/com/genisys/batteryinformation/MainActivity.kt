package com.genisys.batteryinformation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.genisys.batteryinformation.ShellUtils.CommandResult
import com.pgyersdk.crash.PgyCrashManager
import java.math.BigDecimal

class MainActivity : AppCompatActivity() {
    var runnable: Runnable = object : Runnable {
        override fun run() {
            //要做的事情
            refreshrate()
            mHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        PgyCrashManager.register() //注册日志接口
        //询问获取日志的资格
        val sp = getPreferences(MODE_PRIVATE)
        val havereadlogs = sp.getInt("havereadlogs", 2)
        if (havereadlogs == 2) {
            val editor = sp.edit()
            editor.putInt("havereadlogs", 1)
            editor.commit()
            val dialog2 = AlertDialog.Builder(this@MainActivity)
            dialog2.setTitle("发送日志")
            dialog2.setMessage("为了帮助开发者更加方便地抓爬虫，在发生闪退时应用会自动发送您的日志。这可能会包括您手机的一些信息，如果您不想发送，也可以选择不允许。")
            dialog2.setCancelable(false)
            dialog2.setPositiveButton("明白了") { dialog, which -> }
            dialog2.setNegativeButton("👴不允许") { dialog, which ->
                Toast.makeText(this@MainActivity, "将不会发送日志", Toast.LENGTH_SHORT).show()
                PgyCrashManager.unregister()
            }
            dialog2.show()
        }
        //使用toolbar顶替原有action bar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        //状态栏沉浸，使用特殊方法适配各类定制ui
        StatusBarUtil.setRootViewFitsSystemWindows(this, true)
        StatusBarUtil.setTranslucentStatus(this)
        StatusBarUtil.setStatusBarColor(this, -0xc0ae4b)
        //检查是否给予了权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            val dialog = AlertDialog.Builder(this@MainActivity)
            dialog.setTitle("权限授予")
            dialog.setMessage("应用检测到你没有授予基本的权限。如果没有授予权限，将不能读取相关信息。")
            dialog.setCancelable(false)
            dialog.setPositiveButton("明白了，授予") { dialog, which -> ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1) }
            dialog.setNegativeButton("👴就是不给") { dialog, which -> Toast.makeText(this@MainActivity, "(눈_눈)", Toast.LENGTH_SHORT).show() }
            dialog.show()
        }
        //判断是不是coloros，来使用root权限
        if (OSUtils.isOppo == true) {
            val dialog = AlertDialog.Builder(this@MainActivity)
            dialog.setTitle("ColorOS特别说明")
            dialog.setMessage("检测到设备的系统为ColorOS，由于ColorOS的限制，读取电池信息必须授予root权限。请在授予后继续。")
            dialog.setCancelable(false)
            dialog.setPositiveButton("明白了，授予") { dialog, which ->
                ShellUtils.Companion.checkRootPermission()
                useroot = true
            }
            dialog.setNegativeButton("退出") { dialog, which -> finish() }
            dialog.show()
        }
    }

    //重写菜单初始化方法
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        //load the file of menu that you created
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    //重写菜单选项被点击方法
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        return if (id == R.id.about) {
            val dialog = AlertDialog.Builder(this@MainActivity)
            dialog.setTitle("关于")
            dialog.setMessage("作者：酷安@Genisys\n一个很简单的app，数据仅供参考。")
            dialog.setCancelable(true)
            dialog.setPositiveButton("不认识") { dialog, which -> }
            dialog.show()
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    //刷新电池信息方法
    fun refresh(view: View?) {
        val refreshview = findViewById<TextView>(R.id.textfresh)
        refreshview.text = "手动刷新电池信息(慢)"
        //初始化电池信息，需要捕捉异常
        try {
            refreshrate()
        } catch (e: Exception) {
            PgyCrashManager.reportCaughtException(e)
            e.printStackTrace()
            Toast.makeText(this@MainActivity, "读取时发生错误", Toast.LENGTH_SHORT).show()
        }
        //定时刷新，需要捕捉异常
        try {
            mHandler.postDelayed(runnable, 1000)
        } catch (e: Exception) {
            PgyCrashManager.reportCaughtException(e)
            e.printStackTrace()
            Toast.makeText(this@MainActivity, "读取时发生错误", Toast.LENGTH_SHORT).show()
        }
    }

    //每秒刷新
    private val mHandler = Handler()
    fun refreshrate() {
        //更改标题
        val welcome = findViewById<TextView>(R.id.welcome)
        welcome.text = "您的设备的最新电池数据计算结果如下，仅供参考。"

        //手机型号
        val phoneinfo = findViewById<TextView>(R.id.phoneinfo)
        //返回 厂商+型号（部分厂商的手机型号不包含品牌，所以要获得品牌名）
        phoneinfo.text = SystemUtil.deviceBrand + " " + SystemUtil.systemModel

        //电池设计容量
        val battcc = findViewById<TextView>(R.id.battcc)
        battcc.text = BatteryInfo.getBatteryCapacity(this)

        //电池实际容量
        val battfcc = findViewById<TextView>(R.id.battfcc)
        val getbatt = "cat /sys/class/power_supply/battery/batt_fcc"
        val cmd: CommandResult = ShellUtils.Companion.execCommand(getbatt, useroot, true)
        if (cmd.result == 0) {
            val battccdou = BatteryInfo.getBatteryCapacity(this).toDouble()
            val battfccint = cmd.successMsg!!.toInt()
            val battccdem = BigDecimal(battccdou)
            val battfccdem = BigDecimal(battfccint)
            val healthrate = SystemUtil.CalculateUtil(battfccdem, battccdem)
            battfcc.text = cmd.successMsg + ".0" + "（" + healthrate + "）"
        } else {
            battfcc.text = "读取失败"
        }

        //电池健康状况
        val batthealth = findViewById<TextView>(R.id.battstate)
        val battstate = "cat /sys/class/power_supply/battery/health"
        val cmdstate: CommandResult = ShellUtils.Companion.execCommand(battstate, useroot, true)
        if (cmdstate.result == 0) {
            if (cmdstate.successMsg == "Good") {
                batthealth.text = "良好"
            } else {
                batthealth.text = "状态异常(过热，损坏等，请及时检修)"
            }
        } else {
            battfcc.text = "读取失败"
        }

        //电池技术
        val batttech = findViewById<TextView>(R.id.batttech)
        val batttechnology = "cat /sys/class/power_supply/battery/technology"
        val cmdtech: CommandResult = ShellUtils.Companion.execCommand(batttechnology, useroot, true)
        if (cmdtech.result == 0) {
            batttech.text = cmdtech.successMsg
        } else {
            batttech.text = "读取失败"
        }

        //充电类型
        val chargetp = findViewById<TextView>(R.id.chargetype)
        val chargetyp = "cat /sys/class/power_supply/battery/charge_type"
        val cmdchtyp: CommandResult = ShellUtils.Companion.execCommand(chargetyp, useroot, true)
        if (cmdchtyp.result == 0) {
            chargetp.text = cmdchtyp.successMsg
        } else {
            chargetp.text = "读取失败"
        }

        //当前电压
        val voltp = findViewById<TextView>(R.id.voltnow)
        val volt1 = "cat /sys/class/power_supply/battery/voltage_now"
        val cmdvolt1: CommandResult = ShellUtils.Companion.execCommand(volt1, useroot, true)
        if (cmdvolt1.result == 0) {
            voltp.text = cmdvolt1.successMsg + "uV"
        } else {
            voltp.text = "读取失败"
        }

        //最高电压
        val voltma = findViewById<TextView>(R.id.voltmax)
        val volt2 = "cat /sys/class/power_supply/battery/voltage_max"
        val cmdvolt2: CommandResult = ShellUtils.Companion.execCommand(volt2, useroot, true)
        if (cmdvolt2.result == 0) {
            voltma.text = cmdvolt2.successMsg + "uV"
        } else {
            voltma.text = "读取失败"
        }

        //最低电压
        val voltmi = findViewById<TextView>(R.id.voltmin)
        val volt3 = "cat /sys/class/power_supply/battery/voltage_min"
        val cmdvolt3: CommandResult = ShellUtils.Companion.execCommand(volt3, useroot, true)
        if (cmdvolt3.result == 0) {
            voltmi.text = cmdvolt3.successMsg + "uV"
        } else {
            voltmi.text = "读取失败"
        }

        //充电状态
        val charstat = findViewById<TextView>(R.id.charstatus)
        val charge = "cat /sys/class/power_supply/battery/status"
        val cmdchar: CommandResult = ShellUtils.Companion.execCommand(charge, useroot, true)
        if (cmdchar.result == 0) {
            if (cmdchar.successMsg == "Not charging") {
                charstat.text = "不在充电"
            } else if (cmdchar.successMsg == "Charging") {
                charstat.text = "正在充电"
            }
        } else {
            charstat.text = "读取失败"
        }

        //适配器固件更新
        val adapter = findViewById<TextView>(R.id.adapter)
        val adapt = "cat /sys/class/power_supply/battery/adapter_fw_update"
        val cmdadap: CommandResult = ShellUtils.Companion.execCommand(adapt, useroot, true)
        if (cmdadap.result == 0) {
            if (cmdadap.successMsg == "0") {
                adapter.text = "没有更新或不在充电"
            } else if (cmdadap.successMsg == "1") {
                adapter.text = "有更新，且正在更新"
            }
        } else {
            adapter.text = "未知"
        }

        //是否支持阶梯式充电
        val stepchar = findViewById<TextView>(R.id.step)
        val step = "cat /sys/class/power_supply/battery/step_charging_enabled"
        val cmdstep: CommandResult = ShellUtils.Companion.execCommand(step, useroot, true)
        if (cmdstep.result == 0) {
            if (cmdstep.successMsg == "0") {
                stepchar.text = "否"
            } else if (cmdstep.successMsg == "1") {
                stepchar.text = "是，且已启用"
            }
        } else {
            stepchar.text = "读取失败"
        }

        //vooc识别
        val vooctext = findViewById<TextView>(R.id.voocstat)
        val vooc = "cat /sys/class/power_supply/battery/voocchg_ing"
        val cmdvooc: CommandResult = ShellUtils.Companion.execCommand(vooc, useroot, true)
        if (cmdvooc.result == 0) {
            if (cmdvooc.successMsg == "0") {
                vooctext.text = "未激活或不在充电"
            } else if (cmdvooc.successMsg == "1") {
                vooctext.text = "已激活vooc快充"
            }
        } else {
            vooctext.text = "读取失败"
        }

        //当前电流
        val currentnowview = findViewById<TextView>(R.id.currentnow)
        val currenttext = "cat /sys/class/power_supply/battery/current_now"
        val cmdcurrentnow: CommandResult = ShellUtils.Companion.execCommand(currenttext, useroot, true)
        if (cmdcurrentnow.result == 0) {
            if (cmdcurrentnow.successMsg!!.toInt() > 0) {
                currentnowview.text = "放电，" + cmdcurrentnow.successMsg + "mA"
            } else if (cmdcurrentnow.successMsg!!.toInt() < 0) {
                var current = cmdcurrentnow.successMsg!!.toInt()
                current = current * -1
                val currentstr = Integer.toString(current)
                currentnowview.text = "充电，" + currentstr + "mA"
            }
        } else {
            currentnowview.text = "未知"
        }

        //充电器电压
        val voltadapview = findViewById<TextView>(R.id.voltadapnow)
        val voltadapnow = "cat /sys/class/power_supply/usb/voltage_now"
        val cmdadapvolt: CommandResult = ShellUtils.Companion.execCommand(voltadapnow, useroot, true)
        if (cmdadapvolt.result == 0) {
            if (cmdadapvolt.successMsg == "0") {
                voltadapview.text = "没有连接充电器"
            } else {
                voltadapview.text = cmdadapvolt.successMsg + "uV"
            }
        } else {
            voltadapview.text = "读取失败"
        }

        //充电功率
        val powernowview = findViewById<TextView>(R.id.powernow)
        //判断充电状态
        if (cmdadapvolt.successMsg == "0") {
            powernowview.text = "当前不在充电"
        } else {
            //处理电压数据
            val voltnow = cmdadapvolt.successMsg!!.toDouble()
            val volt = Math.rint(voltnow / 1000000).toInt()
            //处理电流数据
            val currentnow = cmdcurrentnow.successMsg!!.toDouble()
            val current = Math.rint(currentnow / 1000).toInt()
            //获得功率
            val power = volt * current * -1
            powernowview.text = power.toString() + "W"
        }
    }

    companion object {
        var useroot = false
    }
}