package org.ludvin.cartographe;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**	//non utilisé pour le moment
 * 
 * Formatage de données JSON. 
 * Pour utiliser la classe JsonIO dans une activity :
 * 	1) instancier un nouveau JsonIO dans le onCreate.
 *  2) appeler une des fonctions publiques pour construire le json :
 *  	jsonObj = nouvBatiment(getApplicationContext(), nom),
 *  	appeler le sendJson de l'activity (new sendJson().execute(jsonObj1, jsonObj2, ...)),
 *  	le context est facultatif.
 *  3) 42.
 * @author Johana Bodard
 */
public class JsonIO {

	public Boolean debug_mode=false;	//active les msg verbeux vers Log.d() pour la classe

	public JSONObject getListeBatimentJSON(Context context) throws JSONException {
		//Context appContext = (Context) context;
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("action", "CARTOGRAPHE_LISTE_BATIMENT");
		return jsonObj;
	}
	
	public JSONObject getListeNiveauxJSON(Context context, int id_bat) throws JSONException {
		//Context appContext = (Context) context;
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("action", "CARTOGRAPHE_LISTE_NIVEAU");
		jsonObj.put("id_bat", id_bat);
		return jsonObj;
	}
	
	public JSONObject getListeZonesJSON(Context context, int id_eta) throws JSONException {
		//Context appContext = (Context) context;
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("action", "CARTOGRAPHE_LISTE_BATIMENT");
		jsonObj.put("id_eta", id_eta);
		return jsonObj;
	}
	
	public JSONObject nouvBatimentJSON(Context context, String nom) throws JSONException {
		//Context appContext = (Context) context;
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("action", "CARTOGRAPHE_NOUVEAU_BATIMENT");
		jsonObj.put("nom", nom);
		return jsonObj;
	}
	
	public JSONObject supprBatimentJSON(Context context, int id_bat) throws JSONException {
		//Context appContext = (Context) context;
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("action", "CARTOGRAPHE_SUPPRIMER_BATIMENT");
		jsonObj.put("id_bat", id_bat);
		return jsonObj;
	}
	
	public JSONObject nouvZoneJSON(Context context, int id_eta, String nom, String comment, boolean urgence, boolean poi) throws JSONException {
		//Context appContext = (Context) context;
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("action", "CARTOGRAPHE_NOUVELLE_ZONE");
		jsonObj.put("id_eta", id_eta);
		jsonObj.put("nom", nom);
		jsonObj.put("comment", comment);
		jsonObj.put("urgence", urgence);
		jsonObj.put("poi", poi);
		return jsonObj;
	}
	
	public JSONObject supprZoneJSON(Context context, int id_zone) throws JSONException {
		//Context appContext = (Context) context;
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("action", "CARTOGRAPHE_SUPPRIMER_ZONE");
		jsonObj.put("id_bat", id_zone);
		return jsonObj;
	}
	
	/**
	 * Envoi d'un flux JSON au serveur (fonctionne).
	 * @param jsonMsg Message JSON à envoyer.
	 * @return Message retour du serveur (ou null si erreur)
	 */
	/*
	private String postData(JSONObject jsonMsg) {
		HttpClient httpclient= new DefaultHttpClient();
		HttpPost httppost= new HttpPost(Constants.ws_json_url);
		try {
			//fonctionne : (source : http://www.codeproject.com/Articles/267023/Send-and-receive-json-between-android-and-php)
		    httppost.setEntity(new ByteArrayEntity(jsonMsg.toString().getBytes("UTF8")));
		    //httppost.addHeader("Content-Encoding", "gzip");	//compresser sinon data trop gros - par défaut
		    httppost.addHeader("Accept-Encoding", "gzip");	//on accepte aussi la compression en retour
		    StringEntity stringEntity = new StringEntity(jsonMsg.toString(),"UTF-8");
		    stringEntity.setContentType("application/json;charset=UTF-8");
		    stringEntity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json;charset=UTF-8"));
		    httppost.setEntity(stringEntity);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			if (this.debug_mode)
				Log.d(Constants.TAG,"ERREUR postData() : "+e.toString());
		}
		try {
			HttpResponse response = httpclient.execute(httppost);
			if(response != null) {
				InputStream is = response.getEntity().getContent();
				
				Header contentEncoding = response.getFirstHeader("Content-Encoding");	//source : http://stackoverflow.com/questions/1573391/android-http-communication-should-use-accept-encoding-gzip
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
						if (this.debug_mode)
							Log.d(Constants.TAG,line);	//debug
					}
				} catch (IOException e) {
					e.printStackTrace();
					if (this.debug_mode)
						Log.d(Constants.TAG,"ERREUR postData() : "+e.toString());
				} finally {
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
						if (this.debug_mode)
							Log.d(Constants.TAG,"ERREUR : "+e.toString());
					}
				}
				return sb.toString();
			}
			if(this.debug_mode)
				Log.d(Constants.TAG, "réponse serveur null 1");
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			if (this.debug_mode)
				Log.d(Constants.TAG,"ERREUR : "+e.toString());
		} catch (IOException e) {
			e.printStackTrace();
			if (this.debug_mode)
				Log.d(Constants.TAG,"ERREUR : "+e.toString());
		}
		if (this.debug_mode)
			Log.d(Constants.TAG, "reponse serveur null 2");
		return null;
	}*/
	
	
	public void envoiAsync(JSONObject jsonMsg) {
		new sendJSON().execute(jsonMsg);
	}
	
