package org.ludvin.cartographe;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class Mesure {

	private String email;	//cartographe des mesures
	private int idcompte;
	private String nom;
	private int nb;			//nb de mesures
	private String jour;	//date du jour YYYY-MM-DD
	private int idrepere;	//ID du repère liè aux mesures
	private Context context;
	
	public Mesure (Context context, String email, String nom, int idcompte, int nb, String jour, int idrepere) {
		this.email=email;
		this.nom=nom;
		this.idcompte=idcompte;
		this.nb=nb;
		this.jour=jour;
		this.idrepere=idrepere;
		this.context=context;
	}
	
	/**
	 * Retourne le cartographe des mesures.
	 * @return Login de l'auteur.
	 */
	protected String getEmail() {
		return this.email;
	}

	/**
	 * Retourne le ID du cartographe des mesures.
	 * @return ID du cartographe.
	 */
	public int getIdCompte() {
		return this.idcompte;
	}
	/**
	 * Retourne le jour des mesures.
	 * @return Date ISO YYYY-MM-DD.
	 */
	public String getJour() {
		return this.jour;
	}

	/**
	 * Retourne le jour des mesures.
	 * @return Date format européen DD-MM-YYYY
	 */
	public String getJourEurope() {
		try {
			String[] str = this.jour.split("-");
			return String.valueOf(str[2]+"."+str[1]+"."+str[0]);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	/**
	 * Retourne le repère.
	 * @return ID du repère lié aux mesures.
	 */
	public int getIdRepere() {
		return this.idrepere;
	}
	
	/**
	 * Créer un objet JSON générique pour lister les mesures.
	 * @param context Contexte.
	 * @param repere_ID ID du repère parent.
	 * @return Objet JSON à envoyer au serveur (ou <code>null</code> si erreur).
	 */
	public static JSONObject getJSONList(Context context, int repere_ID) {
		if (context!=null) {
			try {
				JSONObject msgjson = new JSONObject();
				JSONObject jsoncontenu = new JSONObject();
				msgjson.put("objet", "mesure");
				msgjson.put("action", "lister");
					jsoncontenu.put("repere_ID",repere_ID);
				msgjson.put("contenu",jsoncontenu);
				int uid = GlobalVars.getUserID(context);
				if (uid==0)
					return null;
				msgjson.put("user_id", uid);
				return msgjson;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * Transforme une réponse JSON simple en ArrayList.
	 * @param context Contexte.
	 * @param jObject Données formatées en JSON (objet <code>reponses</code>).
	 * @return Liste d'objets.
	 */
	public static ArrayList<Mesure> JSON2mesures(Context context, JSONObject jObject) {
		ArrayList<Mesure> ArMesures = new ArrayList<Mesure>();
		try {
			int nb = jObject.getInt("nb");
			if (nb>0) {
				JSONObject jstmp;
				JSONArray jsobjets = jObject.getJSONArray("mesures");
				for (int i = 0; i<jsobjets.length();i++) {
					jstmp = jsobjets.getJSONObject(i);
					if (jstmp != null) {
						String email = jstmp.getJSONObject("mesure").optString("email");
						String nom = jstmp.getJSONObject("mesure").optString("nom");
						int idcompte = jstmp.getJSONObject("mesure").optInt("idcompte");
						int nbm = jstmp.getJSONObject("mesure").optInt("nb");
						String jour = jstmp.getJSONObject("mesure").optString("jour");
						int idrepere = jstmp.getJSONObject("mesure").optInt("idrepere");
						ArMesures.add(new Mesure(context, email, nom, idcompte, nbm, jour, idrepere));
					}
				}
			} else {
				return null;
			}
        } catch (JSONException je) {
        	if (Constants.debug_mode) {
        		Log.e(Constants.TAG, "JSON Exception de JSON2mesures : retour : "+ArMesures.toString());	//debug
        		je.printStackTrace();
        	}
        }
		return ArMesures;
	}
	
	/**
	 * Vérifie si la valeur en cache est récente (durée dans <code>Constants.CACHE_LIFETIME</code>).
	 * @param context Contexte.
	 * @param parent_ID Identifiant de l'élément parent.
	 * @return <code>true</code> si le cache est récent et utilisable, sinon <code>false</code>.
	 */
	public static boolean cacheRecent(Context context, int parent_ID) {
		if (context==null)
			return false;
		if (GlobalVars.getCacheLifetime(context)==-1)
			return true;
		if (GlobalVars.getCacheLifetime(context)==0)
			return false;
		if (parent_ID!=0 ) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.PREFS_MODE);
			Calendar cal = new GregorianCalendar();
			String x = String.valueOf(parent_ID);
			try {
				long datecache = prefs.getLong("cache_mesure_"+x+"_date", 0);
				long datenow = cal.getTimeInMillis()/1000;
				if (Constants.debug_mode)
	        		Log.d(Constants.TAG, "Mesure.cacheRecent() datecache="+String.valueOf(datecache)+" + getCacheLifetime="+String.valueOf(GlobalVars.getCacheLifetime(context))+ " > datenow="+String.valueOf(datenow));
				if (datecache+GlobalVars.getCacheLifetime(context) > datenow)	//cache valide
					return true;
			} catch (ClassCastException e) { 
					e.printStackTrace();
			} catch (IllegalArgumentException e) {
					e.printStackTrace();
			}
		} else if (Constants.debug_mode) {
    		Log.e(Constants.TAG, "Mesure.cacheRecent(), erreur:parent_ID==0 debug : context="+String.valueOf(context)+" / parent_ID="+String.valueOf(parent_ID)+ " / getCacheLifetime()="+String.valueOf(GlobalVars.getCacheLifetime(context)));
		}
		return false;
	}
	
	/**
	 * Mise en cache d'un flux d'objets + la date du flux.
	 * @param context Contexte.
	 * @param json Flux JSON global du serveur.
	 * @param parent_ID Identifiant de l'élément parent.
	 * @return <code>true</code> si enregistrement en mémoire effectué (sinon <code>false</code>).
	 */
	public static boolean setCacheList(Context context, JSONObject json, int parent_ID) {
		if (Constants.debug_mode)
    		Log.i(Constants.TAG, "Mesure.setCacheList()");
		if (context!=null && json!=null && parent_ID!=0) {
			String x = String.valueOf(parent_ID);
			long date=0;
			try {
				date=(long) json.getLong("date");
				SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.PREFS_MODE);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putLong("cache_mesure_"+x+"_date", date);
				editor.putString("cache_mesure_"+x, json.toString());
				if (Constants.debug_mode) {
	        		Log.i(Constants.TAG, "Mesure.setCacheList() : cache_mesure_"+x+" / date="+String.valueOf(date)+ "/ json="+json.toString());
	        		if (! json.toString().contains("\"idrepere\":"+x))
	        			Log.e(Constants.TAG, "Mesure.setCacheList() : erreur pas le bon repère !?");
				}
				return editor.commit();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * Récupère les objets depuis le cache.
	 * @param context Contexte.
	 * @param parent_ID Identifiant de l'élément parent.
	 * @return Retourne le JSON global (ou <code>null</codes> si erreur).
	 */
	public static JSONObject getCacheList(Context context, int parent_ID) {
		if (context!=null && parent_ID!=0) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.PREFS_MODE);
			String x = String.valueOf(parent_ID);
			try {
				String tmp = prefs.getString("cache_mesure_"+x, null);
				if (Constants.debug_mode)
	        		Log.i(Constants.TAG, "Mesure.getCacheList() : cache_mesure_"+x+" / json="+tmp);
				if (tmp!=null)
					return new JSONObject(tmp);
			} catch (JSONException e) {
					e.printStackTrace();
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	
	/**
	 * Indique si des relevés de mesures sont en cache et doivent être envoyées au serveur.
	 * @param context Contexte.
	 * @return <code>true</code> si des nouvelles mesures sont à envoyer (sinon <code>false</code>).
	 */
	public static boolean hasOfflineMesures(Context context) {
		if (context!=null) {
			SharedPreferences cache = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.PREFS_MODE);
			try {
				String tmp = cache.getString("cache_offline_mesures", null);
				if (tmp!=null && tmp.length()>0) {
					Log.v(Constants.TAG, "Mesure.hasOfflineMesures() il y a des mesures en cache (longueur="+String.valueOf(tmp.length()));
					return true;
				}
				Log.i(Constants.TAG, "Mesure.hasOfflineMesures() pas de mesure en cache à envoyer au serveur");
			} catch (ClassCastException e) {
				Log.e(Constants.TAG, "Mesure.hasOfflineMesures() erreur");
				e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * Efface ou met en cache des mesures relevées ne pouvant être envoyées au serveur. S'il y a déjà des mesures en cache, les nouvelles sont ajoutées.
	 * @param context Contexte.
	 * @param json Flux JSON des mesures. Mesures effacées si <code>null</code>.
	 * @return Etat de l'enregistrement en mémoire.
	 */
	public static boolean setOfflineMesures(Context context, JSONObject json) {
		if (context!=null) {
			SharedPreferences cache = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.PREFS_MODE);
			SharedPreferences.Editor editor = cache.edit();
			if (json!=null) {
				try {
					ArrayList<JSONObject> listtmp = new ArrayList<JSONObject>(); //liste des objets JSON
					String txtjson="";
					//1 on récupère les valeurs du cache si elles existent
					if (hasOfflineMesures(context))
						listtmp = getOfflineMesures(context);
					//2 on ajoute le nouvel objet à stocker
					listtmp.add(json);
					//3 on boucle la liste pour transformer en String global avec un séparateur
					for (JSONObject s : listtmp) {
						txtjson += s.toString() + Constants.CACHE_SEPARATOR;
					}
					//4 patch pour supprimer le dernier séparateur
					txtjson = txtjson.substring(0, txtjson.length()-Constants.CACHE_SEPARATOR.length());	
					if (Constants.debug_mode)
						Log.i(Constants.TAG, "setOfflineMesures() à stocker=" +String.valueOf(txtjson.length()+"c soit nb éléments=" +String.valueOf(listtmp.size())+ " / ajout=" +String.valueOf(json.toString().length())+"c"));
					//5 on stocke en mémoire le String global
					editor.putString("cache_offline_mesures", txtjson);
					return editor.commit();
				} catch (IndexOutOfBoundsException e) {
					if (Constants.debug_mode)
						e.printStackTrace();
				} catch (UnsupportedOperationException e) {
					if (Constants.debug_mode)
						e.printStackTrace();
				} catch (ClassCastException e) {
					if (Constants.debug_mode)
						e.printStackTrace();
				} catch (IllegalArgumentException e) {
					if (Constants.debug_mode)
						e.printStackTrace();
				}
			} else {
				if (Constants.debug_mode)
					Log.i(Constants.TAG, "setOfflineMesures() on efface les mesures en cache offline.");
				return editor.remove("cache_offline_mesures").commit();
			}
		}
		return false;
	}
	
	
	/**
	 * Créé une tâche pour envoyer en arrière plan les mesures stockées offline.
	 * @param context Contexte.
	 */
	public static void sendOfflineMesures(Context context) {
		if (Constants.debug_mode)
			Log.i(Constants.TAG, "sendOfflineMesures()");
		if (context!=null) {
			ArrayList<JSONObject> liste = getOfflineMesures(context);
			if (liste!=null) {
				new SendMesuresTask().execute(context, liste);
			}
		}
	}
	
	/**
	 * Récupère les mesures à envoyer au serveur depuis le cache. La méthode vérifie au pralable si des mesures existent en mémoire.
	 * @param context Contexte.
	 * @return Liste d'objets JSON des mesures à envoyer au serveur (ou <code>null</code> si vide ou erreur).
	 */
	public static ArrayList<JSONObject> getOfflineMesures(Context context) {
		if (hasOfflineMesures(context)) {
			SharedPreferences cache = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.PREFS_MODE);
			ArrayList<JSONObject> listtmp = new ArrayList<JSONObject>(); //liste des objets JSON
			try {
				//1 on récup les valeurs en cache
				String tmp = cache.getString("cache_offline_mesures", null);
				if (tmp!=null) {
					try {
						//2 on découpe la chaine complète pour séparer chaque objet
						String[] flux = tmp.split(Constants.CACHE_SEPARATOR);
						int x=0;
						//3 on boucle pour récupérer chaque objet du cache qu'on met dans une liste
						for (x=0; x<flux.length; x++) {
							listtmp.add(new JSONObject(flux[x]));
						}
						if (Constants.debug_mode)
							Log.i(Constants.TAG, "getOfflineMesures() retourné depuis cache="+String.valueOf(tmp.length())+"c soit nb éléments=" +String.valueOf(listtmp.size())+"");
						//4 on retourne la liste
						return listtmp;
					} catch (NullPointerException e) {
						if (Constants.debug_mode)
							e.printStackTrace();
					}
				}
			} catch (JSONException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			} catch (ClassCastException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * Vérification des mesures stockées offline à envoyer au serveur.  
	 * Demande préalable à l'utilisateur par popup (ou auto activé).
	 * @param context Contexte.
	 */
	public static void checkOfflineMesures(final Context context) {
		 if (Mesure.hasOfflineMesures(context) && GlobalVars.isNetworkAvailable(context)) {
			 if (getAutoSendOfflineMesure(context)) {
				 if (Constants.debug_mode)
						Log.i(Constants.TAG, "checkOfflineMesures(), envoi auto des mesures, d'après getAutoSendOfflineMesure()");
				 Mesure.sendOfflineMesures(context);
			 } else {
			    AlertDialog.Builder alert = new AlertDialog.Builder(context);
				alert.setTitle(R.string.label_measures_not_sent);
				alert.setMessage(R.string.msg_ask_send_measures_offline);
				alert.setPositiveButton(R.string.btn_Yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Mesure.sendOfflineMesures(context);
					}
				});
				alert.setNegativeButton(R.string.btn_No, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) { 
						dialog.cancel();
					}
				});
				CheckBox cb = new CheckBox(context);
				cb.setText(R.string.label_send_auto);
				cb.setOnCheckedChangeListener (new OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
						setAutoSendOfflineMesure(context, isChecked);
					}
				});
				alert.setView(cb);
				alert.show();	
			 }
		 }
	}
	
	/**
	 * Enregistre le choix de l'utilisateur pour l'envoi automatique des mesures offline.
	 * @param context Contexte.
	 * @param state Indique si le chargement doit être automatique (sans boîte de dialogue).
	 */
	public static void setAutoSendOfflineMesure(Context context, boolean state) {
		SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		if (Constants.debug_mode)
			Log.i(Constants.TAG, "autoSendOfflineMesure(), choix de l'utilisateur : "+String.valueOf(state));
		editor.putBoolean("store_auto_send_mesures", state).apply();
	}
	
	/**
	 * Vérifie si l'utilisateur a choisi l'envoi automatique des mesures offline.
	 * @param context Contexte.
	 * @return Indique si le chargement doit être automatique (sans boite de dialogue).
	 */
	public static boolean getAutoSendOfflineMesure(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, Context.MODE_PRIVATE);
		try {
			return prefs.getBoolean("store_auto_send_mesures", false);
		} catch (ClassCastException e) { }
		return false;
	}
	
	/**
	 * Retourne un texte.
	 * @return Texte formaté des éléments
	 */
	public String toString() {
		if (GlobalVars.getUserID(this.context)==this.getIdCompte())
			return this.getJourEurope()+" ("+this.nb+" mesure"+(this.nb>1?"s":"")+")";
		else
			return this.getJourEurope()+" ("+this.nb+" mesure"+(this.nb>1?"s":"")+" / "+this.nom+")";
	}
	
}