package wssensor;

import hawmetering.HAWMeteringWebservice;
import hawmetering.HAWMeteringWebserviceService;
import hawmetering.WebColor;

import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.jws.soap.SOAPBinding.*;

@WebService
@SOAPBinding(style = Style.RPC)
public class SensorWebservice {

	public SensorWebservice() {

	}

	public void doSomething(@WebParam(name = "value") double value) {
		HAWMeteringWebserviceService service = new HAWMeteringWebserviceService();
		HAWMeteringWebservice meter = service.getHAWMeteringWebservicePort();

		meter.clearIntervals();
		meter.setTitle("NerdWest");
		WebColor wc = new WebColor();
		wc.setGreen(255);
		wc.setAlpha(150);
		meter.setIntervals("low", 0, 50, wc);

		meter.setValue(value);
	}

	public void newTick() {

	}

	public/* Sensor */void getKoordinator() {

	}

	public boolean iWantThisGauges(/* Gauge liste */) {
		return false;// TODO
	}

	public void registerSensor() {

	}

	public void setGaugeAssignments(/* Liste */) {

	}
	/* election */
}
