package lo.wolo.pokerclient;

import java.util.ArrayList;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DebugAdapter extends BaseAdapter
{

	private ArrayList<String> list = new ArrayList<String>();
//	private Context mContext = null;
	private SelectSessionActivity baseActivity;
	DebugAdapter(SelectSessionActivity baseActivity) 
	{
		this.baseActivity = baseActivity;
	}
	
	private int currentFlag = DebugFlags.All;

	public static class DebugFlags
    {
		static int	Error = 0x0001;
		static int  Warning         = 0x0002;
		static int  Verbose         = 0x0004;

		static int  Registration    = 0x1000;
		static int  Discovery       = 0x2000;
		static int  Invitation      = 0x4000;
		static int  Communication   = 0x8000;
		static int  CloudDiscovery  = 0x0100;
		static int  All  = Error | Warning | Verbose | Registration | Discovery | Invitation | Communication | CloudDiscovery;
    }

    /*public void passContext(Context ctx){
    	mContext = ctx;
    }*/
    public void setDebugFilter(int flag){
    	list.clear();
    	currentFlag = flag;
    }
    public void debugMsg(int flag, final String msg){
    	if(flag == currentFlag)
    	{
    		baseActivity.runOnUiThread(new Runnable(){
                @Override
                public void run() {
                	//This will append the list from zeroth position and the old data will move to next position.
            		list.add(0, msg);
            		//Collections.sort(list);
                	notifyDataSetChanged();
                }
            });

    	}
    }
	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return list.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int index, View convertView, ViewGroup parent) {
		
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater)parent.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.debug_row, null);
		} else
			convertView.setVisibility(View.VISIBLE);
		
		try{
			TextView msg = (TextView)convertView.findViewById(R.id.row_msg);
			msg.setText(list.get(index));
		}catch(Exception e){
			Log.e("", "getView error: "+e.getMessage());
		}
		
		return convertView;
	}

}
