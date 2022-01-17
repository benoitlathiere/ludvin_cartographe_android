package org.ludvin.cartographe;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class Position {
	
	private String borne;
	private int ID;
	private int type;	//0=zone, 1=POI, d'après base de données
	private int repere_ID;
	private String compte;
	private int nb;
	private String datejour;
	
	
	public Position(String compte, int nb, String datejour) {
		super();
		this.compte=compte;
		this.nb=nb;
		this.datejour=datejour;
	}
	
	
	/**
	 * OBSOLETE !
	 * @param borne
	 * @param ID
	 * @param type
	 * @param repere_ID
	 */
	public Position(String borne, int ID, int type, int repere_ID) {
		super();
		this.borne=borne;
		this.ID=ID;
		this.repere_ID=repere_ID;
		this.type=type;
	}
	
	public String getBorne() {
		return this.borne;
	}
	
	public int getId() {
		return this.ID;
	}
	
	public int getRepere_ID() {
		return this.repere_ID;
	}
	
	public int getType() {
		return this.type;
	}
	
	/**
	 * Transforme une réponse JSON simple en ArrayList
	 * @param json Données formatées en JSON
	 * @return Liste d'objets
	 */
	public static ArrayList<Position> JSON2positions(String json) {
		ArrayList<Position> ArPositions = new ArrayList<Position>();
		try {
			JSONObject jstmp;
			JSONObject jObject=new JSONObject(json);
			JSONArray jsobjets = jObject.getJSONArray("positions");
			for (int i = 0; i<=jsobjets.length();i++) {
				jstmp = jsobjets.getJSONObject(i);
				if (jstmp != null) {
					String compte = jstmp.getJSONObject("position").optString("compte");
					int nb = jstmp.getJSONObject("position").optInt("nb");
					String datejour = jstmp.getJSONObject("position").optString("datejour");
					ArPositions.add(new Position(compte, nb, datejour));
				}
			}
        } catch (JSONException je) {
        	je.printStackTrace();
        	if (Constants.debug_mode)
        		Log.d(Constants.TAG, "JSON Exception de GlobalVars.JSON2positions()");
        }
		return ArPositions;
	}

	
	/**
	 * API : méthode générique
	 */
    @Override
    public String toString() {
    	String tmp = this.compte+" ("+this.nb+" mesures) le "+this.datejour;
    	return tmp;
    }

}
