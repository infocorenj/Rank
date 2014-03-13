/***************************************************************************
 * 
 * Copyright (c) 2014 bileisoft.cn, Inc. All Rights Reserved
 * 
 **************************************************************************/
package com.bileisoft.rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.app.ActivityManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;

/**
 * 
 * @author 
 *
 */
public class TrafficUtil
{
	/**
	 * 初始化数据库
	 */
	public static void initDB(Context context, String dbName, String tableName)
	{
		List<AppInfo> listAppInfo = new ArrayList<AppInfo>();
		listAppInfo = getAllInstalledAppsUID(context);
		
		SQLiteDatabase db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null);  
		db.execSQL("DROP TABLE IF EXISTS " + tableName);
		//创建traffic表
		db.execSQL("CREATE TABLE " + tableName +" (id INTEGER PRIMARY KEY AUTOINCREMENT,  uid INTEGER, " +
				"wifi_1 INTEGER, wifi_2 INTEGER, wifi_total INTEGER, last_total INTEGER," +
				"since_boot INTEGER, total INTEGER, flag INTEGER, shuju_traffic INTEGER, packageName TEXT)");
		
		boolean isWifiAlive = isWifiAvailable(context);
		
		for(int i = 0; i < listAppInfo.size(); i++)
		{
			AppInfo appInfo = listAppInfo.get(i);
			
			int uid = appInfo.getAppUid();
			
			//如果该应用不可以访问3G网络，则无需加入数据库
			if(!canAccessInternet(appInfo, context))
				continue;
			
			//开机以来的流量；只统计现在开始的流量，之前的不算
			long since_boot = TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);	
			//该值可能为-2，需要判断
			if(since_boot < 0 )
				since_boot = 0;
			
			//设置初始值
			ContentValues values = new ContentValues();	
			values.put("id", i+1);
			values.put("uid", uid);
			values.put("wifi_1", since_boot);
			values.put("wifi_2", -1);
			values.put("wifi_total", since_boot);
			values.put("last_total", 0);
			values.put("since_boot", 0);
			values.put("total", 0);
			values.put("flag", isWifiAlive == true ? 1 : 0);						
			values.put("shuju_traffic", 0);
			values.put("packageName", listAppInfo.get(i).getPackageName());
			
