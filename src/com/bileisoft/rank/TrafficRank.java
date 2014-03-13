/***************************************************************************
 * 
 * Copyright (c) 2014 bileisoft.cn, Inc. All Rights Reserved
 * 
 **************************************************************************/
package com.bileisoft.rank;

import java.util.List;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;

/**
 * 
 * @author 
 *
 */
public class TrafficRank
{
	//数据库名
	private String dbName;
	//数据库表名
	private String tableName;
	//上下文
	private Context context;
	private SharedPreferences sp;
	private Editor editor;  
	
	public TrafficRank(Context context, String dbName, String tableName, String sharedPeferenceFileName)
	{
		this.context = context;
		this.dbName = dbName;
		this.tableName = tableName;
		//持久化对象
		sp = context.getSharedPreferences(sharedPeferenceFileName, Context.MODE_PRIVATE);
		editor = sp.edit();
	}
	
	/**
	 * 初始化数据库
	 */
	public boolean initDatabase()
	{
		boolean isInitSuccess = false;
		
		try
		{
			TrafficUtil.initDB(this.context, this.dbName, this.tableName);		
			setTrafficRankDBInit(true);
			isInitSuccess = true;
		} 
		catch (Exception e)
		{
			// TODO: handle exception
			setTrafficRankDBInit(false);
		}	
		
		return isInitSuccess;
	}
	
	/**
	 * 获取 3G/wifi 流量数据，保存在AppInfo中,已排好序
	 */
	public List<AppInfo> getTrafficDatas(int which)
	{
		//判断并显示
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return null;
		}
		
		//先更新数据库
		TrafficUtil.updateTraffic(this.context, this.dbName, this.tableName);
		
		if(which == 1)
		{
			return TrafficUtil.getTrafficData(this.context, this.dbName, this.tableName, "3G");
		}
		else 
		{
			return TrafficUtil.getTrafficData(this.context, this.dbName, this.tableName, "wifi");
		}
	}
	
	/**
	 * 刷新流量数据,服务中使用，用户查看时使用
	 */
	public boolean updateTraffic()
	{
		//判断并显示
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return false;
		}    
		
		return TrafficUtil.updateTraffic(this.context, this.dbName, this.tableName);
	}
	
	/**
	 * wifi状态发生改变时进行处理
	 */
	public void updateForWifiStateChange()
	{
		//判断并显示
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return ;
		}    
		
		TrafficUtil.forWifiStateChanged(this.context, this.dbName, this.tableName);
	}
	
	/**
	 * 关机时保存数据
	 * @param 
	 */
	public void saveTrafficData()
	{
		//判断并显示
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return ;
		} 
		
		TrafficUtil.forShutdown(this.context, this.dbName, this.tableName);
	}
	
	/**
	 * 安装应用时增加一条数据
	 * @param 
	 */
	public void addAppInfoData(int uid)
	{
		//判断并显示
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return ;
		} 
		
		TrafficUtil.addAppInfoData(this.context, this.dbName, this.tableName, uid);
	}
	
	/**
	 * 卸载应用时删除一条数据
	 * @param 
	 */
	public void deleteAppInfoData(int uid)
	{
		//判断并显示
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return ;
		} 
		
		TrafficUtil.deleteAppInfoData(this.context, this.dbName, this.tableName, uid);
	}
	
	/**
	 * 清空数据库
	 */
	public boolean clearAllDatas()
	{
		//判断并显示
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return false;
		} 
		
		return TrafficUtil.deleteAll(this.context, this.dbName, this.tableName);
	}
	
	/**
	 * 开机时检测是否有异常关机
	 */
	public void checkShutdown()
	{
		//判断并显示
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return ;
		} 
		
		SQLiteDatabase db =  this.context.openOrCreateDatabase(this.dbName,  Context.MODE_PRIVATE, null);  
		
		if(!TrafficUtil.isShutdownNormally(db, this.tableName))
		{
			TrafficUtil.modifyUnNormalShutdown(db, this.tableName, TrafficUtil.isWifiAvailable(this.context));
		}
		
		db.close();		
	}
	
	/**
	 * 设置数据库是否已经初始化
	 * @param 
	 */
	private void setTrafficRankDBInit(boolean isInit)
	{
		try 
		{
			this.editor.putBoolean("isTrafficRankDBInit", true);													
			this.editor.commit();
		} 
		catch (Exception e)
		{
			// TODO: handle exception
		}
	}
	
	public boolean getTrafficRankDBInit()
	{
		return this.sp.getBoolean("isTrafficRankDBInit", false);
	}
}