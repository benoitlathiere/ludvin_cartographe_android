package org.ludvin.cartographe;

/**
 * Constantes globales à l'application
 * @author Benoit Lathière
 *
 */

interface Constants {
	
	/** A MODIFIER selon besoins (debug, prod, demo...) **/
	public final boolean MODE_DEMO = false;						//si appli compilée pour une démo (utilise une base bac à sable 'mysql:ludvin_demo' )
	public static final boolean debug_mode = true;				//active les msg debug/verbose vers Log()
	public static final boolean CACHE_CLEAN_ONSTART = false;	//on supprime le cache au démarrage de l'application (TRUE conseillé pour DEMO), voir GlobalVars.getCleanCache().

	
	/**
	 * Certaines valeurs sont changées par l'utilisateur, 
	 * utiliser les getter/setter de la (future) classe UserPrefs.
	 */
	
	//ID (et ordre) des menus
	public final int MENU_DETAILS = 0;
	public final int MENU_QUIT = 1;
	public final int MENU_RENAME = 2;
	
	public final int MENU_LIST_BUILDINGS = 30;
	public final int MENU_ADD_BUILDING = 31;
	public final int MENU_REMOVE_BUILDING = 32;
	
	public final int MENU_SUPPRIMER_NIVEAU = 41;
	public final int MENU_LISTE_NIVEAUX = 40;
	
	public final int MENU_LISTE_REPERES = 50;
	public final int MENU_SUPPRIMER_REPERE = 52;
	
	public final int MENU_SUPPRIMER_POSITION = 62;
	
	public final int MENU_CLEAN_CACHE = 90;
	public final int MENU_PARAMETRES = 100;
	
	//WebService v2 JSON 
	//ne pas appeler directement ces constantes, /!\ utiliser GlobalVars.urlServer()
	public static final String ws_carto_json 		= "http://handiman.univ-paris8.fr/~ludvin/json/ws.carto.php";
	public static final String ws_carto_json_DEMO 	= "http://handiman.univ-paris8.fr/~ludvin/json/ws.carto.DEMO.php";	//mode DEMO, base "bac à sable"
	
	//Scan bornes Wifi (temps total de scans = nb_scans*delay_scans)
	//	/!\ utiliser getter/setter.
	public final int wifi_nb_scans=5;		//nombre de scans >0 ("6" conseillé), /!\ utiliser GlobalVars.getWifiDelay().
	public final int wifi_delay_scans=2;	//délai entre deux scans en secondes, ("2" conseillé), /!\ utiliser GlobalVars.getWifiNbScans(); 
	
	//SharedPreferences général :
	public static final String PREFS_FILENAME = "AppPrefs";		//fichier des préférences
	public static final int PREFS_MODE = 0;						//0 = MODE_PRIVATE
	
	//SharedPreferences pour le cache (voir aussi la (future) classe UserPrefs)
	public static final String CACHE_FILENAME = "CacheStore";	//fichier du cache
	public static final int CACHE_MODE = 0;						//0 = MODE_PRIVATE
	public static final long CACHE_LIFETIME = 30;				//durée de validité du cache en min, 0=pas de cache, -1=cache éternel, 30 conseillé, /!\ utiliser GlobalVars.getCacheLifetime().
	public static final String CACHE_SEPARATOR = "#";			//chaine utilisée pour séparer les objets JSON dans une chaine stockée en mémoire, complexifier la chaine si problème.
	
	//divers
	public final static String TAG = "Cartographe";				//pour Log()

}
