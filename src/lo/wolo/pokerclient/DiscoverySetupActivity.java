package lo.wolo.pokerclient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.intel.startup.CloudAuthorizationActivity;
import com.intel.stc.utility.StcConstants;

public class DiscoverySetupActivity extends Activity {

	Button register = null;
	Button skip = null;
	EditText userName = null;
	EditText deviceName = null;
	private static final int START_CLOUD		= 129031;
//	private static final int CLOUD_COMPLETED	= 1091902;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_discovery_setup);
		
		userName = (EditText)findViewById(R.id.userName);
		deviceName = (EditText)findViewById(R.id.deviceName);
		register = (Button)findViewById(R.id.registerButton);
		skip = (Button)findViewById(R.id.skipButton);
		
		//Setting default name to Username and device name.
		userName.setText(android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL);
		deviceName.setText("Android "+ (isTablet(this)?"Tablet" : "Phone"));
		
		register.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {				
				
				Intent intent = new Intent(getApplicationContext(), CloudAuthorizationActivity.class);
				intent.putExtra("clientID", SimpleChatRegisterApp.id.clientId);
				intent.putExtra("redirectURL", SimpleChatRegisterApp.redirectURL);
				intent.putExtra("appId", SimpleChatRegisterApp.id.appId.toString());
				
				startActivityForResult(intent, START_CLOUD);
			}
		});
		
		skip.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				setIntentDescription();
			}
		});
	}

	private void setIntentDescription(){
		Intent intent = new Intent();
		String uName = userName.getText().toString();
		intent.putExtra("USER_NAME", uName!=null && !uName.equals("") ? uName : android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL);
		String dName = deviceName.getText().toString();
		intent.putExtra("DEVICE_NAME", dName!=null && !dName.equals("") ? dName : "Android "+ (isTablet(this)?"Tablet" : "Phone"));
		setResult(RESULT_OK, intent);
		finish();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == START_CLOUD)
		{
			if (resultCode == StcConstants.STC_RESULT_OK){
				setIntentDescription();
			}else{
				Toast.makeText(this, "Unable to complete cloud registration, please try again.",Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	//Validating device is a Tablet or Phone.
		private boolean isTablet(Context context) {  
	        return (context.getResources().getConfiguration().screenLayout   
	                & Configuration.SCREENLAYOUT_SIZE_MASK)    
	                >= Configuration.SCREENLAYOUT_SIZE_LARGE; 
	    }
}
