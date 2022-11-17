package iot.unipi.it;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.LogManager;

import org.eclipse.californium.core.CoapServer;

public class Collector {
	
	private static CO2DBService cs = CO2DBService.getInstance();
	//private static CO2Device c;
	//protected static Collection<CO2Device> devices = Collections.synchronizedList(new ArrayList<CO2Device>());
	protected static ArrayList<CO2Device> devices = new ArrayList<CO2Device>();
	
	
	public static void main(String[] args) throws InterruptedException {
        LogManager.getLogManager().reset();
        
		
		CollectorMQTT cm = new CollectorMQTT();
		CollectorServer server = new CollectorServer();
		server.start();
	}
}
