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

import java.util.UUID;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/***
 * Activity that handles the listview for received text, the edit field for text to 
 * send and the send button.
 * <p>
 * There is nothing c3 specific here.
 */
public class ChatActivity extends AbstractServiceUsingActivity {

	ChatAdapter cadapter;
	ListView lview;
	private Button raiseButton = null;
	private Button foldButton = null;
	private Button checkButton = null;
	private Button callButton = null;
	private Button allinButton = null;
	private Button betButton = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);
		
		lview = (ListView)findViewById(R.id.chat_history);
		cadapter = new ChatAdapter(getApplicationContext());
		lview.setAdapter(cadapter);

		raiseButton = (Button)findViewById(R.id.raiseButton);
		raiseButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) { writeLine("raise"); }
		});

		foldButton = (Button)findViewById(R.id.foldButton);
		foldButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) { writeLine("fold"); }
		});

		checkButton = (Button)findViewById(R.id.checkButton);
		checkButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) { writeLine("check"); }
		});

		callButton = (Button)findViewById(R.id.callButton);
		callButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) { writeLine("call"); }
		});

		allinButton = (Button)findViewById(R.id.allinButton);
		allinButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) { writeLine("allin"); }
		});

		betButton = (Button)findViewById(R.id.betButton);
		betButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) { writeLine("bet"); }
		});
		
	}
	
	@Override
	public void onBackPressed() {
		//To disconnect the connection rather than closing the app.
		chatService.remoteDisconnect();
		super.onBackPressed();
	}

	@Override
	public void lineReceived(int line) {
		myHandler.post(new Runnable() {
			public void run() {
				cadapter.setNewChatList(chatService.getLines());
				cadapter.notifyDataSetChanged();
			}
		});
	}

	@Override
	public void remoteDisconnect() {
		myHandler.post(new Runnable() {
			public void run() 
			{
				Toast.makeText(ChatActivity.this, "Remote session disconnected.", Toast.LENGTH_SHORT).show();
				finish();
			}
		});
	}
	
	public void writeLine(String str) {
		chatService.writeString(str);
	}
	
	@Override
	public void onStcLibPrepared()
	{
		myHandler.post(new Runnable() {
			public void run() {
				ListView lview = (ListView)findViewById(R.id.chat_history);	
				lview.setAdapter(cadapter);
				cadapter.setNewChatList(chatService.getLines());
			}
		});
	}
	
	@Override
	public void sessionListChanged() {
	}

	@Override
	public void connected(boolean didConnect) {
	}

	@Override
	public void localSessionChanged() {
	}

	@Override
	public void inviteAlert(UUID inviterUuid, int inviteHandle, byte[] oobData) {
		
	}
}
