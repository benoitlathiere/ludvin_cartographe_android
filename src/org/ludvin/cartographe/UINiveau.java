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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Toast;

public class UINiveau extends FragmentActivity implements ActionBar.OnNavigationListener, OnClickListener, OnItemClickListener, OnItemSelectedListener {
	
	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
	
	//global
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;
	
	//UI
	private ActionBar actionbar;
	private ListView LVReperes;
	private EditText ETRepere;
	private EditText ETCommentaire;
	private Button btnCreerRepere;
	private RadioGroup RGRepere;
	private CheckBox CBUrgence;
	private ImageView IVRefresh;
	private ProgressBar PBRefresh;
	
	private ArrayAdapter<Repere> RepAdapter;
	private ArrayAdapter<Niveau> NiveauxAdapter;

		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uiniveau);
		
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
		//actionbar.setTitle(R.string.label_level);
		//actionbar.setSubtitle(R.string.label_spots);
		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionbar.setDisplayShowCustomEnabled(true);
		IVRefresh = new ImageView(this);
		IVRefresh.setClickable(true);
		IVRefresh.setImageResource(R.drawable.navigation_refresh);
		IVRefresh.setOnClickListener(this);
		PBRefresh =  new ProgressBar(this);
		actionbar.setCustomView(IVRefresh);
		actionbar.getCustomView().setClickable(true);
		LVReperes = (ListView) findViewById(R.id.LVReperes);		
		ETRepere = (EditText) findViewById(R.id.ETRepere);
		ETCommentaire = ((EditText) findViewById(R.id.ETCommentaire));
		btnCreerRepere = (Button) findViewById(R.id.btnCreerRepere);
		btnCreerRepere.setOnClickListener(this);
		RGRepere = (RadioGroup) findViewById(R.id.RGRepere);
		CBUrgence = (CheckBox) findViewById(R.id.CBUrgence);
		
		listerNiveaux(true);
	}
	
	/**
	 * Retourne la position de l'objet courant pour le Spinner (ActionBar). Prends en compte l'Adapter de l'objet.
	 * @return Position de l'objet courant, sinon 0
	 */
	private int SpinPosNiveau() {
		for (int i=0 ; i<NiveauxAdapter.getCount(); i++) {
			if  ( ((Niveau) NiveauxAdapter.getItem(i)).getId()==prefs.getInt("tmp_niveau_id",0) ) {
				return i;
			}
		}
		if (Constants.debug_mode)
			Log.d(Constants.TAG,"ERREUR : Problème UINiveau.SpinPosNiveau() - retourne 0 !");	//debug
		return 0;
	}
	
	/**
	 * Objets dans ActionBar.
	 * @param liste Liste des objets
	 */
	private void Niveaux(ArrayList<Niveau> liste) {
		if (Constants.debug_mode)
			Log.i(Constants.TAG,"UINiveaux.Niveaux()");
		if (liste!=null) {
			NiveauxAdapter = new ArrayAdapter<Niveau>(this, android.R.layout.simple_list_item_1, liste);
			if (NiveauxAdapter != null) {
				actionbar.setListNavigationCallbacks(NiveauxAdapter, this);
				actionbar.setSelectedNavigationItem(SpinPosNiveau());			
			}  else {
				Log.e(Constants.TAG,"Problème pour lister dans Niveaux()");
				Toast toast = Toast.makeText(this, R.string.msg_problem_list_levels, Toast.LENGTH_LONG);toast.show();
			}
		} else {
			Log.e(Constants.TAG,"Problème 2 pour lister dans Niveaux()");
			Toast toast = Toast.makeText(this, R.string.msg_problem_list_levels, Toast.LENGTH_LONG);toast.show();
		}
	}
	
	/**
	 * Objets dans View.
	 * @param liste Liste des objets.
	 */
	private void Reperes(ArrayList<Repere> liste) {
		if (Constants.debug_mode)
			Log.i(Constants.TAG, "UINiveau.Reperes()");
		LVReperes.setAdapter(null);
		if (liste!=null) {
			RepAdapter = new ArrayAdapter<Repere>(this, android.R.layout.simple_list_item_1, liste);
			if (RepAdapter != null) {
				LVReperes.setAdapter(RepAdapter);
				registerForContextMenu(LVReperes);
				LVReperes.setOnItemClickListener(this);
			} else {
				if (Constants.debug_mode)
					Log.e(Constants.TAG,"Problème pour lister les reperes dans UINiveau.Reperes()");
				Toast toast = Toast.makeText(this, R.string.msg_problem_list_spots, Toast.LENGTH_LONG);toast.show();
			}
		} else {
			Toast toast = Toast.makeText(this, R.string.msg_empty_list, Toast.LENGTH_SHORT);toast.show();
		}
	}
	
	/**
	 * Demande la liste des objets.
	 * @param cache Autoriser l'utilisation du cache si disponible.
	 */
	private void listerNiveaux(boolean cache) {
		Log.v(Constants.TAG,"UINiveau.listerNiveaux()");
		if (Niveau.cacheRecent(this, prefs.getInt("tmp_batiment_id", 0)) && cache) {
			try {
				if (Constants.debug_mode)
					Log.i(Constants.TAG,"on essaye de récupérer le cache.");
				JSONObject json = Niveau.getCacheList(this, prefs.getInt("tmp_batiment_id",0));
				if (Constants.debug_mode)
					Log.d(Constants.TAG,"dump json:"+json.toString());
				Niveaux(Niveau.JSON2niveaux(json.getJSONObject("reponses"), null));	//FIXME remplacer null
			} catch (JSONException e) {
				e.printStackTrace();
				Toast toast = Toast.makeText(this, R.string.msg_problem_list_levels, Toast.LENGTH_LONG);toast.show();
			}
		} else if (GlobalVars.isNetworkAvailable(this)){
			JSONObject msgjson = Niveau.getJSONList(getBaseContext(), prefs.getInt("tmp_batiment_id",0));
			if (msgjson!=null) {
				try {
					new sendJSON().execute(msgjson);
				} catch (IllegalStateException e) {
					e.printStackTrace();
					Toast toast = Toast.makeText(this, R.string.msg_problem_list_levels, Toast.LENGTH_LONG);toast.show();
				}
			} else {
				Toast toast = Toast.makeText(this, R.string.msg_problem_list_levels, Toast.LENGTH_LONG);toast.show();
			}
		} else {
			Toast toast = Toast.makeText(this, getString(R.string.msg_internet_unreachable_check_connection), Toast.LENGTH_LONG);toast.show();
		}
	}
	
	/**
	 * Demande la liste des objets.
	 * @param cache Autoriser l'utilisation du cache si disponible.
	 */
	private void listerReperes(boolean cache) {
		if (Repere.cacheRecent(this, getCurrentNiveau().getId()) && cache) {	//recup cache..
			try {
				Log.i(Constants.TAG, "on essaye de récupérer le cache repère");
				JSONObject json = Repere.getCacheList(this, getCurrentNiveau().getId());
				Reperes(Repere.JSON2reperes(json.getJSONObject("reponses"), getCurrentNiveau()));
			} catch (JSONException e) {
					Log.e(Constants.TAG, "listerReperes() , erreur : ");
					e.printStackTrace();
					Toast toast = Toast.makeText(this, R.string.msg_problem_list_spots, Toast.LENGTH_LONG);toast.show();
			}
		}  else if (! GlobalVars.isNetworkAvailable(this)) {
			Toast toast = Toast.makeText(this, getString(R.string.msg_internet_unreachable_check_connection), Toast.LENGTH_LONG);toast.show();
		} else {
			Log.i(Constants.TAG, "on récupère les repères du serveur");
			try {
				JSONObject msgjson = Repere.getJSONList(getBaseContext(), getCurrentNiveau().getId());
				if (msgjson!=null) {
					try {
						new sendJSON().execute(msgjson);
					} catch (IllegalStateException e) {
						e.printStackTrace();
						Toast toast = Toast.makeText(this, R.string.msg_problem_list_spots, Toast.LENGTH_LONG);toast.show();
					}
				} else {
					Toast toast = Toast.makeText(this, R.string.msg_problem_list_spots, Toast.LENGTH_LONG);toast.show();
				}
			} catch (IllegalStateException e) {
				Log.e(Constants.TAG, "listerReperes() , erreur : ");
				e.printStackTrace();
				Toast toast = Toast.makeText(this, R.string.msg_problem_list_spots, Toast.LENGTH_LONG);toast.show();
			}
		}
	}
	
	/**
	 * Création d'un repère.
	 */
	private void creerRepere() {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); //on masque le clavier
		if (! GlobalVars.isNetworkAvailable(this)) {
			Toast toast = Toast.makeText(this, R.string.msg_no_internet_connection, Toast.LENGTH_SHORT);toast.show();
		} else {
			int selectedId = RGRepere.getCheckedRadioButtonId();
			String Nom = ETRepere.getText().toString().trim();
			String Commentaire = ETCommentaire.getText().toString().trim();
			ETRepere.setError(null);
			ETCommentaire.setError(null);
			if (selectedId==-1) {
				Toast toast = Toast.makeText(this, "Veuillez choisir un type de repère (Zone ou Point d'Intérêt).", Toast.LENGTH_SHORT);toast.show();
			} else if (Nom.isEmpty()) {
				Toast toast = Toast.makeText(this, R.string.msg_please_give_a_name, Toast.LENGTH_SHORT);toast.show();
				ETRepere.setError(getString(R.string.msg_please_give_a_name));
			} else {
				//type de repère : (int) 0=zone, 1=POI, d'après database
				int type=0;
				if (selectedId==R.id.RBPoi)
					type=1;
				int urgence=0;
				if (CBUrgence.isChecked())
					urgence=1;
				//RAZ
				ETRepere.setText("");
				ETCommentaire.setText("");
				CBUrgence.setChecked(false);
				((CompoundButton) findViewById(R.id.RBPoi)).setChecked(false);
				
				/*//v2
				JSONObject msgjson = new JSONObject();
				JSONObject jsoncontenu = new JSONObject();
				try {
					msgjson.put("objet", "repere");
					msgjson.put("action", "creer");
					msgjson.put("user_id", prefs.getInt("user_id", 0));
						jsoncontenu.put("niveau_ID", getCurrentNiveau().getId());
						jsoncontenu.put("nom", Nom.replace('"', ' '));
						jsoncontenu.put("commentaire", Commentaire.replace('"', ' '));
						jsoncontenu.put("poi", type);
						jsoncontenu.put("urgence", urgence);
					msgjson.put("contenu",jsoncontenu);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				new sendJSON().execute(msgjson);*/
				JSONObject msgjson = Repere.getJSONCreate(getBaseContext(), Nom, Commentaire, type, urgence, getCurrentNiveau().getId());
				if (msgjson!=null) {
					try {
						new sendJSON().execute(msgjson);
					} catch (IllegalStateException e) {
						e.printStackTrace();
						Toast toast = Toast.makeText(this, R.string.msg_problem_create_spot, Toast.LENGTH_LONG);toast.show();
					}
				} else {
					Toast toast = Toast.makeText(this, R.string.msg_problem_create_spot, Toast.LENGTH_LONG);toast.show();
				}
			}
		}
	}
	
	/**
	 * Renommage d'un repère avec boîte de dialogue
	 * @param ancien Nom à renommer
	 * @param id Identifiant de l'élément à renommer
	 */
	private void renommerRepere(final String ancien, final int id) {
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS); //on masque le clavier
		if (! GlobalVars.isNetworkAvailable(this)) {
			Toast toast = Toast.makeText(this, R.string.msg_no_internet_connection, Toast.LENGTH_SHORT);toast.show();
		} else {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Renommer "+ancien);
			alert.setMessage("Nouveau nom :");
			final EditText input = new EditText(this);	// Set an EditText view to get user input
			input.setText(ancien);
			alert.setView(input);
			alert.setPositiveButton(R.string.btn_OK, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					String nouveau = input.getText().toString().trim();
					if (nouveau.isEmpty()) {
						Toast toast = Toast.makeText(UINiveau.this, "Veuillez donner un nom.", Toast.LENGTH_SHORT);toast.show();
					} else if (nouveau==ancien) {
						Toast toast = Toast.makeText(UINiveau.this, "Le nouveau nom doit être différent de l'ancien.", Toast.LENGTH_SHORT);toast.show();
					} else {
						/*//v2
						JSONObject msgjson = new JSONObject();
						JSONObject jsoncontenu = new JSONObject();
						try {
							msgjson.put("objet", "repere");
							msgjson.put("action", "renommer");
							msgjson.put("user_id", prefs.getInt("user_id", 0));
								jsoncontenu.put("ID",id);
								jsoncontenu.put("nom",ancien);
								jsoncontenu.put("nouveau",nouveau.replace('"', ' '));
							msgjson.put("contenu",jsoncontenu);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						new sendJSON().execute(msgjson);*/
						JSONObject msgjson = Repere.getJSONRename(getBaseContext(), id, ancien, nouveau);
						if (msgjson!=null) {
							try {
								new sendJSON().execute(msgjson);
							} catch (IllegalStateException e) {
								e.printStackTrace();
								Toast toast = Toast.makeText(getBaseContext(), R.string.msg_problem_rename_spot, Toast.LENGTH_LONG);toast.show();
							}
						} else {
							Toast toast = Toast.makeText(getBaseContext(), R.string.msg_problem_rename_spot, Toast.LENGTH_LONG);toast.show();
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
	 * Suppression d'un repère.
	 * @param nom Nom de l'élément à supprimer (facultatif).
	 * @param id Identifiant de l'élément.
	 */
	private void supprimerRepere(final String nom, final int id) {
		if (! GlobalVars.isNetworkAvailable(this)) {
			Toast toast = Toast.makeText(this, R.string.msg_no_internet_connection, Toast.LENGTH_SHORT);toast.show();
		} else {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(getString(R.string.label_delete)+" "+prefs.getString("tmp_repere_nom",""));
			alert.setMessage("Voulez-vous supprimer définitivement le repère et tous les éléments liés ?");
			alert.setPositiveButton(R.string.btn_Yes, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					if (id != 0) {
						/*//v2
						JSONObject msgjson = new JSONObject();
						JSONObject jsoncontenu = new JSONObject();
						try {
							msgjson.put("objet", "repere");
							msgjson.put("action", "supprimer");
							msgjson.put("user_id", prefs.getInt("user_id", 0));
								jsoncontenu.put("nom",nom);
								jsoncontenu.put("ID",id);
							msgjson.put("contenu",jsoncontenu);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						new sendJSON().execute(msgjson);*/
						JSONObject msgjson = Repere.getJSONDelete(getBaseContext(), id);
						if (msgjson!=null) {
							try {
								new sendJSON().execute(msgjson);
							} catch (IllegalStateException e) {
								e.printStackTrace();
								Toast toast = Toast.makeText(getBaseContext(), R.string.msg_problem_delete_spot, Toast.LENGTH_LONG);toast.show();
							}
						} else {
							Toast toast = Toast.makeText(getBaseContext(), R.string.msg_problem_delete_spot, Toast.LENGTH_LONG);toast.show();
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
	 * Retourne le niveau courant sélectionné.
	 * @return Objet
	 */
	private Niveau getCurrentNiveau() {
		return NiveauxAdapter.getItem(actionbar.getSelectedNavigationIndex());
	}

	/**
	 * Analyse la réponse JSON du serveur et execute les actions.
	 * @param json Réponse JSON entière du serveur.
	 */
	private void analyserReponses(JSONObject json) {
		if (Constants.debug_mode)
			Log.i(Constants.TAG, "analyserReponses()");
		String objet="";
		String action="";
		JSONObject reponses ;
		String message = "";
		try {
			action = json.getString("action");
			objet = json.getString("objet");
			reponses = json.getJSONObject("reponses");
				message = reponses.getString("message");
			if (message.contains("OK")) {				
				if (objet.contains("niveau")) {
					if (action.contains("lister")) {
						Niveaux(Niveau.JSON2niveaux(reponses, null));	//FIXME remplacer le null par objet (from server ?)
						Niveau.setCacheList(this, json, getCurrentNiveau().getBatiment_ID());
					} else {
						listerNiveaux(false);
					}
				} else if (objet.contains("repere")) {
					if (action.contains("lister")) {
						Reperes(Repere.JSON2reperes(reponses, getCurrentNiveau()));
						Repere.setCacheList(this, json, getCurrentNiveau().getId());	//mise en cache
					} else {
						listerReperes(false);
					}
				} else {	//pas normal d'être ici...
					listerNiveaux(true);
				}
			} else {
				if (Constants.debug_mode)
					Log.e(Constants.TAG, "y'a une erreur : "+reponses.getString("erreur"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
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
			if (Constants.debug_mode)
				Log.d(Constants.TAG, "onPreExecute()");
		}
		
		@Override
		protected JSONObject doInBackground(JSONObject... jsonMsg) {
			if (Constants.debug_mode)
				Log.d(Constants.TAG, "doInBackground():"+jsonMsg[0].toString());
			//V2
			String reponse = GlobalVars.postJsonServer(jsonMsg[0]);
			try {
				return new JSONObject(reponse);
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			return null;
		}
		
		protected void onPostExecute(JSONObject jreponses) {
			if (Constants.debug_mode)
				Log.d(Constants.TAG, "onPostExecute() "+jreponses.toString());
			analyserReponses(jreponses);
			actionbar.setCustomView(IVRefresh);
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


	/**
	 * API : Evènement lors d'un élément cliqué dans la ListView
	 */
	@Override
	public void onItemClick(AdapterView<?> adapter, View v, int pos, long id) {
		editor=prefs.edit();
		editor.putInt("tmp_repere_id",  ((Repere) adapter.getItemAtPosition(pos)).getId());
		editor.apply();
		Intent myIntent = new Intent(this, UIRepere.class);
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
		Repere idd= (Repere) LVReperes.getItemAtPosition(info.position);
		editor = prefs.edit();
		editor.putInt("tmp_repere_id" , idd.getId());
		editor.putString("tmp_repere_nom" , idd.getNom());
		editor.apply();
		//astuce: on met le Nom dans le titre et le Id de l'objet en GroupId du menu
		if (v.getId()==R.id.LVReperes) {
			menu.setHeaderTitle (idd.getNom());
			//menu.add(idd.getId(), Constants.MENU_DETAILS, 0, R.string.label_see_details);
		    menu.add(idd.getId(), Constants.MENU_RENAME, 0, R.string.label_rename);
		    menu.add(idd.getId(), Constants.MENU_SUPPRIMER_REPERE, 0, R.string.label_delete);
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
			case android.R.id.home:
				Intent myintent = new Intent(this, UIBatiment.class);
				startActivity(myintent);
				finish();
				//navigateUpTo(myintent);
				break;
	        default:
	        	Log.d(Constants.TAG,"ERREUR : onOptionsItemSelected() n'a pas de valeur pour item="+String.valueOf(item.getItemId()));
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
			case (R.id.btnCreerRepere):
				creerRepere();
				break;
		}
		if (v.getId()==IVRefresh.getId()) {
			if (Constants.debug_mode)
				Log.v(Constants.TAG,"on refresh Niveaux()");
			listerNiveaux(false);
		}
	}
	
	/**
	 * API : Evènement sur item du menu contextuel
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		//astuce : item.getGroupId() : Id de l'objet passé par onCreateContextMenu
		switch (item.getItemId()) {
			case Constants.MENU_DETAILS:
				//TODO ... v0.15.0
				break;
			case Constants.MENU_SUPPRIMER_REPERE:
				supprimerRepere(prefs.getString("tmp_repere_nom",""), item.getGroupId());
				break;
			case Constants.MENU_RENAME:
				renommerRepere(prefs.getString("tmp_repere_nom",""), item.getGroupId());
				break;
			default:
				Toast toast = Toast.makeText(this, "Elément inconnu du menu contextuel: "+item.getTitle(), Toast.LENGTH_SHORT);toast.show();
		}
		return true;
	}


	/**
	 * API : Appel de cette méthode lors de la mise en avant de l'activité.
	 */
	@Override
	protected void onResume() {
	    super.onResume();
	    Mesure.checkOfflineMesures(this);
	}
	
	/**
	 * API : Appel de cette méthode lors du réveil de l'activité.
	 */
	@Override
	protected void onStart() {
	    super.onStart();
	}


	/**
	 * API : Appel de cette méthode lors de la suppression l'activité en mémoire.
	 */
	@Override
	protected void onStop() {
	    super.onStop();
	    Log.d(Constants.TAG,"IUBatiment.onStop()");
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
			View rootView = inflater.inflate(R.layout.fragment_uiniveau_dummy, container, false);
			return rootView;
		}
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
		getSupportFragmentManager().beginTransaction().replace(R.id.containeruiniveau, fragment).commit();
		editor = prefs.edit();
		editor.putInt("tmp_niveau_id", getCurrentNiveau().getId());
		editor.putString("tmp_niveau_nom", getCurrentNiveau().getNom());
		editor.apply();
		listerReperes(true);
		return true;
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


	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
	}
}
