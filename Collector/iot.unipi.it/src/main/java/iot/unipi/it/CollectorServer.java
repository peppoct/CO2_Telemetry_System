package iot.unipi.it;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.californium.core.CoapServer;

public class CollectorServer extends CoapServer {
	//private static final Logger logger = LogManager.getLogger(CollectorServer.class);
	
	public CollectorServer() {
		this.add(new ResRegistration("registration"));
		System.out.println("Coap server is ready!");
	}
}
