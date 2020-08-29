import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class Solution {
	private long id; // solution ID
	private double durationSolution = 0.0; // Travel distance = waiting time + driving time
	private LinkedList<Route> routes; // list of routes in this solution
	private int passengers=0;// number of patient + home care staff and + paramedic transported

	// Setters 
	public void setId(long id) {this.id = id;}
	public void setDurationSolution(double costs) {this.durationSolution = costs;}
	public void setRoutes(LinkedList<Route> routes) {this.routes = routes;}
	public void setPassengers(int p) {this.passengers = p;}

	// Getters
	public long getId() { return id;}
	public double getDurationSolution() { return durationSolution;}
	public int getPassengers() { return passengers;}
	public LinkedList<Route> getRoutes() {return routes;}




}
