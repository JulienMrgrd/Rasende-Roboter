package server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import other.LeSaviezVousGenerator;
import other.Protocole;
import other.ProtocoleCreator;

public class Server{
	
	public static final int PORT = 2016;
	private Map<String, Joueur> mapJoueurs;
	private int nbJoueurs=0;
	private Session session;
	private Object sync;
	private int secondsBeforeStartSession;
	
	public Server() {
		mapJoueurs = new HashMap<String, Joueur>();
		session = new Session(mapJoueurs, this);
		sync = new Object(); // Objet pour notification
		
		System.out.println("--------");
		System.out.println("Serveur demarre sur le port : "+PORT);
		System.out.println("--------\n");
	}
	
	public void start() {
		
		ServerSocket serverSocket = null;
		try	{
			serverSocket = new ServerSocket(PORT);
			startSessionIfPossible();
			while (true) // attente en boucle de connexion (bloquant sur ss.accept)
			{
				Socket client = serverSocket.accept();
				System.out.println("Ah, il y a un nouveau joueur !...");
				new Joueur(client,this).start(); // un client se connecte, un nouveau thread client est lancé
			}

		}
		catch (Exception e) { }
		finally {
			if(serverSocket!=null && !serverSocket.isClosed()){
				try {
					serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	} 
	
	/** Démarre la session (ou en relance une) si il y a au moins 2 joueurs. */
	public void startSessionIfPossible(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true){
					System.out.println("(startSessionIfNeeded) avec nbJoueurs="+nbJoueurs+" et sessionHasStarted="+session.hasStarted());
					synchronized (sync) {
						while(nbJoueurs<2){ // Préconditions pour lancer une session
							try {
								sync.wait();
							} catch (InterruptedException e) {
								System.out.println("(startSessionIfNeeded) wait impossible");
							}
						}
					}
					
					secondsBeforeStartSession = Session.SECONDS_BEFORE_START;
					sendAll(ProtocoleCreator.create(Protocole.SESSION_START_TIME, Integer.toString(Session.SECONDS_BEFORE_START)));
					System.out.println("Début de partie dans "+Session.SECONDS_BEFORE_START+" secondes");
					try {
						LeSaviezVousGenerator gen = new LeSaviezVousGenerator();
						do{
							Thread.sleep(Session.SECONDS_FOR_DISPLAY_SAVIEZVOUS*1000);
							sendAll(ProtocoleCreator.create(Protocole.LE_SAVIEZ_VOUS,gen.get()));
							secondsBeforeStartSession -= Session.SECONDS_FOR_DISPLAY_SAVIEZVOUS;
						} while(secondsBeforeStartSession>0);
					} catch (InterruptedException e) {
						System.out.println("(startSessionIfPossible) attente impossible");
					}
					secondsBeforeStartSession=0;
					
					if(nbJoueurs<2){ // Un ou plusieurs joueurs ont quittés
						sendAll(ProtocoleCreator.create(Protocole.SESSION_START_CANCEL));
						continue;
					}
					
					session.startSession(); // Bloquant
					System.out.println("(startSessionIfNeeded) after startSession");
					
				}
			}
		}).start();
	}

	synchronized public void sendAll(String message) {
		for (Entry<String, Joueur> onejoueur : mapJoueurs.entrySet()){ // parcours de la table des connectés
			try {
				onejoueur.getValue().sendToJoueur(message);
			} catch (IOException e) {
				System.out.println("(sendAll) exception sur joueur "+onejoueur.getKey());
			}
		}
	}
	
	synchronized public void sendAllButThis(String message, Joueur toNotInclude) {
		for (Entry<String, Joueur> onejoueur : mapJoueurs.entrySet()){ // parcours de la table des connectés
			try {
				if( !onejoueur.getKey().equals(toNotInclude.getPseudo()) ){
					onejoueur.getValue().sendToJoueur(message);
				}
			} catch (IOException e) {
				System.out.println("(sendAllButThis) exception sur joueur "+onejoueur.getKey());
			}
		}
	}
	
	synchronized public boolean addJoueur(Joueur joueur) {
		if(this.mapJoueurs.containsKey(joueur.getPseudo())) return false;
		mapJoueurs.put(joueur.getPseudo(), joueur);
		nbJoueurs++;
		session.addJoueur(joueur);
		if(session.hasStarted()){
			try {
				joueur.sendToJoueur(ProtocoleCreator.create(Protocole.WAIT));
			} catch (IOException e) { /* Un joueur s'est connecté et immédiatement déconnecté*/ }
		} else {
			try {
				if(secondsBeforeStartSession!=0)
					joueur.sendToJoueur(ProtocoleCreator.create(Protocole.SESSION_START_TIME, Integer.toString(secondsBeforeStartSession)));
			} catch (IOException e) { removeJoueur(joueur); }
		}
		synchronized (sync) {
			sync.notify();
		}
		return true;
	}
	
	synchronized public boolean removeJoueur(Joueur joueur) {
		if( !this.mapJoueurs.containsKey(joueur.getPseudo()) ) return false;
		mapJoueurs.remove(joueur.getPseudo());
		session.removeJoueur(joueur);
		nbJoueurs--;
		return true;
	}

	synchronized public int getNbJoueurs() {
		return nbJoueurs;
	}
	

	public static void main(String args[])
	{
		Server server = new Server(); // instance de la classe principale
		server.start();
	}


}