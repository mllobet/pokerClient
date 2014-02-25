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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.intel.stc.events.InviteRequestEvent;
import com.intel.stc.events.InviteResponseEvent;
import com.intel.stc.events.StcException;
import com.intel.stc.events.StcSessionUpdateEvent;
import com.intel.stc.interfaces.IStcActivity;
import com.intel.stc.interfaces.StcConnectionListener;
import com.intel.stc.interfaces.StcLocalSessionUpdateListener;
import com.intel.stc.interfaces.StcSessionUpdateListener;
import com.intel.stc.lib.StcLib;
import com.intel.stc.slib.StcServiceInet;
import com.intel.stc.utility.StcApplicationId;
import com.intel.stc.utility.StcSession;
import com.intel.stc.utility.StcSocket;

/**
 * This part of the application has the majority of the STC integration. All STC
 * applications written to the android CCF SDK require a subclass of
 * StcServiceInet but SimpleChat puts the entire engine for communication in
 * this service. That avoids all race conditions as activities bind and unbind,
 * suspend and resume.
 * <p>
 * SimpleChatService has four sections that are STC specific. The first are the
 * set of method overrides required by our StcServiceInet superclass. Those are
 * listed just past the comment section "METHODS REQUIRED BY StcServiceInet" and
 * are needed by any subclass of StcServiceInet.
 * <p>
 * The second set of methods handle CCF event call backs. These are in the
 * section "STC NOTIFICATION METHODS" and handle both discovery and
 * communication callbacks for SimpleChat.
 * <p>
 * The third set of methods are in the section "CALLBACKS FROM BUNDLE PARSING"
 * and handle callbacks from the bundle parsing. Both the platform (through the
 * SimpleChatNotificationActivity thunk) and ICC can start an
 * application for a particular purpose. That purpose is encoded in the bundle.
 * The SDK handles bundle parsing, and returns the results through callbacks
 * into the IStcActivity.
 * <p>
 * Finally the method "inviteSession" in the section "PUBLIC METHODS FOR SERVICE
 * USERS TO REQUEST ACTIONS" is quite CCF specific. In that it shows how to
 * invite another session to connect.
 * <p>
 * All of the I/O is handled in the WriteEngine and ReadEngine. Those
 * implementations are not very c3 specific because the result of the
 * interaction with CCF is a generic write stream and generic read stream.
 */
