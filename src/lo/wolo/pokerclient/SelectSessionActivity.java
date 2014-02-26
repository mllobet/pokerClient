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
package lo.wolo.pokerclient;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.intel.stc.events.DiscoveryNodeUpdateEvent;
import com.intel.stc.events.DiscoveryNodeUpdateEvent.EventType;
import com.intel.stc.events.StcException;
import com.intel.stc.interfaces.StcDiscoveryNodeUpdateEventListener;
import com.intel.stc.ipc.STCLoggingLevel;
import com.intel.stc.ipc.STCLoggingMode;
import com.intel.stc.ipc.STCLoggingModule;
import com.intel.stc.lib.StcLib;
import com.intel.stc.lib.StcLib.NodeFlags;
import com.intel.stc.utility.StcDiscoveryNode;
import com.intel.stc.utility.StcSession;
import com.intel.stc.utility.d;

/***
 * This activity runs a session selection screen and either waits for an incoming
 * invitation or waits for the user to select another session to invite.
 * <p>
 * There is no c3 specific code here.
 */
public class SelectSessionActivity extends AbstractServiceUsingActivity implements ISimpleChatEventListener,
		OnClickListener, StcDiscoveryNodeUpdateEventListener
{
	static final String	LOGC		= "sc select session";
	SessionAdapter			sessionAdapter;
	NodeListAdapter			nodeAdapter;
	Bundle				bundle;
	Dialog				upDialog	= null;
	ListView			lview		= null;
	Button				dNodeButton	= null;
	Button				createNodeButton, joinNodeButton;
	Button				nextButton	= null;
	EditText 			enterNodeName = null;
	TextView			screenLabel		= null;
	//ListView			logview		= null;
	ListView 			nodelistView = null;
	ProgressBar			progressBar;
	LinearLayout		selectContent;
	//private DebugAdapter adapter;
	private final static int DISCOVERY_SETUP = 21;
	private StcLib lib;
	private TextView emptytxtNode = null;
	private TextView emptytxtSession = null;
	//String appGuid;
	UUID appGuid;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		bundle = this.getIntent().getExtras();
				
		doStartService();
		initializeUI();
		
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
	}

	private void StartAgent()
	{
		// Enable this app for Intel CCF Developer Central Online logging.
		//
		// WARNING:  Enabling online debugging should be made only while the product is in a development environment on secure (non-public) networks.
		//		                     It should NOT be enabled for release products as enabling online debugging poses security risks on non-secure networks.
		//		                     Prior to releasing a product, either remove this call or specify OFFLINE logging only.
		//Start the Agent
		appGuid = UUID.fromString(SimpleChatRegisterApp.appId);
		String appName = this.getString(R.string.app_name);
		
		String logPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
		try {
			if(chatService != null){
				lib = chatService.getSTCLib();
				if (lib != null) {					
					lib.startAgent(appGuid, appName,
							STCLoggingMode.LogMode_Live, logPath
									+ "/simpleChatAgentLog.txt",
							STCLoggingModule.LogModule_All,
							STCLoggingLevel.Info, false);
				}
			}
		} catch (StcException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	//Initializing the UI components.
	private void initializeUI(){
		setContentView(R.layout.select);
		lview = (ListView) findViewById(R.id.sessionListView);
		//logview = (ListView) findViewById(R.id.logListView);
		dNodeButton = (Button) findViewById(R.id.node_button);
		createNodeButton = (Button) findViewById(R.id.create_node);
		joinNodeButton = (Button) findViewById(R.id.join_node);
		nextButton = (Button) findViewById(R.id.next);
		enterNodeName = (EditText)findViewById(R.id.node_text);
		screenLabel = (TextView) findViewById(R.id.platformVersionLabel);
		nodelistView = (ListView) findViewById(R.id.nodeListView);	
		emptytxtNode = (TextView)findViewById(R.id.emptyTextNodeListView);
		emptytxtSession  = (TextView)findViewById(R.id.emptyTextSessionListView);
		
		emptytxtNode.setText("No Nodes available");
		emptytxtSession.setText("No Users available");
		
		enterNodeName.addTextChangedListener(mTextEditorWatcher);
		dNodeButton.setOnClickListener(this);
		createNodeButton.setOnClickListener(this);
		joinNodeButton.setOnClickListener(this);
		nextButton.setOnClickListener(this);
		
		dNodeButton.setVisibility(View.GONE);
		lview.setVisibility(View.GONE);
		screenLabel.setText("Discovery Nodes");
		
		//if(adapter == null){
			//adapter = new DebugAdapter(this);
			//adapter.setDebugFilter(DebugAdapter.DebugFlags.All);
			//logview.setAdapter(adapter);
		//}
		
		progressBar = (ProgressBar) findViewById(R.id.progress_bar);		
		selectContent = (LinearLayout) findViewById(R.id.select_content);
		selectContent.setVisibility(View.GONE);
		//logview.setVisibility(View.GONE);
	}
	@Override
	protected void onDestroy()
	{
		if (upDialog != null)
			upDialog.dismiss();
		super.onDestroy();
	}

	@Override
	public void onBackPressed()
	{
		Log.i(LOGC, "back pressed");
		if(nodeAdapter!=null){
			nodeAdapter.removeAllDiscoveryNode();
		}
		finish();
		doStopService();
		super.onBackPressed();
	}

	public void inviteSession(StcSession session)
	{
		if (chatService.inviteSession(session))
			upDialog = ProgressDialog.show(this, "", "Waiting for connection");
	}

	// /
	// / ISimpleChatEventListener methods
	// /

	public void sessionListChanged()
	{
		myHandler.post(new Runnable() {
			public void run()
			{
				if (sessionAdapter != null)
				{
					Log.i(LOGC, "updating list");
					sessionAdapter.setNewSessionList(chatService.getSessions());
					sessionAdapter.notifyDataSetChanged();
					
					if(sessionAdapter.isEmpty() && lview.getVisibility() == View.VISIBLE){
						emptytxtSession.setVisibility(View.VISIBLE);
					}else{
						emptytxtSession.setVisibility(View.GONE);
					}
					
					int debugType = DebugAdapter.DebugFlags.All | DebugAdapter.DebugFlags.Verbose|DebugAdapter.DebugFlags.Discovery;
					//adapter.debugMsg(debugType , "Discovery process called sessionListChanged");
				}
			}
		});
	}

	@Override
	public void connected(final boolean didConnect)
	{
		myHandler.post(new Runnable() {
			public void run()
			{
				if (didConnect)
				{
					Log.i(LOGC, "successful connection");
					if(upDialog!=null && upDialog.isShowing()){
						upDialog.dismiss();
					}
					Intent ni = new Intent(SelectSessionActivity.this, ChatActivity.class);
					startActivity(ni);

//					finish();
				}
				else
				{
					Log.i(LOGC, "connection failed, shutting down.");
					if(upDialog!=null && upDialog.isShowing()){
						upDialog.dismiss();
						Toast.makeText(SelectSessionActivity.this, "Invite rejected or timed out.", Toast.LENGTH_SHORT).show();
					}
				}
				
			}

		});
	}

	public void lineReceived(int count)
	{
		Log.i(LOGC, "line received event in SelectSessionActivity");
	}

	public void remoteDisconnect()
	{
		Log.i(LOGC, "remote disconnect event in SelectSessionActivity");
	}

	@Override
	public void inviteAlert(final UUID inviterUuid, final int inviteHandle, final byte[] oobData) {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		String oobDataStr = "";
		if(oobData != null)
			oobDataStr = "\"" + new String(oobData) + "\"";
		else
			oobDataStr = "empty";
		
		Log.i(LOGC, String.format("Out of band data is %s", oobDataStr));
		
		myHandler.post(new Runnable() {
			
			public void run() {
				
				List<StcSession> list = chatService.getSessions();
				String userName = null;
				for(StcSession user : list){
					if(user.getSessionUuid().compareTo(inviterUuid)==0){
						userName = user.getUserName();
						break;
					}
				}
			
				builder.setPositiveButton("Accept", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   chatService.doConnectionRequest(inviterUuid, inviteHandle);
				           }
				       });
				builder.setNegativeButton("Ignore", new DialogInterface.OnClickListener() {
				           public void onClick(DialogInterface dialog, int id) {
				        	   chatService.rejectInvite(inviteHandle);
				           }
				       });
				
				builder.setTitle("SimpleChat invite");
				builder.setMessage("Accept connection from "+userName+"?");
				builder.show();
			}
		});
	}
	// /
	// / SimpleChatAbstractActivity required overrides.
	// /

	@Override
	public void onStcLibPrepared()
	{
		lib = chatService.getSTCLib();
		
		if(lib == null)
		{
			return;
		}
		// Call startAgent here so we can log unboxing logs to the DevCentral.
		d.print("Application", "SelectSessionActivity", "onStcLibPrepared","Starting DevCentral agent.");
		StartAgent();
		
		if(sessionAdapter == null){
			sessionAdapter = new SessionAdapter(chatService, this);
		}
		if(nodeAdapter == null){
			nodeAdapter = new NodeListAdapter(this);
		}
		chatService.parseInitBundle(bundle);
		lib.addStcDiscoveryNodeUpdateEventListener(SelectSessionActivity.this);

		try
		{
			if(!lib.isUnboxed()){
				Intent intent = new Intent(SelectSessionActivity.this, DiscoverySetupActivity.class);
				startActivityForResult(intent, DISCOVERY_SETUP);
			}
			Log.i("", "LocalUser Registered with Cloud "+lib.queryLocalSession().isRegisteredWithCloud());
		}
		catch (StcException e)
		{
			e.printStackTrace();
		}
		
		myHandler.post(new Runnable() {
			public void run()
			{
				
				lview.setAdapter(sessionAdapter);
				
				nodelistView.setAdapter(nodeAdapter);
				
				if(nodeAdapter.isEmpty() && nodelistView.getVisibility() == View.VISIBLE){
					emptytxtNode.setVisibility(View.VISIBLE);
				}else{
					emptytxtNode.setVisibility(View.GONE);
				}
				
				int debugType = DebugAdapter.DebugFlags.All | DebugAdapter.DebugFlags.Verbose;
				//adapter.debugMsg(debugType , "Platform prepared, onStcLibPrepared called");
				//StartAgent();

			}
		});
		progressBar.setVisibility(View.GONE);
		selectContent.setVisibility(View.VISIBLE);
		//logview.setVisibility(View.VISIBLE);
	}

	@Override
	public void onClick(View v)
	{
		switch(v.getId()){
		case R.id.node_button :
			updateDNvisiblity(true);
			break;
		case R.id.create_node :
			createDN();
			break;
		case R.id.join_node :
			joinDN();
			break;
		case R.id.next :
			updateDNvisiblity(false);
			//d.WriteAgentLog(lib, SimpleChatRegisterApp.appId, STCLoggingModule.LogModule_Application , 
			//				STCLoggingLevel.Info, "Next button is clicked.", 0, null);
			break;
		}
	}
	
	@Override
	public void localSessionChanged()
	{
	}
	
	//Create a discovery node
	private void createDN()
	{
		String nodeName = enterNodeName.getText().toString().trim();
		try
		{
			if(validateCloudServerStatus()){
				EnumSet<NodeFlags> flags = EnumSet.of(NodeFlags.PUBLISH);
				StcLib lib = chatService.getSTCLib();
				if(lib == null)
					return;
				int status = lib.createDiscoveryNode(SimpleChatRegisterApp.id.appId, nodeName, flags);
				
				int debugType = DebugAdapter.DebugFlags.All | DebugAdapter.DebugFlags.Verbose;
				//adapter.debugMsg(debugType , "Creating Node " + nodeName + " Status: " + status);
				enterNodeName.setText(null);
			}
		}
		catch (StcException e)
		{
			e.printStackTrace();
			if(e.errorCode == -10){
				nodeAlreadyJoined(nodeName);
				enterNodeName.setText(null);
			}
		}

	}
	
	//Join a discovery node
	private void joinDN()
	{
		String nodeName = enterNodeName.getText().toString().trim();
		try
		{
			if(validateCloudServerStatus()){
				EnumSet<NodeFlags> flags = EnumSet.of(NodeFlags.PUBLISH);
				StcLib lib = chatService.getSTCLib();
				if(lib == null)
					return;
				int status = lib.joinDiscoveryNode(SimpleChatRegisterApp.id.appId, nodeName, flags);
				
				int debugType = DebugAdapter.DebugFlags.All | DebugAdapter.DebugFlags.Verbose;
				//adapter.debugMsg(debugType , "Joining Node " + nodeName + " Status: " + status);
				enterNodeName.setText(null);
			}
		}
		catch (StcException e)
		{
			e.printStackTrace();
			if(e.errorCode == -10){
				nodeAlreadyJoined(nodeName);
				enterNodeName.setText(null);
			}
		}
	}
	
	/**
	 * This method is used to leave the join node.
	 * @param Value
	 * 			node value to be removed.
	 */
	public void leaveDN(String value)
	{
		try
		{
			if(validateCloudServerStatus()){
				EnumSet<NodeFlags> flags = EnumSet.of(NodeFlags.PUBLISH);
				StcLib lib = chatService.getSTCLib();
				if(lib == null)
					return;
				int status = lib.leaveDiscoveryNode(SimpleChatRegisterApp.id.appId, value, flags);
				
				int debugType = DebugAdapter.DebugFlags.All | DebugAdapter.DebugFlags.Verbose;
				//adapter.debugMsg(debugType , "Leaving Node " + value + " Status: " + status);
			}
		}
		catch (StcException e)
		{
			e.printStackTrace();
		}
	}
	
	//Updating UI on button click.
	private void updateDNvisiblity(boolean value){
		
		if(value){
			enterNodeName.setVisibility(View.VISIBLE);
			createNodeButton.setVisibility(View.VISIBLE);
			joinNodeButton.setVisibility(View.VISIBLE);
			nextButton.setVisibility(View.VISIBLE);
			nodelistView.setVisibility(View.VISIBLE);
			
			dNodeButton.setVisibility(View.GONE);
			lview.setVisibility(View.GONE);
			emptytxtSession.setVisibility(View.GONE);
			screenLabel.setText("Discovery Nodes");
			
			if(nodeAdapter.isEmpty()){
				emptytxtNode.setVisibility(View.VISIBLE);
			}else{
				emptytxtNode.setVisibility(View.GONE);
			}
			
		}else{
			enterNodeName.setVisibility(View.GONE);
			createNodeButton.setVisibility(View.GONE);
			joinNodeButton.setVisibility(View.GONE);
			nextButton.setVisibility(View.GONE);
			nodelistView.setVisibility(View.GONE);
			
			dNodeButton.setVisibility(View.VISIBLE);
			lview.setVisibility(View.VISIBLE);
			screenLabel.setText("Discovery");
			emptytxtNode.setVisibility(View.GONE);
			
			if(sessionAdapter.isEmpty()){
				emptytxtSession.setVisibility(View.VISIBLE);
			}else{
				emptytxtSession.setVisibility(View.GONE);
			}
		}
		
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == RESULT_OK && requestCode == DISCOVERY_SETUP){
			String user = data.getStringExtra("USER_NAME");
			String device = data.getStringExtra("DEVICE_NAME");
			
			try {
				lib = chatService.getSTCLib();
				if(lib!=null){
					lib.setUserName(user!=null ? user:"SimpleChat");
					Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.generic_avatar50x50);
					lib.setAvatar(bm);
					lib.setSessionName(device!=null ? device:"SimpleChatDevice");
					//StartAgent();
				}
			} catch (StcException e) {
				e.printStackTrace();
			}
			
		}
	}

	//Callback for Discovery Node events
	@Override
	public void discoveryNodeUpdate(final DiscoveryNodeUpdateEvent event)
	{		
		final int debugType = DebugAdapter.DebugFlags.All | DebugAdapter.DebugFlags.Verbose;
		Log.i("", "discoveryNodeUpdate event.getStatus()"+event.getStatus()+event.getNode().getName()+"Event type: "+event.getEventType()+"error code: "+event.getDiscoveryNodeError());
		//adapter.debugMsg(debugType , "Discovery Node Update event called");
		
		if(event.getStatus() == 1)
		{
			//adapter.debugMsg(debugType, "Discovery Node update failed - "+event.getDiscoveryNodeError().toString());
			if(!event.getDiscoveryNodeError().equals(DiscoveryNodeUpdateEvent.DiscoveryNodeError.noError))
				displayErrorToast(event.getDiscoveryNodeError().toString());
			return;
		}
		
		StcDiscoveryNode node = event.getNode();
		
		switch(event.getEventType())
		{
			case CREATE:
				boolean value = nodeAdapter.addDiscoveryNode(node);
				//if(!value){
					//adapter.debugMsg(debugType , "Node '"+node.getName()+"' already exist");
				//}else{
					//adapter.debugMsg(debugType , "Creating Node "+node.getName());
				//}
				break;
			case DELETE:
				//adapter.debugMsg(debugType , "Deleting Node "+node.getName());
				break;
			case JOIN:
				boolean temp = nodeAdapter.addDiscoveryNode(node);
				//if(!temp){
					//adapter.debugMsg(debugType , "Node '"+node.getName()+"' already joined.");
				//}else{
					//adapter.debugMsg(debugType , "Joining Node "+node.getName());
				//}
				break;
			case LEAVE:
				//adapter.debugMsg(debugType , "Leaving Node "+node.getName());
				break;
			case PUBLISH:
				//adapter.debugMsg(debugType , "Publishing Node "+node.getName());
				break;
			default:
				break;
		}

			//Updating NodeAdapter on empty state.
			myHandler.post(new Runnable() {
				
				@Override
				public void run() {
					if(nodeAdapter.isEmpty()){
						emptytxtNode.setVisibility(View.VISIBLE);
					}else{
						emptytxtNode.setVisibility(View.GONE);
					}
					
				}
			});
	}
	
	private final TextWatcher  mTextEditorWatcher = new TextWatcher() 
	{
        
        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {

        }

        public void onTextChanged(CharSequence s, int start, int before, int count)
        {
           
        }

        public void afterTextChanged(Editable s)
        {
        	if(s.toString().matches(""))
        	{
        		createNodeButton.setEnabled(false);
        		joinNodeButton.setEnabled(false);
        		
        	}
        	else
        	{
        		createNodeButton.setEnabled(true);
        		joinNodeButton.setEnabled(true);
        	}
        }
	};
	
	//This method will be used for validating Cloud server is up and running
	//before making createNode, joinNode and leaveNode calls.
	private boolean validateCloudServerStatus() throws StcException{
		StcLib lib = chatService.getSTCLib();
		if(lib!=null && lib.queryDiscoveryNodeServiceStatus() == 0){
			return true;
		}else{
			myHandler.post(new Runnable() {
				
				@Override
				public void run() {
					final AlertDialog.Builder builder = new AlertDialog.Builder(SelectSessionActivity.this);
					builder.setTitle("Warning");
					builder.setMessage("Cloud service is not online, try later.");
					builder.setCancelable(true);
					
					builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							if(builder!=null)
								dialog.dismiss();
						}
					});
					builder.show();
				}
			});
			return false;
		}
	}
	
	//Display toast-Node already joined.
	private void nodeAlreadyJoined(final String value){
		myHandler.post(new Runnable() {
			
			@Override
			public void run() {
				final AlertDialog.Builder builder = new AlertDialog.Builder(SelectSessionActivity.this);
				builder.setTitle("Node '"+value+"' already joined.");
				builder.setCancelable(true);
				
				builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if(builder!=null)
							dialog.dismiss();
					}
				});
				builder.show();
			}
		});
		
	}
	
	//Display toast- Discovery Node update failed.
	private void displayErrorToast(final String value){
		myHandler.post(new Runnable() {
			
			@Override
			public void run() {
				final AlertDialog.Builder builder = new AlertDialog.Builder(SelectSessionActivity.this);
				builder.setTitle("Discovery node update failed - "+value);
				builder.setCancelable(true);
				
				builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if(builder!=null)
							dialog.dismiss();
					}
				});
				builder.show();
			}
		});
		
	}
}
