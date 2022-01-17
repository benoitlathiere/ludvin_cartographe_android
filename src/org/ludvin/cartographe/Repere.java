package org.ludvin.cartographe;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class Repere {
	
	private String nom;
	private int ID;
	private int nb_zones;
	private int nb_poi;
	private int etage_ID;	//bachward compatibility
	private int type;	//0=zone, 1=POI
	private Niveau niveau;	//default=null for the moment	//TODO à faire !
	
	
	public Repere(String nom, int ID, int nb_zones, int nb_poi, int etage_ID, int type, Niveau niveau) {
		super();
		this.nom=nom;
		this.ID=ID;
		this.etage_ID=etage_ID;
		this.nb_zones=nb_zones;
		this.nb_poi=nb_poi;
		this.type=type;
		this.niveau=niveau;
	}
	
	public String getNom() {
		return this.nom;
	}
	
	public int getId() {
		return this.ID;
	}
	
	public int getEtage_ID() {
		return this.etage_ID;
	}
	
	public int getNb_zones() {
		return this.nb_zones;
	}
	
	public int getNb_poi() {
		return this.nb_poi;
	}	
	
	public Niveau getNiveau() {
		return this.niveau;
	}
	
	/**
	 * Créer un objet JSON générique pour lister les repères.
	 * @param context Contexte.
	 * @param niveau_ID ID du niveau parent.
	 * @return Objet JSON à envoyer au serveur (ou <code>null</code> si erreur).
	 */
	public static JSONObject getJSONList(Context context, int niveau_ID) {
		if (context!=null && niveau_ID!=0) {
			try {
				JSONObject msgjson = new JSONObject();
				JSONObject jsoncontenu = new JSONObject();
				msgjson.put("objet", "repere");
				msgjson.put("action", "lister");
					jsoncontenu.put("niveau_ID",niveau_ID);
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
	 * Créer un objet JSON générique pour créer un repère.
	 * @param context Contexte.
	 * @param name Nom du nouveau repère. 
	 * @param commentaire Commentaire.
	 * @param type Type de repère : (int) 0=zone, 1=POI, d'après database.
	 * @param urgence Urgence=1, sinon 0.
	 * @param batiment_ID ID du niveau parent.
	 * @return Objet JSON à envoyer au serveur (ou <code>null</code> si erreur).
	 */
	public static JSONObject getJSONCreate(Context context, String name, String commentaire, int type, int urgence, int niveau_ID) {
		if (context!=null && !name.isEmpty() && niveau_ID!=0) {
			try {
				JSONObject msgjson = new JSONObject();
				JSONObject jsoncontenu = new JSONObject();
				msgjson.put("objet", "repere");
				msgjson.put("action", "creer");
				jsoncontenu.put("niveau_ID", niveau_ID);
				jsoncontenu.put("nom", name.replace('"', ' '));
				jsoncontenu.put("commentaire", commentaire.replace('"', ' '));
				jsoncontenu.put("poi", type);
				jsoncontenu.put("urgence", urgence);
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
	 * Créer un objet JSON générique pour supprimer un repère.
	 * @param context Contexte.
	 * @param id ID du repère à supprimer.
	 * @return Objet JSON à envoyer au serveur (ou <code>null</code> si erreur).
	 */
	public static JSONObject getJSONDelete(Context context, int id) {
		if (context!=null && id!=0) {
			try {
				JSONObject msgjson = new JSONObject();
				JSONObject jsoncontenu = new JSONObject();
				msgjson.put("objet", "repere");
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
	 * Créer un objet JSON générique pour renommer un repère.
	 * @param context Contexte.
	 * @param id ID du repère à renommer.
	 * @param oldname Ancien nom.
	 * @param newname Nouveau nom.
	 * @return Objet JSON à envoyer au serveur (ou <code>null</code> si erreur).
	 */
	public static JSONObject getJSONRename(Context context, int id, String oldname, String newname) {
		if (context!=null && id!=0 && !newname.isEmpty()) {
			try {
				JSONObject msgjson = new JSONObject();
				JSONObject jsoncontenu = new JSONObject();
				msgjson.put("objet", "repere");
				msgjson.put("action", "renommer");
					jsoncontenu.put("ID",id);
					jsoncontenu.put("nom",oldname);	//à supprimer 
					jsoncontenu.put("nouveau",newname.replace('"', ' '));
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
	 * Transforme un objet JSON simple en ArrayList.
	 * @param json Données formatées en JSON (objet <code>reponses</code>).
	 * @param niveau <code>Niveau</code> du repère.
	 * @return Liste d'objets (ou <code>null</code> si erreur ou vide).
	 */
	public static ArrayList<Repere> JSON2reperes(JSONObject jObject, Niveau niveau) {
		if (jObject!=null) {	//FIXME ajouter && niveau!=null
			ArrayList<Repere> ArReperes = new ArrayList<Repere>();
			try {
				int nb = jObject.getInt("nb");
				if (nb>0) {
					JSONObject jstmp;
					JSONArray jsobjets = jObject.getJSONArray("reperes");
					for (int i = 0; i<jsobjets.length();i++) {
						jstmp = jsobjets.getJSONObject(i);
						if (jstmp != null) {
							int ID = jstmp.getJSONObject("repere").optInt("ID");
							String nom = jstmp.getJSONObject("repere").optString("nom");
							//int nb_zones = jstmp.getJSONObject("repere").optInt("zones");
							//int nb_poi = jstmp.getJSONObject("repere").optInt("poi");
							int type = jstmp.getJSONObject("repere").optInt("type");
							int etage_ID = jstmp.getJSONObject("repere").optInt("etage_ID");
							ArReperes.add(new Repere(nom, ID, 0, 0, etage_ID, type, niveau));
						}
					}
				} else {
					return null;
				}
	        } catch (JSONException je) {
	        	if (Constants.debug_mode) {
	        		Log.e(Constants.TAG, "JSON Exception de JSON2reperes : retour : "+ArReperes.toString());
	        		Log.e(Constants.TAG, jObject.toString());
	        		je.printStackTrace();
	        	}
	        	return null;
	        }
			return ArReperes;
		}
		return null;
	}
	
	/**
	 * Mise en cache d'un flux d'objets + la date du flux.
	 * @param context Contexte.
	 * @param json Flux JSON global du serveur.
	 * @param parent_ID Identifiant de l'élément parent.
	 * @return Retourne <code>false</code> si erreur de mise en cache.
	 */
	public static boolean setCacheList(Context context, JSONObject json, int parent_ID) {
		if (context!=null && json!=null && parent_ID!=0) {
			String x = String.valueOf(parent_ID);
			if (json!=null) {
				long date=0;
				try {
					date=(long) json.getLong("date");
					SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.CACHE_MODE);
					SharedPreferences.Editor editor = prefs.edit();
					editor.putLong("cache_repere_"+x+"_date", date);
					editor.putString("cache_repere_"+x, json.toString());
					return editor.commit();
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return false;
	}
	
	/**
	 * Vérifie si la valeur en cache est récente (durée dans <code>Constants.CACHE_LIFETIME</code>).
	 * @param context Contexte.
	 * @param parent_ID Identifiant de l'élément parent.
	 * @return <code>true</code> si le cache est récent, sinon <code>false</code>.
	 */
	public static boolean cacheRecent(Context context, int parent_ID) {
		if (context!=null && parent_ID!=0) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.CACHE_MODE);
			Calendar cal = new GregorianCalendar();
			String x = String.valueOf(parent_ID);
			try {
				long datecache = prefs.getLong("cache_repere_"+x+"_date", 0);
				long datenow = cal.getTimeInMillis()/1000;
				if (Constants.CACHE_LIFETIME==-1 || datecache+(Constants.CACHE_LIFETIME*60) > datenow)	//cache valide
					return true;
			} catch (ClassCastException e) { 
				if (Constants.debug_mode)
					e.printStackTrace();
			} catch (IllegalArgumentException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * Récupère les objets depuis le cache.
	 * @param context Contexte.
	 * @param parent_ID Identifiant de l'élément parent.
	 * @return Retourn le JSON "reponses" (ou <code>null</code> si erreur).
	 */
	public static JSONObject getCacheList(Context context, int parent_ID) {
		if (context!=null && parent_ID!=0) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.CACHE_MODE);
			String x = String.valueOf(parent_ID);
			try {
				String tmp = prefs.getString("cache_repere_"+x, null);
				if (tmp!=null)
					return new JSONObject(tmp);
			} catch (JSONException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	

	/**
	 * Ajoute un repere_ID dans la liste des repères dont il faut forcer la mise à jour de la liste des mesures depuis le serveur (après un <code>SendMesuresTask</code>). 
	 * ID_repere est ajouté sans vérifier sa présence au préalable.
	 * @param context Contexte.
	 * @param repere_ID ID du repère.
	 */
	public static void addRepereForceRefresh(Context context, int repere_ID) {
		SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.CACHE_MODE);
		SharedPreferences.Editor editor = prefs.edit();
		//les ID sont séparés par des $ / ex : "5$45$7$23"
		String txt="";
		try {
			txt=prefs.getString("cache_reperes_forcerefresh", "");
			if (txt.isEmpty()) {
				txt = String.valueOf(repere_ID);
			} else {
				if (Constants.debug_mode)
					Log.i(Constants.TAG,"addRepereForceRefresh() ancienne chaine 'cache_reperes_forcerefresh' : "+txt);
				txt += "$"+String.valueOf(repere_ID);
			}
		} catch (ClassCastException e) {}
		if (Constants.debug_mode)
			Log.i(Constants.TAG,"addRepereForceRefresh() nouvelle chaine 'cache_reperes_forcerefresh' : "+txt);
		editor.putString("cache_reperes_forcerefresh", txt).apply();
	}
	
	/**
	 * Enlève le repere_ID de la liste des repères dont il faut mettre à jour la liste des mesures (après un <code>SendMesuresTask</code>).
	 * @param context Contexte.
	 * @param repere_ID ID du repère.
	 */
	public static void removeRepereForceRefresh(Context context, int repere_ID) {
		SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.CACHE_MODE);
		SharedPreferences.Editor editor = prefs.edit();
		String oldstr = prefs.getString("cache_reperes_forcerefresh", "");
		if (! oldstr.isEmpty()) {
			if (Constants.debug_mode)
				Log.i(Constants.TAG,"removeRepereForceRefresh() ancienne chaine pour 'cache_reperes_forcerefresh' : "+oldstr);
			String[] flux = oldstr.split("\\$");
			String newstr="";
			for (int x=0; x<flux.length; x++) {
				if (!flux[x].isEmpty()) {
					try {
						if (Integer.parseInt(flux[x])!=repere_ID)
						newstr += flux[x]+"$";
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}
			}
			try {
				newstr=newstr.substring(0, newstr.length()-1);	//we remove trailing $
			} catch (IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
			Log.i(Constants.TAG,"removeRepereForceRefresh() nouvelle chaine pour 'cache_reperes_forcerefresh' : "+newstr);
			editor.putString("cache_reperes_forcerefresh", newstr).apply();
		}
	}
	
	/**
	 * Vérifie si la liste des mesures du repère doit être obligatoirement rafrachie depuis le serveur (après un <code>SendMesuresTask</code>).
	 * @param context Contexte.
	 * @param repere_ID ID du repère concerné.
	 * @return Nécessité d'avoir une nouvelle liste de mesures depuis le serveur.
	 */
	public static boolean needForceRefresh(Context context, int repere_ID) {
		SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.CACHE_MODE);
		//les ID sont séparés par des $ / ex : "5$45$7$23"
		try {
			return ("$"+prefs.getString("cache_reperes_forcerefresh", "")+"$").contains("$"+String.valueOf(repere_ID)+"$");
		} catch (ClassCastException e) {}
		return false;
	}
	
	/**
	 * API : méthode générique
	 */
    @Override
    public String toString() {
    	String tmp = this.nom;
    		tmp += (this.type==1?"*":"");	//astérisque si POI	//TODO améliorer !
    	//tmp += " ("+String.valueOf(this.nb_zones+this.nb_poi)+" repères)";	//on concatène les sous-éléments 
    	return tmp;
    }

}
