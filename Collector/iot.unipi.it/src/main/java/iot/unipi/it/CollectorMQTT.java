package iot.unipi.it;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.parser.ParseException;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class CollectorMQTT implements MqttCallback{

	private static String broker = "tcp://127.0.0.1:1883";
	private static String clientId = "JavaCollector";
	private static String subTopic = "co2";
	private static String pubTopic = "alarm";
	private static MqttClient mqttClient = null;
	private short state = 0;
	private static final Logger logger = LogManager.getLogger(CollectorMQTT.class);
	private static final CO2DBService cs = CO2DBService.getInstance();

	
	public CollectorMQTT() {
		do {
			int timeWindow = 50000;
			try {
				this.mqttClient = new MqttClient(this.broker, this.clientId);
				this.mqttClient.setCallback(this);
				this.mqttClient.connect();
				this.mqttClient.subscribe(subTopic);
			} catch (MqttException me){
				System.out.println("CollectorMQTT unable to connect, Retrying ..." + me);
				try {
					Thread.sleep(timeWindow);
				} catch (InterruptedException ie) {
					System.out.println("Something wrong with thread sleep!" + ie);
				}
			}
		} while (!this.mqttClient.isConnected());
		System.out.println("MQTT connected!");
	}
	
	public void publish(String content, String node) throws MqttException{
		try {
			MqttMessage message = new MqttMessage(content.getBytes());
			//this.mqttClient.publish(this.pubTopic+node, message);
			this.mqttClient.publish(this.pubTopic+node, message);
			System.out.println("MQTT alarm published!");
		} catch(MqttException me) {
			System.out.println("Impossible to publish!" + me);
		}
	}
	
	@Override
	public void connectionLost(Throwable cause) {
		// TODO Auto-generated method stub	
		System.out.println("Connection lost!");
		int timeWindow = 3000;
		while(!this.mqttClient.isConnected()) {
			try {
				System.out.println("Trying to reconnect in " + timeWindow/1000 + " seconds.");
				Thread.sleep(timeWindow);
				System.out.println("Reconnecting..");
				timeWindow *= 2;
				this.mqttClient.connect();
				this.mqttClient.subscribe(this.subTopic);
				System.out.println("Connection restored.");	
			} catch (MqttException me) {
				System.out.println("CollectorMQTT unable to connect");
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
	}

	
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// TODO Auto-generated method stub
		byte[] payload = message.getPayload();
		System.out.println("Message arrived: " + new String(payload));
		JSONObject sensorMessage = (JSONObject) JSONValue.parseWithException(new String(payload));
		if (sensorMessage.containsKey("co2")) {
			int timestamp = Integer.parseInt(sensorMessage.get("timestamp").toString());
			Integer value = Integer.parseInt(sensorMessage.get("co2").toString());
			String nodeId = sensorMessage.get("node").toString();
			if(!cs.checkSensorExistence("mqtt://"+nodeId)) {
				cs.addSensor("mqtt://"+nodeId);
			}
			cs.addObservation("mqtt://"+nodeId, timestamp, value);
			int lower_bound = 500;
			int upper_bound = 3500;
			boolean on = false;
			String reply;
			
			if (value > upper_bound) {
				if (state != 2) {
					state = 2;
					reply = "CO2 too high";
					publish(reply, nodeId);
					System.out.println("[CRITICAL] - "+ nodeId +" - the level of CO2 is too high!");
				}
			} else if (value > lower_bound && value <= upper_bound) {
				if (state != 1) {
					state = 1;
					reply = "CO2 high";
					publish(reply, nodeId);
					System.out.println("[WARNING] - "+ nodeId +" - the level of CO2 is high!");
				}
			} else {
				if (state != 0) {
					state = 0;
					reply = "CO2 ok";
					publish(reply, nodeId);
					System.out.println("[NORMAL] - "+ nodeId +" - the level of CO2 is OK!");
				}
			}
		}
	}

	
	public void deliveryComplete(IMqttDeliveryToken token) {
		// TODO Auto-generated method stub
		System.out.println("Delivery completed.");
	}

}
