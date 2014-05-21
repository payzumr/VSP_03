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
		//args [3] eigener Port args[4] meter, args [5] = firstQuestionURL
		wssensor.SensorWebservice webservice;
		String meterURL = "http://" + args[1] + ":" + args[2] + "/hawmetering/" + args[4] + "?WSDL";
		if (args.length>5) {
			System.out.println("mit firstQuestion");
			webservice = new SensorWebservice(args[0], meterURL, args[5]);	
		}else {
			System.out.println("ohne firstQuestion");
			webservice = new SensorWebservice(args[0], meterURL);
		}
		
		System.out.println("sensor lauft...");
        Endpoint.publish(myUrl, webservice);
        if(!webservice.initializeMeter(myUrl)){
        	System.out.println("ERROR");
        	System.exit(0);
        }
        
	}
}
