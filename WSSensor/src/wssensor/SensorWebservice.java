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

	URL firstQuestion, myURL , koordinatorUrl, meterURL = null;
	hawsensor.SensorWebservice koordinator, ichbins;
	// Meter welche der Sensor ansprechen will
	hawmetering.HAWMeteringWebservice meterService;
	// NULL = unbelegt
	//Sensorservice + dazugehoerige Anzeige
	HashMap<hawsensor.SensorWebservice, String> assignments = new HashMap<hawsensor.SensorWebservice, String>();
	String myMeter;
	URL baseUrlMeter = HAWMeteringWebserviceService.class.getResource(".");
	URL baseUrlSensor = SensorWebserviceService.class.getResource(".");
	String sensorTitle = "";

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
				koordinatorUrl = koorUrl;
				hawsensor.SensorWebserviceService koorService = new SensorWebserviceService(koorUrl, new QName("http://wssensor/", "SensorWebserviceService"));
				hawsensor.SensorWebservice koor = koorService.getSensorWebservicePort();
				System.out.println("registerSensor:");
				
				if(koor.registerSensor(myMeter, myUrl.toString())){
						meterService = service.getHAWMeteringWebservicePort();
						hawsensor.SensorWebserviceService service2 = new SensorWebserviceService(myURL, new QName("http://wssensor/", "SensorWebserviceService"));
						ichbins = service2.getSensorWebservicePort();
						meterService.setTitle(sensorTitle);
				}else{
					retValue = false;
				}
				
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		
		return retValue;

	}

	public void newTick() {

		long lTicks = new Date().getTime();
		int messwert = ((int) (lTicks % 20000)) / 100;
		if (messwert > 100) {
			messwert = 200 - messwert;
		}
		meterService.setValue(messwert);
		System.out.println("------------------------------------------------------");
		//Ausgabe zum gucken ob alle den richtigen wert haben
		for (hawsensor.SensorWebservice e : assignments.keySet()) {
			System.out.println(e.toString());
		}
		
		
	}

	public URL getKoordinator() {
		
		return koordinatorUrl;
	}

	public boolean registerSensor(@WebParam(name = "meter") String meter, @WebParam(name = "sensor") String sensor) {
		System.out.println("Sensor " + sensor + " will sich fuer die Anzeige " + meter + " registrieren.");
		boolean retValue = true;
		
		//Durchlaufe alle Zuordnungen
		for (hawsensor.SensorWebservice e : assignments.keySet()) {
			//Wenn der Sensor bereits eine Zuordnung hat --> false
			if(assignments.get(e).equals(meter)){
				retValue= false;
				System.out.println("");
			}
		}
		
		//Wenn alle geforderten Anzeigen noch frei...
		if (retValue) {
			URL tmpurl;
			try {
				tmpurl = new URL(sensor);
				hawsensor.SensorWebserviceService service = new SensorWebserviceService(tmpurl, new QName("http://wssensor/", "SensorWebserviceService"));
				hawsensor.SensorWebservice sws = service.getSensorWebservicePort();
				//Sensor hat noch keine Zuordnung und kann hinzugefuegt werden zur eigenen Liste
				assignments.put(sws, meter);
				//aktuellen Stand der Zuordnung an alle Sensoren uebertragen
				uebertrageAktuellenStand();				

			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		return retValue;
	}
	
	public void uebertrageAktuellenStand(){
		StringArray sensors = new StringArray();
		StringArray meters = new StringArray();
		
		//Arrays zum Uebertragen fuellen
		for (hawsensor.SensorWebservice temp : assignments.keySet()){
			//Sensor ins Array packen
			sensors.getItem().add(temp.getMyUrl());
			//Meter zu dem Sensor ins Array packen
			meters.getItem().add(assignments.get(temp));
		}
		
		//Arrays an alle Sensoren ausser sich selbst uebertragen
		for (hawsensor.SensorWebservice serv : assignments.keySet()) {
			if (!assignments.get(serv).equals(myMeter)) {
				serv.setMeterAssignments(sensors, meters);
			}
		}
	}
	
	public void setMeterAssignments(@WebParam(name = "sensor") String[] sensor,@WebParam(name = "meter")  String[] meter) {
		
		//alten Stand vergessen
		assignments.clear();
		URL tmpurl;
		hawsensor.SensorWebserviceService service;
		hawsensor.SensorWebservice sws;
		
		System.out.println(sensor.length);
		
		//Trage alle uebergebenen Assignments ein
		for (int i = 0; i < sensor.length; i++) {
			try {
				//Url fuer den Sensor erzeugen
				tmpurl = new URL(sensor[i]);
				//Sensor Objekt erzeugen
				service = new SensorWebserviceService(tmpurl, new QName("http://wssensor/", "SensorWebserviceService"));
				sws = service.getSensorWebservicePort();
				//Trage den Sensor + passenden Meter ein
				assignments.put(sws,meter[i]);
				System.out.println("Assignment eingetragen: Sensor: " + sws.toString() + " Meter: " + meter[i]);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}	
		}
	}
	
	public void removeMeterAssignments(@WebParam(name = "sensor") String sensor) {

		assignments.remove(sensor);
		System.out.println("removeMeterAssigments");
	}

	/* election */
	public void triggerSensors() {
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		final Runnable ticker = new Runnable() {
			@Override
			public void run() {
				ArrayList<hawsensor.SensorWebservice> deleteList = new ArrayList<hawsensor.SensorWebservice>();
 				for (hawsensor.SensorWebservice e : assignments.keySet()) {
					try{
						e.newTick();
					}
					catch(Exception ex){
						String tmp = assignments.get(e);
							
							try {
								hawmetering.HAWMeteringWebservice deleteService;
								URL deleteUrl;
								deleteUrl = new URL(baseUrlMeter, tmp);
								HAWMeteringWebserviceService service = new HAWMeteringWebserviceService(deleteUrl, new QName("http://hawmetering/", "HAWMeteringWebserviceService"));
								deleteService = service.getHAWMeteringWebservicePort();
								//Bezeichnung wiederherstellen (NW, NO, SW, SO)
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
 	 					for (hawsensor.SensorWebservice e : assignments.keySet()) {
 	 							e.removeMeterAssignments(assignments.get(e));
 	 					}
 					}
 					//wenn geloescht dann informiere andere ueber aktuellenStand
 					uebertrageAktuellenStand(); 					
				}
 				
 				
 				
 				
 				deleteList.clear();
				newTick();

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
	
	public String getMyUrl(){
		return myURL.toString();
	}
}
