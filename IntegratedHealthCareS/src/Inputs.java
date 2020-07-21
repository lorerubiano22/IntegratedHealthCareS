import java.util.Collections;
import java.util.LinkedList;
import java.util.List;



public class Inputs {
	/* INSTANCE FIELDS & CONSTRUCTOR */

	private final List<Jobs> nodes; // array of all nodes (node 0 is
											// depot)
	private final List<Jobs> clients=new LinkedList<Jobs>(); // clients job
	private final List<Jobs> patients=new LinkedList<Jobs>(); // patients job
	private final TimeMatrix walkTravelTime;
	private final TimeMatrix drivingTime;
	private final List<AttributeNurse> nurses;
	private final List<AttributeParamedics> paramedics;
	private final List<VehicleBase> vehicles; 
	


	public Inputs(List<Jobs> nodes, TimeMatrix carCost, TimeMatrix walkCost,
			List<AttributeNurse> nurse, List<AttributeParamedics> paramedic, List<VehicleBase> vehicles) {
		this.nodes = nodes;
		this.walkTravelTime = walkCost;
		this.drivingTime = carCost;
		this.nurses = nurse;
		this.paramedics = paramedic;
		this.vehicles = vehicles;
		
		for(Jobs j:nodes) {
			if(j.getReqQualification()>0) {
				clients.add(j);
			}
			else {
				patients.add(j);
			}
		}
	}
	/* GET METHODS */

	public List<Jobs> getclients() {
		return clients;
	}
	public List<Jobs> getpatients() {
		return patients;
	}
	public List<Jobs> getNodes() {
		return Collections.unmodifiableList(nodes);
	}
	
	public TimeMatrix getWalkCost() {
		return walkTravelTime;
	}

	public TimeMatrix getCarCost() {
		return drivingTime;
	}

	public List<AttributeNurse> getNurse() {
		return nurses;
	}
	public List<AttributeParamedics> getParamedic() {
		return paramedics;
	}

	public List<VehicleBase> getVehicles() {
		return vehicles;
	}

	

}
