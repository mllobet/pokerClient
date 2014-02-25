/*
Copyright (c) 2011-2013, Intel Corporation

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
      this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation
      and/or other materials provided with the distribution.

    * Neither the name of Intel Corporation nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.intel.c3.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.content.Context;

import com.intel.stc.lib.AppRegisterService;
import com.intel.stc.lib.GadgetRegistration;
import com.intel.stc.utility.StcApplicationId;

/***
 * C3 requires a subclass of RegisterApp to be in every application.
 * SimpleChatRegisterApp is that subclass for SimpleChat. All of this class is
 * c3 specific. Details are included in the SDK documentation.
 */
public class SimpleChatRegisterApp extends AppRegisterService
{
	public static final String		LAUNCH_INTENT		= "com.intel.c3.sample";
	public static final String		appId				= "7A1B397B-B576-44C4-943F-1BEF4F490C06";	
	// TODO Generate Simple Chat Keys
	private static final String		clientId			= "WqODOHahg3xw6WVB0BbTMi9yazTkBoQG";
	private static final String		clientSecret		= "i8wP2TGxoVjINdX6";
	private static final boolean	allowCloudTransport	= true;

	private static final UUID		appGuid				= UUID.fromString(appId); // New StcApplicationId takes AppGuid as UUID instead of String.
	static final StcApplicationId	id					= new StcApplicationId(appGuid, clientId, clientSecret,
																allowCloudTransport);
	static final String				redirectURL			= "http://www.intel.com/robots.txt";
	
	@Override
	protected List<GadgetRegistration> getGadgetList(Context context)
	{
		String appName = context.getString(R.string.app_name);
		String appDescription = context.getString(R.string.app_description);

		ArrayList<GadgetRegistration> list = new ArrayList<GadgetRegistration>();
		list.add(new GadgetRegistration(appName, R.drawable.ic_launcher, appGuid.toString(), appDescription, LAUNCH_INTENT, 2,
				R.string.schat_inv_text, R.string.timeout_toast_text, 0, context));
		return list;
	}
}
