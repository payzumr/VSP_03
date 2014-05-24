package wssensor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import hawmetering.HAWMeteringWebservice;
import hawmetering.HAWMeteringWebserviceService;
import hawmetering.WebColor;
import hawsensor.*;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.*;
import javax.xml.namespace.QName;

import net.java.dev.jaxb.array.StringArray;

@WebService
@SOAPBinding(style = Style.RPC)
public class SensorWebservice {

	URL firstQuestion, myURL, koordinatorUrl, meterURL = null;
	hawsensor.SensorWebservice koordinator, ichbins;
	// Meter welche der Sensor ansprechen will
	hawmetering.HAWMeteringWebservice meterService;
	// Sensorservice + dazugehoerige Anzeige
	HashMap<hawsensor.SensorWebservice, String> assignments = new HashMap<hawsensor.SensorWebservice, String>();
	String myMeter;
	URL baseUrlMeter = HAWMeteringWebserviceService.class.getResource(".");
	URL baseUrlSensor = SensorWebserviceService.class.getResource(".");
	String sensorTitle = "";

	/* election */
	boolean electionPhase = false;
	int tickSeqN = 0;
	int oldTick = -1;

	public SensorWebservice(String title, String meterURL, String firstQuestion) {
		try {
			this.myMeter = meterURL;
			this.meterURL = new URL(baseUrlMeter, meterURL);

			this.firstQuestion = new URL(baseUrlSensor, firstQuestion);
			this.sensorTitle = title;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public SensorWebservice(String title, String meterURL) {
		try {
			this.myMeter = meterURL;
			this.meterURL = new URL(baseUrlMeter, meterURL);
			this.sensorTitle = title;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public boolean initializeMeter(String myUrl) {
		boolean retValue = true;
		try {
			this.myURL = new URL(baseUrlSensor, myUrl);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}
		HAWMeteringWebserviceService service = new HAWMeteringWebserviceService(meterURL, new QName("http://hawmetering/", "HAWMeteringWebserviceService"));
		meterService = service.getHAWMeteringWebservicePort();

		if (firstQuestion == null) {
			koordinatorUrl = myURL;
			meterService.setTitle(sensorTitle + "-K");
			triggerSensors();

			hawsensor.SensorWebserviceService service2 = new SensorWebserviceService(myURL, new QName("http://wssensor/", "SensorWebserviceService"));
			koordinator = service2.getSensorWebservicePort();

			assignments.put(koordinator, myMeter);

		} else {
			hawsensor.SensorWebserviceService service1 = new SensorWebserviceService(firstQuestion, new QName("http://wssensor/", "SensorWebserviceService"));
			hawsensor.SensorWebservice ersterKoordinator = service1.getSensorWebservicePort();

			try {
				URL koorUrl = new URL(SensorWebserviceService.class.getResource("."), ersterKoordinator.getKoordinator());
				// TODO Schleife einbauen die bei getKoordinator == null sleep macht und dann wieder fragt
				koordinatorUrl = koorUrl;
				hawsensor.SensorWebserviceService koorService = new SensorWebserviceService(koorUrl, new QName("http://wssensor/", "SensorWebserviceService"));
				hawsensor.SensorWebservice koor = koorService.getSensorWebservicePort();
				System.out.println("registerSensor:");

				if (koor.registerSensor(myMeter, myUrl.toString())) {
					meterService = service.getHAWMeteringWebservicePort();
					hawsensor.SensorWebserviceService service2 = new SensorWebserviceService(myURL, new QName("http://wssensor/", "SensorWebserviceService"));
					ichbins = service2.getSensorWebservicePort();
					meterService.setTitle(sensorTitle);

					startTickChecker();
				} else {
					retValue = false;
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		return retValue;

	}

	public void newTick(@WebParam(name = "seqN") int seqN) {

		tickSeqN = seqN;

		long lTicks = new Date().getTime();
		int messwert = ((int) (lTicks % 20000)) / 100;
		if (messwert > 100) {
			messwert = 200 - messwert;
		}
		meterService.setValue(messwert);
		System.out.println("------------------------------------------------------");
		// Ausgabe zum gucken ob alle den richtigen wert haben
		for (hawsensor.SensorWebservice e : assignments.keySet()) {
			System.out.println(e.toString());
		}

	}

	public URL getKoordinator() {
		URL retValue = null;
		if (!electionPhase) {
			retValue = koordinatorUrl;
		}
		return retValue;
	}

	public boolean registerSensor(@WebParam(name = "meter") String meter, @WebParam(name = "sensor") String sensor) {
		System.out.println("Sensor " + sensor + " will sich fuer die Anzeige " + meter + " registrieren.");
		boolean retValue = true;

		// Durchlaufe alle Zuordnungen
		for (hawsensor.SensorWebservice e : assignments.keySet()) {
			// Wenn der Sensor bereits eine Zuordnung hat --> false
			if (assignments.get(e).equals(meter)) {
				retValue = false;
				System.out.println("");
			}
		}

		// Wenn alle geforderten Anzeigen noch frei...
		if (retValue) {
			URL tmpurl;
			try {
				tmpurl = new URL(sensor);
				hawsensor.SensorWebserviceService service = new SensorWebserviceService(tmpurl, new QName("http://wssensor/", "SensorWebserviceService"));
				hawsensor.SensorWebservice sws = service.getSensorWebservicePort();
				// Sensor hat noch keine Zuordnung und kann hinzugefuegt werden zur eigenen Liste
				assignments.put(sws, meter);
				// aktuellen Stand der Zuordnung an alle Sensoren uebertragen
				uebertrageAktuellenStand();

			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		return retValue;
	}

	public void uebertrageAktuellenStand() {
		StringArray sensors = new StringArray();
		StringArray meters = new StringArray();

		// Arrays zum Uebertragen fuellen
		for (hawsensor.SensorWebservice temp : assignments.keySet()) {
			// Sensor ins Array packen
			sensors.getItem().add(temp.getMyUrl());
			// Meter zu dem Sensor ins Array packen
			meters.getItem().add(assignments.get(temp));
		}

		// Arrays an alle Sensoren ausser sich selbst uebertragen
		for (hawsensor.SensorWebservice serv : assignments.keySet()) {
			if (!assignments.get(serv).equals(myMeter)) {
				serv.setMeterAssignments(sensors, meters);
			}
		}
	}

	public void setMeterAssignments(@WebParam(name = "sensor") String[] sensor, @WebParam(name = "meter") String[] meter) {

		// alten Stand vergessen
		assignments.clear();
		URL tmpurl;
		hawsensor.SensorWebserviceService service;
		hawsensor.SensorWebservice sws;

		// Trage alle uebergebenen Assignments ein
		for (int i = 0; i < sensor.length; i++) {
			try {
				// Url fuer den Sensor erzeugen
				tmpurl = new URL(sensor[i]);
				// Sensor Objekt erzeugen
				service = new SensorWebserviceService(tmpurl, new QName("http://wssensor/", "SensorWebserviceService"));
				sws = service.getSensorWebservicePort();
				// Trage den Sensor + passenden Meter ein
				assignments.put(sws, meter[i]);
				System.out.println("Assignment eingetragen: Sensor: " + sws.toString() + " Meter: " + meter[i]);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}

	public void triggerSensors() {
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		meterService.setTitle(sensorTitle + "-K");
		final Runnable ticker = new Runnable() {
			int tickSeqN = 0;

			@Override
			public void run() {
				tickSeqN++;
				ArrayList<hawsensor.SensorWebservice> deleteList = new ArrayList<hawsensor.SensorWebservice>();
				for (hawsensor.SensorWebservice e : assignments.keySet()) {
					try {
						e.newTick(tickSeqN);
					} catch (Exception ex) {
						String tmp = assignments.get(e);

						try {
							hawmetering.HAWMeteringWebservice deleteService;
							URL deleteUrl;
							deleteUrl = new URL(baseUrlMeter, tmp);
							HAWMeteringWebserviceService service = new HAWMeteringWebserviceService(deleteUrl, new QName("http://hawmetering/", "HAWMeteringWebserviceService"));
							deleteService = service.getHAWMeteringWebservicePort();
							// Bezeichnung wiederherstellen (NW, NO, SW, SO)
							deleteService.setTitle(tmp.substring(34, 36).toUpperCase());
							deleteService.setValue(0);
							deleteList.add(e);
						} catch (MalformedURLException e1) {
							e1.printStackTrace();
						}
					}
				}
				if (!deleteList.isEmpty()) {
					for (hawsensor.SensorWebservice d : deleteList) {
						assignments.remove(d);
					}
					// wenn geloescht dann informiere andere ueber aktuellenStand
					uebertrageAktuellenStand();
				}

				deleteList.clear();
				newTick(tickSeqN);

			}
		};

		// Starten:
		final ScheduledFuture<?> tickerHandle = scheduler.scheduleAtFixedRate(ticker, 2000 - new Date().getTime() % 2000, 2000, TimeUnit.MILLISECONDS);

		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				tickerHandle.cancel(true);
				scheduler.shutdown();
			}
		}, 600, TimeUnit.SECONDS);
	}

	public String getMyUrl() {
		return myURL.toString();
	}

	/* election */
	/**
	 * jedem mitteilen, das neuer koordinator gesucht werden muss
	 */
	public void setElectionPhase(@WebParam(name = "condition") boolean condition) {
		if (condition) {
			System.out.println("WahlPhase begonnen!!!");
			electionPhase = true;
			//delete koordinator
			hawsensor.SensorWebservice del = null;
			try {
				for (hawsensor.SensorWebservice sensor : assignments.keySet()) {
					del = sensor;
					sensor.getMyUrl();
				}
			} catch (Exception e) {
				
				URL koorMeterUrl = null;
				try {
					koorMeterUrl = new URL(baseUrlMeter, assignments.get(del));
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				HAWMeteringWebserviceService service = new HAWMeteringWebserviceService(koorMeterUrl, new QName("http://hawmetering/", "HAWMeteringWebserviceService"));
				service.getHAWMeteringWebservicePort().setTitle("free");
				
				assignments.remove(del);
			}
			tickSeqN = 0;
			oldTick = -1;
		} else {
			System.out.println("Wahlphase beendet!!!");
			electionPhase = false;
		}
	}

	/**
	 * prueft die seqN des aktuellen ticks bei
	 */
	private void startTickChecker() {
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		final Runnable ticker = new Runnable() {
			@Override
			public void run() {
				if (!electionPhase) {
					if (tickSeqN == oldTick) {// wenn sich tickSeqN nicht geaendert hat, ist der Koor. ausgefallen
						System.out.println("Koordinatorausfall erkannt!!!");
						setElectionPhase(true);
						for (hawsensor.SensorWebservice sensor : assignments.keySet()) {
							if (!assignments.get(sensor).equals(myMeter)) {
								System.out.println(assignments.get(sensor));
								sensor.setElectionPhase(true);// andere sensoren informieren
							}

						}
						electionAlgo();// wahl ausloesen
					} else {
						oldTick = tickSeqN;// oldTick aktualisieren
					}
				}
			}
		};

		// Starten:
		final ScheduledFuture<?> tickerCheck = scheduler.scheduleAtFixedRate(ticker, 3000, 3000, TimeUnit.MILLISECONDS);

		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				tickerCheck.cancel(true);
				scheduler.shutdown();
			}
		}, 600, TimeUnit.SECONDS);
	}

	// bloedsinn->
	// /**
	// * recive() methode aus heitmanns folie 8 aus dem wahlen-PDF
	// *
	// * @return
	// */
	// public boolean elect() {
	// electionAlgo();
	// return true;// aufrufer melden das man wahl angenmmen hat
	// }
	// ^^^^bloedsinn^^^^

	public void electionAlgo() {
		System.out.println("Wahl Algo ausgelöst!!!");
		boolean ichBinKoordinator = false;// scheiß name, denkt euch mal nen besseren aus
		// liste mit "groesseren" sensoren als man selber
		ArrayList<hawsensor.SensorWebservice> koordinatorCandidates = new ArrayList<hawsensor.SensorWebservice>();

		for (hawsensor.SensorWebservice sensor : assignments.keySet()) {
			if (sensor.getMyUrl().compareTo(this.getMyUrl()) > 0) {// wenn sensor > this
				koordinatorCandidates.add(sensor);
			}

		}

		if (koordinatorCandidates.isEmpty()) {
			// kein groesserer sensor gefunden -> ich bin koordinator
			ichBinKoordinator = true;
		} else {
			// groessere sensoren informieren und electionAlgo ausfuehren lassen.
			for (hawsensor.SensorWebservice sensor : koordinatorCandidates) {
				sensor.electionAlgo();
			}
		}

		if (ichBinKoordinator) {
			// anderen sensoren mitteilen, das ich koordinator bin
			for (hawsensor.SensorWebservice sensor : assignments.keySet()) {
				sensor.setKoordinator(this.getMyUrl());
			}

			triggerSensors();// trigger starten
			// electionPhase beenden
			for (hawsensor.SensorWebservice sensor : assignments.keySet()) {
				sensor.setElectionPhase(false);
			}
		}

	}

	public void setKoordinator(@WebParam(name = "newKoor") String newKoor) {
		System.out.println("neuer Koordinator gesetzt!!!");
		try {
			URL koorUrl = new URL(SensorWebserviceService.class.getResource("."), newKoor);
			koordinatorUrl = koorUrl;
			hawsensor.SensorWebserviceService koorService = new SensorWebserviceService(koorUrl, new QName("http://wssensor/", "SensorWebserviceService"));
			hawsensor.SensorWebservice koor = koorService.getSensorWebservicePort();

			koordinator = koor;// neuen koordinator setzen

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

}
