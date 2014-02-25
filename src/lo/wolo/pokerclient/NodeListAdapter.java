package lo.wolo.pokerclient;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.intel.stc.utility.StcDiscoveryNode;

public class NodeListAdapter extends BaseAdapter {

	private ArrayList<StcDiscoveryNode> discoveryNodeList = new ArrayList<StcDiscoveryNode>();
	
	private SelectSessionActivity baseActivity;
	
	public NodeListAdapter(SelectSessionActivity baseActivity) {
		this.baseActivity = baseActivity;
	}
	
	//Returns false if node already exists otherwise returns true.
	public boolean addDiscoveryNode(final StcDiscoveryNode node)
	{
		 //Since discoveryNodeList.contains(node) is returning false, using the below technique to check
		 //whether user exists in the list or not and if exists don't add to discoveryNodeList.
		 for(int index=0 ; index<discoveryNodeList.size();index++){
			 if(discoveryNodeList.get(index).getNodeId().equals(node.getNodeId())){
				return false;
			 }
		 }
		
		baseActivity.runOnUiThread(new Runnable(){
            @Override
            public void run() {
            	discoveryNodeList.add(node);
            	notifyDataSetChanged();
            }
        });
		
		return true;
	}
	
	private void removeDiscoveryNode(final StcDiscoveryNode node){
		if(discoveryNodeList.contains(node))
		{
			baseActivity.runOnUiThread(new Runnable(){
	            @Override
	            public void run() {
	            	baseActivity.leaveDN(node.getName());
	    			discoveryNodeList.remove(node);
	            	notifyDataSetChanged();
	            }
	        });
			
		}
	}
	
	//Leave all join nodes while exiting the app.
	public void removeAllDiscoveryNode(){
		for(int i=0 ;i < discoveryNodeList.size();i++){
			baseActivity.leaveDN(discoveryNodeList.get(i).getName());
		}
	}
		
	@Override
	public int getCount() {
		synchronized (discoveryNodeList) {
			return discoveryNodeList.size();
		}
	}

	@Override
	public Object getItem(int position) {
		
		synchronized(discoveryNodeList) {
			if (discoveryNodeList != null && position < discoveryNodeList.size() && position >= 0)
				return discoveryNodeList.get(position);
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
		
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)baseActivity.getApplicationContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.node_row, null);
		} else
			convertView.setVisibility(View.VISIBLE);

		StcDiscoveryNode curNode = null;
		synchronized (discoveryNodeList) {
			if (position >= 0 && position < discoveryNodeList.size()) 
				curNode = (StcDiscoveryNode)getItem(position);
		}
		
		if( curNode == null ) {
			convertView.setVisibility(View.GONE);
			return convertView;
		}

		// get the name from the session and put it into the text view
		TextView nodeName = (TextView)convertView.findViewById(R.id.node_name);
		nodeName.setText(curNode.getName());

		// setup a click handler to pass invites up to the service.
		final StcDiscoveryNode node = curNode;
		convertView.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				displayDialog(node);
			}
		});
		
		return convertView;
	}
	
	private void displayDialog(final StcDiscoveryNode node){
		final AlertDialog.Builder builder = new AlertDialog.Builder(baseActivity);
		builder.setTitle("Leave Discovery Node: "+node.getName());
		builder.setCancelable(false);
		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				removeDiscoveryNode(node);
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				if(builder!=null)
					dialog.dismiss();
			}
		});
		builder.show();
	}

}