			db.insert(tableName, null, values);				
		}
		
		db.close();
	}
	
	/**
	 * 判断一个app是否有访问网络的权限
	 */
	public static boolean canAccessInternet(AppInfo appinfo, Context context)
	{
		boolean hasThePerm = false;
		
		try 
		{
			PackageManager  pm = context.getPackageManager();
		
			String[] permissions = pm.getPackageInfo(appinfo.getPackageName(),
				PackageManager.GET_PERMISSIONS).requestedPermissions;
			
			for(int i = 0; i < permissions.length; i++)
			{
				if(permissions[i].equals("android.permission.INTERNET"))
				{
					hasThePerm = true;
					break;
				}
			}
		} 
		catch (Exception e)
		{
			// TODO: handle exception
		}
		
		return hasThePerm;		
	}
	
	
	/**
	 * 获取所有安装的app的UID, packageName 封装在AppInfo中
	 */	
	public static List<AppInfo>  getAllInstalledAppsUID(Context context)
	{
		List<AppInfo> listAppInfo = new ArrayList<AppInfo>();
		
		PackageManager  pm = context.getPackageManager();
		List<PackageInfo> packinfos = pm
                .getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES
                        | PackageManager.GET_PERMISSIONS);				
			
		for (PackageInfo info : packinfos) 
		{
			AppInfo appInfo = new AppInfo();			
			appInfo.setAppUid(info.applicationInfo.uid);
			appInfo.setPackageName(info.packageName);
			
			listAppInfo.add(appInfo);
		}
		
		return listAppInfo;
	}
	
	/**
	 * 判断wifi是否可用
	 * @param context
	 * @return
	 * @throws Exception
	 */
	public static boolean isWifiAvailable(Context context)
	{
		boolean isWifiAvailable = false;
		
		try 
		{
			ConnectivityManager cm = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo wifiNetworkInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
								
			if(wifiNetworkInfo != null && wifiNetworkInfo.isAvailable())
			{
				if(wifiNetworkInfo.isConnected())
				{
					isWifiAvailable = true;
				}
			}
		} 
		catch (Exception e) 
		{
			// TODO: handle exception
		}
		
		return isWifiAvailable;
	}
	
	/** 
     * 用来判断服务是否运行. 
     * @param context 
     * @param className 判断的服务名字 
     * @return true 在运行 false 不在运行 
     */
	public static boolean isServiceRunning(Context mContext, String className)
	{
		boolean isRunning = false;
		ActivityManager activityManager = (ActivityManager) mContext
				.getSystemService(Context.ACTIVITY_SERVICE);
		
		List<ActivityManager.RunningServiceInfo> serviceList = activityManager
				.getRunningServices(30);
		
		if (!(serviceList.size() > 0)) 
		{
			return false;
		}
		
		for (int i = 0; i < serviceList.size(); i++) 
		{
			if (serviceList.get(i).service.getClassName().equals(className) == true) 
			{
				isRunning = true;
				break;
			}
		}
		return isRunning;
	}
	
	/**
	 * 读取数据库中UID的流量值
	 */	
	public static int getTrafficOfUid(int uid, String which,String dbName,
										String tableName, SQLiteDatabase db)
	{
		int traffic = 0;
		
		try 
		{					
			Cursor c = db.rawQuery("SELECT * FROM " + tableName + " WHERE uid = ?", new String[]{String.valueOf(uid)});
			
			if(c != null)
			{
				while (c.moveToNext()) 
				{  
					if(which.contains("3G"))
					{
						traffic = c.getInt(c.getColumnIndex("shuju_traffic"));	
					}
					else 
					{
						traffic = c.getInt(c.getColumnIndex("wifi_total"));	
					}
					
					break;
				}			
			}
			else
			{
				traffic = -1;
			}
						
		} 
		catch (Exception e)
		{
			// TODO: handle exception
			traffic = -1;
		}
				
		return traffic;
	}
	
	/**
	 * 获取数据流量，供显示用
	 */
	public static List<AppInfo> getTrafficData(Context context,
			String dbName, String tableName, String which)
	{
		List<AppInfo> listAppInfo = new ArrayList<AppInfo>();
		
		PackageManager  pm = context.getPackageManager();
		//打开数据库		
		SQLiteDatabase db = context.openOrCreateDatabase(dbName,  Context.MODE_PRIVATE, null); 
		Cursor c = db.rawQuery("SELECT * FROM " + tableName, null);
		
		while(c.moveToNext())
		{
			int uid = c.getInt(c.getColumnIndex("uid"));	
			String packageName = c.getString(c.getColumnIndex("packageName"));
			
			try 
			{
				//获取ApplicationInfo
				ApplicationInfo applicationInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
				
				//封装流量数据
				AppInfo appInfo = new AppInfo();
				
				appInfo.setAppIcon(applicationInfo.loadIcon(pm));
				appInfo.setAppUid(uid);
				
				//根据UID 读取数据库中的3G流量
				int traffic = getTrafficOfUid(uid, which, dbName, tableName, db);				
				appInfo.setTraffic(traffic);
							
				String appName = applicationInfo.loadLabel(pm).toString();			
				appInfo.setAppLabel(appName);
				
				if(traffic >= 0)
					listAppInfo.add(appInfo);
			} 
			catch (Exception e) 
			{
				// TODO: handle exception
			}			
		}					
		
		db.close();
		
		//流量排序
		ComparatorUser comparator=new ComparatorUser();
		Collections.sort(listAppInfo, comparator);
		  
		return listAppInfo;
	}
	
	/**
	 * 更新数据流量
	 */
	public static boolean updateTraffic(Context context, String dbName, String tableName)
	{
		boolean isUpdated = false;
		
		try
		{
			//打开数据库		
			SQLiteDatabase db = context.openOrCreateDatabase(dbName,  Context.MODE_PRIVATE, null); 
			Cursor c = db.rawQuery("SELECT * FROM " + tableName, null);
			
			while(c.moveToNext())
			{
				int uid = c.getInt(c.getColumnIndex("uid"));
				
				try
				{
					//开机到现在的流量
					long since_boot = TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);	
					if(since_boot < 0)
						since_boot = 0;		
					
					//从数据库中读取想应数据
					long total = 0;
					long wifi_1 = c.getInt(c.getColumnIndex("wifi_1"));	
					long wifi_total =c.getInt(c.getColumnIndex("wifi_total"));
					long last_total = c.getInt(c.getColumnIndex("last_total"));
					long flag = c.getInt(c.getColumnIndex("flag"));																					
							
					total = last_total + since_boot;
					
					//存入相应的值
					ContentValues cv = new ContentValues();  	
					cv.put("total", total);		
					
					//3G流量
					long shujuTraffic = 0;
					
					//如果当前wifi已关闭
					if(flag == 0)
					{
						shujuTraffic = total - wifi_total;			
					}
					else 
					{
						if(since_boot - wifi_1 < 0)
						{
							shujuTraffic = total - wifi_total;				
							cv.put("wifi_1", 0);								 
						}
						else 
						{
							shujuTraffic = total - wifi_total - (since_boot - wifi_1);
							cv.put("wifi_1", since_boot);
						}		
					}	
					
					cv.put("shuju_traffic", shujuTraffic);
					
					//wifi流量
					double wifiTraffic = (double)total - shujuTraffic;		
					
					cv.put("wifi_total", wifiTraffic);
					
					db.update(tableName, cv, "uid = ?", new String[]{String.valueOf(uid)});		
				} 
				catch (Exception e) 
				{
					// TODO: handle exception
				}									
			}
			
			isUpdated = true;
			db.close();
		} 
		catch (Exception e) 
		{
			// TODO: handle exception
		}
				
		return isUpdated;		
	}
	
	/**
	 * 检测是否有异常关机,异常关机返回false
	 */
	public static boolean isShutdownNormally(SQLiteDatabase db, String tableName)
	{
		boolean isNormally = true;
		
		Cursor cursor = db.rawQuery("SELECT * FROM " + tableName, new String[]{});
		while (cursor.moveToNext()) 
		{ 
			int uid = cursor.getInt(cursor.getColumnIndex("uid"));
			
			long since_boot = TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);	
			if(since_boot < 0)
				since_boot = 0;		
			
			long wifi_1 = cursor.getInt(cursor.getColumnIndex("wifi_1"));
			
			if(since_boot - wifi_1 < 0)
			{
				isNormally = false;
				break;
			}
		}
		
		return isNormally;
	}
	
	/**
	 * 修改异常关机导致的流量变少的问题
	 */
	public static void modifyUnNormalShutdown(SQLiteDatabase db, String tableName, boolean flag)
	{
		try 
		{			
			Cursor cursor = db.rawQuery("SELECT * FROM " + tableName, new String[]{});
			while (cursor.moveToNext()) 
			{  
				int uid = cursor.getInt(cursor.getColumnIndex("uid"));		
				long total = cursor.getInt(cursor.getColumnIndex("total"));	
				
				long since_boot = TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);	
				if(since_boot < 0)
					since_boot = 0;		
				
				//wifi_1 归零， flag 状态根据网络状态确定， last_total = total;				
				ContentValues cv = new ContentValues(); 				
				cv.put("wifi_1", flag == true ? since_boot : 0);
				cv.put("last_total", total);
				cv.put("flag", flag == true ? 1 : 0);
				
				db.update(tableName, cv, "uid = ?", new String[]{String.valueOf(uid)});
			}
		}
		catch(Exception e)
		{
			
		}
	}
	
	/**
	 * update for wifi state change
	 */
	public static void forWifiStateChanged(Context context, String dbName, String tableName)
	{
		try 
		{			
			//如果网络状态发生改变，判断wifi是否可用			
			boolean isWifiAvailable = isWifiAvailable(context);
			
			SQLiteDatabase db =  context.openOrCreateDatabase(dbName,  Context.MODE_PRIVATE, null);  
			Cursor cursor = db.rawQuery("SELECT * FROM " + tableName, new String[]{});
			
			while (cursor.moveToNext()) 
			{  	
				int uid = cursor.getInt(cursor.getColumnIndex("uid"));
				
				try 
				{
					if(isWifiAvailable)
					{
						//开启
						//记录当前uid应用的流量.
						long wifi_1 = TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);							
						if(wifi_1 <0 )
							wifi_1 = 0;
						
						ContentValues cv = new ContentValues();  
						cv.put("wifi_1", wifi_1);  
						cv.put("flag", 1);
						
						db.update(tableName, cv, "uid = ?", new String[]{String.valueOf(uid)});
					}
					else
					{
						//如果关闭, 结余本次wifi过程中 uid应用的 流量															
						long wifi_1 = cursor.getInt(cursor.getColumnIndex("wifi_1"));
						long wifi_total = cursor.getInt(cursor.getColumnIndex("wifi_total"));
						long last_total = cursor.getInt(cursor.getColumnIndex("last_total"));
						long flag = cursor.getInt(cursor.getColumnIndex("flag"));												
						
						//判断是否是从wifi切换到其他网络
						if(flag == 1)
						{
							long wifi_2 = TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);	
							if(wifi_2 < 0)
								wifi_2 = 0;								 												
							
							ContentValues cv = new ContentValues(); 													
							cv.put("wifi_total", wifi_2 - wifi_1 > 0 ? wifi_2 - wifi_1 + wifi_total : wifi_total);
							cv.put("wifi_1", 0);
							cv.put("wifi_2", -1);
							cv.put("since_boot", 0);
							cv.put("total", last_total + wifi_2);
							cv.put("flag", 0);

							db.update(tableName, cv, "uid = ?", new String[]{String.valueOf(uid)});														
						}
						else
						{
							
						}
					}
				} 
				catch (Exception e)
				{
					// TODO: handle exception
				}													
			}
			
			db.close();
		} 
		catch (Exception e) 
		{
			// TODO: handle exception
			//初始化数据库
		}
	}
	
	/**
	 * 关机时的处理
	 */
	public static void forShutdown(Context context, String dbName, String tableName)
	{
		try 
		{			
			SQLiteDatabase db =  context.openOrCreateDatabase(dbName,  Context.MODE_PRIVATE, null);  
			Cursor c = db.rawQuery("SELECT * FROM "+ tableName , null);	

			while(c.moveToNext())
			{
				int uid = c.getInt(c.getColumnIndex("uid"));
				
				try 
				{
					long since_boot = TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);
					if(since_boot < 0)
						since_boot = 0;
					
					long last_total = 0;		
					long flag = -1;
					long wifi_1 = -1;	
					long wifi_total = 0;
					
					last_total = c.getInt(c.getColumnIndex("last_total"));			
					flag = c.getInt(c.getColumnIndex("flag"));
					wifi_1 = c.getInt(c.getColumnIndex("wifi_1"));
					wifi_total = c.getInt(c.getColumnIndex("wifi_total"));
										
					if(flag == 0)
					{
						ContentValues cv = new ContentValues();  
						cv.put("last_total", since_boot + last_total);  
						
						db.update(tableName, cv, "uid = ?", new String[]{String.valueOf(uid)});
					}
					else
					{
						ContentValues cv = new ContentValues();  
						if(since_boot - wifi_1 > 0)
						{
							cv.put("wifi_total", wifi_total + since_boot - wifi_1);  
						}
						else 
						{
							cv.put("wifi_total", wifi_total + 0);  
						}
						cv.put("wifi_1", 0);
						cv.put("wifi_2", -1);
						cv.put("since_boot", 0);
						cv.put("last_total", since_boot + last_total);				
						
						db.update(tableName, cv, "uid = ?", new String[]{String.valueOf(uid)});
					}				
				} 
				catch (Exception e) 
				{
					// TODO: handle exception
				}								
			}
			
			db.close();
		}
		catch(Exception e)
		{
			
		}		
	}
	
	/**
	 * 增加一条数据
	 */
	public  static void addAppInfoData(Context context, String dbName, String tableName, int uid)
	{
		String packageName = getPackageNameByUID(context, uid);
	
		AppInfo appInfo = new AppInfo();
		appInfo.setPackageName(packageName);
		
		//判断应用是否有internet权限
		if(!canAccessInternet(appInfo, context))
			return;
		
		SQLiteDatabase db =  context.openOrCreateDatabase(dbName,  Context.MODE_PRIVATE, null);  
		
		//开机以来的流量；只统计现在开始的流量，之前的不算
		long since_boot = TrafficStats.getUidRxBytes(uid) + TrafficStats.getUidTxBytes(uid);	
		//该值可能为-2，需要判断
		if(since_boot < 0 )
			since_boot = 0;
		
		//设置初始值
		ContentValues values = new ContentValues();	
		
		values.put("uid", uid);
		values.put("wifi_1", since_boot);
		values.put("wifi_2", -1);
		values.put("wifi_total", since_boot);
		values.put("last_total", 0);
		values.put("since_boot", 0);
		values.put("total", 0);
		
		boolean isWifiAlive = isWifiAvailable(context);
		
		if(isWifiAlive)
		{
			values.put("flag", 1);
		}
		else
		{
			values.put("flag", 0);
		}
		
		values.put("shuju_traffic", 0);
		values.put("packageName", packageName);
		
		db.insert(tableName, null, values);		
		
		db.close();
	}
	
	/**
	 * 删除一条数据
	 */
	public static void deleteAppInfoData(Context context, String dbName, String tableName, int uid)
	{
		try 
		{
			SQLiteDatabase db =  context.openOrCreateDatabase(dbName,  Context.MODE_PRIVATE, null);			
			
			db.delete(tableName, "uid = ?", new String[]{String.valueOf(uid)});
			
			db.close();
		} 
		catch (Exception e) 
		{
			// TODO: handle exception
		}		
	}
	
	/**
	 * 清空
	 */
	public static boolean deleteAll(Context context, String dbName, String tableName)
	{
		boolean isDeleted = false;
		
		try 
		{
			initDB(context, dbName, tableName);
			isDeleted = true;
		} 
		catch (Exception e) 
		{
			// TODO: handle exception
		}	
		
		return isDeleted;
	}
	
	/**
	 * 根据uID获取包名
	 */
	public static String getPackageNameByUID(Context context, int uid)
	{
		String packageName = "";
		
		List<AppInfo> list = getAllInstalledAppsUID(context);
		
		for(int i = 0; i < list.size(); i++)
		{
			if(list.get(i).getAppUid() == uid)
			{
				packageName = list.get(i).getPackageName();
				break;
			}
		}
		
		return packageName;
	}
}


/**
 * 比较流量大小
 * @author 
 *
 */
class ComparatorUser implements Comparator<AppInfo>
{
	public ComparatorUser()
	{
		super();
	}
	
	public int compare(AppInfo arg0, AppInfo arg1) 
	{
		AppInfo user0 = (AppInfo) arg0;
		AppInfo user1 = (AppInfo) arg1;
		
		int flag = 0;
		
		if(user0.getTraffic() > user1.getTraffic())
			flag = -1;	
		else if (user0.getTraffic() < user1.getTraffic())
			flag = 1;
				
		return flag;			
	}	 
} 