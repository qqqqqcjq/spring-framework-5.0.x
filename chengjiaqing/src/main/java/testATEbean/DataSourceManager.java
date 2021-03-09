package testATEbean;

/**
 * @date 2019/12/8 18:18
 * @author chengjiaqing
 * @version : 0.1
 */ 
 
  public class DataSourceManager {

  	private String name;
  	private String passwrord;
  	private String url;
  	private String port;

	public String getPasswrord() {
		return passwrord;
	}

	public void setPasswrord(String passwrord) {
		this.passwrord = passwrord;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DataSourceManager(String name, String passwrord, String url, String port) {
		this.name = name;
		this.passwrord = passwrord;
		this.url = url;
		this.port = port;
	}
}