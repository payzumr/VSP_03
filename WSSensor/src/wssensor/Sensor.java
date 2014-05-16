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
		//args[0] = MeterName, args[1] = Adresse des Meters (z.b. localhost), args [2]= Port des Meters
		//args[3] = Anzahl an Meter, args[4] Array von Namen der Meter, args[5] = firstQuestionURL
		wssensor.SensorWebservice webservice;
		String [] meterURLS = new String [Integer.parseInt(args[3])];
		for (int i = 0; i < meterURLS.length; i++) {
			meterURLS[i] = "http://" + args[1] + ":" + args[2] + "/hawmetering/" + args[i+4] + "?WSDL";
		}
		if (args.length>4+meterURLS.length) {
			System.out.println("mit firstQuestion");
			webservice = new SensorWebservice(args[0], meterURLS, args[4+meterURLS.length+1]);	
		}else {
			System.out.println("ohne firstQuestion");
			webservice = new SensorWebservice(args[0], meterURLS);
		}
		
		System.out.println("sensor lauft...");
		//+ args[0]
        Endpoint.publish("http://0.0.0.0:8888/hawsensor/", webservice);
	}
}
