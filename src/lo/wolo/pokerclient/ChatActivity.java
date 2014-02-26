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
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
	TextView currentRoleView = null;
	TextView currentMoneyView = null;
	private Button raiseButton = null;
	private Button foldButton = null;
	private Button checkButton = null;
	private Button callButton = null;
	private Button allinButton = null;
	private Button betButton = null;
	private ImageView card1Image = null;
	private ImageView card2Image = null;
	int card1 = -1;
	int card2 = -1;
	int curbet = 0;
	int minbet = 0;
	int money  = 0;
	int amount = 0;

	int hIndex = 0;

	static final int cardDrawables[] = {
		R.drawable.card_00, R.drawable.card_01, R.drawable.card_02, R.drawable.card_03, R.drawable.card_04,
		R.drawable.card_05, R.drawable.card_06, R.drawable.card_07, R.drawable.card_08, R.drawable.card_09,
		R.drawable.card_10, R.drawable.card_11, R.drawable.card_12, R.drawable.card_13, R.drawable.card_14,
		R.drawable.card_15, R.drawable.card_16, R.drawable.card_17, R.drawable.card_18, R.drawable.card_19,
		R.drawable.card_20, R.drawable.card_21, R.drawable.card_22, R.drawable.card_23, R.drawable.card_24,
		R.drawable.card_25, R.drawable.card_26, R.drawable.card_27, R.drawable.card_28, R.drawable.card_29,
		R.drawable.card_30, R.drawable.card_31, R.drawable.card_32, R.drawable.card_33, R.drawable.card_34,
		R.drawable.card_35, R.drawable.card_36, R.drawable.card_37, R.drawable.card_38, R.drawable.card_39,
		R.drawable.card_40, R.drawable.card_41, R.drawable.card_42, R.drawable.card_43, R.drawable.card_44,
		R.drawable.card_45, R.drawable.card_46, R.drawable.card_47, R.drawable.card_48, R.drawable.card_49,
		R.drawable.card_50, R.drawable.card_51
	};

	static enum roles {
		NONE,
		SMALLBLIND,
		BIGBLIND,
		DEALER
	};

	roles role = roles.NONE;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chat);

		currentMoneyView = (TextView)findViewById(R.id.currentMoney);
		updateMoney();

		currentRoleView = (TextView)findViewById(R.id.currentRole);
		updateRole();

		card1Image = (ImageView)findViewById(R.id.leftCardImage);
		card2Image = (ImageView)findViewById(R.id.rightCardImage);

		lview = (ListView)findViewById(R.id.chat_history);
		cadapter = new ChatAdapter(getApplicationContext());
		lview.setAdapter(cadapter);

		raiseButton = (Button)findViewById(R.id.raiseButton);
		raiseButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				raiseDialog();
			}
		});
		
		foldButton = (Button)findViewById(R.id.foldButton);
		foldButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				writeLine("fold");
			}
		});

		checkButton = (Button)findViewById(R.id.checkButton);
		checkButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				writeLine("check");
			}
		});

		callButton = (Button)findViewById(R.id.callButton);
		callButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				writeLine("call");
			}
		});

		allinButton = (Button)findViewById(R.id.allinButton);
		allinButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				writeLine("allin");
			}
		});

		betButton = (Button)findViewById(R.id.betButton);
		betButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View arg0) {
				betDialog();
			}
		});
		
	}
	
	private void processRaise() {
		if (amount <= 0) {
			Toast.makeText(getApplicationContext(), "Invalid amount", Toast.LENGTH_LONG).show();
			return;
		}
		if (amount > money) {
			Toast.makeText(getApplicationContext(), "Not enough money", Toast.LENGTH_LONG).show();
			return;
		}
		if (amount < curbet) {
			Toast.makeText(getApplicationContext(), "Raise is too low", Toast.LENGTH_LONG).show();
			return;
		}
		writeLine("raise "+amount);
	}

	private void processBet() {
		if (amount <= 0) {
			Toast.makeText(getApplicationContext(), "Invalid amount", Toast.LENGTH_LONG).show();
			return;
		}
		if (amount <= minbet) {
			Toast.makeText(getApplicationContext(), "Bet is too low", Toast.LENGTH_LONG).show();
			return;
		}
		if (amount > money) {
			Toast.makeText(getApplicationContext(), "Not enough money", Toast.LENGTH_LONG).show();
			return;
		}
		writeLine("bet "+amount);
	}
	
	@Override
	public void onBackPressed() {
		//To disconnect the connection rather than closing the app.
		chatService.remoteDisconnect();
		super.onBackPressed();
	}

	private void raiseDialog() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		alert.setView(input);
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				amount = Integer.parseInt(input.getText().toString());
				dialog.dismiss();
				processRaise();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				amount = 0;
				dialog.cancel();
			}
		});
		alert.show();
	}

	private void betDialog() {
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_NUMBER);
		alert.setView(input);
		alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				amount = Integer.parseInt(input.getText().toString());
				dialog.dismiss();
				processBet();
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				amount = 0;
				dialog.cancel();
			}
		});
		alert.show();
	}

	private void updateRole() {
		switch (role) {
			case NONE:
				currentRoleView.setText(R.string.role_none);
				break;
			case DEALER:
				currentRoleView.setText(R.string.role_dealer);
				break;
			case SMALLBLIND:
				currentRoleView.setText(R.string.role_smallblind);
				break;
			case BIGBLIND:
				currentRoleView.setText(R.string.role_bigblind);
				break;
		}
	}

	private void updateMoney() {
		currentMoneyView.setText("$ " + money);
		int color = (money > 0) ? Color.GREEN : Color.RED;
		currentMoneyView.setTextColor(color);
	}

	private void parseLine(String line) {
		Log.d("ReceivedLine", " == "+line);
		String[] lines = line.split(";");
		for (String l : lines) {
			Log.d("ReceivedLine", l);
			if (l.startsWith("cmds ")) {
				String msg = l.substring(5);
				int cmds = Integer.parseInt(msg);
				raiseButton.setEnabled((cmds & Constants.RAISE) != 0);
				foldButton .setEnabled((cmds & Constants.FOLD ) != 0);
				checkButton.setEnabled((cmds & Constants.CHECK) != 0);
				callButton .setEnabled((cmds & Constants.CALL ) != 0);
				allinButton.setEnabled((cmds & Constants.ALLIN) != 0);
				betButton  .setEnabled((cmds & Constants.BET  ) != 0);
			} else if (l.startsWith("cards ")) {
				String msg = l.substring(6);
				String[] cards = msg.split(" ");
				card1 = Integer.parseInt(cards[0]);
				card2 = Integer.parseInt(cards[1]);
				if (card1 < 0) card1Image.setImageResource(R.drawable.card_back);
				else card1Image.setImageResource(cardDrawables[card1]);
				if (card2 < 0) card2Image.setImageResource(R.drawable.card_back);
				else card2Image.setImageResource(cardDrawables[card2]);
			} else if (l.startsWith("curbet ")) {
				String msg = l.substring(7);
				curbet = Integer.parseInt(msg);
			} else if (l.startsWith("minbet ")) {
				String msg = l.substring(7);
				minbet = Integer.parseInt(msg);
			} else if (l.startsWith("money ")) {
				String msg = l.substring(6);
				money = Integer.parseInt(msg);
				updateMoney();
			} else if (l.startsWith("role ")) {
				String msg = l.substring(5);
				if (msg.equals("none"))
					role = roles.NONE;
				else if (msg.equals("smallBlind"))
					role = roles.SMALLBLIND;
				else if (msg.equals("bigBlind"))
					role = roles.BIGBLIND;
				else if (msg.equals("dealer"))
					role = roles.DEALER;
				else
					return;
				updateRole();
			}
		}
	}

	@Override
	public void lineReceived(int line) {
		myHandler.post(new Runnable() {
			public void run() {
				List<String> lines = chatService.getLines();
				cadapter.setNewChatList(lines);
				cadapter.notifyDataSetChanged();

				for (int i = hIndex; i < lines.size(); i++) {
					parseLine(lines.get(i));
				}
				hIndex = lines.size()-1;
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
