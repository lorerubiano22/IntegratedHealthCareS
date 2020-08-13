import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;



public class Inputs {
	/* INSTANCE FIELDS & CONSTRUCTOR */

	private final List<Jobs> nodes; // array of all nodes (node 0 is
	// depot)
	private final List<Jobs> clients=new LinkedList<Jobs>(); // clients job
	private final List<Jobs> patients=new LinkedList<Jobs>(); // patients job at patient home
	private final List<Jobs> medicalCentre=new LinkedList<Jobs>(); // patients job at medical centre

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
				clients.add(j);
				if(maxQualificationLevel<j.getReqQualification()) {
					maxQualificationLevel=j.getReqQualification();
				}
			}
			else { // patients <- it considers location of the medical centre. Beacause there is a drop-off and pick-up job
				if(j.getSoftStartTime()!=0 && j.getId()!=1) {
					j.setPatient(true);
					patients.add(j);
				}
				else {
					if(j.getId()!=1) {
					j.setMedicalCentre(true);
					medicalCentre.add(j);}
				}
			}
		}
		assigmentMedicalCentre();// patient to medical centre
	}
	private void assigmentMedicalCentre() {
		HashMap<Integer, Jobs> asssigmentPatients=new HashMap<>();
		while(asssigmentPatients.size()<patients.size()) {
			for(int i=0; i<medicalCentre.size();i++) {
				Jobs medicalCentreLocation=medicalCentre.get(i);
				for(int j=asssigmentPatients.size();j<patients.size();j++) { 
					Jobs patient=patients.get(j);
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
