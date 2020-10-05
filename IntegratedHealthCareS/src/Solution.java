import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class Solution {
	private long id; // solution ID
	private double durationSolution = 0.0; // Travel distance = waiting time + driving time
	private LinkedList<Route> routes = new LinkedList<Route>(); // list of routes in this solution
	private int passengers=0;// home care staff and + paramedic transported que salen del depot
	private double waitingTime=0;// Total waiting time
	private double serviceTime=0;
	private double drivingTime=0;
	private double walkingTime=0;
	private double paramedic=0;// los paramedicos que salen del depot
	private double homeCareStaff=0;// los paramedicos que salen del depot

	// Setters 
	public void setId(long id) {this.id = id;}
	public void setDurationSolution(double costs) {this.durationSolution = costs;}
	public void setRoutes(LinkedList<Route> routes) {this.routes = routes;}

	public void setPassengers(int p) {
		for(Route r:this.routes) {
			paramedic+=r.getAmountParamedic();// los paramedicos que salen del depot
			homeCareStaff+=r.getHomeCareStaff();// los paramedicos que salen del depot	
		}
		this.passengers = p;
	}


	public void setWaitingTime(double w) {waitingTime=w;}
	public void setServiceTime(double s) {serviceTime=s;}
	public void setdrivingTime(double s) {drivingTime=s;}
	public void setWalkingTime(double w) {walkingTime=w;}

	// Getters
	public long getId() { return id;}
	public double getDurationSolution() { return durationSolution;}
	public int getPassengers() { return passengers;}
	public LinkedList<Route> getRoutes() {return routes;}
	public double getWaitingTime(){return waitingTime; }
	public double getServiceTime() {return serviceTime;}
	public double getdrivingTime(){return drivingTime;}
	public double getWalkingTime() {return walkingTime;}

	@Override
	public String toString() 
	{   String s = "";
	s = s.concat("\nID Solution: " + id);
	s = s.concat("\nFO: Travel time: " + durationSolution);
	s = s.concat("\nWalking time: " + walkingTime);
	s = s.concat("\n Total passengers: " + passengers);
	s = s.concat("\n Total paramedic: " + paramedic);
	s = s.concat("\n Total home care staff: " + homeCareStaff);
	s = s.concat("\n Waiting time: " + waitingTime);
	s= s.concat("\n Service time: "+  serviceTime);
	s = s.concat("\n List of jobs: ");
	for(Route r:routes) {
		if(!r.getSubJobsList().isEmpty()) {
			s= s.concat("\n Route: "+ r.getIdRoute());
			for(SubJobs j:r.getSubJobsList()) {	
				s = s.concat(" ( Id" + j.getSubJobKey()+", A_"+j.getArrivalTime()+", B_"+j.getstartServiceTime()+", reqTime_"+j.getReqTime()+") ");}
		}	
	}
	return s;
	}

}

