/***************************************************************************
 * 
 * Copyright (c) 2014 bileisoft.cn, Inc. All Rights Reserved
 * 
 **************************************************************************/
package com.bileisoft.rank;

import android.graphics.drawable.Drawable;

/**
 * ·â×°appÐÅÏ¢
 * @author 
 *
 */
public class AppInfo 
{  
    private Drawable appIcon ;  
    private String appLabel;
    private String packageName;
    private int appUid;
    private int traffic;
    
    public AppInfo(){}
      
    public Drawable getAppIcon() 
    {
        return appIcon;
    }
    
    public void setAppIcon(Drawable appIcon) 
    {
        this.appIcon = appIcon;
    }     
    
    public String getAppLabel()
    {
    	return appLabel;
    }
    
    public void setAppLabel(String appLabel)
    {
    	this.appLabel = appLabel;
    }
    
    public int getAppUid()
    {
    	return appUid;
    }
    
    public void setAppUid(int uid)
    {
    	this.appUid = uid;
    }
    
    public int getTraffic()
    {
    	return traffic;
    }
    
    public void setTraffic(int traffic)
    {
    	this.traffic = traffic;
    }
    
    public void setPackageName(String packName)
    {
    	this.packageName = packName;
    }
    
    public String getPackageName()
    {
    	return this.packageName;
    }
}