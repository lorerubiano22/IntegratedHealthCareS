import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;



public class Inputs {
	/* INSTANCE FIELDS & CONSTRUCTOR */

	private final List<Jobs> nodes; // array of all nodes (node 0 is
	// depot)
	private final HashMap<Integer,Jobs> clients=new HashMap<Integer,Jobs> (); // clients job
	private final HashMap<Integer,Jobs>  patients=new HashMap<Integer,Jobs> (); // patients job at patient home
	private final HashMap<Integer,Jobs>  medicalCentre=new HashMap<Integer,Jobs> (); // patients job at medical centre

	private final TimeMatrix walkTravelTime;
	private final TimeMatrix drivingTime;
	private final List<AttributeNurse> nurses;
	private final List<AttributeParamedics> paramedics;
	private final List<VehicleBase> vehicles; 
	private int maxQualificationLevel; 



	public Inputs(List<Jobs> nodes, TimeMatrix carCost, TimeMatrix walkCost,
			List<AttributeNurse> nurse, List<AttributeParamedics> paramedic, List<VehicleBase> vehicles) {
		this.nodes = nodes;
		this.walkTravelTime = walkCost;
		this.drivingTime = carCost;
		this.nurses = nurse;
		this.paramedics = paramedic;
		this.vehicles = vehicles;
		maxQualificationLevel= Integer.MIN_VALUE;
		for(Jobs j:nodes) {
			if(j.getReqQualification()>0 && j.getId()!=1) {
				j.setClient(true);
				clients.put(j.getId(),j);
				if(maxQualificationLevel<j.getReqQualification()) {
					maxQualificationLevel=j.getReqQualification();
				}
			}
			else { // patients <- it considers location of the medical centre. Beacause there is a drop-off and pick-up job
				if(j.getSoftStartTime()!=0 && j.getId()!=1) {
					j.setPatient(true);
					patients.put(j.getId(),j);
				}
				else {
					if(j.getId()!=1) {
					j.setMedicalCentre(true);
					medicalCentre.put(j.getId(),j);}
				}
			}
		}
		assigmentMedicalCentre();// patient to medical centre
	}
	private void assigmentMedicalCentre() {
		HashMap<Integer, Jobs> asssigmentPatients=new HashMap<>();
		while(asssigmentPatients.size()<patients.size()) {
			for(Jobs medicalCentreLocation:medicalCentre.values()) {
				for(Jobs patient:patients.values()) { 
					if(!asssigmentPatients.containsKey(patient.getId())) {
						patient.setPair(medicalCentreLocation);
						medicalCentreLocation.getAssignedJobToMedicalCentre().add(patient);
						asssigmentPatients.put(patient.getId(),patient);
						break;
					}
				}
			}
		}
	}



	/* GET METHODS */

	public HashMap<Integer,Jobs> getclients() {
		return clients;
	}
	public HashMap<Integer,Jobs> getpatients() {
		return patients;
	}
	public HashMap<Integer,Jobs> getMedicalCentre() {
		return medicalCentre;
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
	
	
	public int getMaxQualificationLevel() {
		return maxQualificationLevel;
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
