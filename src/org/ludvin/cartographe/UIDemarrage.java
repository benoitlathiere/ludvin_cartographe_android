package org.ludvin.cartographe;


import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class UIDemarrage extends Activity implements OnClickListener {
	
	//global
	private SharedPreferences prefs;
	//private SharedPreferences.Editor editor;
	
	//UI
	private EditText ET_login;
	private EditText ET_pass;
	private Button btnIdentification;
	private LinearLayout LL_identification;
	private View LL_attente;
	private TextView TVVersion;
	private CheckBox CBKeepCnx;
	private TextView TVDemo;
	private String appversion;
	private AsyncTask<JSONObject, Integer, JSONObject> tacheupdate;
	
	//tmp
	private String login;
	private String pass;
	private boolean keepcnx;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uidemarrage);
		
		prefs = getSharedPreferences(Constants.PREFS_FILENAME, MODE_PRIVATE);
		
		//UI
		ET_login = (EditText) findViewById(R.id.ET_login);
		ET_pass = (EditText) findViewById(R.id.ET_pass);
		btnIdentification = (Button) findViewById(R.id.btnIdentification);
		btnIdentification.setOnClickListener(this);
		LL_identification = (LinearLayout) findViewById(R.id.LL_identification);
		LL_attente = (View) findViewById(R.id.LL_attente);
		//progressBar1 = (ProgressBar) findViewById(R.id.progressBar1);
		TVVersion = (TextView) findViewById(R.id.TVVersion);
		CBKeepCnx = (CheckBox) findViewById(R.id.CBKeepCnx);
		TVDemo = (TextView) findViewById(R.id.TVDemo);
		
		//version
		PackageInfo pInfo = null;
		appversion ="";
		try {
			pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			appversion = pInfo.versionName;
		} catch (NameNotFoundException e) {
			if (Constants.debug_mode)
				e.printStackTrace();
		}
		TVVersion.setText(getString(R.string.label_version)+" "+appversion);
		
		checkAppliUpdate();
		
		//mode DEMO
		if (Constants.MODE_DEMO) {
			TVDemo.setVisibility(View.VISIBLE);
			CBKeepCnx.setVisibility(View.GONE);
			ET_login.setText("ludvin@demo");
			ET_pass.setText("demo");
		}
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		
		//prefs
		if (GlobalVars.getUserKeep(this) && (!Constants.MODE_DEMO)) {
			ET_login.setText(GlobalVars.getUserLogin(this));
			ET_pass.setText(GlobalVars.getUserPass(this));
		}
		CBKeepCnx.setChecked(GlobalVars.getUserKeep(this));
		
		GlobalVars.setCleanCache(this);
		
		//check Internet connection at startup
		if (! GlobalVars.isNetworkAvailable(this)) {
			Toast toast = Toast.makeText(this, R.string.msg_internet_unreachable_check_connection, Toast.LENGTH_LONG);toast.show();
		}
	}

	public void onClick(View v) {
		if (v.getId()==R.id.btnIdentification) {
			identification();
		}
	}
	
	/**
	 * Contrôle l'identification à la soumission du formulaire.
	 */
	private void identification() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); //on masque le clavier
		
		//UI
		/*	//obsolète
		LL_identification.setVisibility(View.GONE);
		LL_attente.setVisibility(View.VISIBLE);
		//progressbar d'attente, 2s
		LL_attente.animate().setDuration(2000).alpha(1).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
					//obsolète, géré par classe sendJSON
				// LL_attente.setVisibility(View.GONE);
				//LL_identification.setVisibility(View.VISIBLE);
			}
		});*/
		
		login = ET_login.getText().toString().trim();
		pass = ET_pass.getText().toString();
		keepcnx = CBKeepCnx.isChecked();
		if (login.isEmpty()) {
			ET_login.setError(getString(R.string.msg_bad_email_address));
		} else if (pass.isEmpty()) {
			ET_pass.setError(getString(R.string.msg_please_give_password));
		} else  {
			//source : http://www.vogella.com/articles/AndroidNetworking/article.html
			//nécessite la permission android.permission.INTERNET
			if (GlobalVars.isNetworkAvailable(this)) {
				JSONObject msgjson = new JSONObject();
				JSONObject jsoncontenu = new JSONObject();
				try {
					msgjson.put("objet", "identification");
					msgjson.put("action", "identifier");
					msgjson.put("user_id", prefs.getInt("user_id", 0));
						jsoncontenu.put("login",login);
						jsoncontenu.put("pass",pass);
					msgjson.put("contenu",jsoncontenu);
					try {
						new sendJSON().execute(msgjson);
					} catch (IllegalStateException e) {
						e.printStackTrace();
						Toast toast = Toast.makeText(this, getString(R.string.msg_problem)+"\n"+getString(R.string.msg_cant_identify), Toast.LENGTH_LONG);toast.show();
					}
				} catch (JSONException e) {
					e.printStackTrace();
					Toast toast = Toast.makeText(this, getString(R.string.msg_problem)+"\n"+getString(R.string.msg_cant_identify), Toast.LENGTH_LONG);toast.show();
				}
			} else {
				Toast toast = Toast.makeText(this, getString(R.string.msg_no_internet_connection)+"\n"+getString(R.string.msg_cant_identify), Toast.LENGTH_LONG);toast.show();
			}
		}
	}

	/**
	 * Envoi d'un message au serveur en tâche de fond.
	 * @param Message à envoyer au serveur doInBackground(JSONObject)
	 * @param Integer Valeur de progression onProgessUpdate(int)
	 * @param String Valeur de retour en fin de tâche doInBackground()
	 * @return Retourne en interne un objet JSON des réponses du serveur.
	 */
	private class sendJSON extends AsyncTask<JSONObject, Integer, JSONObject> {
		protected void onPreExecute() {
			LL_identification.setVisibility(View.GONE);
			LL_attente.setVisibility(View.VISIBLE);
		}
		@Override
		protected JSONObject doInBackground(JSONObject... jsonMsg) {
			Log.d(Constants.TAG, "doInBackground()");
			String reponse = GlobalVars.postJsonServer(jsonMsg[0]);
			try {
				JSONObject jreponse=new JSONObject(reponse);
				return jreponse;
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			return null;
		}
		protected void onPostExecute(JSONObject jreponse) {
			if (Constants.debug_mode)
				Log.v(Constants.TAG, "onPostExecute() : "+jreponse.toString());
			displayProgressbar(false);
			if (jreponse!=null) {
				try {
					JSONObject reponses = jreponse.getJSONObject("reponses");
					if (reponses.optString("reponse").contains("identification_acceptee")  && reponses.optInt("ID",0)>0) {
						//editor=prefs.edit();
						GlobalVars.setUserKeep(getBaseContext(), keepcnx);
						//editor.putBoolean("user_keep", keepcnx);
						if (keepcnx && !Constants.MODE_DEMO) {	//save infos
							//editor.putString("store_user_login", login);
							GlobalVars.setUserLogin(getBaseContext(), login);
							//editor.putString("store_user_pass", pass);
							GlobalVars.setUserPass(getBaseContext(), pass);
						}
						//editor.putInt("user_id", reponses.optInt("ID",0));
						//editor.apply();
						GlobalVars.setUserID(getBaseContext(), reponses.optInt("ID",0));
						goTo(true);
					} else {
						Log.d(Constants.TAG,reponses.optString("erreur"));
						goTo(false);
					}
					if (Constants.debug_mode)
						Log.v(Constants.TAG,reponses.optString("reponse")+" & "+reponses.optInt("ID",0));
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Display or not progress bar during identification check.
	 * @param display State to do.
	 */
	private void displayProgressbar(boolean display) {
		if (display) {
			LL_attente.setVisibility(View.VISIBLE);
			LL_identification.setVisibility(View.GONE);
		} else {
			LL_attente.setVisibility(View.GONE);
			LL_identification.setVisibility(View.VISIBLE);
		}
	}
	
	/**
	 * Orienter selon l'état de l'identification.
	 * @param go Validatino de l'identification.
	 */
	private void goTo(boolean go) {
		if (go) {	//identification OK
			startActivity (new Intent (this, UIAccueil.class));
			this.finish();
		} else {
			Toast toast = Toast.makeText(this, "Erreur d'identification ! Veuillez recommencer.", Toast.LENGTH_LONG);toast.show();
			if (! Constants.MODE_DEMO)
				ET_pass.setText("");
		}
	}
	
	/**
	 * Demande la vérification d'une version plus récente disponible de l'application.
	 */
	private void checkAppliUpdate() {
		if (GlobalVars.isNetworkAvailable(this)) {
			JSONObject msgjson = new JSONObject();
			JSONObject jsoncontenu = new JSONObject();
			try {
				msgjson.put("objet", "application");
				msgjson.put("action", "verifier");
				msgjson.put("user_id", 0);
					jsoncontenu.put("appversion", appversion);
				msgjson.put("contenu",jsoncontenu);
				try {
					tacheupdate = new sendUpdate().execute(msgjson);
				} catch (IllegalStateException e) {
					if (Constants.debug_mode)
						e.printStackTrace();
				}
			} catch (JSONException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			}
		} else if (Constants.debug_mode) {
			Log.i(Constants.TAG,"Pas de connexion Internet. On ne peut pas vérifier la mise à jour éventuelle.");
		}
	}
	
	/**
	 * Compare la version courante avec la nouvelle version.
	 * @param last Dernière version en ligne (de la forme 1.2.3).
	 * @param current Version courante de l'application (de la forme 1.2.3).
	 * @return <code>true</code> si une mise à jour est disponible.
	 */
	private boolean compareVersion(String last, String current) {
		if (Constants.debug_mode)
			Log.i(Constants.TAG,"compareVersion() Version appli : "+current+" / dernière version selon serveur : "+last);
		if(last==null || current==null || last.isEmpty() || current.isEmpty()) {
			if (Constants.debug_mode)
				Log.e(Constants.TAG, "compareVersion() Problème de conversion des versions.");
			return false;
		}
		List<Integer> lastversion = GlobalVars.convertVersionNumbers(last);
		List<Integer> currentversion = GlobalVars.convertVersionNumbers(current);
		if (lastversion==null || currentversion==null) {
			if (Constants.debug_mode)
				Log.e(Constants.TAG, "compareVersion() Problème de conversion des versions.");
			return false;
		}
		try {
			if (lastversion.get(0)>currentversion.get(0)) {
				return true;
			} else if (lastversion.get(0)==currentversion.get(0))
				if (lastversion.get(1)>currentversion.get(1)) {
					return true;	
				} else if (lastversion.get(1)==currentversion.get(1)) {
					if (lastversion.get(2) > currentversion.get(2))
						return true;
			}
		} catch (NumberFormatException e) {
			if (Constants.debug_mode)
				e.printStackTrace();
		} catch (IndexOutOfBoundsException e) {
			if (Constants.debug_mode)
				e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * Vérifie la version de l'application avec celle reçue du serveur 
	 * et propose le téléchargement le cas échéant.
	 * @param json Objet JSON contenant la dernière version disponible. ex : reponses{lastversion:"1.2.3"}
	 */
	private void checkAppliUpdate(JSONObject json) {
		/**
		 * L'objet JSON retourné doit être au minimum :
		 * reponses {
		 * 			lastversion:"1.2.3"
		 * 		}
		 */
		if (json!=null) {
			boolean mettreajour=false;
			String txtlastversion="";
			try {
				JSONObject reponses = json.getJSONObject("reponses");
				txtlastversion = reponses.getString("lastversion");
				mettreajour = compareVersion(txtlastversion, appversion);
				/*
				List<Integer> lastversion = GlobalVars.convertVersionNumbers(reponses.getString("lastversion"));
				List<Integer> myversion = GlobalVars.convertVersionNumbers(appversion);
				if(lastversion==null || myversion==null || lastversion.isEmpty() || myversion.isEmpty()) {
					Log.e(Constants.TAG, "Problème de conversion des versions.");
				} else {
					if (Constants.debug_mode)
						Log.i(Constants.TAG,"Version appli : "+String.valueOf(myversion)+" / dernière version selon serveur : "+lastversion.toString());
					if ((lastversion.get(0)>myversion.get(0)) || (lastversion.get(1)>myversion.get(1)) || (lastversion.get(2) > myversion.get(2)) ) {
						mettreajour=true;
						TVVersion.setText(TVVersion.getText()+" ("+getString(R.string.msg_outdated)+")");
					} else {	//up-to-date
						TVVersion.setText(TVVersion.getText()+" ("+getString(R.string.msg_up_to_date)+")");
					}
				} */
			} catch (JSONException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			} catch (NumberFormatException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			} catch (IndexOutOfBoundsException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			}
			//new version available :
			if (mettreajour) {
				TVVersion.setText(TVVersion.getText()+" ("+getString(R.string.msg_outdated)+")");
				if (Constants.debug_mode)
					Log.i(Constants.TAG,"L'application devrait être mise à jour.");
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle(R.string.label_update);
				alert.setMessage(getString(R.string.msg_new_version_available) +" (v."+txtlastversion+")\n"+ getString(R.string.msg_do_you_want_to_visite_website));
				alert.setPositiveButton(R.string.btn_Yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Intent myWebLink = new Intent(android.content.Intent.ACTION_VIEW);
			             myWebLink.setData(Uri.parse(getString(R.string.url_website_ludvin)));
			             startActivity(myWebLink);
					}
				});
				alert.setNegativeButton(R.string.btn_No, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) { }
				});
				alert.show();
			} else {
				TVVersion.setText(TVVersion.getText()+" ("+getString(R.string.msg_up_to_date)+")");
			}
		}
	}
	
	/**
	 * Vérification de la dernière version de l'application auprès du serveur.
	 * @param Message à envoyer au serveur doInBackground(JSONObject)
	 * @param Integer Valeur de progression onProgessUpdate(int)
	 * @param String Valeur de retour en fin de tâche doInBackground()
	 * @return Retourne en interne un objet JSON des réponses du serveur.
	 */
	private class sendUpdate extends AsyncTask<JSONObject, Integer, JSONObject> {
		@Override
		protected JSONObject doInBackground(JSONObject... jsonMsg) {
			if (Constants.debug_mode)
				Log.v(Constants.TAG, "sendUpdate.doInBackground()");
			String reponse = GlobalVars.postJsonServer(jsonMsg[0]);
			if (reponse != null) {
				try {
					JSONObject jreponse=new JSONObject(reponse);
					return jreponse;
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
			}
			return null;
		}
		protected void onPostExecute(JSONObject jreponse) {
			if (Constants.debug_mode)
				Log.v(Constants.TAG, "sendUpdate.onPostExecute()");
			if (jreponse != null)
				checkAppliUpdate(jreponse);
		}
	}
	
	/**
	 * API : Appel de cette méthode lors de la mise en avant de l'activité.
	 */
	@Override
	protected void onResume() {
	    super.onResume();
	}
	
	/**
	 * API : Appel de cette méthode lors de l'arrêt de l'activité.
	 */
	@Override
	protected void onStop() {
	    super.onStop();
	    tacheupdate.cancel(true);
	}
	
	/**
	 * API : Appel de cette méthode lors de la destruction de l'activité.
	 */
	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    if (tacheupdate.cancel(true))
	    	Log.i(Constants.TAG,"UIDemarrage.onDestroy() tâche sendUpdate tuée.");
	}
	
	/**
	 * API : Appel de cette méthode lors du réveil de l'activité.
	 */
	@Override
	protected void onStart() {
	    super.onStart();
	}
	
}
