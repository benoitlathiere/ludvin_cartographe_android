package org.ludvin.cartographe;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ToggleButton;

public class UIRepere extends FragmentActivity implements ActionBar.OnNavigationListener,OnItemSelectedListener, OnItemClickListener, OnClickListener {

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
	
	private SharedPreferences prefs;
	
	//UI
	ActionBar actionbar;
	private ListView LVMesures;
	private Button BtnEnrPosition;
	private ToggleButton TogWifi;
	private LinearLayout LLAttente;
	private LinearLayout LLReperesGlobal;
	private ProgressBar PBhorizontal;
	private ImageView IVRefresh;
	private ProgressBar PBRefresh;
	
	
	//listes
	private ArrayAdapter<Repere> ReperesAdapter;
	//mesures
	private ArrayAdapter<Mesure> MesAdapter;
	
	//divers
	private Toast toast;
	protected WifiManager wifi;
	private BroadcastReceiver receiver;
	private ArrayList<Borne> Bornes = new ArrayList<Borne>();
	//private DecimalFormat f = new DecimalFormat();

	//timer
	private TimerTask scanTask;
	private final Handler handler = new Handler();
	private Timer t = new Timer();
	private int nbscan=0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uirepere);
		Log.d(Constants.TAG,"UIRepere.onCreate()");
		
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		
		prefs = getSharedPreferences(Constants.PREFS_FILENAME, Constants.PREFS_MODE);
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		
		//UI
		actionbar = getActionBar();
		actionbar.show();
		actionbar.setHomeButtonEnabled(true);
		actionbar.setDisplayUseLogoEnabled(true);	//logo in manifest/activity
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setDisplayShowTitleEnabled(false);
		//actionbar.setTitle(R.string.label_level);
		//actionbar.setSubtitle(R.string.label_spots);	//doit être court
		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionbar.setDisplayShowCustomEnabled(true);
		IVRefresh = new ImageView(this);
		IVRefresh.setClickable(true);
		IVRefresh.setImageResource(R.drawable.navigation_refresh);
		IVRefresh.setOnClickListener(this);
		PBRefresh =  new ProgressBar(this);
		actionbar.setCustomView(IVRefresh);
		actionbar.getCustomView().setClickable(true);
		LVMesures = (ListView) findViewById(R.id.LVMesures);
		BtnEnrPosition = (Button) findViewById(R.id.BtnEnrPosition);
		BtnEnrPosition.setOnClickListener(this);
		TogWifi = (ToggleButton) findViewById(R.id.TogWifi);
		TogWifi.setChecked(wifi.isWifiEnabled());
		TogWifi.setOnClickListener(this);
		LLReperesGlobal = (LinearLayout) findViewById(R.id.LLReperesGlobal);
		LLAttente = (LinearLayout) findViewById(R.id.LLAttente);
		LLAttente.setVisibility(8);
		PBhorizontal = (ProgressBar) findViewById(R.id.PBhorizontal);

		// Register Broadcast Receiver
		if (receiver == null)
			receiver = new WiFiScanReceiver(this);
		registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
	    etatWifi();
	    
	    listerReperes(true);
	}
	
	/**
	 * Retourne la position de l'objet courant pour le Spinner (ActionBar). Prends en compte l'Adapter de l'objet.
	 * @return Position de l'objet courant, sinon 0.
	 */
	private int SpinPosRepere() {
		for (int i=0 ; i<ReperesAdapter.getCount(); i++) {
			if  ( ((Repere) ReperesAdapter.getItem(i)).getId()==prefs.getInt("tmp_repere_id",0) )
				return i;
		}
		return 0;
	}
	
	/**
	 * Objets dans ActionBar.
	 * @param liste Liste des objets
	 */
	private void Reperes(ArrayList<Repere> liste) {
		if (liste!=null) {
			ReperesAdapter = new ArrayAdapter<Repere>(this, android.R.layout.simple_list_item_1, liste);
			if (ReperesAdapter != null) {
				actionbar.setListNavigationCallbacks(ReperesAdapter, this);
				actionbar.setSelectedNavigationItem(SpinPosRepere());
			}  else {
				Log.e(Constants.TAG,"Problème pour lister dans Reperes()");
				Toast toast = Toast.makeText(this, R.string.msg_problem_list_spots, Toast.LENGTH_SHORT);toast.show();
			}
		} else {
			Toast toast = Toast.makeText(this, R.string.msg_problem_list_spots, Toast.LENGTH_SHORT);toast.show();
		}
	}
	
	/**
	 * Objets dans View.
	 * @param liste Liste des objets.
	 */
	private void Mesure(ArrayList<Mesure> liste) {
		LVMesures.setAdapter(null);
		if (liste!=null) { 
			MesAdapter = new ArrayAdapter<Mesure>(this, android.R.layout.simple_list_item_1, liste);
			if (MesAdapter != null) {
				LVMesures.setAdapter(MesAdapter);
				registerForContextMenu(LVMesures);
				LVMesures.setOnItemClickListener(this);
			} else {
				if (Constants.debug_mode)
					Log.e(Constants.TAG,"Problème pour lister les reperes dans UINiveau.Mesures()");
				Toast toast = Toast.makeText(this, R.string.msg_empty_list, Toast.LENGTH_SHORT);toast.show();
			}
		} else {
			Toast toast = Toast.makeText(this, R.string.msg_empty_list, Toast.LENGTH_SHORT);toast.show();
		}
	}
	
	
	/**
	 * Demande la liste des objets.
	 * @param cache Autoriser l'utilisation du cache si disponible.
	 */
	private void listerReperes(boolean cache) {
		Log.i(Constants.TAG, "UIRepere.listerReperes()");
		if (Repere.cacheRecent(this, prefs.getInt("tmp_niveau_id", 0)) && cache) {
			try {
				if (Constants.debug_mode)
					Log.i(Constants.TAG,"on essaye de récupérer le cache.");
				JSONObject json = Repere.getCacheList(this, prefs.getInt("tmp_niveau_id",0));
				Reperes(Repere.JSON2reperes(json.getJSONObject("reponses"), null ));	//FIXME remplacer null
			} catch (JSONException e) {
				e.printStackTrace();
				Toast toast = Toast.makeText(this, R.string.msg_problem_list_spots, Toast.LENGTH_LONG);toast.show();
			}
		} else if (GlobalVars.isNetworkAvailable(this)){
			Log.i(Constants.TAG, "UIRepere.listerReperes() on interroge le serveur");
			JSONObject msgjson = Repere.getJSONList(getBaseContext(), prefs.getInt("tmp_niveau_id",0));
			if (msgjson!=null) {
				try {
					new sendJSON().execute(msgjson);
				} catch (IllegalStateException e) {
					e.printStackTrace();
					Log.e(Constants.TAG, "UIRepere.listerReperes() : problème création tâche !");
					Toast toast = Toast.makeText(this, R.string.msg_problem_list_spots, Toast.LENGTH_LONG);toast.show();
				}
			} else {
				Log.e(Constants.TAG, "UIRepere.listerReperes() : json est null !");
				Toast toast = Toast.makeText(this, R.string.msg_problem_list_spots, Toast.LENGTH_LONG);toast.show();
			}
		} else {
			Toast toast = Toast.makeText(this, getString(R.string.msg_internet_unreachable_check_connection), Toast.LENGTH_LONG);toast.show();
		}
	}
	
	
	/**
	 * Demande le mise à jour de la liste des objets dans la View.
	 * @param cache Autorise l'utilisation du cache si disponible.
	 */
	private void listerMesures(boolean cache) {
		Log.i(Constants.TAG, "UIRepere.listerMesures()");
		if (Mesure.cacheRecent(this, getCurrentRepere().getId()) && cache && !Repere.needForceRefresh(this, getCurrentRepere().getId())) {	//recup cache..
			if (Constants.debug_mode)
				Log.i(Constants.TAG, "on essaye de récupérer le cache mesures");
			try {
				JSONObject json = Mesure.getCacheList(this, getCurrentRepere().getId());
				if (Constants.debug_mode)
					Log.d(Constants.TAG, "état de json du cache (rep="+getCurrentRepere().getId()+") ");
				Mesure(Mesure.JSON2mesures(this, json.getJSONObject("reponses")));
			} catch (JSONException e) {
				Log.e(Constants.TAG, "listerMesures() , erreur : ");
				e.printStackTrace();
			}
		} else if (GlobalVars.isNetworkAvailable(this)) {
			if (Constants.debug_mode)
				Log.v(Constants.TAG, "on récupère les mesures du serveur car cache="+String.valueOf(cache)+ " / needForceRefresh()="+String.valueOf(Repere.needForceRefresh(this, getCurrentRepere().getId()))+" / cacheRecent="+String.valueOf(Mesure.cacheRecent(this, getCurrentRepere().getId())));
			JSONObject msgjson = Mesure.getJSONList(getBaseContext(), getCurrentRepere().getId());
			if (Constants.debug_mode)
				Log.d(Constants.TAG, "pour le serveur msgjson="+msgjson.toString());
			Toast toast = Toast.makeText(this, R.string.msg_problem_list_measures, Toast.LENGTH_LONG);
			if (msgjson==null || msgjson.length()==0) {
				toast.show();
				if (Constants.debug_mode)
					Log.e(Constants.TAG, "UIRepere.listerMesures() msgjson= null ou vide !");
			} else {
				try {
					new sendJSON().execute(msgjson);
				} catch (IllegalStateException e) {
					e.printStackTrace();
					toast.show();
				}
			}
			
			Repere.removeRepereForceRefresh(this, getCurrentRepere().getId());
		} else  {
			Toast toast = Toast.makeText(this, getString(R.string.msg_internet_unreachable_check_connection), Toast.LENGTH_LONG);toast.show();
		}
	}
	
	
	
	
	/**
	 * Retourne le repère courant sélectionné.
	 * @return Repere
	 */
	private Repere getCurrentRepere() {
		return ReperesAdapter.getItem(actionbar.getSelectedNavigationIndex());
	}
	
	
	/**
	 * Analyse la réponse JSON du serveur et execute les actions.
	 * @param json Réponse JSON entière du serveur.
	 */
	private void analyserReponses(JSONObject json) {
		if (Constants.debug_mode)
			Log.i(Constants.TAG, "analyserReponses()");
		if (json!=null) {
			String objet="";
			String action="";
			JSONObject reponses ;
			String message = "";
			try {
				action = json.getString("action");
				objet = json.getString("objet");
				reponses = json.getJSONObject("reponses");
				message = reponses.getString("message");
				if (Constants.debug_mode)
					Log.d(Constants.TAG, "UIRepere.analyserReponses() action="+action+" / objet="+objet+" / message="+message+" / json="+json.toString());
				if (message.contains("OK")) {
					if (objet.contains("repere")) {
						if (action.contains("lister")) {
							Reperes(Repere.JSON2reperes(reponses, null));	//FIXME remplacer null
							Repere.setCacheList(this, json, getCurrentRepere().getEtage_ID());
						} else {
							listerReperes(false);
						}
					} else if (objet.contains("mesure")) {
						if (action.contains("lister")) {
							Mesure(Mesure.JSON2mesures(this, reponses));
							Mesure.setCacheList(this, json, getCurrentRepere().getId());
						} else {
							listerMesures(false);
						}
					}
				} else {
					if (Constants.debug_mode)
						Log.e(Constants.TAG, "UIRepere.analyserReponses() erreur, réponse serveur :"+ reponses.getString("erreur"));
					toast = Toast.makeText(this, R.string.msg_problem, Toast.LENGTH_LONG);toast.show();
				}
			} catch (JSONException e) {
				e.printStackTrace();
				toast = Toast.makeText(this, R.string.msg_problem, Toast.LENGTH_LONG);toast.show();
			}
		} else {
			if (Constants.debug_mode)
				Log.e(Constants.TAG, "analyserReponses() erreur json=null !");
			toast = Toast.makeText(this, R.string.msg_problem, Toast.LENGTH_LONG);toast.show();
		}
	}
	
	
	/**
	 * Appel de la méthode pour l'enregistrement de la position.
	 * @return Retourne l'état de réussite de l'enregistrement.
	 */
	private void EnregistrerPosition() {
		if (! GlobalVars.isNetworkAvailable(this)) {
			toast = Toast.makeText(this, R.string.msg_internet_unreachable_check_connection, Toast.LENGTH_SHORT);toast.show();
		} else if (! wifi.isWifiEnabled()) {
			toast = Toast.makeText(this, R.string.msg_please_activate_wifi, Toast.LENGTH_SHORT);toast.show();
			demanderActivationWifi();
		} else {
			autoscan();
		}
	}
	
	/**
	 * Suppression de mesures enregistrées.
	 * @param jour Date du jour concerné.
	 * @param auteur Auteur des mesures.
	 * @param idrepere ID du repère lié aux mesures.
	 */
	private void supprimerMesures(final String jour, final String auteur, final int idrepere) {
		if (! GlobalVars.isNetworkAvailable(this)) {
			Toast toast = Toast.makeText(this, R.string.msg_no_internet_connection, Toast.LENGTH_SHORT);toast.show();
		} else {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(R.string.label_delete_measures);
			alert.setMessage("Voulez-vous supprimer définitivement les mesures sélectionnées ?");
			alert.setPositiveButton(R.string.btn_Yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					JSONObject msgjson = new JSONObject();
					JSONObject jsoncontenu = new JSONObject();
					try {
						msgjson.put("objet", "mesure");
						msgjson.put("action", "supprimer");
						msgjson.put("user_id", GlobalVars.getUserID(getBaseContext()));
							jsoncontenu.put("jour",jour);
							jsoncontenu.put("auteur",auteur);
							jsoncontenu.put("repere_ID",idrepere);
						msgjson.put("contenu",jsoncontenu);
						try {
							new sendJSON().execute(msgjson);
						} catch (IllegalStateException e) {
							e.printStackTrace();
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			});
			alert.setNegativeButton(R.string.btn_No, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) { }
			});
			alert.show();
		}
	}
	
	/**
	 * Ouvre un popup pour demander l'activation du Wifi.
	 */
	private void demanderActivationWifi() {
		//source : http://developer.android.com/guide/topics/ui/dialogs.html#AlertDialog
		// 1. Instantiate an AlertDialog.Builder with its constructor
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		// 2. Chain together various setter methods to set the dialog characteristics
		builder.setMessage("La fonction Wifi doit être activée pour enregistrer la position. Voulez-vous l'activer ?");
		builder.setTitle(R.string.msg_wifi_required);
		//listeners sur dialog :
		builder.setPositiveButton(R.string.btn_Yes, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   changeWifi(true);
		           }
			});
		builder.setNegativeButton(R.string.btn_No, new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   //rien à faire
		           }
			});
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	
	/**
	 * Fait un scan des bornes Wifi alentours pour enregister les signaux.
	 */
	private void scanner() {
		wifi.startScan();
		List<ScanResult> scannes = wifi.getScanResults();
		for (ScanResult scanne : scannes) {
			Bornes.add( new Borne(scanne.SSID, scanne.BSSID, GlobalVars.convertFreq(scanne.frequency), scanne.level, this ) );
		}
		nbscan++;
		if (Constants.debug_mode)
			Log.d(Constants.TAG, "mesures stockées : "+String.valueOf(Bornes.size()));
		autoscan();
	}
	
	
	/**
	 * Change l'état du scan automatique des bornes Wi-Fi.
	 */
	private void autoscan() {
		if (nbscan==0) {
			PBhorizontal.setVisibility(ProgressBar.VISIBLE);
			PBhorizontal.setProgress(0);
			PBhorizontal.setMax(Constants.wifi_nb_scans+5);	//5 étapes après le scan
			LLReperesGlobal.setVisibility(8);	//on masque
			LLAttente.setVisibility(0);			//on affiche
			if (Constants.debug_mode)
				Log.d(Constants.TAG, "Début du scan automatique des mesures");
			scanTask = new TimerTask() {
		        public void run() {
		                handler.post(new Runnable() {
		                        public void run() {
		                        	PBhorizontal.setProgress(nbscan+1);
		                        	scanner();
		                        }
		               });
		        }};
		    t.schedule(scanTask, 1000, (GlobalVars.getWifiDelay()*1000));	//délai entre 2 scans de bornes
		} else if (nbscan >= GlobalVars.getWifiNbScans(this)) {
			scanTask.cancel();
			if (Constants.debug_mode)
				{toast = Toast.makeText(this, "(Fin de relevé des signaux. On envoie au serveur.)", Toast.LENGTH_SHORT); toast.show();}	//debug
			PBhorizontal.setProgress(PBhorizontal.getProgress()+1);
			JSONArray jsonbornes = Borne.parcoursBornes(Bornes);
			if (jsonbornes!=null) {
				try {
					JSONObject msgjson = new JSONObject();
					JSONObject jsoncontenu = new JSONObject();
					msgjson.put("objet", "mesure");
					msgjson.put("action", "creer");
					msgjson.put("user_id", GlobalVars.getUserID(getBaseContext()));
						jsoncontenu.put("mobile", GlobalVars.getDeviceName());
						jsoncontenu.put("bornes",jsonbornes);
						jsoncontenu.put("repere_ID", getCurrentRepere().getId());
					msgjson.put("contenu",jsoncontenu);
					PBhorizontal.setProgress(PBhorizontal.getProgress()+1);
					if (GlobalVars.isNetworkAvailable(this)) {
						try {
							new sendJSON().execute(msgjson);
						} catch (IllegalStateException e) {
							e.printStackTrace();
						}
					} else {
						//pas de connexion, on stocke les mesures pour envoi ultérieur	//FIXME à tester...
						Toast toast = Toast.makeText(this, getString(R.string.msg_no_internet_connection)+"\n"+getString(R.string.msg_measures_sent_later), Toast.LENGTH_LONG);toast.show();
						Mesure.setOfflineMesures(this, msgjson );
					}
				} catch (JSONException e) {
					e.printStackTrace();
				} catch (ClassCastException e) {
					e.printStackTrace();
				}
			} else {
				Toast toast = Toast.makeText(this, "Aucune borne WiFi détectée !", Toast.LENGTH_LONG);toast.show();
			}
			LLAttente.setVisibility(8);	//on masque la barre de progression circulaire
			LLReperesGlobal.setVisibility(0);
			nbscan=0;
		}
	}

	
	/**
	 * Envoi d'un message au serveur en tâche de fond.
	 * @param Message à envoyer au serveur doInBackground()
	 * @param Integer Valeur de progression onProgessUpdate()
	 * @param String Valeur de retour en fin de tâche doInBackground()
	 */
	private class sendJSON extends AsyncTask<JSONObject, Integer, JSONObject> {
		
		protected void onPreExecute() {
			BtnEnrPosition.setActivated(false);
			actionbar.setCustomView(PBRefresh);
			if (Constants.debug_mode)
				Log.v(Constants.TAG, "onPreExecute()");
		}
		
		@Override
		protected JSONObject doInBackground(JSONObject... jsonMsg) {
			String reponse = GlobalVars.postJsonServer(jsonMsg[0]);
			if (reponse!=null) {
				try {
					return new JSONObject(reponse);
				} catch (JSONException e1) {
					if (Constants.debug_mode)
						e1.printStackTrace();
				}
			}
			return null;
		}
		
		protected void onPostExecute(JSONObject jreponses) {
			if (Constants.debug_mode) {
				Log.i(Constants.TAG, "onPostExecute() ");
			}
			actionbar.setCustomView(IVRefresh);
			BtnEnrPosition.setActivated(true);
			LLAttente.setVisibility(8);	//on masque l'attente
			LLReperesGlobal.setVisibility(0);
			if (jreponses!=null) {
				analyserReponses(jreponses);
			} else {
				if (Constants.debug_mode) 
					Log.e(Constants.TAG, "onPostExecute() réponse du serveur null !");
			}
		}
	}
	
	
	/**
	 * Change l'état de l'antenne Wifi et du bouton Wifi on/off.
	 * @param activer Etat désiré de l'antenne Wifi.
	 */
	private void changeWifi(boolean activer) {
		if (activer) {
			wifi.setWifiEnabled(true);
			toast = Toast.makeText(this, getString(R.string.msg_enabling_wifi), Toast.LENGTH_SHORT);toast.show();
			if (Constants.debug_mode)
				Log.d(Constants.TAG,"Activation du Wifi.");
			TogWifi.setChecked(true);
		} else {
			wifi.setWifiEnabled(false);
			toast = Toast.makeText(this, getString(R.string.msg_disabling_wifi), Toast.LENGTH_SHORT);toast.show();
			if (Constants.debug_mode)
				Log.d(Constants.TAG,"Désactivation du Wifi.");
			TogWifi.setChecked(false);
		}
	}

	/**
	 * Méthode qui vérifie l'état de l'antenne Wifi pour l'état de la View Toggle.
	 */
	private void etatWifi() {
		if (wifi.isWifiEnabled()) {
			TogWifi.setChecked(true);
		} else {
			TogWifi.setChecked(false);
		}
	}
	
	/**
	 * API : évènement onClick() dans l'activité.
	 * @param View Vue cliquée.
	 */
	public void onClick(View v) {
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);	//on masque le clavier
		switch (v.getId()) {
			case (R.id.BtnEnrPosition):
				EnregistrerPosition();
				break;
			case (R.id.TogWifi):
				changeWifi(! wifi.isWifiEnabled());
			break;
		}
		if (v.getId()==IVRefresh.getId()) {
			if (Constants.debug_mode)
				Log.i(Constants.TAG,"btn refresh Reperes() depuis serveur");
			listerReperes(false);
		}
	}
	
	/**
	 * API : Option sélectionnée depuis le menu.
	 * @param item Element du menu choisi.
	 */
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
		switch (item.getItemId()){
			case Constants.MENU_LISTE_REPERES:
				listerReperes(false);
				break;
			case Constants.MENU_CLEAN_CACHE:
				GlobalVars.cleanCache(this);
				break;
			case Constants.MENU_QUIT:
				GlobalVars.quitter();
				break;
			case android.R.id.home:
				Intent myintent = new Intent(this, UINiveau.class);
				startActivity(myintent);
				finish();
				//navigateUpTo(myintent);
				break;
	        default:
	        	if (Constants.debug_mode)
	        		Log.d(Constants.TAG,"ERREUR : onOptionsItemSelected() n'a pas de valeur pour item="+String.valueOf(item.getItemId()));
	        	break;
		}
		return true;
    }
	


	/**
	 * API : menu de l'activité.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.uirepere, menu);
		menu.add(0, Constants.MENU_CLEAN_CACHE, 0, R.string.label_clean_cache);
		menu.add(0, Constants.MENU_QUIT, 0, R.string.label_quit);
		return true;
	}
	
	/**
	 * API : Création du menu contextuel sur item de ListView (voir registerForContextMenu()).
	 * @param menu Menu en cours de construction.
	 * @param v Vue pour laquelle le menu est construit.
	 * @param menuInfo
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		switch (v.getId()) {
			case (R.id.LVPositions):
				menu.setHeaderTitle (R.string.label_locations);
			    menu.add(0, Constants.MENU_SUPPRIMER_POSITION, 0, R.string.label_delete);
			    break;
			case (R.id.LVMesures):
				Mesure mestmp = (Mesure) LVMesures.getItemAtPosition(info.position);
				menu.setHeaderTitle(getString(R.string.label_measures)+" "+mestmp.getJourEurope());
				if (Constants.debug_mode)
					Log.d(Constants.TAG, mestmp.getEmail().trim()+" / "+prefs.getString("user_login", "").trim());
				if (mestmp.getIdCompte() == GlobalVars.getUserID(getBaseContext())) {	//seul le cartographe supprime ses mesures
					//astuce: on met la position de l'élément sélectionné en GroupId
					menu.add(info.position, Constants.MENU_SUPPRIMER_POSITION, Constants.MENU_SUPPRIMER_POSITION, R.string.label_delete);
				}
				break;
		}
	}
	
	/**
	 * API : Evènement sur item du menu contextuel
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		//astuce : item.getGroupId() : Id  de l'objet passé par onCreateContextMenu
		switch (item.getItemId()) {
			case (Constants.MENU_SUPPRIMER_POSITION):
				Mesure tmpmes= (Mesure) LVMesures.getItemAtPosition(item.getGroupId());
				supprimerMesures( tmpmes.getJour(), tmpmes.getEmail(), tmpmes.getIdRepere() );
				break;
			default:
				if (Constants.debug_mode)
					{Toast toast = Toast.makeText(this, "Elément inconnu du menu contextuel: "+item.getTitle(), Toast.LENGTH_SHORT);toast.show();}
		}
		return true;
	}
	
	/**
	 * API : Mise en avant de l'activité.
	 */
	@Override
	protected void onResume() {
	    super.onResume();
	    Log.i(Constants.TAG,"IURepere.onResume()");
	    etatWifi();
	    Mesure.checkOfflineMesures(this);
	}
	
	/**
	 * API : Réveil de l'activité.
	 */
	@Override
	protected void onStart() {
	    super.onStart();
	    Log.i(Constants.TAG,"IURepere.onStart()");
	}
	
	/**
	 * API : Mise en arrière plan de l'activité.
	 */
	@Override
	protected void onPause() {
	    super.onPause();
	    Log.i(Constants.TAG,"IURepere.onPause()");
	    try {
	    	unregisterReceiver(receiver);
	    } catch (IllegalArgumentException e) {
	    	if (Constants.debug_mode)
	    		e.printStackTrace();
	    }
	}
	
	/**
	 * API : Appel de cette méthode lors de la suppression l'activité en mémoire.
	 */
	@Override
	protected void onStop() {
	    super.onStop();
	    Log.i(Constants.TAG,"IURepere.onStop()");
	}

	
	/**
	 * API : lors d'un clic sur un item du spinner
	 */
	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
	}

	
	/**
	 * API : au changement de la liste navigation (ActionBar)
	 */
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		// When the given dropdown item is selected, show its contents in the container view.
		Fragment fragment = new DummySectionFragment();
		Bundle args = new Bundle();
		args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, itemPosition + 1);
		fragment.setArguments(args);
		getSupportFragmentManager().beginTransaction().replace(R.id.containeruirepere, fragment).commit();
		if (Constants.debug_mode)
			Log.d(Constants.TAG,"onNavigationItemSelected() : "+itemId +" et Repere.id="+getCurrentRepere().getId());
		listerMesures(true);
		return true;
	}
	
	/**
	 * A dummy fragment representing a section of the app, but that simply displays dummy text.
	 */
	public static class DummySectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";
		public DummySectionFragment() { }
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_uirepere_dummy, container, false);
			return rootView;
		}
	}
	
	/**
	 * Backward-compatible version of {@link ActionBar#getThemedContext()} that
	 * simply returns the {@link android.app.Activity} if
	 * <code>getThemedContext</code> is unavailable.
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private Context getActionBarThemedContextCompat() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			return getActionBar().getThemedContext();
		} else {
			return this;
		}
	}
	
	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
			getActionBar().setSelectedNavigationItem(
				savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar().getSelectedNavigationIndex());
	}
	
}
