package wssensor;

import hawsensor.*;

public class testframe {

	public static void main(String[] args) {
		hawsensor.SensorWebserviceService service = new SensorWebserviceService();
		hawsensor.SensorWebservice sws = service.getSensorWebservicePort();
		
		sws.doSomething(50);

	}

}
