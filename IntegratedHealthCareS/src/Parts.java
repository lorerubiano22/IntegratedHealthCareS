import java.util.ArrayList;
import java.util.HashMap;

public class Parts {
	private String key="";
	private int qualificationLevel=-1;
	private boolean paramedicSchift=false;
	private boolean homecareStaffSchift=false;
	private boolean driver=false;
	private  Route routeVehicle; // the shift is a route in order to count the working hours
	private  ArrayList<SubJobs> listSubJobs= new ArrayList<>(); // the shift is a route in order to count the working hours

	private  HashMap<String, SubJobs> directorySubjobs= new HashMap<>(); // the shift is a route in order to count the working hours
	private  HashMap<String, Edge> directoryConnections= new HashMap<>(); // las conecciones entre las partes// este me permite controlar los subtours


	// set of getters

	public Parts(Parts part) {
		key=part.getKey();
		qualificationLevel=part.getQualificationLevel();
		paramedicSchift=part.isParamedicSchift();
		homecareStaffSchift=part.isHomecareStaffSchift();
		driver=part.isDriver();
		for(SubJobs n:part.getListSubJobs()) {
			Jobs formalJob=new Jobs(n);
			listSubJobs.add(new SubJobs(formalJob));
			directorySubjobs.put(n.getSubJobKey(), n);
		}
	}


	public Parts() {
		listSubJobs= new ArrayList<>(); // the shift is a route in order to count the working hours
		directorySubjobs= new HashMap<>(); // the shift is a route in order to count the working hours
		directoryConnections= new HashMap<>(); // las conecciones entre las partes// este me permite controlar los subtours

	}



	public String getKey() {return key;}
	public int getQualificationLevel() {
		return qualificationLevel;
	}
	public boolean isParamedicSchift() {
		return paramedicSchift;
	}
	public boolean isHomecareStaffSchift() {
		return homecareStaffSchift;
	}
	public boolean isDriver() {
		return driver;
	}
	public Route getRouteVehicle() {
		return routeVehicle;
	}
	public ArrayList<SubJobs> getListSubJobs() {
		return listSubJobs;
	}
	public HashMap<String, SubJobs> getDirectorySubjobs() {
		return directorySubjobs;
	}
	public HashMap<String, Edge> getDirectoryConnections() {
		return directoryConnections;
	}


	public void setListSubJobs(ArrayList<SubJobs> listSubJobs) {
		this.listSubJobs = listSubJobs;
	}
	public void setKeyParts(String k) {
		this.key = k;
	}

}
