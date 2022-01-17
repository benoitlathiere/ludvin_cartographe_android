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


public class Niveau {

	private String nom;
	private int ID;
	private int batiment_ID;	//for compatibility
	private int nb_reperes;
	private Batiment batiment;	//bâtiment parent
	
	public Niveau(String nom, int ID, int nb_reperes, int batiment_ID, Batiment batiment) {
		super();
		this.nom=nom;
		this.ID=ID;
		this.batiment_ID=batiment_ID;
		this.nb_reperes=nb_reperes;
		this.batiment=batiment;
	}
	
	public String getNom() {
		return this.nom;
	}
	
	public int getId() {
		return this.ID;
	}
	
	public int getBatiment_ID() {
		return this.batiment_ID;
	}
	
	public Batiment getBatiment() {
		return this.batiment;
	}
	
	public int getNb_reperes() {
		return this.nb_reperes;
	}
	
	/**
	 * Créer un objet JSON générique pour lister les niveaux.
	 * @param context Contexte.
	 * @param batiment_ID ID du bâtiment parent.
	 * @return Objet JSON à envoyer au serveur (ou null si erreur).
	 */
	public static JSONObject getJSONList(Context context, int batiment_ID) {
		if (context!=null && batiment_ID!=0) {
			try {
				JSONObject msgjson = new JSONObject();
				JSONObject jsoncontenu = new JSONObject();
				msgjson.put("objet", "niveau");
				msgjson.put("action", "lister");
					jsoncontenu.put("batiment_ID",batiment_ID);
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
	 * Créer un objet JSON générique pour créer un niveau.
	 * @param context Contexte.
	 * @param name Nom du nouveau niveau.
	 * @param batiment_ID ID du bâtiment parent.
	 * @return Objet JSON à envoyer au serveur (ou null si erreur).
	 */
	public static JSONObject getJSONCreate(Context context, String name, int batiment_ID) {
		if (context!=null && !name.isEmpty() && batiment_ID!=0) {
			try {
				JSONObject msgjson = new JSONObject();
				JSONObject jsoncontenu = new JSONObject();
				msgjson.put("objet", "niveau");
				msgjson.put("action", "creer");
					jsoncontenu.put("batiment_ID",batiment_ID);
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
	 * Créer un objet JSON générique pour supprimer un niveau.
	 * @param context Contexte.
	 * @param id ID du niveau à supprimer.
	 * @return Objet JSON à envoyer au serveur (ou null si erreur).
	 */
	public static JSONObject getJSONDelete(Context context, int id) {
		if (context!=null && id!=0) {
			try {
				JSONObject msgjson = new JSONObject();
				JSONObject jsoncontenu = new JSONObject();
				msgjson.put("objet", "niveau");
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
	 * Créer un objet JSON générique pour renommer un niveau.
	 * @param context Contexte.
	 * @param id ID du niveau à renommer.
	 * @param oldname Ancien nom.
	 * @param newname Nouveau nom.
	 * @return Objet JSON à envoyer au serveur (ou null si erreur).
	 */
	public static JSONObject getJSONRename(Context context, int id, String oldname, String newname) {
		if (context!=null && id!=0 && !oldname.isEmpty() && !newname.isEmpty()) {
			try {
				JSONObject msgjson = new JSONObject();
				JSONObject jsoncontenu = new JSONObject();
				msgjson.put("objet", "niveau");
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
	 * Transforme une réponse JSON simple en ArrayList.
	 * @param json Données formatées en JSON (objet <code>reponses</code>).
	 * @param batiment Objet <code>Batiment</code>.
	 * @return Liste d'objets (ou <code>null</code> si erreur).
	 */
	public static ArrayList<Niveau> JSON2niveaux(JSONObject jObject, Batiment batiment) {
		if (jObject!=null ) {	//FIXME && batiment!=null
			ArrayList<Niveau> ArNiveaux = new ArrayList<Niveau>();
			try {
				int nb=jObject.getInt("nb");
				if (nb>0) {
					JSONObject jstmp;
					JSONArray jsobjets = jObject.getJSONArray("niveaux");
					for (int i=0; i<jsobjets.length();i++) {
						try {
							jstmp = jsobjets.getJSONObject(i);
							if (jstmp != null) {
								int ID = jstmp.getJSONObject("niveau").optInt("ID");
								String nom = jstmp.getJSONObject("niveau").optString("nom");
								int nb_reperes = jstmp.getJSONObject("niveau").optInt("nb_reperes");
								int batiment_ID = jstmp.getJSONObject("niveau").optInt("batiment_ID");
								ArNiveaux.add(new Niveau(nom, ID, nb_reperes, batiment_ID, batiment ));	//FIXME remplacer le null
							}
						} catch (JSONException js) {
							if (Constants.debug_mode) {
								Log.e(Constants.TAG,"Niveau.JSON2Niveaux : JSONException dans boucle for() ");
								js.printStackTrace();
							}
				        }
					}
				} else {
					return null;
				}
	        } catch (JSONException je) {
	        	if (Constants.debug_mode) {
	        		Log.e(Constants.TAG,"Niveau.JSON2niveaux() : JSONException niveau 1. objet="+jObject.toString());
	        		je.printStackTrace();
	        	}
	            return null;
	        }
			return ArNiveaux;
			}
		return null;
	}
	
	/**
	 * Mise en cache d'un flux d'objets + la date du flux.
	 * @param context Contexte.
	 * @param json Flux JSON global du serveur.
	 * @param parent_ID Identifiant de l'élément parent.
	 * @return <code>False</code> si erreur de mise en cache sinon <code>True</code>.
	 */
	public static boolean setCacheList(Context context, JSONObject json, int parent_ID) {
		if (context!=null && json!=null && parent_ID!=0) {
			String x = String.valueOf(parent_ID);
			if (json!=null) {
				long date=0;
				try {
					date=(long) json.getLong("date");
				} catch (JSONException e) {
					e.printStackTrace();
				}
				SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.CACHE_MODE);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putLong("cache_niveau_"+x+"_date", date);
				editor.putString("cache_niveau_"+x, json.toString());
				return editor.commit();
			}
		}
		return false;
	}
	
	/**
	 * Récupère les objets depuis le cache.
	 * @param context Contexte.
	 * @param parent_ID Identifiant de l'élément parent.
	 * @return Retourn le JSON "reponses" (ou null).
	 */
	public static JSONObject getCacheList(Context context, int parent_ID) {
		if (context!=null && parent_ID !=0) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.CACHE_MODE);
			String x = String.valueOf(parent_ID);
			try {
				String tmp = prefs.getString("cache_niveau_"+x, null);
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
		if (context!=null && parent_ID !=0) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.CACHE_FILENAME, Constants.CACHE_MODE);
			Calendar cal = new GregorianCalendar();
			String x = String.valueOf(parent_ID);
			try {
				long datecache = prefs.getLong("cache_niveau_"+x+"_date", 0);
				long datenow = cal.getTimeInMillis()/1000;
				if (datecache+GlobalVars.getCacheLifetime(context) > datenow)	//cache valide
					return true;
			} catch (ClassCastException e) { 
				if (Constants.debug_mode)
					e.printStackTrace();
			} catch (IllegalArgumentException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			}
		} else if (Constants.debug_mode) {
    		Log.e(Constants.TAG, "Mesure.cacheRecent(), erreur, debug : context="+String.valueOf(context)+" / parent_ID="+String.valueOf(parent_ID)+ " / getCacheLifetime()="+String.valueOf(GlobalVars.getCacheLifetime(context)));
		}
		return false;
	}
	
	/**
	 * API : méthode générique, appelée par l'Adapter pour la ListView
	 */
    @Override
    public String toString() {
    	String tmp = this.nom;
    	//if (this.nb_reperes>0)	//on concatène le nb de sous-éléments
    	//	tmp += " ("+this.nb_reperes+" repères)"; 
    	return tmp;
    }
}