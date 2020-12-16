import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

public class Schift {

	private int id=0;
	private String key="";
	private int qualificationLevel=-1;
	private boolean paramedicSchift=false;
	private boolean homecareStaffSchift=false;
	private boolean driver=false;
	private  Route route; // the shift is a route in order to count the working hours
	private  ArrayList<Parts> routeParts= new ArrayList<>(); // the shift is a route in order to count the working hours
	private  ArrayList<Parts> routeParts1= new ArrayList<Parts>();
	private  ArrayList<Edge> listConnections= new ArrayList<Edge>();

	// debo cambiar tambien la siguiente
	private  HashMap<String, Parts> listParts= new HashMap<>(); // the shift is a route in order to count the working hours
	

	// constructor
	public Schift(Route r1, int id) {
		this.id=id;
		route=r1;
		if(r1.getAmountParamedic()>0) {
			paramedicSchift=true;
		}
		if(r1.getHomeCareStaff()>0) {
			homecareStaffSchift=true;
		}
		if(r1.getAmountDriver()>0) {
			driver=true;
		}

		for(Parts part:r1.getPartsRoute()) {// inserting the parts according to the times
			Parts newPart= new Parts(part);		
			key= "T"+id+"P"+id;
			listParts.put(key, newPart);
		}

		for(Parts copy:listParts.values()) {
			routeParts.add(copy);
		}
		// definition of the qualification levels
		qualificationLevel=-1;
		for(SubJobs j:r1.getSubJobsList()) {
			if(j.getReqQualification()>qualificationLevel) {
				qualificationLevel=j.getReqQualification();
			}
		}
		System.out.println("all shift are defined");
	}	


	public Schift(Schift schiftRoute) {
		id=schiftRoute.getId();
		key=schiftRoute.getKey();
		qualificationLevel=schiftRoute.getQualificationLevel();
		paramedicSchift=schiftRoute.isParamedicSchift();
		homecareStaffSchift=schiftRoute.isHomecareStaffSchift();
		driver=schiftRoute.isDriverSchift();
		route=schiftRoute.getRoute(); // the shift is a route in order to count the working hours
		listConnections=schiftRoute.getConnections();
		for(Parts a:schiftRoute.getlistParts().values()) {
			Parts newParts=new Parts (a);
			key= "T"+id+"P"+id;
			listParts.put(key, newParts);
		}
		for(Parts a:listParts.values()) {
			routeParts.add(a);
		}
	}




	// setters
	public void setParamedicSchift(boolean paramedicSchift) {this.paramedicSchift = paramedicSchift;}
	public void setRouteList(Route route) {this.route = route;}
	public void setHomecareStaffSchift(boolean homecareStaffSchift) {this.homecareStaffSchift = homecareStaffSchift;}
	public void setId(int id) {this.id = id;}
	public void setQualificationLevel(int q) {this.qualificationLevel =q;}



	// getters
	public boolean isParamedicSchift() {return paramedicSchift;}
	public boolean isHomecareStaffSchift() {return homecareStaffSchift;}
	public boolean isDriverSchift() {return driver;}
	public Route getRoute() {return route;}
	public ArrayList<Parts> getRouteParts() {return routeParts;}
	public HashMap<String, Parts> getlistParts() {return listParts;}
	public int getId() {return id;}
	public int getQualificationLevel() {return qualificationLevel;}
	public String getKey() {return key;}
	public ArrayList<Edge> getConnections() {return listConnections;}
	






	public String toString() 
	{   String s = "";
	s = s.concat("\nId: " + (key));
	s = s.concat("\n qualification Level: " + qualificationLevel);
	s = s.concat("\n first Job: " + (routeParts.get(0).getListSubJobs().get(1)));
	s = s.concat("\n start service:" + routeParts.get(0).getListSubJobs().get(1).getstartServiceTime());
	return s;
	}



}