	public void testRetour(String retour) {
		Log.d(Constants.TAG,"---------ATTENTION LES YEUX : "+retour);
	}
	
	/**
	 * Envoi d'un message JSON en tâche de fond (inutile ici ?).
	 * @author Johana Bodard
	 * @param JSONObject Message JSON à envoyer au serveur doInBackground(JSONObject)
	 * @param Integer Valeur de progression onProgessUpdate(int)
	 * @param String Valeur de retour en fin de tâche doInBackground()
	 */
	private class sendJSON extends AsyncTask<JSONObject, Integer, String> {
		
		protected void onPreExecute() {	
			//do some setup here
		}
		
		@Override
		protected String doInBackground(JSONObject... jsonMsg) {
			Log.d(Constants.TAG, "doInBackground() : "+jsonMsg);
			HttpClient httpclient= new DefaultHttpClient();
			HttpPost httppost= new HttpPost(GlobalVars.urlServer());
			
			//publishProgress((int) ((i / (float) count) * 100));	//exemple
			
			try {
				//fonctionne : (source : http://www.codeproject.com/Articles/267023/Send-and-receive-json-between-android-and-php)
			    httppost.setEntity(new ByteArrayEntity(jsonMsg.toString().getBytes("UTF8")));
			    //httppost.addHeader("Content-Encoding", "gzip");	//compresser - par défaut
			    httppost.addHeader("Accept-Encoding", "gzip");	//on accepte aussi la compression en retour
			    StringEntity stringEntity = new StringEntity(jsonMsg.toString(),"UTF-8");
			    Log.d(Constants.TAG, "XXXX"+jsonMsg.toString());
			    stringEntity.setContentType("application/json;charset=UTF-8");
			    stringEntity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,"application/json;charset=UTF-8"));
			    httppost.setEntity(stringEntity);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				if (Constants.debug_mode)
					Log.d(Constants.TAG,"ERREUR postData() : "+e.toString());
			}
			try {
				HttpResponse response = httpclient.execute(httppost);
				if(response != null) {
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
							if (Constants.debug_mode)
								Log.d(Constants.TAG,line);	//debug
						}
					} catch (IOException e) {
						e.printStackTrace();
						if (Constants.debug_mode)
							Log.d(Constants.TAG,"ERREUR postData() : "+e.toString());
					} finally {
						try {
							is.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
					return sb.toString();
				}
				if(Constants.debug_mode)
					Log.d(Constants.TAG, "réponse serveur null 1");
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (Constants.debug_mode)
				Log.d(Constants.TAG, "reponse serveur null 2");
			return null;
		}
		
		protected void onPostExecute(String result) {
			//do some post execution stuff like informing the user about the result of the execution
			Log.d(Constants.TAG, "onPostExecute()");
			testRetour(result);
		}
	}
	
	
	/**
	 * Construit une borne au format JSON
	 * @author Johana Bodard
	 * @param bssid Nom de la borne (optionnel)
	 * @param mac Adresse MAC de la borne
	 * @param signal Force du signal en dBm
	 * @param frequence Fréquence utilisé par le signal en GHz
	 * @param date Date au format ISO
	 * @return Message JSON complet à envoyer au serveur.
	 */
	public static JSONObject buildJsonAP(String bssid, String mac, int signal, Double frequence, String date) {
		JSONObject AP = new JSONObject();
		try {
			AP.put("nom", bssid);
			AP.put("MAC", mac);
			AP.put("signal", signal);
			AP.put("frequence", frequence);
			AP.put("date", date);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return AP;
	}

	
}
