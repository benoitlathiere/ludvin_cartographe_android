package org.ludvin.cartographe;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


public class Batiment implements Serializable {

	private static final long serialVersionUID = 1L;
	private String nom;
	private int ID;
	private int nb_etages;
	
	public Batiment(String nom, int ID, int nb_etages) {
		super();
		this.nom=nom;
		this.ID=ID;
		this.nb_etages=nb_etages;
	}
	
	public String getNom() {
		return this.nom;
	}
	
	public int getId() {
		return this.ID;
	}

	public int getNb_etages() {
		return this.nb_etages;
	}
	
	/**
	 * Créer un objet JSON générique pour lister les bâtiments.
	 * @param context Contexte.
	 * @return Objet JSON à envoyer au serveur.
	 */
	public static JSONObject getJSONList(Context context) {
		if (context!=null) {
			try {
				JSONObject msgjson = new JSONObject();
				msgjson.put("objet", "batiment");
				msgjson.put("action", "lister");
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
	 * Créer un objet JSON générique pour créer un bâtiment.
	 * @param context Contexte.
	 * @param name Nom du nouveau bâtiment.
	 * @return Objet JSON à envoyer au serveur.
	 */
	public static JSONObject getJSONCreate(Context context, String name) {
		if (context!=null && !name.isEmpty()) {
			try {
				JSONObject msgjson = new JSONObject();
				JSONObject jsoncontenu = new JSONObject();
				msgjson.put("objet", "batiment");
				msgjson.put("action", "creer");
				jsoncontenu.put("nom",name.replace('"', ' '));
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
	 * Créer un objet JSON générique pour supprimer un bâtiment.
	 * @param context Contexte.
	 * @param id ID du bâtiment à supprimer.
	 * @return Objet JSON à envoyer au serveur.
	 */
	public static JSONObject getJSONDelete(Context context, int id) {
		if (context!=null && id>0) {
			try {
				JSONObject msgjson = new JSONObject();
				JSONObject jsoncontenu = new JSONObject();
				msgjson.put("objet", "batiment");
				msgjson.put("action", "supprimer");
					jsoncontenu.put("ID",id);
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
	 * Créer un objet JSON générique pour renommer un bâtiment.
	 * @param context Contexte.
	 * @param id ID du bâtiment à renommer.
	 * @param oldname Ancien nom du bâtiment.
	 * @param newname Nouveau nom du bâtiment.
	 * @return Objet JSON à envoyer au serveur.
	 */
	public static JSONObject getJSONRename(Context context, int id, String oldname, String newname) {
		if (context!=null && id!=0 && !oldname.isEmpty() && !newname.isEmpty()) {
			try {
				JSONObject msgjson = new JSONObject();
				JSONObject jsoncontenu = new JSONObject();
				msgjson.put("objet", "batiment");
				msgjson.put("action", "renommer");
					jsoncontenu.put("nom",oldname);	//à supprimer
					jsoncontenu.put("nouveau",newname.replace('"', ' '));
					jsoncontenu.put("ID", id);
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
	 * Méthode générique qui transforme une réponse JSON en ArrayList.
	 * @param json	Objet JSON "batiments" avec Arrays "batiment".
	 * @return Liste d'objets Batiment.
	 */
	public static ArrayList<Batiment> JSON2batiments (JSONObject jObject) {
		ArrayList<Batiment> ArBatiments = new ArrayList<Batiment>();
		try {
			JSONArray jsobjets = jObject.getJSONArray("batiments");
			JSONObject jstmp;
			for (int i = 0; i<jsobjets.length();i++) {
				try {
					jstmp = jsobjets.getJSONObject(i);
					if (jstmp != null) {
						int ID = jstmp.getJSONObject("batiment").optInt("ID");
						String nom = jstmp.getJSONObject("batiment").optString("nom");
						int etages = jstmp.getJSONObject("batiment").optInt("niveaux");
						//pour ajouter le nb de niveaux :
							//if (Integer.parseInt(jstmp.getJSONObject("batiment").optString("niveaux"))>0)
							//	nom += " ("+jstmp.getJSONObject("batiment").optString("niveaux")+" niveaux)";
						ArBatiments.add(new Batiment(nom, ID, etages));
					}
				} catch (JSONException je) {
					if (Constants.debug_mode) {
						je.printStackTrace();
						Log.d(Constants.TAG, "debug1: "+je.toString());
					}
				}
			}
        } catch (Exception e) {
            e.printStackTrace();
            if (Constants.debug_mode)
            	Log.d(Constants.TAG, "debug2: "+e.toString());
        }
		return ArBatiments;
	}
	
	
	/**
	 * Mise en cache d'un flux de batiments + la date du flux.
	 * @param context Contexte.
	 * @param json Flux JSON global du serveur.
	 * @return
	 */
	public static boolean setCacheList(Context context, JSONObject json) {
		if (json!=null) {
			long date=0;
			try {
				date=(long) json.getLong("date");
			} catch (JSONException e) {
				e.printStackTrace();
			}
			SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.CACHE_MODE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putLong("cache_batiment_date", date);
			editor.putString("cache_batiment", json.toString());
			return editor.commit();
		}
		return false;
	}
	
	/**
	 * Récupère les Batiments depuis le cache.
	 * @param context Contexte.
	 * @return Retourn le JSON "reponses" (ou null).
	 */
	public static JSONObject getCacheList(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.CACHE_MODE);
		try {
			return new JSONObject(prefs.getString("cache_batiment", null));
		} catch (JSONException e) {
			if (Constants.debug_mode)
				e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Vérifie si la valeur en cache est récente (durée dans <code>Constants.CACHE_LIFETIME</code>).
	 * @return <code>true</code> si le cache est récent et utilisable, sinon <code>false</code>.
	 */
	public static boolean cacheRecent(Context context) {
		if (context==null)
			return false;
		if (GlobalVars.getCacheLifetime(context)==-1)
			return true;
		if (GlobalVars.getCacheLifetime(context)==0)
			return false;
		SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.CACHE_MODE);
		Calendar cal = new GregorianCalendar();
		try {
			long datecache = prefs.getLong("cache_batiment_date", 0);
			long datenow = cal.getTimeInMillis()/1000;
			if (datecache+GlobalVars.getCacheLifetime(context) > datenow )	//cache valide
				return true;
		} catch (ClassCastException e) { 
			if (Constants.debug_mode)
				e.printStackTrace();
		} catch (IllegalArgumentException e) {
			if (Constants.debug_mode)
				e.printStackTrace();
		}
		if (Constants.debug_mode) 
    		Log.e(Constants.TAG, "Mesure.cacheRecent(), erreur, debug : context="+String.valueOf(context)+" / getCacheLifetime()="+String.valueOf(GlobalVars.getCacheLifetime(context)));
		return false;
	}
	
	/**
	 * API : appelé par l'Adapter pour la ListView.
	 */
    @Override
    public String toString() {
    	String tmp = this.nom;
    	return tmp;
    }
	
}
