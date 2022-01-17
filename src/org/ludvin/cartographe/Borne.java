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

public class Borne {

	private static SharedPreferences prefs;
	
	private String nom;
	private String mac;
	private Double frequence;
	private int signal;
	private String date;
	private Calendar calendrier;


	public Borne(String nom, String mac, Double frequence, int signal, Context context) {
		calendrier = new GregorianCalendar();
		//prefs
		prefs = context.getSharedPreferences(Constants.PREFS_FILENAME, 0);
		this.nom = nom;
		this.mac = mac;
		this.frequence = frequence;	//en GHz
		this.signal = signal;		//en dBm
		try {
			//valeurs sur 2 chiffres
			int mois = calendrier.get(Calendar.MONTH)+1;	//   /!\ Calendar.MONTH is zero-based !!
			int jour = calendrier.get(Calendar.DAY_OF_MONTH);
			int heure = calendrier.get(Calendar.HOUR_OF_DAY); 
			int min = calendrier.get(Calendar.MINUTE);
			int sec = calendrier.get(Calendar.SECOND);
			this.date = calendrier.get(Calendar.YEAR) + "-" + String.format("%02d", mois) + "-" + String.format("%02d", jour) + " " + String.format("%02d", heure) + ":" + String.format("%02d", min) + ":" + String.format("%02d", sec);
		} catch (IllegalArgumentException e) {
			if (Constants.debug_mode)
				e.printStackTrace();
			this.date = "";
		} catch (NullPointerException e) {
			if (Constants.debug_mode)
				e.printStackTrace();
			this.date = "";
		} catch (ArrayIndexOutOfBoundsException e) {
			if (Constants.debug_mode)
				e.printStackTrace();
			this.date = "";
		}
	}

	
	/**
	 * Retourne le nom (SSID) de la borne.
	 * @return SSID de la borne
	 */
	public String getSSID() {
		return this.nom;
	}
	

	/**
	 * Retourne la force du signal de la borne.
	 * @return Force du signal en dBm
	 */
	public int getSignal() {
		return this.signal;
	}
	
	
	/**
	 * Retourne l'adresse MAC de la borne.
	 * @retun	Adresse MAC
	 */
	public String getMac() {
		return this.mac;
	}
	
	
	/**
	 * Retourne la fréquence d'emission de la borne.
	 * @retun	Fréquence en GHz
	 */
	public Double getFrequence() {
		return this.frequence;
	}	

	
	/**
	 * Retourne la date/heure où le signal a été enregistré (OBSOLETE).
	 * @retun	Date/heure
	 */
	/*
	public String getDate() {
		return this.date. toLocaleString();
	}*/	
	

	
	/**
	 * Retourne la date/heure au format ISO dusignal enregistré.
	 * @retun	Date/heure au format YYYY-MM-DD HH:MM:SS
	 */
	public String getDateISO() {
		//return this.date.getYear()+"-"+this.date.getMonth()+"-"+this.date.getDay()+" "+this.date.getHours()+":"+this.date.getMinutes()+":"+this.date.getSeconds();
		return this.date;
	}	
	
	
	/**
	 * Retourne un texte contenant les éléments pertinents de la borne.
	 * @return Texte formaté des éléments
	 */
	public String toString() {
		return this.nom+" ("+this.mac+")"+this.signal+" dBm (à "+this.date+")";
	}
	
	
	/**
	 * Parcours des bornes enregistrées pour créer les objets JSON.
	 * @param Bornes Ensemble des mesures Wifi faites.
	 * @return Ensemble des borne mesurées (ou null si erreur).
	 */
	public static JSONArray parcoursBornes(ArrayList<Borne> Bornes) {
		JSONObject jglobal = new JSONObject();	//JSON global
		JSONArray jsonAPs = new JSONArray();	//tableau des mesures
		//on boucle sur toutes les bornes enregistrées pour construire un objet JSON
		if (Bornes.size()>0) {
			Borne borne = null;				
			//on boucle sur les mesures enregistrées :
			for (int a=0; a < Bornes.size(); a++) {
				borne = Bornes.get(a);
				jsonAPs.put( JsonIO.buildJsonAP(borne.getSSID(), borne.getMac(), borne.getSignal(), borne.getFrequence(), borne.getDateISO()) );
			}
			Bornes.clear();
			
			//on construit le JSON global :
			try {
				jglobal.put("repere", prefs.getInt("tmp_repere_id",0));
				jglobal.put("mobile", GlobalVars.getDeviceName());
				jglobal.put("bornes", jsonAPs);	//les mesures
			} catch (JSONException e) {
				e.printStackTrace();
				if(Constants.debug_mode)
					Log.d(Constants.TAG, "ERREUR parcoursBornes() JSON global : "+e.toString()); //DEBUG
				return null;
			} catch (ClassCastException e) {
				e.printStackTrace();
				if(Constants.debug_mode)
					Log.d(Constants.TAG, "ERREUR parcoursBornes() prefs : "+e.toString()); //DEBUG
			}
			return jsonAPs;	//FIXME on retourne le array plutôt que l'objet global
		} else {
			return null;
		}
	}
	
	

}
