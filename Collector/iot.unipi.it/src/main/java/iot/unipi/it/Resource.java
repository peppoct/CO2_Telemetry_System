package iot.unipi.it;

public class Resource {
	String nodeAddress;
	String resourcePath;
	String uri;
	
	public Resource(String address, String path) {
		this.nodeAddress = address;
		this.resourcePath = path;
		this.uri = "coap://[" + this.nodeAddress + "]/" + this.resourcePath;
	}
	
	public String getNodeAddress() {
		return this.nodeAddress;
	}

	public String getResourcePath() {
		return this.resourcePath;
	}
	
	public String getResourceURI() {
		return this.uri;
	}

	public void setNodeAddress(String address) {
		this.nodeAddress = address;
	}
	
	public void setResourcePath(String path) {
		this.resourcePath = path;
	}

}
