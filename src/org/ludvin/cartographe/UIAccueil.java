package org.ludvin.cartographe;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public  class UIAccueil extends Activity implements OnClickListener, OnItemClickListener, OnItemSelectedListener, OnItemLongClickListener{

	//global
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;
	
	//UI
	private ActionBar actionbar;
	//private EditText ETNouvBat;
	private Button btnCreerNouvBat;
	private ListView LVBatiments;
	private ImageView IVRefresh;
	private ImageView IVBuildPlus;
	private ProgressBar PBRefresh;

	//bâtiments 
	private ArrayAdapter<Batiment> BatimentsAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uiaccueil);
		
	  	prefs = getSharedPreferences(Constants.PREFS_FILENAME, MODE_PRIVATE);
		
		//UI
		actionbar = getActionBar();
		actionbar.show();
		actionbar.setHomeButtonEnabled(false);
		actionbar.setDisplayUseLogoEnabled(false);
		actionbar.setDisplayHomeAsUpEnabled(false);
		actionbar.setDisplayShowTitleEnabled(true);
		actionbar.setTitle(R.string.title_activity_home);
		actionbar.setSubtitle(R.string.label_buildings_management);
		//actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);	//inutile ici
		actionbar.setDisplayShowCustomEnabled(true);
		IVRefresh = new ImageView(this);
		IVRefresh.setClickable(true);
		IVRefresh.setImageResource(R.drawable.navigation_refresh);
		IVRefresh.setOnClickListener(this);
		actionbar.setCustomView(IVRefresh);
		actionbar.getCustomView().setClickable(true);
		PBRefresh =  new ProgressBar(this);
		//ETNouvBat = (EditText) findViewById(R.id.ETNouvBat);
		btnCreerNouvBat = (Button) findViewById(R.id.btnCreerNouvBat);
		btnCreerNouvBat.setOnClickListener(this);
		LVBatiments = (ListView) findViewById(R.id.LVBatiments);
		IVBuildPlus = (ImageView) findViewById(R.id.IVBuildPlus);
		IVBuildPlus.setClickable(true);
		IVBuildPlus.setOnClickListener(this);

		listerBatiments(true);
	}

	/**
	 * Liste les bâtiments reçus dans la View.
	 * @param BatimentsAdapter
	 */
	public void Batiments(ArrayList<Batiment> liste) {
		LVBatiments.setAdapter(null);
		if (liste!=null) {
			Log.v(Constants.TAG,"listerBatiments()");
			BatimentsAdapter = new ArrayAdapter<Batiment>(this, android.R.layout.simple_list_item_1, liste);
			if (BatimentsAdapter != null) {
				LVBatiments.setAdapter(BatimentsAdapter);
				registerForContextMenu(LVBatiments);
				LVBatiments.setOnItemClickListener(this);
			}  else {
				Log.e(Constants.TAG,"Problème pour lister les bâtiments dans Batiments()");
				Toast toast = Toast.makeText(this, R.string.msg_problem_list_buildings, Toast.LENGTH_LONG);toast.show();
			}
		} else {
			Toast toast = Toast.makeText(this, R.string.msg_empty_list, Toast.LENGTH_SHORT);toast.show();
		}
	}
	
	/**
	 * Demande la liste des bâtiments.
	 * @param cache Autoriser l'utilisation du cache si disponible.
	 */
	private void listerBatiments(boolean cache) {
		if (Batiment.cacheRecent(this) && cache) {
			JSONObject json = Batiment.getCacheList(this);
			try {
				if (Constants.debug_mode)
					Log.i(Constants.TAG,"on essaye de récupérer le cache.");
				Batiments(Batiment.JSON2batiments(json.getJSONObject("reponses")));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else if (GlobalVars.isNetworkAvailable(this)){
			JSONObject msgjson = Batiment.getJSONList(this);
			if (msgjson!=null)
				new sendJSON().execute(msgjson);
			else {
				Toast toast = Toast.makeText(this, getString(R.string.msg_problem_list_buildings)+" "+getString(R.string.msg_internet_unreachable_check_connection), Toast.LENGTH_SHORT);toast.show();
			}
		} else {
			Toast toast = Toast.makeText(this, getString(R.string.msg_problem_list_buildings)+" "+getString(R.string.msg_internet_unreachable_check_connection), Toast.LENGTH_SHORT);toast.show();
		}
	}

	
	/**
	 * Création d'un nouveau bâtiment et son rez-de-chaussée sur le serveur
	 */
	private void creerBatiment() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); //on masque le clavier
		
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.label_new_building).setMessage(R.string.label_new_name);
		final EditText input = new EditText(this);
		alert.setView(input);
		alert.setPositiveButton(R.string.btn_OK, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String nouveau = input.getText().toString().replace('"',' ').trim();
				InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);	//on masque le clavier
				if (nouveau.isEmpty()) {
					Toast toast = Toast.makeText(getBaseContext(), R.string.msg_please_give_name, Toast.LENGTH_SHORT);toast.show();
				} else {
					getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);	//masque le clavier virtuel
					JSONObject msgjson = Batiment.getJSONCreate(getBaseContext(), nouveau);
					if (msgjson!=null) {
						try {
							new sendJSON().execute(msgjson);
						} catch (IllegalStateException e) {
							e.printStackTrace();
						}
					} else {
						Toast toast = Toast.makeText(getBaseContext(), R.string.msg_problem_create_building, Toast.LENGTH_SHORT);toast.show();
					}
				}
			}
		});
		alert.setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) { }
		});
		alert.show();
	}

	/**
	 * Suppression d'un bâtiment.
	 * @param nom Nom du bâtiment à supprimer.
	 * @param id Identifiant du bâtiment.
	 */
	private void supprimerBatiment(final String nom, final int id) {
		if (id!=0) {
			if (! GlobalVars.isNetworkAvailable(this)) {
				Toast toast = Toast.makeText(this, R.string.msg_internet_unreachable_check_connection, Toast.LENGTH_LONG);toast.show();
			} else {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle(getString(R.string.label_delete)+" "+ nom);
				alert.setMessage("Voulez-vous supprimer définitivement le bâtiment et tous les éléments liés ?");	//TODO à traduire
				alert.setPositiveButton(R.string.btn_Yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						JSONObject msgjson = Batiment.getJSONDelete(getBaseContext(), id);
						if (msgjson!=null) {
							try {
								new sendJSON().execute(msgjson);
							} catch (IllegalStateException e) {
								e.printStackTrace();
							}
						} else {
							Toast toast = Toast.makeText(getBaseContext(), R.string.msg_problem_delete_building, Toast.LENGTH_SHORT);toast.show();
						}
					}
				});
				alert.setNegativeButton(R.string.btn_No, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) { }
				});
				alert.show();
			}
		}
	}
	
	/**
	 * Renommage d'un bâtiment avec boîte de dia.logue.
	 * @param ancien Nom actuel du bâtiment à renommer.
	 * @param id ID du bâtiment.
	 */
	private void renommerBatiment(final String ancien, final int id) {
		if (! GlobalVars.isNetworkAvailable(this)) {
			Toast toast = Toast.makeText(this, R.string.msg_internet_unreachable_check_connection, Toast.LENGTH_SHORT);toast.show();
		} else {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(getString(R.string.label_rename)+" "+ancien);
			alert.setMessage("Nouveau nom :");
			final EditText input = new EditText(this);
			input.setText(ancien);
			alert.setView(input);
			alert.setPositiveButton(R.string.btn_OK, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String nouveau = input.getText().toString().trim();
					InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
					inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);	//on masque le clavier
					if (nouveau.isEmpty()) {
						Toast toast = Toast.makeText(getBaseContext(), R.string.msg_please_give_a_name, Toast.LENGTH_SHORT);toast.show();
					} else if (nouveau==ancien) {
						Toast toast = Toast.makeText(getBaseContext(), R.string.msg_new_name_must_be_different, Toast.LENGTH_SHORT);toast.show();
					} else {
						getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);	//masque le clavier virtuel
						JSONObject msgjson = Batiment.getJSONRename(getBaseContext(), id, ancien, nouveau);
						if (msgjson!=null) {
							try {
								new sendJSON().execute(msgjson);
							} catch (IllegalStateException e) {
								e.printStackTrace();
							}
						} else {
							Toast toast = Toast.makeText(getBaseContext(), R.string.msg_problem_rename_building, Toast.LENGTH_SHORT);toast.show();
						}
					}
				}
			});
			alert.setNegativeButton(R.string.btn_Cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); //on masque le clavier
				}
			});
			alert.show();
		}
	}
	
	
	/**
	 * Envoi d'un message au serveur en tâche de fond.
	 * @param Message à envoyer au serveur <code>doInBackground()</code>.
	 * @param Integer Valeur de progression <code>onProgessUpdate()</code>.
	 * @param String Valeur de retour en fin de tâche <code>doInBackground()</code>.
	 */
	private class sendJSON extends AsyncTask<JSONObject, Integer, JSONObject> {
		
		protected void onPreExecute() {
			actionbar.setCustomView(PBRefresh);
		}
		
		@Override
		protected JSONObject doInBackground(JSONObject... jsonMsg) {
			String reponse = GlobalVars.postJsonServer(jsonMsg[0]);
			try {
				return new JSONObject(reponse);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			return null;
		}
		
		protected void onPostExecute(JSONObject jreponses) {
			analyserReponses(jreponses);
			actionbar.setCustomView(IVRefresh);
		}
	}
	
	
	
	/**
	 * Analyse la réponse JSON du serveur et execute les actions.
	 * @param json Réponse JSON entière du serveur.
	 */
	private void analyserReponses(JSONObject json) {
		String objet="";
		String action="";
		JSONObject reponses ;
		String message = "";
		try {
			action = json.getString("action");
			objet = json.getString("objet");
			reponses = json.getJSONObject("reponses");
				message = reponses.getString("message");
			if (! message.contains("erreur")) {
				if (objet.contains("batiment")) {
					if (action.contains("lister")) {
						Batiments(Batiment.JSON2batiments(reponses));
						Batiment.setCacheList(this, json);	//mise en cache
					} else {
						listerBatiments(false);
					}
				}
			} else if (Constants.debug_mode) {
				Log.e(Constants.TAG, "analyserReponses() ERREUR : "+reponses.getString("erreur"));
			}
		} catch (JSONException e) {
			if (Constants.debug_mode)
				e.printStackTrace();
		}
	}
	
	
	/**
	 * API : évènement onClick() dans l'activité
	 * @param	View
	 */
	public void onClick(View v) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); //on masque le clavier
		switch (v.getId()) {
			case (R.id.btnCreerNouvBat):
				creerBatiment();
				break;
			case (R.id.IVBuildPlus):
				creerBatiment();
				break;
		}
		if (v.getId()==IVRefresh.getId()) {
			if (Constants.debug_mode)
				Log.d(Constants.TAG,"on refresh Batiments()");
			listerBatiments(false);
		}
	}
	
	/**
	 * API : Création du menu de l'activité
	 * @param menu	Menu en cours de création
	 * @return Booléan
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, Constants.MENU_CLEAN_CACHE, 0, R.string.label_clean_cache);
		menu.add(0, Constants.MENU_QUIT, 0, R.string.label_quit);
		return true;
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}

	@Override
	public void onItemSelected(AdapterView<?> adapter, View v, int pos, long id) {
		//Toast toast = Toast.makeText(this, "selected : "+adapter.getItemAtPosition(pos), Toast.LENGTH_SHORT);toast.show();//test
	}

	
	/**
	 * API : Evènement lors d'un clic sur un élément de ListView
	 */
	@Override
	public void onItemClick(AdapterView<?> adapter, View v, int pos, long id) {
		editor = prefs.edit();
		editor.putInt("tmp_batiment_id", ((Batiment) adapter.getItemAtPosition(pos)).getId());
		editor.putString("tmp_batiment_nom", ((Batiment) adapter.getItemAtPosition(pos)).getNom());
		editor.apply();
		Intent myIntent = new Intent(this, UIBatiment.class);
		startActivity(myIntent);
		//finish();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapter, View v, int pos, long id) {
		// inutile car registerForContextMenu() et onCreateContextMenu()
		return true;
	}

	/**
	 * API : Création du menu contextuel sur item de ListView (voir registerForContextMenu())
	 * @param menu Menu en cours de construction
	 * @param v Vue pour laquelle le menu est construit
	 * @param menuInfo
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		Batiment idd= (Batiment) LVBatiments.getItemAtPosition(info.position);
		editor = prefs.edit();
		editor.putString("tmp_batiment_nom", idd.getNom());
		editor.apply();
		//astuce: on met le Nom dans le titre et le Id en GroupId du menu
		if (v.getId()==R.id.LVBatiments) {
			menu.setHeaderTitle (prefs.getString("tmp_batiment_nom",""));
		    menu.add(idd.getId(), Constants.MENU_RENAME, 0, "Renommer");
		    menu.add(idd.getId(), Constants.MENU_REMOVE_BUILDING, 0, "Supprimer");
		}
	}

	/**
	 * API : Evènement sur item du menu contextuel
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		//astuce : item.getGroupId() : Id du bâtiment passé par onCreateContextMenu
		switch (item.getItemId()) {
			case Constants.MENU_REMOVE_BUILDING:
				supprimerBatiment(prefs.getString("tmp_batiment_nom",""), item.getGroupId());
				break;
			case Constants.MENU_RENAME:
				renommerBatiment(prefs.getString("tmp_batiment_nom",""), item.getGroupId());
				break;
			default:
				if (Constants.debug_mode)
	        		Log.e(Constants.TAG,"ERREUR : onContextItemSelected() n'a pas de valeur pour item="+String.valueOf(item.getItemId()));
		}
		return true;
	}
	
	/**
	 * API : Option sélectionnée depuis le menu
	 * @param item Element du menu choisi
	 */
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
		switch (item.getItemId()){
			case Constants.MENU_PARAMETRES:
				Intent myIntent = new Intent(this, UIParametres.class);
				startActivity(myIntent);
				finish();
				break;
			case Constants.MENU_CLEAN_CACHE:
				GlobalVars.cleanCache(this);
				break;
	        case Constants.MENU_QUIT:
				finish();
				break;
			default:
	        	if (Constants.debug_mode)
	        		Log.e(Constants.TAG,"ERREUR : onOptionsItemSelected() n'a pas de valeur pour item="+String.valueOf(item.getItemId()));
	        	break;
		}
		return true;
    }
	
	
	/**
	 * API : Appel de cette méthode lors de la mise en avant de l'activité.
	 */
	@Override
	protected void onResume() {
	    super.onResume();
	    Log.d(Constants.TAG,"UIAccueil.onResume()");
	    Mesure.checkOfflineMesures(this);
	    /*	//fonctionne :
	     //OBSOLETE
	    if (Mesure.hasOfflineMesures(this) && GlobalVars.isNetworkAvailable(this)) {
		    AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Mesures en mémoire.");
			alert.setMessage("Voulez-vous envoyer les mesures en cache ?");
			alert.setPositiveButton(R.string.btn_Yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Mesure.sendOfflineMesures(UIAccueil.this);
				}
			});
			alert.setNegativeButton(R.string.btn_No, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) { }
			});
			alert.show();	
	    }*/
	}
	
	/**
	 * API : Appel de cette méthode lors du réveil de l'activité.
	 */
	@Override
	protected void onStart() {
	    super.onStart();
	    Log.d(Constants.TAG,"UIAccueil.onStart()");
	}
	
}
