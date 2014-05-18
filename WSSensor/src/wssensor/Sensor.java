package wssensor;

import javax.xml.ws.Endpoint;

import hawmetering.HAWMeteringWebservice;
import hawmetering.HAWMeteringWebserviceService;
import hawmetering.WebColor;

public class Sensor {
	public static String myUrl ;
	public static void main(String[] args) throws InterruptedException {
		myUrl = "http://0.0.0.0:" + args[3] + "/hawsensor/" + args[0];
		
		startWebservice(args);
		
	}

	public static void startWebservice(String[] args){
		//args[0] = MeterName, args[1] = Adresse des Meters (z.b. localhost), args [2]= Port des Meters
		//args[4] = Anzahl an Meter, args[5] Array von Namen der Meter, args[6] = firstQuestionURL
		wssensor.SensorWebservice webservice;
		String [] meterURLS = new String [Integer.parseInt(args[4])];
		for (int i = 0; i < meterURLS.length; i++) {
			meterURLS[i] = "http://" + args[1] + ":" + args[2] + "/hawmetering/" + args[i+5] + "?WSDL";
		}
		if (args.length>5+meterURLS.length) {
			System.out.println("mit firstQuestion");
			webservice = new SensorWebservice(args[0], meterURLS, args[5+meterURLS.length]);	
		}else {
			System.out.println("ohne firstQuestion");
			webservice = new SensorWebservice(args[0], meterURLS);
		}
		
		System.out.println("sensor lauft...");
        Endpoint.publish(myUrl, webservice);
        if(!webservice.initializeMeter(myUrl)){
        	System.out.println("ERROR");
        	System.exit(0);
        }
	}
}
