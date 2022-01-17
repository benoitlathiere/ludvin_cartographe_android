package org.ludvin.cartographe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

/**
 * Classe permettant de définir des méthodes et variables globales à l'application
 * Source : http://stackoverflow.com/questions/708012/android-how-to-declare-global-variables
 * @author Projet LUD'VIN 2013 - www.ludvin.org - Paris 8 / Master Handi
 */
public class GlobalVars extends Application{
	
	/**
	 * IMPORTANT : Les constantes sont à mettre dans l'interface Constants.java
	 * Utiliser les getter et setter plutôt que d'appeler les constantes directement.
	 */

	
	/**
	 * Retourne l'URL du Web Service en vigueur (selon état DEMO).
	 * @return Url du WS.
	 */
	 public static String urlServer() {
		if (Constants.MODE_DEMO)
			return Constants.ws_carto_json_DEMO;
		else
			return Constants.ws_carto_json;
	}

	 
	/**
	 * Connexion au serveur pour envoi requête et réception réponse (nouvelle version juin 2013).
	 * @param jsonMsg Message au format JSON à envoyer au serveur.
	 * @return Données retournées par le serveur (ou null si erreur).
	 */
	public static String postJsonServer(JSONObject jsonMsg){
		HttpClient httpclient= new DefaultHttpClient();
		try {
			HttpPost httppost= new HttpPost(GlobalVars.urlServer());
			try {
				//source : http://www.codeproject.com/Articles/267023/Send-and-receive-json-between-android-and-php
			    httppost.setEntity(new ByteArrayEntity(jsonMsg.toString().getBytes("UTF8")));
			    httppost.addHeader("Accept-Encoding", "gzip");	//on accepte aussi la compression en retour
			    StringEntity stringEntity = new StringEntity(jsonMsg.toString(),"UTF-8");
			    stringEntity.setContentType("application/json;charset=UTF-8");
			    stringEntity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json;charset=UTF-8"));
			    httppost.setEntity(stringEntity);
			} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
			}
			try {
				HttpResponse response = httpclient.execute(httppost);
				if(response != null) {
					try {
						InputStream is = response.getEntity().getContent();
						Header contentEncoding = response.getFirstHeader("Content-Encoding");	//source : http://stackoverflow.com/questions/1573391/android-http-communication-should-use-accept-encoding-gzip
						//si data compressé
						if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
							try {
								is = new GZIPInputStream(is);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
						BufferedReader reader = new BufferedReader(new InputStreamReader(is));
						StringBuilder sb = new StringBuilder();
						String line = null;
						try {
							while ((line = reader.readLine()) != null) {
								sb.append(line + "\n");
							}
						} catch (IOException e) {
							if (Constants.debug_mode)
								e.printStackTrace();
						} finally {
							try {
								is.close();
							} catch (IOException e) {
								if (Constants.debug_mode)
									e.printStackTrace();
							}
						}
						return sb.toString();
					} catch (IllegalStateException e) {
						if (Constants.debug_mode)
							e.printStackTrace();
					}
					return null;
				} else if(Constants.debug_mode)
					Log.e(Constants.TAG, "ERREUR : réponse serveur null");
			} catch (HttpResponseException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			} catch (ClientProtocolException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			} catch (IOException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			}
			return null;
		} catch (IllegalArgumentException e) {
			if (Constants.debug_mode)
				e.printStackTrace();
		}
		return null;
	}
	

	
	/**
	 * Retourne le modèle du mobile. 
	 * Source : http://stackoverflow.com/questions/14030223/android-device-name
	 * @return	Modèle du mobile (ou constructeur si erreur).
	 */
	public static String getDeviceName() {
		String manufacturer = Build.MANUFACTURER;
		String model = Build.MODEL;
		try {
			if (model.startsWith(manufacturer)) {
				return model;
			} else {
				return manufacturer + " " + model;
			}
		} catch (NullPointerException  e) {
			return model;
		}
	}
	
