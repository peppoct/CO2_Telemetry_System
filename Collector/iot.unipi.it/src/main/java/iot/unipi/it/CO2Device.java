package iot.unipi.it;

public class CO2Device extends Resource {

	public CO2Device(String ipAddress, String path)  {
		// TODO Auto-generated constructor stub
		super(ipAddress, path);
	}


	public String getIP() {
		return getNodeAddress();
	}
	
}
