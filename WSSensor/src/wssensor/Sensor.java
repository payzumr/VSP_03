package wssensor;

import javax.xml.ws.Endpoint;

import hawmetering.HAWMeteringWebservice;
import hawmetering.HAWMeteringWebserviceService;
import hawmetering.WebColor;

public class Sensor {

	public static void main(String[] args) throws InterruptedException {
		startWebservice();
		
	}

	public static void startWebservice(){
		SensorWebservice webservice = new SensorWebservice();
		System.out.println("sensor lauft...");
        Endpoint.publish("http://0.0.0.0:8888/hawsensor/", webservice);
	}
}
