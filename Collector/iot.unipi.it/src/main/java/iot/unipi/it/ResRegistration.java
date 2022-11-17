package iot.unipi.it;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.server.resources.ResourceAttributes;
import org.eclipse.californium.core.server.resources.ResourceObserver;

public class ResRegistration extends CoapResource {

	private static final Logger logger = LogManager.getLogger(CO2Device.class);
	private static Collection<CO2Device> devices = Collections.synchronizedList(new ArrayList<CO2Device>());
	private static final CO2DBService cs = CO2DBService.getInstance();
	private CO2Device c;
	
	public ResRegistration(String name) {
		super(name);
	}
	
	@Override
	public void handlePOST(CoapExchange exchange) {
				
		exchange.accept();
		String deviceType = exchange.getRequestText();
		String sourceAddress = exchange.getSourceAddress().getHostAddress();
		
		CoapClient client = new CoapClient("coap://[" + sourceAddress + "]:5683/.well-known/core");
		CoapResponse response = client.get(MediaTypeRegistry.APPLICATION_JSON);
		

		
		//boolean registered = true;
		
		if (contains(sourceAddress) < 0) {
			if(cs.addSensor(sourceAddress)) {
				synchronized(devices) {
					ResRegistration.devices.add(new CO2Device(sourceAddress, this.getPath()));
					CO2Device c = new CO2Device(sourceAddress, this.getPath());
					observe(c);
				}
				System.out.println("New device registered! Address: [" + sourceAddress + "]");
				exchange.respond(CoAP.ResponseCode.CREATED, "Registration successful!".getBytes(StandardCharsets.UTF_8));
			} else {
				System.out.println("Impossible to add new device!");
				exchange.respond(CoAP.ResponseCode.NOT_ACCEPTABLE, "Registration unsuccessful".getBytes(StandardCharsets.UTF_8));	
			}
		} else {
			System.out.println("Device " + sourceAddress + " already registered!");		
		}
	}
	

	private static int contains(final String ipAddress) {
		int idx = -1;
		
		for(CO2Device device : devices) {
			idx++;
			if(device.getIP().contentEquals(ipAddress))
				return idx;
		}
		return -1;
	}
	
	private static void observe(CO2Device c) {
		CollectorCOAP observer = new CollectorCOAP(c);
		observer.startObserving();
	}

}