public class SimpleChatService extends StcServiceInet implements
		StcSessionUpdateListener, StcConnectionListener, IServiceIOListener, StcLocalSessionUpdateListener,
		IStcActivity {

	static final String LOGC = "sc service";

	// The service handles one and only one connection. The state machine for
	// the service
	// state starts in NEVER_CONNECTED and ends with CONNECTION_CLOSED.
	public enum ChatState {
		NEVER_CONNECTED, // we have not yet connected
		INVITE_SENT, // we have sent an invite.
		INVITE_RECEIVED, // we have received an invite.
		CONNECTED, // we are connected
		CONNECTION_CLOSED // we have been connected, but are no longer.
	};

	// Listener to listen to ISimpleChatEventListener.
	ArrayList<ISimpleChatEventListener> listeners = new ArrayList<ISimpleChatEventListener>();
	StcSocket socket = null;
	WriteEngine wengine;
	ReadEngine rengine;
	Bundle initBundle;
	boolean bundleParsed = false;
	ChatState state = ChatState.NEVER_CONNECTED;
	
	// /
	// / METHODS REQUIRED BY StcServiceInet
	// /
	@Override
	protected void stcLibPrepared(StcLib slib) {
		if (slib == null)
			throw new SimpleChatError(
					"the stclib is null in stcLibPrepared. Have you given your app local socket permissions? Have you given your app a registration object?");
		slib.setConnectionListener(this);
		slib.setStcSessionListListener(this);
		slib.addLocalSessionListener(this);
		Log.i(LOGC, "registered with the platform");
		
		// bundle will be parsed by parseInitBundle()
		tryParseBundle();

		postSessionListChanged();
	}

	@Override
	protected void stcPlatformMissing() {
		// TODO Auto-generated method stub

	}

	@Override
	public StcApplicationId getAppId() {
		return SimpleChatRegisterApp.id;
	}

	@Override
	public StcConnectionListener getConnListener() {
		return this;
	}

	// /
	// / STC NOTIFICATION METHODS
	// /

	public void sessionUpdated(StcSessionUpdateEvent arg) {
		postSessionListChanged();
	}

	@Override
	public void localSessionUpdated() {
		postLocalSessionChanged();
	}

	public void connectionCompleted(InviteResponseEvent arg) {
		boolean connectionComplete = false;
		synchronized (state) {
			if (arg.getStatus() == InviteResponseEvent.InviteStatus.sqtAccepted) {
				try {
					socket = getSTCLib().getPreparedSocket(
							arg.getSessionGuid(), arg.getConnectionHandle());

					InputStream iStream = socket.getInputStream();
					rengine = new ReadEngine(iStream, this);

					OutputStream oStream = socket.getOutputStream();
					wengine = new WriteEngine(oStream, this);

					connectionComplete = true;
				} catch (StcException e) {
					Log.e(LOGC, e.toString());
				}
			}

			if (connectionComplete)
				state = ChatState.CONNECTED;
			else
				state = ChatState.CONNECTION_CLOSED;
		}
		postConnected(connectionComplete);
	}

	/***
	 * Extract the critical information from the arg and pass it to the worker
	 * function.
	 */
	public void connectionRequest(InviteRequestEvent event) {
		synchronized (state) {
			//If the local user is already connected to the remote user, ignore the invites from other users.
			if(state == ChatState.CONNECTED){
				//Notify the other users on rejecting the invite.
				rejectInvite(event.getConnectionHandle());
				return;
			}
				
			postInviteAlert(event.getSessionUuid(), event.getConnectionHandle(), event.getOOBData());
		}
	}

	private void postInviteAlert(UUID inviterUuid, int inviteHandle, byte[] oobData) {
		synchronized (listeners) {
			for (ISimpleChatEventListener listen : listeners)
				listen.inviteAlert(inviterUuid, inviteHandle, oobData);
		}
	}
	/***
	 * Attempt to accept the invitation if we have not already done so
	 * 
	 * @param uuid
	 *            sessionguid
	 * @param handle
	 *            handle of the connection
	 */
	public void doConnectionRequest(UUID uuid, int handle) {
		synchronized (state) {
			if (state == ChatState.NEVER_CONNECTED || state == ChatState.CONNECTION_CLOSED) {
				boolean connected = false;
				try {
					socket = getSTCLib().acceptInvitation(uuid, handle);

					InputStream iStream = socket.getInputStream();
					rengine = new ReadEngine(iStream, this);

					OutputStream oStream = socket.getOutputStream();
					wengine = new WriteEngine(oStream, this);
					connected = true;
				} catch (StcException e) {
					Log.e(LOGC, "exception on connection", e);
				}

				if (connected) {
					Log.i(LOGC, "connected");
					state = ChatState.CONNECTED;
				} else {
					Log.i(LOGC, "connection failed");
					state = ChatState.CONNECTION_CLOSED;
				}
				postConnected(connected);
			}
		}

	}

	// /
	// / PUBLIC METHODS FOR SERVICE USERS TO REQUEST ACTIONS
	// /

	/***
	 * Request to invite a session.
	 * 
	 * @param session
	 * @return true if an invitation was sent.
	 */
	public boolean inviteSession(StcSession session) {
		synchronized (state) {
			if (state != ChatState.NEVER_CONNECTED && state != ChatState.CONNECTION_CLOSED)
				return false;
			
			if(!session.isAvailableProximity() && !session.isAvailableCloud())
			{
				Log.e(LOGC, "Session is not available or expired");
				return false;
			}
			try {
				Log.i(LOGC, "inviting session " + session.getSessionUuid().toString()
						+ " " + session.getUserName());
				//// The oobData can be anything, or null.  It's intention is to help identify the purpose of the invite.
				byte[] oobData = (new String("TEST BLOB")).getBytes();
				//byte[] oobData = null;

				// Setting the invite timeout to 180 seconds.
				getSTCLib().inviteSession(session.getSessionUuid(), oobData, (short) 180);
				return true;
			} catch (StcException e) {
				Log.e(LOGC, "invitation unexpected exception", e);
			}

		}

		return false;
	}

	/***
	 * Gets the current session list. Returns an empty list if stclib is not
	 * initialized or there are no sessions or something else goes wrong.
	 * 
	 * @return
	 */
	public List<StcSession> getSessions() {
		StcLib lib = getSTCLib();
		if (lib != null) {
			try {
				return lib.getSessionListWithAvatar();
			} catch (StcException e) {
				Log.e(LOGC, "unexpected exception", e);
			}
		}
		return new ArrayList<StcSession>();
	}

	/***
	 * @return lines that have been received from the other side.
	 */
	public List<String> getLines() {
		if (rengine != null)
			return rengine.getLines();
		else
			return new ArrayList<String>();
	}

	/***
	 * Writes str to the other side.
	 * 
	 * @param str
	 */
	public void writeString(String str) {
//		str = str + "\r";
		wengine.writeString(str);
	}

	/***
	 * Forces the service to shutdown and terminate.
	 */
	public void exitConnection() {
		Log.i(LOGC, "exit requested");
		synchronized (state) {
			state = ChatState.CONNECTION_CLOSED;

			if (wengine != null)
				wengine.stop();
			if (rengine != null)
				rengine.stop();
			if (socket != null) {
				try {
					socket.close();
				} catch (StcException e) {
					Log.i(LOGC, "", e);
				}
				socket = null;
			}
		}
	}

	/***
	 * Have the sdk reject the invitation that has timed out.
	 * 
	 * @param handle
	 */
	public void rejectInvite(int handle) {
		StcLib lib = getSTCLib();
		try {
			if (lib != null)
				lib.rejectInvitation(handle);
			else
				Log.e(LOGC, "unexpected null in rejectInvite");
		} catch (StcException e) {
			Log.e(LOGC, "unexpected exception", e);
		}
	}

	/**
	 * Save away the invite bundle and try to parse it.
	 * 
	 * @param bundle
	 */
	public void parseInitBundle(Bundle bundle) {
		initBundle = bundle;
		bundleParsed = false;
		tryParseBundle();
	}

	/**
	 * Attempt to parse the bundle. We should only do this once. We can only do
	 * this when we have an stclib.
	 */
	private void tryParseBundle() {
		StcLib lib = getSTCLib();
		if (lib == null)
			return;

		synchronized (this) {
			if (bundleParsed)
				return;
			bundleParsed = true;
		}

		lib.parseStartMethod(this, initBundle, this);
	}

	// /
	// / LISTENER MANAGEMENT AND EVENT PROPAGATION
	// /
	public boolean addListener(ISimpleChatEventListener listener) {
		synchronized (listeners) {
			if (listeners.contains(listener))
				return false;
			else
			{
				boolean ret = listeners.add(listener);
				return ret;
			}
		}
	}

	public boolean removeListener(ISimpleChatEventListener listener) {
		synchronized (listeners) {
			return listeners.remove(listener);
		}
	}

	private void postConnected(boolean connected) {
		synchronized (listeners) {
			for (ISimpleChatEventListener l : listeners)
				l.connected(connected);
		}
	}

	private void postSessionListChanged() {
		synchronized (listeners) {
			for (ISimpleChatEventListener l : listeners)
				l.sessionListChanged();
		}
	}
	
	private void postLocalSessionChanged() {
		synchronized (listeners) {
			for (ISimpleChatEventListener l : listeners)
				l.localSessionChanged();
		}
	}

	public void lineReceived(int line) {
		synchronized (listeners) {
			for (ISimpleChatEventListener l : listeners)
				l.lineReceived(line);
		}
	}

	public void remoteDisconnect() {
		synchronized (listeners) {
			for (ISimpleChatEventListener l : listeners) {
				l.remoteDisconnect();
			}
		}
		exitConnection();
	}

	// /
	// / CALLBACKS FROM BUNDLE PARSING
	// /

	/** Nothing to do as this is the default behavior of the SelectSessionActivity. */
	@Override
	public void onStartNormal() {
	}

	/** Nothing to do as this is not used and may be deprecated */
	@Override
	public void onStartServer(UUID sessionUuid) {
	}

	/***
	 * Started to handle an invitation (that we are going to accept.)
	 */
	@Override
	public void onStartClient(UUID inviterUuid, int inviteHandle) {
		doConnectionRequest(inviterUuid, inviteHandle);
	}

	// /
	// / SERVICE LIFECYCLE
	// /
	// / registered just for the logging events.
	@Override
	public void onDestroy() {
		Log.i(LOGC, "destroying");
		super.onDestroy();
	}

	@Override
	public void onCreate() {
		Log.i(LOGC, "creating");
		super.onCreate();
	}

	/* Simple Chat Cloud Registration */
	@Override
	public Intent GetCloudIntent() {
		return null;
	}

	@Override
	public Class<?> GetUnboxActivityClass() {
		return null;
	}
}
