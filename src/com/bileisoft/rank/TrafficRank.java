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
	//���ݿ���
	private String dbName;
	//���ݿ����
	private String tableName;
	//������
	private Context context;
	private SharedPreferences sp;
	private Editor editor;  
	
	public TrafficRank(Context context, String dbName, String tableName, String sharedPeferenceFileName)
	{
		this.context = context;
		this.dbName = dbName;
		this.tableName = tableName;
		//�־û�����
		sp = context.getSharedPreferences(sharedPeferenceFileName, Context.MODE_PRIVATE);
		editor = sp.edit();
	}
	
	/**
	 * ��ʼ�����ݿ�
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
	 * ��ȡ 3G/wifi �������ݣ�������AppInfo��,���ź���
	 */
	public List<AppInfo> getTrafficDatas(int which)
	{
		//�жϲ���ʾ
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return null;
		}
		
		//�ȸ������ݿ�
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
	 * ˢ����������,������ʹ�ã��û��鿴ʱʹ��
	 */
	public boolean updateTraffic()
	{
		//�жϲ���ʾ
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return false;
		}    
		
		return TrafficUtil.updateTraffic(this.context, this.dbName, this.tableName);
	}
	
	/**
	 * wifi״̬�����ı�ʱ���д���
	 */
	public void updateForWifiStateChange()
	{
		//�жϲ���ʾ
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return ;
		}    
		
		TrafficUtil.forWifiStateChanged(this.context, this.dbName, this.tableName);
	}
	
	/**
	 * �ػ�ʱ��������
	 * @param 
	 */
	public void saveTrafficData()
	{
		//�жϲ���ʾ
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return ;
		} 
		
		TrafficUtil.forShutdown(this.context, this.dbName, this.tableName);
	}
	
	/**
	 * ��װӦ��ʱ����һ������
	 * @param 
	 */
	public void addAppInfoData(int uid)
	{
		//�жϲ���ʾ
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return ;
		} 
		
		TrafficUtil.addAppInfoData(this.context, this.dbName, this.tableName, uid);
	}
	
	/**
	 * ж��Ӧ��ʱɾ��һ������
	 * @param 
	 */
	public void deleteAppInfoData(int uid)
	{
		//�жϲ���ʾ
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return ;
		} 
		
		TrafficUtil.deleteAppInfoData(this.context, this.dbName, this.tableName, uid);
	}
	
	/**
	 * ������ݿ�
	 */
	public boolean clearAllDatas()
	{
		//�жϲ���ʾ
		boolean isInit = getTrafficRankDBInit();
		if (!isInit) 
		{	
			return false;
		} 
		
		return TrafficUtil.deleteAll(this.context, this.dbName, this.tableName);
	}
	
	/**
	 * ����ʱ����Ƿ����쳣�ػ�
	 */
	public void checkShutdown()
	{
		//�жϲ���ʾ
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
	 * �������ݿ��Ƿ��Ѿ���ʼ��
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