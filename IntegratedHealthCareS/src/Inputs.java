import java.util.Collections;
import java.util.List;



public class Inputs {
	/* INSTANCE FIELDS & CONSTRUCTOR */

	private final List<Jobs> nodes; // array of all nodes (node 0 is
											// depot)
	private final TimeMatrix walkTravelTime;
	private final TimeMatrix drivingTime;
	private final List<AttributeNurse> nurses;
	private final List<AttributeParamedics> paramedics;
	private final List<VehicleBase> vehicles; 
	
	/* GET METHODS */

	public List<Jobs> getNodes() {
		return Collections.unmodifiableList(nodes);
	}

	public Inputs(List<Jobs> nodes, TimeMatrix carCost, TimeMatrix walkCost,
			List<AttributeNurse> nurse, List<AttributeParamedics> paramedic, List<VehicleBase> vehicles) {
		this.nodes = nodes;
		this.walkTravelTime = walkCost;
		this.drivingTime = carCost;
		this.nurses = nurse;
		this.paramedics = paramedic;
		this.vehicles = vehicles;
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
