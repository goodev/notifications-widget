package com.roymam.android.nils.services;

import android.content.Intent;
import android.widget.RemoteViewsService;

import com.roymam.android.nils.ui.NotificationsViewFactory;

public class NotificationsRemoteViewsFactoryService extends RemoteViewsService
{
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) 
	{
		return new NotificationsViewFactory(this, intent);
	}
}
