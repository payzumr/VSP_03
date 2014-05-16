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

	URL firstQuestion, myURL = null;
	URL[] meterURL;
	hawsensor.SensorWebservice koordinator;
	// Meter welche der Sensor ansprechen will
	hawmetering.HAWMeteringWebservice[] meter;
	// NULL = unbelegt
	HashMap<String, hawsensor.SensorWebservice> assignedSensor = new HashMap<String, hawsensor.SensorWebservice>();
	ArrayList<hawsensor.SensorWebservice> sensors = new ArrayList<hawsensor.SensorWebservice>();
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
			// TODO Auto-generated catch block
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void initializeMeter(String myUrl) {
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
				meter[i].setTitle(sensorName);
				triggerSensors();
			}
			hawsensor.SensorWebserviceService service = new SensorWebserviceService(myURL, new QName("http://wssensor/", "SensorWebserviceService"));
			koordinator = service.getSensorWebservicePort();
			
		} else {
			hawsensor.SensorWebserviceService service = new SensorWebserviceService(firstQuestion, new QName("http://wssensor/", "SensorWebserviceService"));
			hawsensor.SensorWebservice ersterKoordinator = service.getSensorWebservicePort();
			try {
				URL koor = new URL(SensorWebserviceService.class.getResource("."), ersterKoordinator.getKoordinator());
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

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
				sensors.add(sws);

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return retValue;
	}

	public void setMeterAssignments(@WebParam(name = "sensor") HashMap<String, hawsensor.SensorWebservice> assignedSensors, ArrayList<hawsensor.SensorWebservice> sensors) {
		this.assignedSensor = assignedSensors;
		this.sensors = sensors;
	}

	/* election */
	public void triggerSensors() {
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

		final Runnable ticker = new Runnable() {
			@Override
			public void run() {
				for (hawsensor.SensorWebservice e : sensors) {
					e.newTick();
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
		}, 20, TimeUnit.SECONDS);
	}
}
