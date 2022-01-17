package org.ludvin.cartographe;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class UIBatiment extends FragmentActivity implements ActionBar.OnNavigationListener, OnClickListener, OnItemClickListener, OnItemSelectedListener {

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
	
	//global
	//public static GlobalVars globalvars ;
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;
	
	//UI
	private ActionBar actionbar;
	private ListView LVNiveaux;
	private EditText ETNiveau;
	private Button btnCreerEtage;
	private ImageView IVRefresh;
	private ProgressBar PBRefresh;
	
	private ArrayAdapter<Niveau> NivAdapter;
	private ArrayAdapter<Batiment> BatimentsAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uibatiment);

		if (Constants.debug_mode)
	    	Log.d(Constants.TAG,"IUBatiment.onResume()");
		
		//globalvars = ((GlobalVars) getApplication());	//variables globales
		prefs = getSharedPreferences(Constants.PREFS_FILENAME, MODE_PRIVATE);

		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);
		
		//UI
		actionbar = getActionBar();
		actionbar.show();
		actionbar.setHomeButtonEnabled(true);
		actionbar.setDisplayUseLogoEnabled(true);	//logo in manifest/activity
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setDisplayShowTitleEnabled(false);
		//actionbar.setTitle(R.string.label_building);
		//actionbar.setSubtitle(R.string.label_levels);
		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionbar.setDisplayShowCustomEnabled(true);
		IVRefresh = new ImageView(this);
		IVRefresh.setClickable(true);
		IVRefresh.setImageResource(R.drawable.navigation_refresh);
		IVRefresh.setOnClickListener(this);
		PBRefresh =  new ProgressBar(this);
		actionbar.setCustomView(IVRefresh);
		actionbar.getCustomView().setClickable(true);
		LVNiveaux = (ListView) findViewById(R.id.LVNiveaux);
		ETNiveau = (EditText) findViewById(R.id.ETNiveau);
		btnCreerEtage = (Button) findViewById(R.id.btnCreerNiveau);
		btnCreerEtage.setOnClickListener(this);
		
		listerBatiments(true);
	}
	
	
	/**
	 * Retourne la position de l'objet courant pour le Spinner (ActionBar). Prends en compte l'Adapter de l'objet.	
	 * @return Position de l'objet courant, sinon 0
	 */ 
	private int SpinPosBatiment() {
		for (int i=0 ; i<BatimentsAdapter.getCount(); i++) {
			if  ( ((Batiment) BatimentsAdapter.getItem(i)).getId()==prefs.getInt("tmp_batiment_id",0) ) {
				return i;
			}
		}
		if (Constants.debug_mode)
			Log.e(Constants.TAG,"ERREUR : Problème UIBatiment.SpinPosBatiment() - retourne 0 !");	//debug
		return 0;
	}

	
	/**
	 * Objets dans ActionBar.
	 * @param listeBatiments Liste des bâtiments
	 */
	private void Batiments(ArrayList<Batiment> liste) {
		if (Constants.debug_mode)
			Log.i(Constants.TAG,"UIBatiments.Batiments()");
		if (liste!=null) {
			BatimentsAdapter = new ArrayAdapter<Batiment>(this, android.R.layout.simple_list_item_1, liste);
			if (BatimentsAdapter != null) {
				actionbar.setListNavigationCallbacks(BatimentsAdapter, this);
				actionbar.setSelectedNavigationItem(SpinPosBatiment());
			}  else {
				Log.e(Constants.TAG,"Problème pour lister les bâtiments dans Batiments()");
				Toast toast = Toast.makeText(this, R.string.msg_problem_list_buildings, Toast.LENGTH_LONG);toast.show();
			}
		} else {
			Log.e(Constants.TAG,"Problème 2 pour lister dans Batiments()");
			Toast toast = Toast.makeText(this, R.string.msg_problem_list_buildings, Toast.LENGTH_LONG);toast.show();
		}
	}

	
	/**
	 * Niveaux dans View.
	 * @param liste Liste des objets.
	 */
	private void Niveaux(ArrayList<Niveau> liste) {
		if (Constants.debug_mode)
			Log.i(Constants.TAG, "UIBatiment.Niveaux()");
		LVNiveaux.setAdapter(null);
		if (liste!=null) {
			NivAdapter = new ArrayAdapter<Niveau>(this, android.R.layout.simple_list_item_1, liste);
			if (NivAdapter != null) {
				LVNiveaux.setAdapter(NivAdapter);
				registerForContextMenu(LVNiveaux);
				LVNiveaux.setOnItemClickListener(this);
			} else {
				if (Constants.debug_mode)
					Log.e(Constants.TAG,"Problème pour lister les niveaux dans UIBatiment.Niveaux()");
				Toast toast = Toast.makeText(this, R.string.msg_problem_list_levels, Toast.LENGTH_LONG);toast.show();
			}
		} else {
			Toast toast = Toast.makeText(this, R.string.msg_empty_list, Toast.LENGTH_SHORT);toast.show();
		}
	}
	
	/**
	 * Demande la liste des objets.
	 * @param cache Autoriser l'utilisation du cache si disponible.
	 */
	private void listerBatiments(boolean cache) {
		if (Batiment.cacheRecent(this) && cache) {
			JSONObject json = Batiment.getCacheList(this);
			try {
				Batiments(Batiment.JSON2batiments(json.getJSONObject("reponses")));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		} else if (GlobalVars.isNetworkAvailable(this)){
			JSONObject msgjson = Batiment.getJSONList(this);
			if (msgjson!=null) {
				new sendJSON().execute(msgjson);
			} else {
				Toast toast = Toast.makeText(this, R.string.msg_problem_list_buildings, Toast.LENGTH_LONG);toast.show();
			}
		} else {
			Toast toast = Toast.makeText(this, getString(R.string.msg_internet_unreachable_check_connection), Toast.LENGTH_LONG); toast.show();
		}
	}
	
	/**
	 * Demande la liste des objets.
	 * @param cache Autoriser l'utilisation du cache si disponible.
	 */
	private void listerNiveaux(boolean cache) {
		if (Niveau.cacheRecent(this, getCurrentBatiment().getId()) && cache) {
			try {
				JSONObject json = Niveau.getCacheList(this, getCurrentBatiment().getId());
				Niveaux(Niveau.JSON2niveaux(json.getJSONObject("reponses"),getCurrentBatiment()));
			} catch (JSONException e) {
					Log.e(Constants.TAG, "UIBatiment.listerNiveaux() , erreur : ");
					e.printStackTrace();
			}
		}  else if (! GlobalVars.isNetworkAvailable(this)) {
			Toast toast = Toast.makeText(this, R.string.msg_internet_unreachable_check_connection, Toast.LENGTH_LONG);toast.show();
		} else {
			try {
				JSONObject msgjson = Niveau.getJSONList(this, getCurrentBatiment().getId());
				if (msgjson!=null) {
					new sendJSON().execute(msgjson);
				} else {
					Toast toast = Toast.makeText(this, R.string.msg_problem_list_levels, Toast.LENGTH_SHORT);toast.show();
				}
			} catch (IllegalStateException e) {
				Log.e(Constants.TAG, "UIBatiment.listerNiveaux() , erreur : ");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Création d'un niveau.
	 */
	private void creerNiveau() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); //on masque le clavier
		if (! GlobalVars.isNetworkAvailable(this)) {
			Toast toast = Toast.makeText(this, R.string.msg_no_internet_connection, Toast.LENGTH_LONG);toast.show();
		} else {
			String Nom = ETNiveau.getText().toString().trim();
			if (Nom.isEmpty()) {
				Toast toast = Toast.makeText(this, R.string.msg_please_give_a_name, Toast.LENGTH_LONG);toast.show();
				ETNiveau.setError(getString(R.string.msg_please_give_a_name));
			} else {
				ETNiveau.setText("");
				JSONObject msgjson = Niveau.getJSONCreate(this, Nom, getCurrentBatiment().getId());
				if (msgjson!=null) {
					try {
						new sendJSON().execute(msgjson);
					} catch (IllegalStateException e) {
						e.printStackTrace();
					}
				} else {
					Toast toast = Toast.makeText(this, R.string.msg_problem_create_level, Toast.LENGTH_SHORT);toast.show();
				}
				
			}
		}
	}
	
	
	/**
	 * Renommage d'un bâtiment avec boîte de dialogue.
	 * @param ancien Nom du bâtiment à renommer.
	 * @param id ID deu niveau à renommer
	 */
	private void renommerNiveau(final String ancien, final int id) {
		if (! GlobalVars.isNetworkAvailable(this)) {
			Toast toast = Toast.makeText(this, R.string.msg_no_internet_connection, Toast.LENGTH_LONG);toast.show();
		} else {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(getString(R.string.label_rename)+" "+ancien);
			alert.setMessage(R.string.label_new_name);
			final EditText input = new EditText(this);	// Set an EditText view to get user input
			input.setText(ancien);
			alert.setView(input);
			alert.setPositiveButton(R.string.btn_OK, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); //on masque le clavier
					String nouveau = input.getText().toString().trim();
					if (nouveau.isEmpty()) {
						Toast toast = Toast.makeText(UIBatiment.this, R.string.msg_please_give_a_name, Toast.LENGTH_SHORT);toast.show();
					} else if (nouveau==ancien) {
						Toast toast = Toast.makeText(getBaseContext(), R.string.msg_new_name_must_be_different, Toast.LENGTH_SHORT);toast.show();
					} else {
						JSONObject msgjson = Niveau.getJSONRename(getBaseContext(), id, ancien, nouveau);
						/*JSONObject msgjson = new JSONObject();
						JSONObject jsoncontenu = new JSONObject();
						msgjson.put("objet", "niveau");
						msgjson.put("action", "renommer");
						msgjson.put("user_id", prefs.getInt("user_id", 0));
							jsoncontenu.put("ID",id);
							jsoncontenu.put("nom",ancien);
							jsoncontenu.put("nouveau",nouveau.replace('"', ' '));
						msgjson.put("contenu",jsoncontenu);*/
						if (msgjson!=null) {
							try {
								new sendJSON().execute(msgjson);
							} catch (IllegalStateException e) {
								e.printStackTrace();
								Toast toast = Toast.makeText(getBaseContext(), R.string.msg_problem_rename_level, Toast.LENGTH_SHORT);toast.show();
							}
						} else {
							Toast toast = Toast.makeText(getBaseContext(), R.string.msg_problem_rename_level, Toast.LENGTH_SHORT);toast.show();
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
	 * Suppression d'un niveau et son contenu.
	 * @param nom Nom du niveau à supprimer (facultatif).
	 * @param id Identifiant du niveau.
	 */
	private void supprimerNiveau(final String nom, final int id) {
		if (id==0) {
			if (Constants.debug_mode)
				Log.e(Constants.TAG, "UIBatiment.SupprimerNiveau() : erreur valeurs");
			Toast toast = Toast.makeText(this, R.string.msg_problem_delete_level, Toast.LENGTH_LONG);toast.show();
		} else {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(getString(R.string.label_delete)+" "+nom.trim());
			alert.setMessage("Voulez-vous supprimer définitivement le niveau et tous les éléments liés ?");
			alert.setPositiveButton(R.string.btn_Yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					if (id !=0) {
						JSONObject msgjson = Niveau.getJSONDelete(getBaseContext(), id);
						if (msgjson!=null) {
							try {
								new sendJSON().execute(msgjson);
							} catch (IllegalStateException e) {
								e.printStackTrace();
								Toast toast = Toast.makeText(getBaseContext(), R.string.msg_problem_delete_level, Toast.LENGTH_LONG);toast.show();
							}
						} else {
							Toast toast = Toast.makeText(getBaseContext(), R.string.msg_problem_delete_level, Toast.LENGTH_LONG);toast.show();
						}
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
	 * Retourne le bâtiment courant.
	 * @return Objet Batiment
	 */
	private Batiment getCurrentBatiment() {
		return BatimentsAdapter.getItem(actionbar.getSelectedNavigationIndex());
	}
	
	
	/**
	 * Analyse la réponse JSON du serveur et execute les actions.
	 * @param json Réponse JSON entière du serveur.
	 */
	private void analyserReponses(JSONObject json) {
		Log.v(Constants.TAG, "analyserReponses()");
		String objet="";
		String action="";
		JSONObject reponses ;
		String message = "";
		try {
			action = json.getString("action");
			objet = json.getString("objet");
			reponses = json.getJSONObject("reponses");
				message = reponses.getString("message");
			Log.v(Constants.TAG, "reponses: "+reponses.toString());	//debug
			if (message.contains("OK")) {
				if (objet.contains("batiment")) {
					if (action.contains("lister")) {
						Batiments(Batiment.JSON2batiments(reponses));
						Batiment.setCacheList(this, json);	//mise en cache
					} else {
						listerBatiments(false);
					}
				} else if (objet.contains("niveau")) {
					if (action.contains("lister")) {
						Niveaux(Niveau.JSON2niveaux(reponses, getCurrentBatiment()));
						Niveau.setCacheList(this, json, getCurrentBatiment().getId());
					} else {
						listerNiveaux(false);
					}
				}
			} else {
				Log.e(Constants.TAG, "y'a une erreur : "+reponses.getString("erreur"));
				Toast toast = Toast.makeText(getBaseContext(), R.string.msg_problem, Toast.LENGTH_SHORT);toast.show();
			}
		} catch (JSONException e) {
			e.printStackTrace();
			Toast toast = Toast.makeText(getBaseContext(), R.string.msg_problem, Toast.LENGTH_SHORT);toast.show();
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
			actionbar.setCustomView(PBRefresh);
			Log.d(Constants.TAG, "onPreExecute()");
		}
		
		@Override
		protected JSONObject doInBackground(JSONObject... jsonMsg) {
			Log.d(Constants.TAG, "doInBackground():"+jsonMsg[0].toString());
			String reponse = GlobalVars.postJsonServer(jsonMsg[0]);
			try {
				return new JSONObject(reponse);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			return null;
		}

		protected void onPostExecute(JSONObject jreponses) {
			if (jreponses!=null) {
				if (Constants.debug_mode) {
					Log.d(Constants.TAG, "onPostExecute() ");
					Log.d(Constants.TAG,jreponses.toString());
				}
				analyserReponses(jreponses);
				actionbar.setCustomView(IVRefresh);
			}
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
	

	/**
	 * API : Création du menu de l'activité.
	 * @param menu	Menu en cours de création.
	 * @return Booléan.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, Constants.MENU_CLEAN_CACHE, 0, R.string.label_clean_cache);
		//menu.add(0, Constants.MENU_PARAMETRES, 0, "Paramètres");	//TODO traduire
		return true;
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
		getSupportFragmentManager().beginTransaction().replace(R.id.containeruibatiment, fragment).commit();
		editor = prefs.edit();
		editor.putInt("tmp_batiment_id", getCurrentBatiment().getId());
		editor.putString("tmp_batiment_nom", getCurrentBatiment().getNom());
		editor.apply();
		listerNiveaux(true);
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
			View rootView = inflater.inflate(R.layout.fragment_uibatiment_dummy, container, false);
			return rootView;
		}
	}

	
	/**
	 * API : Evènement lors d'un clic sur un élément de ListView
	 */
	@Override
	public void onItemClick(AdapterView<?> adapter, View v, int pos, long id) {
		Intent myIntent = new Intent(this, UINiveau.class);
		editor = prefs.edit();
		editor.putInt("tmp_batiment_id", getCurrentBatiment().getId());
		Log.d(Constants.TAG, "MON ID : "+String.valueOf(getCurrentBatiment().getId()));
		editor.putInt("tmp_niveau_id", ((Niveau) adapter.getItemAtPosition(pos)).getId());
		editor.putString("tmp_niveau_nom", ((Niveau) adapter.getItemAtPosition(pos)).getNom());
		editor.apply();
		startActivity(myIntent);
	}
	
	
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
		Niveau idd= (Niveau) LVNiveaux.getItemAtPosition(info.position);
		editor = prefs.edit();
		editor.putString("tmp_niveau_nom", idd.getNom());
		editor.apply();
		//astuce: on met le nom dans le titre et le Id en GroupId du menu
		if (v.getId()==R.id.LVNiveaux) {
			menu.setHeaderTitle (idd.getNom());
		    menu.add(idd.getId(), Constants.MENU_RENAME, 0, R.string.label_rename);
		    menu.add(idd.getId(), Constants.MENU_SUPPRIMER_NIVEAU, 0, R.string.label_delete);
		} else {
			if (Constants.debug_mode)
				Log.e(Constants.TAG,"onCreateContextMenu() n'a pas de valeur" );
		}
	}
	

	/**
	 * API : Option sélectionnée depuis le menu
	 * @param item Element du menu choisi
	 */
	@Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
		switch (item.getItemId()){
			case Constants.MENU_CLEAN_CACHE:
				GlobalVars.cleanCache(this);
				break;
			case Constants.MENU_QUIT:
				GlobalVars.quitter();
				break;
			case Constants.MENU_PARAMETRES:
				Intent myIntent = new Intent(this, UIParametres.class);
				startActivity(myIntent);
				break;
			case android.R.id.home:
				Intent intent = new Intent(this, UIAccueil.class);
				startActivity(intent);
				finish();
				//navigateUpTo(intent);
		        break;
			default:
	        	Log.e(Constants.TAG,"UIBatiment.onOptionsItemSelected() n'a pas de valeur pour item="+String.valueOf(item.getItemId()));
	        	break;
		}
		return true;
    }
	
	
	@Override
	public void onNothingSelected(AdapterView<?> arg0) {	
	}

	
	/**
	 * API : évènement onClick() dans l'activité.
	 * @param	View
	 */
	public void onClick(View v) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); //on masque le clavier
		switch (v.getId()) {
			case (R.id.btnCreerNiveau):
				creerNiveau();
				break;
		}
		if (v.getId()==IVRefresh.getId()) {
			if (Constants.debug_mode)
				Log.v(Constants.TAG,"on refresh Batiments()");
			listerBatiments(false);
		}
	}
	
	/**
	 * API : Evènement sur item du menu contextuel
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		//astuce : item.getGroupId() : Id du bâtiment passé par onCreateContextMenu
		switch (item.getItemId()) {
			case Constants.MENU_SUPPRIMER_NIVEAU:
				supprimerNiveau(prefs.getString("tmp_niveau_nom",""), item.getGroupId());
				break;
			case Constants.MENU_RENAME:
				renommerNiveau(prefs.getString("tmp_niveau_nom",""), item.getGroupId());
				break;
			default:
				if (Constants.debug_mode)
					Log.e(Constants.TAG,"Elément inconnu du menu contextuel: "+item.getTitle());
		}
		return true;
	}
	
	
	/**
	 * API : Appel de cette méthode lors de la mise en avant de l'activité.
	 */
	@Override
	protected void onResume() {
	    super.onResume();
	    if (Constants.debug_mode)
	    	Log.d(Constants.TAG,"IUBatiment.onResume()");
	    Mesure.checkOfflineMesures(this);
	}
	
	/**
	 * API : Appel de cette méthode lors du réveil de l'activité.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		if (Constants.debug_mode)
			Log.d(Constants.TAG,"IUBatiment.onStart()");
	}
	
	/**
	 * API : Appel de cette méthode lors de la suppression l'activité en mémoire.
	 */
	@Override
	protected void onStop() {
	    super.onStop();
	    Log.d(Constants.TAG,"IUBatiment.onStop()");
	}


	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		
	}
}
