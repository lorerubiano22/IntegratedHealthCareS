import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class Solution {
	private long id; // solution ID
	private double durationSolution = 0.0; // Travel distance = waiting time + driving time
	private LinkedList<Route> routes = new LinkedList<Route>(); // list of routes in this solution
	private int passengers=0;// number of patient + home care staff and + paramedic transported
	private double waitingTime=0;// Total waiting time
	private double serviceTime=0;

	// Setters 
	public void setId(long id) {this.id = id;}
	public void setDurationSolution(double costs) {this.durationSolution = costs;}
	public void setRoutes(LinkedList<Route> routes) {this.routes = routes;}
	public void setPassengers(int p) {this.passengers = p;}
	public void setWaitingTime(double w) {waitingTime=w;}
	public void setServiceTime(double s) {serviceTime=s;}

	// Getters
	public long getId() { return id;}
	public double getDurationSolution() { return durationSolution;}
	public int getPassengers() { return passengers;}
	public LinkedList<Route> getRoutes() {return routes;}
	public double getWaitingTime(){return waitingTime; }
	public double getServiceTime() {return serviceTime;}


	@Override
	public String toString() 
	{   String s = "";
	s = s.concat("\nID Solution: " + id);
	s = s.concat("\nFO: Travel time: " + durationSolution);
	s = s.concat("\n Total passengers: " + passengers);
	s = s.concat("\n Waiting time: " + waitingTime);
	s= s.concat("\n Service time: "+  serviceTime);
	s = s.concat("\n List of jobs: ");
	for(Route r:routes) {
		if(!r.getSubJobsList().isEmpty()) {
			s= s.concat("\n Route: "+ r.getIdRoute());
			for(SubJobs j:r.getSubJobsList()) {	
				s = s.concat(" j_( Id" + j.getSubJobKey()+", B_"+j.getstartServiceTime()+") ");}
		}	
	}
	return s;
	}

}