	/**
	 * Analyse et met en forme un message informatif de retour (ex: ERREUR: Serveur indisponible)
	 * @author Benoit Lathiere
	 * @param msg Message informatif retourné par le serveur
	 * @param type (optionnel) Filtre sur le type de message retourné (ERREUR|INFO)
	 * @return
	 */
	public static String MsgRetour(String msg, String type) {
		if (msg!=null & !msg.isEmpty()) {
			if (type==null) {
				return msg.substring(msg.indexOf(":")+1);
			} else {
				if (msg.contains(type)) {
					return msg.substring(msg.indexOf(":")+2);
				}
			}
		}
		return null;
	}

	
	/**
	 * Récupère l'identifiant du nouvel élément créé //FIXME (utile ?).
	 * @param msg Message contenant le nouvel identifiant
	 * @return Nouvel identifiant, ou 0 si erreur
	 */
	private int NouvelId(String msg) {
		if (msg.contains("ID=")) {
			try {
				String tmp = msg.substring(msg.indexOf("ID=")+3);
				tmp = tmp.replaceAll("\\D+","");
				try {
					return Integer.parseInt(tmp);
				} catch (NumberFormatException e) {
					return 0;
				}
			} catch (IndexOutOfBoundsException e) {
				return 0;
			}
		} else {
			return 0;
		}
	}
	
	
	/**
	 * Récupère l'identifiant du nouvel élément créé sur le serveur en analysant la réponse JSON //FIXME (utile ?).
	 * @param json Réponse JSON du serveur (contenant la clé ID).
	 * @return Nouvel identifiant, ou 0 si erreur.
	 */
	private int NouvelIdJSON(String json) {
		if (!json.isEmpty() && json!=null) {
			try {
				JSONObject jstmp;
				JSONObject jObject=new JSONObject(json);
				JSONArray jsobjets = jObject.getJSONArray("reponses");
				for (int i = 0; i<jsobjets.length();) {
					jstmp = jsobjets.getJSONObject(i);
					String nom = jstmp.getJSONObject("reponse").optString("message");
					if (nom.contains("OK")) {
						int aa =jstmp.getJSONObject("reponse").optInt("ID");
						return aa;
					}
				}
		    } catch (Exception e) {
		        e.printStackTrace();
		        if (Constants.debug_mode)
		        	Log.d(Constants.TAG,e.getLocalizedMessage());
		    }
		}
		return 0;
	}
	

	
	/**
	 * Convertie une fréquence MHz en GHz
	 * @author Benoit Lathiere
	 * @param freq	Fréquence en MHz
	 * @return Fréquence en GHz avec décimales
	 */
	@SuppressLint("DefaultLocale")
	public static double convertFreq(int freq) {
		return (double) (freq/1000.0);
	}
	
	/**
	 * Teste la connexion à Internet. 
	 * Necessite la permission <code>android.permission.ACCESS_NETWORK_STATE</code>. 
	 * @param context Contexte.
	 * @return <code>true</code> s'il y a une connexion valide à Internet, sinon <code>false</code>.
	 */
	public static boolean isNetworkAvailable(Context context) {
		if (context != null) {
		    ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		    NetworkInfo networkInfo = cm.getActiveNetworkInfo();
		    if (networkInfo != null && networkInfo.isConnected())
		        return true;
		}
	    return false;
	}

	/**
	 * Quitter l'activité.
	 */
	public static void quitter() {
		System.exit(0);
	}
	

	/**
	 * Supprime les valeurs des objets en cache (batiment, niveau, repere, mesure).
	 * @return 
	 */
	public static void cleanCache(Context context) {
		if (context != null) {
			Log.v(Constants.TAG,"cleanCache()");
			SharedPreferences cache = context.getSharedPreferences(Constants.CACHE_FILENAME, MODE_PRIVATE);
			Editor editor = cache.edit();
			Toast toast;
			if (editor.clear().commit()) {
				toast = Toast.makeText(context, "Le cache a été vidé.", Toast.LENGTH_SHORT);
			} else {
				toast = Toast.makeText(context, "Le cache n'a pu être vidé.", Toast.LENGTH_SHORT);
			}
			toast.show();
		}
	}
	
