package com.genisys.batteryinformation;

import android.Manifest;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.pgyersdk.crash.PgyCrashManager;

import java.math.BigDecimal;

public class MainActivity extends AppCompatActivity
{
	public static boolean useroot = false;
	Runnable runnable = new Runnable() {
		@Override
		public void run() {
			//要做的事情
			refreshrate();
			mHandler.postDelayed(this, 1000);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		PgyCrashManager.register(); //注册日志接口
		//询问获取日志的资格
		SharedPreferences sp=this.getPreferences(MODE_PRIVATE);
		int havereadlogs =sp.getInt("havereadlogs",2);
		if(havereadlogs==2) {
			SharedPreferences.Editor editor = sp.edit();
			editor.putInt("havereadlogs", 1);
			editor.commit();

			AlertDialog.Builder dialog2 = new AlertDialog.Builder(MainActivity.this);
			dialog2.setTitle("发送日志");
			dialog2.setMessage("为了帮助开发者更加方便地抓爬虫，在发生闪退时应用会自动发送您的日志。这可能会包括您手机的一些信息，如果您不想发送，也可以选择不允许。");
			dialog2.setCancelable(false);
			dialog2.setPositiveButton("明白了", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {

				}
			});
			dialog2.setNegativeButton("👴不允许", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Toast.makeText(MainActivity.this, "将不会发送日志", Toast.LENGTH_SHORT).show();
					PgyCrashManager.unregister();
				}


			});
			dialog2.show();
		}
		//使用toolbar顶替原有action bar
		androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		//状态栏沉浸，使用特殊方法适配各类定制ui
		StatusBarUtil.setRootViewFitsSystemWindows(this,true);
		StatusBarUtil.setTranslucentStatus(this);
		StatusBarUtil.setStatusBarColor(this,0xFF3F51B5);
		//检查是否给予了权限
		if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED | ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
		{
			android.app.AlertDialog.Builder dialog=new android.app.AlertDialog.Builder(MainActivity.this);
			dialog.setTitle("权限授予");
			dialog.setMessage("应用检测到你没有授予基本的权限。如果没有授予权限，将不能读取相关信息。");
			dialog.setCancelable(false);
			dialog.setPositiveButton("明白了，授予", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog,int which)
				{
					ActivityCompat.requestPermissions(MainActivity.this,new
							String[] {Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
				}
			});
			dialog.setNegativeButton("👴就是不给", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog,int which)
				{
					Toast.makeText(MainActivity.this,"(눈_눈)",Toast.LENGTH_SHORT).show();

				}


			});
			dialog.show();
		}
		//判断是不是coloros，来使用root权限
       if (OSUtils.isOppo() == true)
    {
	android.app.AlertDialog.Builder dialog=new android.app.AlertDialog.Builder(MainActivity.this);
	dialog.setTitle("ColorOS特别说明");
	dialog.setMessage("检测到设备的系统为ColorOS，由于ColorOS的限制，读取电池信息必须授予root权限。请在授予后继续。");
	dialog.setCancelable(false);
	dialog.setPositiveButton("明白了，授予", new DialogInterface.OnClickListener()
	{
		@Override
		public void onClick(DialogInterface dialog,int which)
		{
			ShellUtils.checkRootPermission();
			useroot = true ;
		}
	});
	dialog.setNegativeButton("退出", new DialogInterface.OnClickListener()
	{
		@Override
		public void onClick(DialogInterface dialog,int which)
		{
			finish();

		}


	});
	dialog.show();
}


	}

	//重写菜单初始化方法
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//load the file of menu that you created
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	//重写菜单选项被点击方法
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.about) {
			android.app.AlertDialog.Builder dialog=new android.app.AlertDialog.Builder(MainActivity.this);
			dialog.setTitle("关于");
			dialog.setMessage("作者：酷安@Genisys\n一个很简单的app，数据仅供参考。");
			dialog.setCancelable(true);
			dialog.setPositiveButton("不认识", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog,int which)
				{



				}
			});
			dialog.show();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}

	}

	//刷新电池信息方法
	public void refresh(View view)
	{
		TextView refreshview = findViewById(R.id.textfresh);
		refreshview.setText("手动刷新电池信息(慢)");
		//初始化电池信息，需要捕捉异常
		try  {
			refreshrate();
		} catch (Exception e) {
			PgyCrashManager.reportCaughtException(e);
			e.printStackTrace();
			Toast.makeText(MainActivity.this,"读取时发生错误",Toast.LENGTH_SHORT).show();
		}
		//定时刷新，需要捕捉异常
		try  {
			mHandler.postDelayed(runnable, 1000);
		} catch (Exception e) {
			PgyCrashManager.reportCaughtException(e);
			e.printStackTrace();
			Toast.makeText(MainActivity.this,"读取时发生错误",Toast.LENGTH_SHORT).show();
		}


	}
	//每秒刷新
	private Handler mHandler = new Handler();

	public void refreshrate()
	{
		//更改标题
		TextView welcome = findViewById(R.id.welcome);
		welcome.setText("您的设备的最新电池数据计算结果如下，仅供参考。");

		//手机型号
		TextView phoneinfo = findViewById(R.id.phoneinfo);
		//返回 厂商+型号（部分厂商的手机型号不包含品牌，所以要获得品牌名）
		phoneinfo.setText(SystemUtil.getDeviceBrand() + " " + SystemUtil.getSystemModel());

		//电池设计容量
		TextView battcc =findViewById(R.id.battcc);
		battcc.setText(BatteryInfo.getBatteryCapacity(this));

		//电池实际容量
		TextView battfcc = findViewById(R.id.battfcc);
		String getbatt = "cat /sys/class/power_supply/battery/batt_fcc";
		ShellUtils.CommandResult cmd = ShellUtils.execCommand(getbatt,useroot,true);
		if (cmd.result == 0)
		{
			double battccdou = Double.parseDouble((BatteryInfo.getBatteryCapacity(this)));
			int battfccint = Integer.parseInt(cmd.successMsg);
			BigDecimal battccdem = new BigDecimal(battccdou);
			BigDecimal battfccdem = new BigDecimal(battfccint);
			String healthrate = SystemUtil.CalculateUtil(battfccdem,battccdem);
			battfcc.setText(cmd.successMsg + ".0" + "（" + healthrate + "）");
		}
		else
		{
			battfcc.setText("读取失败");
		}

		//电池健康状况
		TextView batthealth = findViewById(R.id.battstate);
		String battstate = "cat /sys/class/power_supply/battery/health";
		ShellUtils.CommandResult cmdstate = ShellUtils.execCommand(battstate,useroot,true);
		if (cmdstate.result == 0)
		{
			if (cmdstate.successMsg.equals("Good"))
			{
				batthealth.setText("良好");
			}
			else
			{
				batthealth.setText("状态异常(过热，损坏等，请及时检修)");
			}
		}
		else
		{
			battfcc.setText("读取失败");
		}

		//电池技术
		TextView batttech = findViewById(R.id.batttech);
		String batttechnology = "cat /sys/class/power_supply/battery/technology";
		ShellUtils.CommandResult cmdtech = ShellUtils.execCommand(batttechnology,useroot,true);
		if (cmdtech.result == 0 )
		{
			batttech.setText(cmdtech.successMsg);
		}
		else
		{
			batttech.setText("读取失败");
		}

		//充电类型
		TextView chargetp = findViewById(R.id.chargetype);
		String chargetyp = "cat /sys/class/power_supply/battery/charge_type";
		ShellUtils.CommandResult cmdchtyp = ShellUtils.execCommand(chargetyp,useroot,true);
		if (cmdchtyp.result == 0 )
		{
			chargetp.setText(cmdchtyp.successMsg);
		}
		else
		{
			chargetp.setText("读取失败");
		}

		//当前电压
		TextView voltp = findViewById(R.id.voltnow);
		String volt1 = "cat /sys/class/power_supply/battery/voltage_now";
		ShellUtils.CommandResult cmdvolt1 = ShellUtils.execCommand(volt1,useroot,true);
		if (cmdvolt1.result == 0 )
		{
			voltp.setText(cmdvolt1.successMsg + "uV");
		}
		else
		{
			voltp.setText("读取失败");
		}

		//最高电压
		TextView voltma = findViewById(R.id.voltmax);
		String volt2 = "cat /sys/class/power_supply/battery/voltage_max";
		ShellUtils.CommandResult cmdvolt2 = ShellUtils.execCommand(volt2,useroot,true);
		if (cmdvolt2.result == 0 )
		{
			voltma.setText(cmdvolt2.successMsg + "uV");
		}
		else
		{
			voltma.setText("读取失败");
		}

		//最低电压
		TextView voltmi = findViewById(R.id.voltmin);
		String volt3 = "cat /sys/class/power_supply/battery/voltage_min";
		ShellUtils.CommandResult cmdvolt3 = ShellUtils.execCommand(volt3,useroot,true);
		if (cmdvolt3.result == 0 )
		{
			voltmi.setText(cmdvolt3.successMsg + "uV");
		}
		else
		{
			voltmi.setText("读取失败");
		}

		//充电状态
		TextView charstat =findViewById(R.id.charstatus);
		String charge = "cat /sys/class/power_supply/battery/status";
		ShellUtils.CommandResult cmdchar = ShellUtils.execCommand(charge,useroot,true);
		if (cmdchar.result == 0 )
		{
			if (cmdchar.successMsg.equals("Not charging"))
			{
				charstat.setText("不在充电");
			}
			else if(cmdchar.successMsg.equals("Charging"))
			{
				charstat.setText("正在充电");
			}
		}
		else
		{
			charstat.setText("读取失败");
		}

		//适配器固件更新
		TextView adapter = findViewById(R.id.adapter);
		String adapt = "cat /sys/class/power_supply/battery/adapter_fw_update";
		ShellUtils.CommandResult cmdadap = ShellUtils.execCommand(adapt,useroot,true);
		if (cmdadap.result == 0 )
		{
			if (cmdadap.successMsg.equals("0"))
			{
				adapter.setText("没有更新或不在充电");
			}
			else if(cmdadap.successMsg.equals("1"))
			{
				adapter.setText("有更新，且正在更新");
			}
		}
		else
		{
			adapter.setText("未知");
		}

		//是否支持阶梯式充电
		TextView stepchar = findViewById(R.id.step);
		String step = "cat /sys/class/power_supply/battery/step_charging_enabled";
		ShellUtils.CommandResult cmdstep = ShellUtils.execCommand(step,useroot,true);
		if (cmdstep.result == 0 )
		{
			if (cmdstep.successMsg.equals("0"))
			{
				stepchar.setText("否");
			}
			else if(cmdstep.successMsg.equals("1"))
			{
				stepchar.setText("是，且已启用");
			}
		}
		else
		{
			stepchar.setText("读取失败");
		}

		//vooc识别
		TextView vooctext = findViewById(R.id.voocstat);
		String vooc = "cat /sys/class/power_supply/battery/voocchg_ing";
		ShellUtils.CommandResult cmdvooc = ShellUtils.execCommand(vooc,useroot,true);
		if (cmdvooc.result == 0 )
		{
			if (cmdvooc.successMsg.equals("0"))
			{
				vooctext.setText("未激活或不在充电");
			}
			else if(cmdvooc.successMsg.equals("1"))
			{
				vooctext.setText("已激活vooc快充");
			}
		}
		else
		{
			vooctext.setText("读取失败");
		}

		//当前电流
		TextView currentnowview = findViewById(R.id.currentnow);
		String currenttext = "cat /sys/class/power_supply/battery/current_now";
		ShellUtils.CommandResult cmdcurrentnow = ShellUtils.execCommand(currenttext,useroot,true);
		if (cmdcurrentnow.result == 0)
		{
			if (Integer.parseInt(cmdcurrentnow.successMsg) > 0 )
			{


				currentnowview.setText("放电，" + cmdcurrentnow.successMsg + "mA");
			}

			else if(Integer.parseInt(cmdcurrentnow.successMsg) < 0 )
			{
				int current = Integer.parseInt(cmdcurrentnow.successMsg);
				current = current * -1 ;
				String currentstr = Integer.toString(current);
				currentnowview.setText("充电，" + currentstr + "mA");
			}
		}
		else
		{
			currentnowview.setText("未知");
		}

		//充电器电压
		TextView voltadapview = findViewById(R.id.voltadapnow);
		String voltadapnow = "cat /sys/class/power_supply/usb/voltage_now";
		ShellUtils.CommandResult cmdadapvolt = ShellUtils.execCommand(voltadapnow,useroot,true);
		if (cmdadapvolt.result == 0 )
		{
			if (cmdadapvolt.successMsg.equals("0"))
			{
				voltadapview.setText("没有连接充电器");
			}
			else
			{
				voltadapview.setText(cmdadapvolt.successMsg + "uV");
			}
		}
		else
		{
			voltadapview.setText("读取失败");
		}

		//充电功率
		TextView powernowview = findViewById(R.id.powernow);
		//判断充电状态
		if (cmdadapvolt.successMsg.equals("0"))
		{
			powernowview.setText("当前不在充电");
		}
		else
		{
			//处理电压数据
			Double voltnow = Double.parseDouble(cmdadapvolt.successMsg);
			int volt = (int) Math.rint(voltnow/1000000);
			//处理电流数据
			Double currentnow =  Double.parseDouble(cmdcurrentnow.successMsg);
			int current = (int) Math.rint(currentnow/1000);
			//获得功率
			int power = volt * current * -1;
			powernowview.setText(power + "W" );
		}
	}
}
