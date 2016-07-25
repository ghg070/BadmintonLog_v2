package nctu.nol.account;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import nctu.nol.badmintonlogprogram.R;

public class RegisterPage extends Activity {

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_page);
        sharedPreferences = getSharedPreferences(getString(R.string.PREFS_NAME),0);
        editor=sharedPreferences.edit();
    }

    public void sumit_click(final View view) {
        Log.v("Tag", "submit click");
        final String account =((EditText)findViewById(R.id.account)).getText().toString();
        String passwd=((EditText)findViewById(R.id.passwd)).getText().toString().trim();
        String passwd2=((EditText)findViewById(R.id.passwd2)).getText().toString().trim();
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(account).matches())
        {
            Log.v("Tag","bad account");
            Toast.makeText(view.getContext(), "account is a invaild email", Toast.LENGTH_LONG).show();
            return;
        }
        if(passwd==null||passwd.isEmpty())
        {
            Log.v("Tag","no passwd");
            Toast.makeText(view.getContext(),"password can't be blank",Toast.LENGTH_LONG).show();
            return;
        }

        if(passwd2==null||passwd2.isEmpty())
        {
            Log.v("Tag", "no passwd2");
            Toast.makeText(view.getContext(),"please comfirm password",Toast.LENGTH_LONG).show();
            return;
        }
        if(!passwd.equals(passwd2))
        {
            Log.v("Tag","passwd not equal");
            Log.v("Tag","passwd"+passwd+".");
            Log.v("Tag","passwd2"+passwd2+".");
            Toast.makeText(view.getContext(),"two passwd different, please type again",Toast.LENGTH_LONG).show();
            ((EditText)findViewById(R.id.passwd)).setText("");
            ((EditText)findViewById(R.id.passwd2)).setText("");
            return;

        }
        //TODO encrypt pw
        final String encry_pw=passwd;
        //sent to server

       API.creste_user(account,encry_pw,new ResponseListener(){
           public void onResponse(JSONObject response)
           {
               try {
                   String result =response.getString("result");
                   if(result.equals("create users success"))
                   {
                       Toast.makeText(view.getContext(),"account created successfully,switching back to login page",Toast.LENGTH_LONG).show();
                       Intent i = new Intent(getApplicationContext(), LoginPage.class);
                       i.putExtra("account",account);
                       Log.d("Tag",account);
                       startActivity(i);
                       finish();
                   }
               } catch (JSONException e) {
                   e.printStackTrace();
                   Log.d("Tag:Create", e.getMessage());
               }
           }

           public void onErrorResponse(VolleyError error){
               Toast.makeText(view.getContext(),"account created error",Toast.LENGTH_LONG).show();
               JSONObject response= null;
               try {
                   response = new JSONObject(new String(error.networkResponse.data));
                   Toast.makeText(view.getContext(),response.getJSONObject("data").getString("account"),Toast.LENGTH_LONG).show();
               } catch (JSONException e) {
                   e.printStackTrace();
               }

           }
       });
    }
}
