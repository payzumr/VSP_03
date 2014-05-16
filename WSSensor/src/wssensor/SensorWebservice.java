package wssensor;

import java.net.MalformedURLException;
import java.net.URL;
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
	URL [] meterURL;
	hawsensor.SensorWebservice koordinator;
	//Meter welche der Sensor ansprechen will
	hawmetering.HAWMeteringWebservice [] meter;
	//0=nw, 1=no, 2=sw, 3=so / NULL = unbelegt
	hawsensor.SensorWebservice [] sensor = new hawsensor.SensorWebservice[4];
    URL baseUrlMeter = HAWMeteringWebserviceService.class.getResource(".");
    URL baseUrlSensor = SensorWebserviceService.class.getResource(".");
    String sensorName="";
    
    public SensorWebservice(String title, String[] meterURL,  String firstQuestion) {
		try {
			
			this.meterURL = new URL[meterURL.length];
			for (int i = 0; i < meterURL.length; i++) {
				this.meterURL[i] = new URL(baseUrlMeter, meterURL[i]);
			}

			this.firstQuestion = new URL(baseUrlSensor, firstQuestion);
			this.myURL = new URL(baseUrlSensor,title);
			this.sensorName = title;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		initializeMeter();
	}
    
    public SensorWebservice(String title, String [] meterURL) {
		try {
			this.meterURL = new URL[meterURL.length];
			for (int i = 0; i < meterURL.length; i++) {
				this.meterURL[i] = new URL(baseUrlMeter, meterURL[i]);
			}
			this.sensorName = title;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		initializeMeter();
	}

	public void initializeMeter() {
		meter = new HAWMeteringWebservice[meterURL.length];
		if (firstQuestion == null) {
			
			for (int i = 0; i < meterURL.length; i++) {
				HAWMeteringWebserviceService service = new HAWMeteringWebserviceService(meterURL[i], new QName("http://hawmetering/", "HAWMeteringWebserviceService"));
				meter[i] = service.getHAWMeteringWebservicePort();
				meter[i].setTitle(sensorName);
			}
		}else {
			hawsensor.SensorWebserviceService service = new SensorWebserviceService(firstQuestion, new QName("http://hawsensor/", "firstQuestion"));
			hawsensor.SensorWebservice ersterKoordinator = service.getSensorWebservicePort();
			ersterKoordinator.getKoordinator();
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
	
	//hawsensor.SensorWebservice
	public boolean getKoordinator() {
		boolean temp = false;
		
		hawsensor.SensorWebserviceService service = new SensorWebserviceService(myURL, new QName("http://hawsensor/", "Koordinator"));
		koordinator = service.getSensorWebservicePort();
		
		return temp;
	}
	//
//	public boolean iWantThisMeter(@WebParam(name = "meter") HAWMeteringWebservice [] meter) {
//		return false;// TODO
//	}

	public void registerSensor() {

	}

//	public void setMeterAssignments(@WebParam(name = "sensor") hawsensor.SensorWebservice [] sensor) {
//
//	}
	/* election */
	public void triggerSensors() {
	        final ScheduledExecutorService scheduler =
	                Executors.newScheduledThreadPool(1);

	            final Runnable ticker = new Runnable() {
	                @Override
	                public void run() {
	                	for (int i = 0; i < sensor.length; i++) {
							if(sensor[i] != null){
								sensor[i].newTick();
							}
							newTick();
	                	}
	               }
	            };
	            
	          //Starten:
		        final ScheduledFuture<?> tickerHandle =
		            scheduler.scheduleAtFixedRate(ticker, 2000 - new Date().getTime() % 2000, 2000, TimeUnit.MILLISECONDS);

		        
		        scheduler.schedule(new Runnable() {
		        	@Override

		            public void run() {
		                tickerHandle.cancel(true);
		                scheduler.shutdown();
		            }
		        }, 20, TimeUnit.SECONDS);
		     }    
	}
