package com.genisys.batteryinfo;

import android.app.*;
import android.os.*;
import android.support.v7.app.*;
import android.support.v7.widget.*;
import com.readystatesoftware.systembartint.*;
import android.view.*;
import android.widget.*;
import android.support.v4.content.*;
import android.*;
import android.content.pm.*;
import android.content.*;
import android.support.v4.app.*;
import java.math.*;

public class MainActivity extends AppCompatActivity
{
	
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		//使用toolbar顶替原有action bar
		android.support.v7.widget.Toolbar toolbar = findViewById(R.id.toolbar);
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
														  String[] {Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_EXTERNAL_STORAGE},1);
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
		//检查root权限
		//读取sharedpreference来查看是否显示过对话框
		final SharedPreferences sp=this.getPreferences(MODE_PRIVATE);
		int havegrantroot =sp.getInt("havegrantroot",2);
		if (havegrantroot == 2)
		{
		android.app.AlertDialog.Builder dialog1=new android.app.AlertDialog.Builder(MainActivity.this);
		dialog1.setTitle("root须知");
		dialog1.setMessage("本应用需要root权限来与系统内核进行交互，以获得相对准确的电池实际容量值，若不授予将无法读取。此对话框仅显示一次。");
		dialog1.setCancelable(false);
		dialog1.setPositiveButton("明白了，授予", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog,int which)
				{
					ShellUtils.checkRootPermission();
					
					SharedPreferences.Editor editor=sp.edit();
					editor.putInt("havegrantroot",1);
					editor.commit();
				}
			});
		dialog1.setNegativeButton("👴就是不给", new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog,int which)
				{
					Toast.makeText(MainActivity.this,"(눈_눈)",Toast.LENGTH_SHORT).show();

				}


			});
		dialog1.show();
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
		ShellUtils.CommandResult cmd = ShellUtils.execCommand(getbatt,true,true);
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
		ShellUtils.CommandResult cmdstate = ShellUtils.execCommand(battstate,true,true);
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
		ShellUtils.CommandResult cmdtech = ShellUtils.execCommand(batttechnology,true,true);
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
		ShellUtils.CommandResult cmdchtyp = ShellUtils.execCommand(chargetyp,true,true);
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
		ShellUtils.CommandResult cmdvolt1 = ShellUtils.execCommand(volt1,true,true);
		if (cmdvolt1.result == 0 )
		{
			voltp.setText(cmdvolt1.successMsg);
		}
		else
		{
			voltp.setText("读取失败");
		}
		
		//最高电压
		TextView voltma = findViewById(R.id.voltmax);
		String volt2 = "cat /sys/class/power_supply/battery/voltage_max";
		ShellUtils.CommandResult cmdvolt2 = ShellUtils.execCommand(volt2,true,true);
		if (cmdvolt2.result == 0 )
		{
			voltma.setText(cmdvolt2.successMsg);
		}
		else
		{
			voltma.setText("读取失败");
		}
		
		//最低电压
		TextView voltmi = findViewById(R.id.voltmin);
		String volt3 = "cat /sys/class/power_supply/battery/voltage_min";
		ShellUtils.CommandResult cmdvolt3 = ShellUtils.execCommand(volt3,true,true);
		if (cmdvolt3.result == 0 )
		{
			voltmi.setText(cmdvolt3.successMsg);
		}
		else
		{
			voltmi.setText("读取失败");
		}
		
		//充电状态
		TextView charstat =findViewById(R.id.charstatus);
		String charge = "cat /sys/class/power_supply/battery/status";
		ShellUtils.CommandResult cmdchar = ShellUtils.execCommand(charge,true,true);
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
		ShellUtils.CommandResult cmdadap = ShellUtils.execCommand(adapt,true,true);
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
			adapter.setText("读取失败,您的充电头可能未适配");
		}
		
		//是否支持阶梯式充电
		TextView stepchar = findViewById(R.id.step);
		String step = "cat /sys/class/power_supply/battery/step_charging_enabled";
		ShellUtils.CommandResult cmdstep = ShellUtils.execCommand(step,true,true);
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
		ShellUtils.CommandResult cmdvooc = ShellUtils.execCommand(vooc,true,true);
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
	}
}
