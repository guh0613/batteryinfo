package com.genisys.batteryinformation;

import android.app.Activity;
import android.content.Context;
import android.telephony.TelephonyManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * 系统工具类
 * 
 */
public class SystemUtil {

    /**
     * 获取当前手机系统语言。
     *
     * @return 返回当前系统语言。例如：当前设置的是“中文-中国”，则返回“zh-CN”
     */
    public static String getSystemLanguage() {
        return Locale.getDefault().getLanguage();
    }

    /**
     * 获取当前系统上的语言列表(Locale列表)
     *
     * @return  语言列表
     */
    public static Locale[] getSystemLanguageList() {
        return Locale.getAvailableLocales();
    }

    /**
     * 获取当前手机系统版本号
     *
     * @return  系统版本号
     */
    public static String getSystemVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    /**
     * 获取手机型号
     *
     * @return  手机型号
     */
    public static String getSystemModel() {
        return android.os.Build.MODEL;
    }

    /**
     * 获取手机厂商
     *
     * @return  手机厂商
     */
    public static String getDeviceBrand() {
        return android.os.Build.BRAND;
    }

    /**
     * 获取手机IMEI(需要“android.permission.READ_PHONE_STATE”权限)
     *
     * @return  手机IMEI
     */
    public static String getIMEI(Context ctx) {
        TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Activity.TELEPHONY_SERVICE);
        if (tm != null) {
            return tm.getDeviceId();
        }
        return null;
    }
	public static String exec(String command){

		try{
			Process process = Runtime.getRuntime().exec(command);
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			int read;
			char[] buffer = new char[4096];
			StringBuffer output = new StringBuffer();
			while((read = reader.read(buffer)) > 0){
				output.append(buffer, 0, read);
			}
			reader.close();
			process.waitFor();
			return output.toString();

		}catch (IOException e){
			throw new RuntimeException(e);
		}catch (InterruptedException e){
			throw new RuntimeException(e);
		}
	}
	
	
		/**
		 * @param a 单数  32
		 * @param b 总数  145
		 * a / b    计算百分比32/145
		 * @return 22.07%
		 */
		public  static String CalculateUtil(BigDecimal a, BigDecimal b){
			String percent =
                b == null ? "-" :
				b.compareTo(new BigDecimal(0)) == 0 ? "-":
				a == null ? "0.00%" :
				a.multiply(new BigDecimal(100)).divide(b,2,BigDecimal.ROUND_HALF_UP) + "%";
			return percent;
		}
    
	
}

