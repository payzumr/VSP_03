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

	URL firstQuestion, myURL = null;
	URL[] meterURL;
	hawsensor.SensorWebservice koordinator;
	// Meter welche der Sensor ansprechen will
	hawmetering.HAWMeteringWebservice[] meter;
	// NULL = unbelegt
	HashMap<String, hawsensor.SensorWebservice> assignedSensor = new HashMap<String, hawsensor.SensorWebservice>();
	HashMap<hawsensor.SensorWebservice, String[]> sensors = new HashMap<hawsensor.SensorWebservice, String[]>();
	String[] wantedMeters;
	URL baseUrlMeter = HAWMeteringWebserviceService.class.getResource(".");
	URL baseUrlSensor = SensorWebserviceService.class.getResource(".");
	String sensorName = "";

	public SensorWebservice(String title, String[] meterURL, String firstQuestion) {
		try {
			this.wantedMeters = meterURL;
			this.meterURL = new URL[meterURL.length];
			for (int i = 0; i < meterURL.length; i++) {
				this.meterURL[i] = new URL(baseUrlMeter, meterURL[i]);
			}

			this.firstQuestion = new URL(baseUrlSensor, firstQuestion);
			this.sensorName = title;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public SensorWebservice(String title, String[] meterURL) {
		try {
			this.wantedMeters = meterURL;
			this.meterURL = new URL[meterURL.length];
			for (int i = 0; i < meterURL.length; i++) {
				this.meterURL[i] = new URL(baseUrlMeter, meterURL[i]);
			}
			this.sensorName = title;
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
		meter = new HAWMeteringWebservice[meterURL.length];
		if (firstQuestion == null) {

			for (int i = 0; i < meterURL.length; i++) {
				HAWMeteringWebserviceService service = new HAWMeteringWebserviceService(meterURL[i], new QName("http://hawmetering/", "HAWMeteringWebserviceService"));
				meter[i] = service.getHAWMeteringWebservicePort();
				meter[i].setTitle(sensorName + "-K");
				triggerSensors();
			}
			hawsensor.SensorWebserviceService service = new SensorWebserviceService(myURL, new QName("http://wssensor/", "SensorWebserviceService"));
			koordinator = service.getSensorWebservicePort();
			
		} else {
			hawsensor.SensorWebserviceService service = new SensorWebserviceService(firstQuestion, new QName("http://wssensor/", "SensorWebserviceService"));
			hawsensor.SensorWebservice ersterKoordinator = service.getSensorWebservicePort();
			StringArray wantedMetersCompType = new StringArray();
			for (int i = 0; i < wantedMeters.length; i++) {
				wantedMetersCompType.getItem().add(wantedMeters[i]);
			}
			try {
				URL koorUrl = new URL(SensorWebserviceService.class.getResource("."), ersterKoordinator.getKoordinator());
				hawsensor.SensorWebserviceService koorService = new SensorWebserviceService(koorUrl, new QName("http://wssensor/", "SensorWebserviceService"));
				hawsensor.SensorWebservice koor = koorService.getSensorWebservicePort();
				System.out.println("registerSensor:");
				if(koor.registerSensor(wantedMetersCompType, myUrl.toString())){
					for (int i = 0; i < meterURL.length; i++) {
						HAWMeteringWebserviceService service1 = new HAWMeteringWebserviceService(meterURL[i], new QName("http://hawmetering/", "HAWMeteringWebserviceService"));
						meter[i] = service1.getHAWMeteringWebservicePort();
						meter[i].setTitle(sensorName);
					}
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
		for (int i = 0; i < meter.length; i++) {
			meter[i].setValue(messwert);
		}
	}

	public URL getKoordinator() {
		
		return myURL;
	}

	public boolean registerSensor(@WebParam(name = "meter") String[] meter, @WebParam(name = "sensor") String sensor) {
		System.out.println("Sensor: " + sensor);
		boolean retValue = true;
		for (int i = 0; i < meter.length; i++) {
			if (assignedSensor.containsKey(meter[i])) {
				retValue = false;
			}
		}
		if (retValue) {
			URL tmpurl;
			try {
				tmpurl = new URL(sensor);
				hawsensor.SensorWebserviceService service = new SensorWebserviceService(tmpurl, new QName("http://wssensor/", "SensorWebserviceService"));
				hawsensor.SensorWebservice sws = service.getSensorWebservicePort();
				for (int j = 0; j < meter.length; j++) {
					assignedSensor.put(meter[j], sws);
				}
				
				sensors.put(sws,meter);

			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		return retValue;
	}

	public void setMeterAssignments(@WebParam(name = "sensor") HashMap<String, hawsensor.SensorWebservice> assignedSensors, HashMap<hawsensor.SensorWebservice, String[]> sensors) {
		this.assignedSensor = assignedSensors;
		this.sensors = sensors;
	}

	/* election */
	public void triggerSensors() {
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		final Runnable ticker = new Runnable() {
			@Override
			public void run() {
				for (hawsensor.SensorWebservice e : sensors.keySet()) {
					try{
						e.newTick();
					}
					catch(Exception ex){
						String[] tmp = sensors.get(e);
						for (int i = 0; i < tmp.length; i++) {
							
							try {
								hawmetering.HAWMeteringWebservice deleteService;
								URL deleteUrl;
								deleteUrl = new URL(baseUrlMeter, tmp[i]);
								HAWMeteringWebserviceService service = new HAWMeteringWebserviceService(deleteUrl, new QName("http://hawmetering/", "HAWMeteringWebserviceService"));
								deleteService = service.getHAWMeteringWebservicePort();
								deleteService.setTitle("free");
								deleteService.setValue(0);
								assignedSensor.remove(tmp[i]);
								sensors.remove(e);
							} catch (MalformedURLException e1) {
								e1.printStackTrace();
							}
						}
					}
				}

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
