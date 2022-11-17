package iot.unipi.it;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.parser.ParseException;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP.Code;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.coap.Request;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class CollectorCOAP {
	
	private CoapClient client;
	private CoapClient resCO2;
	private CO2Device c;
	
	private short state = 0;
	private boolean stopObserve = false;
	
	private static final CO2DBService cs = CO2DBService.getInstance();
	private static final Logger logger = LogManager.getLogger(CO2DBService.class);

	
	public CollectorCOAP(CO2Device c) {
		this.client = new CoapClient("coap://[" + c.getIP() + "]/co2");
		this.c = c;
	}
	
	public void startObserving() {
		CoapObserveRelation newObserveCO2 = this.client.observe(new CoapHandler() {
			public void onLoad(CoapResponse response) {
				boolean success = true;
				
				long timestamp = 0;
				int value = 0;
				int nodeId  = 0;
				
				int LOW_LEVEL_CO2 = 500;
				int HIGH_LEVEL_CO2 = 3500;
				
				if (response.getResponseText() == null | response.getResponseText() == "")
					return;
				
				System.out.println("Response: " + response.getResponseText());
				
				try {
					JSONObject sensorMessage = (JSONObject) JSONValue.parseWithException(response.getResponseText());
					timestamp = Integer.parseInt(sensorMessage.get("timestamp").toString());
					value = Integer.parseInt(sensorMessage.get("co2").toString());
					System.out.println("Value: " + value);
					nodeId = Integer.parseInt(sensorMessage.get("node_id").toString());		
				} catch (org.json.simple.parser.ParseException e) {
					// TODO Auto-generated catch block
					System.out.println("Impossible to parse the response" + e);
					success = false;
					e.printStackTrace();
				}
				
				if (!success)
					return;

				
				if (value < LOW_LEVEL_CO2) {
					state = 0;
					String payload = "mode=on";
                	Request req = new Request(Code.POST);
                	req.setConfirmable(true);
                	req.setPayload(payload);
                	//req.setURI(a.getResourceURI()+"?color=g");
                	req.setURI("coap://[" + c.getIP() + "]/alarm?color=g");
                	req.send();
					System.out.println("CO2 Level OK.");
				} else if (value >= LOW_LEVEL_CO2 && value <= HIGH_LEVEL_CO2) {
					state = 1;
					String payload = "mode=on";
                	Request req = new Request(Code.POST);
                	req.setConfirmable(true);
                	req.setPayload(payload);
                	//req.setURI(a.getResourceURI()+"?color=y");
                	req.setURI("coap://[" + c.getIP() + "]/alarm?color=y");
                	req.send();
					System.out.println("CO2 Level HIGH!");
				} else if (value > HIGH_LEVEL_CO2) {
					state = 2;
					String payload = "mode=on";
                	Request req = new Request(Code.POST);
                	req.setConfirmable(true);
                	req.setPayload(payload);
                	//req.setURI(a.getResourceURI()+"?color=r");
                	req.setURI("coap://[" + c.getIP() + "]/alarm?color=g");
                	req.send();
					System.out.println("CO2 Level TOO HIGH!");
				}
				System.out.println("Writing to DB: " + ("Node: "+ nodeId +
						"\ttimestamp: "+timestamp+
						"\tvalue: "+value));
				
				cs.addObservation("coap://"+nodeId, timestamp, value);
				
			}
			
			public void onError() {
				stopObserve = true;
				System.out.println("Observing failed from " + c.getResourceURI());
				return;
			}

		}, MediaTypeRegistry.APPLICATION_JSON);
		
		if (stopObserve) {
			newObserveCO2.proactiveCancel();
		}
	}
	
	
}