	/**
	 * retourne le numéro de version en 3 entiers (majeur, mineur, autre).
	 * @param version Chaine contenant le numéro de version en 3 parties séparées par un point (ex : 1.2.3).
	 * @return Liste de 3 entiers (ou <code>null</code> si erreur).
	 */
	public static List<Integer> convertVersionNumbers(String version) {
		if (version!=null && !version.isEmpty()) {
			List<Integer> nums = new ArrayList<Integer>();
			try {
				String[] txt = version.split("\\.");
				for(int i=0; i < txt.length ; i++)
					nums.add(Integer.parseInt(txt[i]));
				return nums;
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (UnsupportedOperationException e) {
				e.printStackTrace();
			} catch (NullPointerException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (ClassCastException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	
	/**
	 * Méthodes fournissant les préférences de l'utilisateur (ou valeur système par défaut).
	 */
	
	
	/**
	 * Durée de vie du cache. 0=pas de cache, -1=éternel (défaut : <code>Constants.CACHE_LIFETIME</code>).
	 * @param context Contexte.
	 * @return Durée en secondes.
	 */
	public static long getCacheLifetime(Context context) {
		if (context!=null) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, Constants.PREFS_MODE);
			try {
				return prefs.getLong("user_cache_lifetime", Constants.CACHE_LIFETIME)*60;
			} catch (ClassCastException e) {}
		} else if (Constants.debug_mode) {
			Log.e(Constants.TAG, "getCacheLifetime(), context="+String.valueOf(context));
		}
		return Constants.CACHE_LIFETIME*60;
	}
	
	/**
	 * Nettoyage des valeurs en cache au démarrage de l'application (défaut: Constants.CACHE_CLEAN_ONSTART).
	 * @param context Contexte.
	 * @return Nettoyage ou non du cache.
	 * @see #setCleanCache
	 */
	public static boolean getCleanCache(Context context) {
		if (context!=null) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, Constants.PREFS_MODE);
			try {
				return prefs.getBoolean("user_cache_clean_onstart", Constants.CACHE_CLEAN_ONSTART);
			} catch (ClassCastException e) {}
		}
		return Constants.CACHE_CLEAN_ONSTART;
	}
	
	/**
	 * Nettoie le cache (si autorisé par getCacheOnStart()).
	 * @param context Contexte.
	 * @see #getCleanCache
	 */
	public static void setCleanCache(Context context) {
		if (context!=null) {
			if (getCleanCache(context)) {
				SharedPreferences cache = context.getSharedPreferences(Constants.CACHE_FILENAME, MODE_PRIVATE);
				cache.edit().clear().apply();
			}
		}
	}

	/**
	 * Délai entre deux scans pendant le relevés de mesures.
	 * @return Délai en secondes.
	 * @see #getWifiNbScans
	 */
	public static int getWifiDelay() {
		return Constants.wifi_nb_scans;
	}
	
	/**
	 * Nombre de scans à effectuer pendant le relevés de mesures (defaut : <code>Constant.wifi_nb_scans</code>).
	 * @param context Contexte.
	 * @return Nombre de scans avant envoi au serveur.
	 * @see #getWifiDelay
	 */
	public static int getWifiNbScans(Context context) {
		if (context!=null) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, Constants.PREFS_MODE);
			try {
				return prefs.getInt("user_wifi_nbscans", Constants.wifi_nb_scans);
			} catch (ClassCastException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			}
		}
		return Constants.wifi_nb_scans;
	}
	
	/**
	 * Indique si l'utilisateur veut enregistrer ses identifiant/mot de passe dans l'application. 
	 * @param context Contexte.
	 * @return Etat du souhait de conserver les données (<code>false</code> si erreur).
	 * @see setUserKeep
	 */
	public static boolean getUserKeep(Context context) {
		if (context!=null) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, Constants.PREFS_MODE);
			try {
				return prefs.getBoolean("user_keep", false);
			} catch (ClassCastException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			}
		}
		return false;
	}
	
	/**
	 * Enregistrement du souhait de converser les identifiant/mot de passe de l'utilisateur.
	 * @param context Contexte.
	 * @param keepcnx Etat du souhait de conserver les données.
	 * @see getUserKeep
	 */
	public static void setUserKeep(Context context, boolean keepcnx) {
		if (context!=null) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, Constants.PREFS_MODE);
			prefs.edit().putBoolean("user_keep", keepcnx).commit();
		}
	}
	
	/**
	 * Retourne l'identifiant enregistré de l'utilisateur (si autorisé au préalable).
	 * @param context Contexte
	 * @return Identifiant (ou vide).
	 */
	public static String getUserLogin(Context context) {
		if (context!=null) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, Constants.PREFS_MODE);
			try {
				return prefs.getString("store_user_login", "");
			} catch (ClassCastException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			}
		}
		return "";
	}
	
	/**
	 * Enregistre l'identifiant de l'utilisateur (vérifier autorisation au préalable).
	 * @param context Contexte.
	 * @param login Identifiant.
	 * @see #getUserLogin
	 * @see #getUserKeep
	 */
	public static void setUserLogin(Context context, String login) {
		if (context!=null && login!=null && !login.isEmpty()) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, Constants.PREFS_MODE);
			prefs.edit().putString("store_user_login", login).commit();
		}
	}
	
	/**
	 * Retourne le mot de passe enregistré de l'utilisateur (si autorisé au préalable).
	 * @param context Contexte.
	 * @return Mot de passe (ou vide).
	 * @see #setUserPass
	 */
	public static String getUserPass(Context context) {
		if (context!=null) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, Constants.PREFS_MODE);
			try {
				return prefs.getString("store_user_pass", "");
			} catch (ClassCastException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			}
		}
		return "";
	}
	
	/**
	 * Enregistre le mot de passe de l'utilisateur (vérifier autorisation au préalable).
	 * @param context Contexte.
	 * @param pass Mot de passe.
	 * @see #getUserPass
	 * @see #getUserKeep
	 */
	public static void setUserPass(Context context, String pass) {
		if (context!=null && pass!=null) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, Constants.PREFS_MODE);
			prefs.edit().putString("store_user_pass", pass).commit();
		}
	}
	
	/**
	 * Retourne le UID de l'utilisateur (si correctement identifié).
	 * @param context Contexte.
	 * @return UID (ou 0 si erreur).
	 * @see #setUserID
	 */
	public static int getUserID(Context context) {
		if (context!=null) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, Constants.PREFS_MODE);
			try {
				return prefs.getInt("store_user_id", 0);
			} catch (ClassCastException e) {
				if (Constants.debug_mode)
					e.printStackTrace();
			}
		}
		return 0;
	}
	
	/**
	 * Enregistre le UID de l'utilisateur.
	 * @param context Contexte.
	 * @param ID UID.
	 * @see #getUserID
	 */
	public static void setUserID(Context context, int UID) {
		if (UID !=0 && context!=null) {
			SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, Constants.PREFS_MODE);
			prefs.edit().putInt("store_user_id", UID).commit();
		}
	}

}
