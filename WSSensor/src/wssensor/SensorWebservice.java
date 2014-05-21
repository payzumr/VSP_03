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

@WebService
@SOAPBinding(style = Style.RPC)
public class SensorWebservice {

	URL firstQuestion, myURL , koordinatorUrl, meterURL = null;
	hawsensor.SensorWebservice koordinator, ichbins;
	// Meter welche der Sensor ansprechen will
	hawmetering.HAWMeteringWebservice meterService;
	// NULL = unbelegt
	HashMap<String, hawsensor.SensorWebservice> assignedSensor = new HashMap<String, hawsensor.SensorWebservice>();
	HashMap<hawsensor.SensorWebservice, String> sensors = new HashMap<hawsensor.SensorWebservice, String>();
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
		if (firstQuestion == null) {
			koordinatorUrl = myURL;
			HAWMeteringWebserviceService service = new HAWMeteringWebserviceService(meterURL, new QName("http://hawmetering/", "HAWMeteringWebserviceService"));
			meterService = service.getHAWMeteringWebservicePort();
			meterService.setTitle(sensorTitle + "-K");
			triggerSensors();
			
			hawsensor.SensorWebserviceService service2 = new SensorWebserviceService(myURL, new QName("http://wssensor/", "SensorWebserviceService"));
			koordinator = service2.getSensorWebservicePort();

			sensors.put(koordinator, myMeter);
			
		} else {
			hawsensor.SensorWebserviceService service = new SensorWebserviceService(firstQuestion, new QName("http://wssensor/", "SensorWebserviceService"));
			hawsensor.SensorWebservice ersterKoordinator = service.getSensorWebservicePort();

			try {
				URL koorUrl = new URL(SensorWebserviceService.class.getResource("."), ersterKoordinator.getKoordinator());
				koordinatorUrl = koorUrl;
				hawsensor.SensorWebserviceService koorService = new SensorWebserviceService(koorUrl, new QName("http://wssensor/", "SensorWebserviceService"));
				hawsensor.SensorWebservice koor = koorService.getSensorWebservicePort();
				System.out.println("registerSensor:");
				
				if(koor.registerSensor(myMeter, myUrl.toString())){

						HAWMeteringWebserviceService service1 = new HAWMeteringWebserviceService(meterURL, new QName("http://hawmetering/", "HAWMeteringWebserviceService"));
						meterService = service1.getHAWMeteringWebservicePort();
						hawsensor.SensorWebserviceService service2 = new SensorWebserviceService(myURL, new QName("http://wssensor/", "SensorWebserviceService"));
						ichbins = service2.getSensorWebservicePort();
						meterService.setTitle(sensorTitle);
						sensors.put(ichbins,myMeter);

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
		for (hawsensor.SensorWebservice e : sensors.keySet()) {
			System.out.println(e.toString());
		}
		
		
	}

	public URL getKoordinator() {
		
		return koordinatorUrl;
	}

	public boolean registerSensor(@WebParam(name = "meter") String meter, @WebParam(name = "sensor") String sensor) {
		System.out.println("Sensor: " + sensor);
		boolean retValue = true;
		for (hawsensor.SensorWebservice e : sensors.keySet()) {
			
			if(sensors.get(e).equals(meter)) retValue= false;
		}

		if (retValue) {
			URL tmpurl;
			try {
				tmpurl = new URL(sensor);
				hawsensor.SensorWebserviceService service = new SensorWebserviceService(tmpurl, new QName("http://wssensor/", "SensorWebserviceService"));
				hawsensor.SensorWebservice sws = service.getSensorWebservicePort();
				System.out.println("Neuer Sensor hinzugefügt");
				//Stringarray Complex herstellen
				
				for (hawsensor.SensorWebservice serv : sensors.keySet()) {
					serv.setMeterAssignments(sensor, meter);
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		return retValue;
	}

	public void setMeterAssignments(@WebParam(name = "sensor") String sensor,@WebParam(name = "meter")  String meter) {
		URL tmpurl;
		try {
			tmpurl = new URL(sensor);
			hawsensor.SensorWebserviceService service = new SensorWebserviceService(tmpurl, new QName("http://wssensor/", "SensorWebserviceService"));
			hawsensor.SensorWebservice sws = service.getSensorWebservicePort();
		
			sensors.put(sws,meter);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		System.out.println("setMeterAssigments");
	}
	
	public void removeMeterAssignments(@WebParam(name = "sensor") String sensor) {

		sensors.remove(sensor);
		System.out.println("removeMeterAssigments");
	}

	/* election */
	public void triggerSensors() {
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		final Runnable ticker = new Runnable() {
			@Override
			public void run() {
				ArrayList<hawsensor.SensorWebservice> deleteList = new ArrayList<hawsensor.SensorWebservice>();
 				for (hawsensor.SensorWebservice e : sensors.keySet()) {
					try{
						e.newTick();
					}
					catch(Exception ex){
						String tmp = sensors.get(e);
							
							try {
								hawmetering.HAWMeteringWebservice deleteService;
								URL deleteUrl;
								deleteUrl = new URL(baseUrlMeter, tmp);
								HAWMeteringWebserviceService service = new HAWMeteringWebserviceService(deleteUrl, new QName("http://hawmetering/", "HAWMeteringWebserviceService"));
								deleteService = service.getHAWMeteringWebservicePort();
								deleteService.setTitle("free");
								deleteService.setValue(0);
								deleteList.add(e);
							} catch (MalformedURLException e1) {
								e1.printStackTrace();
							}
					}
				}
 				for (hawsensor.SensorWebservice d : deleteList) {
 					sensors.remove(d);
 					for (hawsensor.SensorWebservice e : sensors.keySet()) {
 							e.removeMeterAssignments(sensors.get(e));
 					}
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
}
