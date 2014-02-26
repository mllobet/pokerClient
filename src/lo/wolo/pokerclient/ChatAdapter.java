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

import java.util.ArrayList;
import java.util.List;

import lo.wolo.pokerclient.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/***
 * Simple adapter that puts lines of text into the chat listview.
 * <p>
 * There is no c3 specific code here.
 */
public class ChatAdapter extends BaseAdapter {
	
	List<String>chatHistory = new ArrayList<String>();
	Context context;
	
	public ChatAdapter( Context context )
	{
		this.context = context;
	}
	
	@Override
	public int getCount() {
		synchronized(chatHistory) {
			return chatHistory.size();
		}
	}

	@Override
	public Object getItem(int position) {
		synchronized(chatHistory) {
			if( position >= 0 && position < chatHistory.size() )
				return chatHistory.get(position);
			else
				return null;
		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if( convertView == null ) {
			LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.chat_row, null);
		} else
			convertView.setVisibility(View.VISIBLE);
		
		String line = null;
		synchronized(chatHistory) {
			if( position >= 0 && position < chatHistory.size())
				line = chatHistory.get(position);
		}
		
		if( line == null ) {
			convertView.setVisibility(View.GONE);
			return convertView;
		}
		
		TextView lineView = (TextView)convertView.findViewById(R.id.row_chat);
		lineView.setText(line);
		
		return convertView;
	}
	
	public void setNewChatList(List<String>newHistory)
	{
		chatHistory = newHistory;
	}

}