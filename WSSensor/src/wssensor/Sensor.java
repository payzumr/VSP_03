package wssensor;

import javax.xml.ws.Endpoint;

import hawmetering.HAWMeteringWebservice;
import hawmetering.HAWMeteringWebserviceService;
import hawmetering.WebColor;

public class Sensor {

	public static void main(String[] args) throws InterruptedException {
		startWebservice(args);
		
	}

	public static void startWebservice(String[] args){
		//args[0] = MeterName, args[1] = Anzahl an URLs, args[2] Array von URLs fuer die Meter, args[3] = firstQuestionURL
		wssensor.SensorWebservice webservice;
		String [] meterURLS = new String [Integer.parseInt(args[1])];
		for (int i = 0; i < meterURLS.length; i++) {
			meterURLS[i] = args[i+2];
		}
		if (args.length>2+meterURLS.length) {
			System.out.println("mit firstQuestion");
			webservice = new SensorWebservice(args[0], meterURLS, args[2+meterURLS.length+1]);	
		}else {
			System.out.println("ohne firstQuestion");
			webservice = new SensorWebservice(args[0], meterURLS);
		}
		
		System.out.println("sensor lauft...");
		System.out.println(args[0]);
		//+ args[0]
        Endpoint.publish("http://0.0.0.0:8888/hawsensor/", webservice);
	}
}
