package org.ludvin.cartographe;


import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

/**
 * Classe permettant l'envoi d'un message au serveur en tâche de fond.
 * @param Object Context + JSONObject à envoyer au serveur doInBackground()
 * @param Integer Valeur de progression onProgessUpdate()
 * @param String Valeur de retour en fin de tâche doInBackground()
 */
public class SendMesuresTask extends AsyncTask<Object, Integer, ArrayList<JSONObject>> {
	
	//private ProgressDialog mProgressDialog;
	private Context context;
	
	protected void onPreExecute() {
		if (Constants.debug_mode)
			Log.v(Constants.TAG, "SendMesuresTask.onPreExecute()");
	}
	
	/**
	 * var[0] : Context,
	 * var[1] : ArrayList<JSONObject>
	 */
	@Override
	protected ArrayList<JSONObject> doInBackground(Object... var) {
		context = (Context) var[0];
		if (var[1] instanceof ArrayList<?>) {
			@SuppressWarnings("unchecked")
			ArrayList<JSONObject> objetsjson = (ArrayList<JSONObject>) var[1];
			ArrayList<JSONObject> reponses = new ArrayList<JSONObject>();
			String reponse;
			JSONObject objtmp;
			
			/*mProgressDialog = new ProgressDialog(context);
	        mProgressDialog.setMessage("Downloading file..");
	        mProgressDialog.setIndeterminate(true);
	        mProgressDialog.setMax(100);
	        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        mProgressDialog.setCancelable(true);
	        mProgressDialog.show();
	        */
			for (JSONObject jsonmsg : objetsjson) {
				try {
					Repere.addRepereForceRefresh(context, jsonmsg.getJSONObject("contenu").getInt("repere_ID"));
				} catch (JSONException e) {
					if (Constants.debug_mode)
						e.printStackTrace();
				}
				reponse = GlobalVars.postJsonServer(jsonmsg);
				if (Constants.debug_mode)
					Log.i(Constants.TAG,"SendMesuresTask.doInBackground envoie une mesure...");
				if (reponse!=null) {
					try {
						objtmp = new JSONObject(reponse);
						reponses.add(objtmp.getJSONObject("reponses"));
					} catch (JSONException e1) {
						if (Constants.debug_mode)
							e1.printStackTrace();
					}
				} else {
				}
			}
			return reponses;
		}
		return null;
	}
	
	protected void onPostExecute(ArrayList<JSONObject> reponses) {
		if (Constants.debug_mode)
			Log.v(Constants.TAG, "SendMesuresTask.onPostExecute() ");
		if (reponses!=null) {
			Log.v(Constants.TAG, "Reponses des envois multiples de caches : "+reponses.toString());
		}
		Mesure.setOfflineMesures(context, null);
		Toast toast = Toast.makeText(context, R.string.msg_measures_sent_to_server, Toast.LENGTH_SHORT);toast.show();
	}
	
	
    protected void createDialog(Context context, int id) {
    	ProgressDialog mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setMessage("Downloading file..");
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setMax(100);
        mProgressDialog.setProgress(id);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);
        mProgressDialog.show();
    }

}
