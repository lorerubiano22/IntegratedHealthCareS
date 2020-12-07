import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Jobs {
	/* INSTANCE FIELDS & CONSTRUCTOR */

	// - 1: a sub-Job which represents that a person is leaving the vehicle
	// 1 : a sub-Job which represents that a person is boarding the vehicle
	private int idUser;
	private String subJobKey="";
	private int id; // node ID (depotID = 0)
	private double hardstartTime; // input data early time window 
	private double hardendTime; // input data end time window
	private double softstartTime; // estimated early soft time window
	private double softendTime; // estimated end soft time window 
	private int reqQualification;
	private double startServiceTime; // time when the service start
	private double endServiceTime; // time when the service start
	private double serviceTime; // required service time
	private double sortETWSizeCriterion; // sort criterion the size of time window and the earliest time
	private double sortLTWSizeCriterion; // sort criterion the size of time window and the latest time
	private int totalPeople=0; // people involven in the service:
	// vehicle 
//	private double vehicleArrivalTime=0; // time for vehicle
//	private double vehicleDepartureTime=0; // time for vehicle
	
	// preparation time
	private double loadUnloadRegistrationTime=0;
	private double loadUnloadTime=0;
	// - 1  es una persona menos en el vehículo + 1 una plaza ocupada en el vehículo + 2 dos plazas ocupadas en el vehículo 
	private double arrivalTime=0; // time for vehicle
	private double walkingTime=0; // time for vehicle
	private double departureTime=0; // time for vehicle
	private boolean isServerd = false;
	private double waitingTime=0;
	private Jobs subJobPair; // it is the near future task asociated to this job
	private boolean isPatient;// type of job in the system- patient job at patient home
	private boolean isMedicalCentre;// type of job in the system- patient job at medical centre home
	private boolean isClient;// type of job in the system- patient job at client home
	private ArrayList<Jobs> assignedJob= new ArrayList<Jobs>(); // Walking Route
	private int idCouple=0;
	private HashMap<Integer,SubJobs> subJobs= new HashMap<>();
	// Constructors
	private LinkedList<Jobs> walkingRoute;
	private double deltaArrivalDeparture=0;
	private double deltaArrivalStartServiceTime=0;
	private double deltarStartServiceTimeEndServiceTime=0;
	
	
	public Jobs(int id, double startTime, double endTime, int reqQualification,
			double reqTime) {
		this(id,startTime,endTime,reqQualification,reqTime,new LinkedList<Jobs>());
	}

	public Jobs(int id, double startTime, double endTime, int reqQualification,
			double reqTime, List<Jobs> formedBy) {
		this.id = id;
		this.hardstartTime = startTime;
		this.hardendTime = endTime;
		this.softstartTime = startTime;
		this.softendTime = endTime;
		this.reqQualification = reqQualification;
		this.serviceTime = reqTime;
		double sizeTW=hardendTime-hardstartTime;
		this.sortETWSizeCriterion=(startTime)*(sizeTW);
		this.sortLTWSizeCriterion=(endTime)*(sizeTW);
		
		// estar service time
		
		
		
		
	}

	public Jobs(Jobs i, double serviceStartTime) {
		this.id = i.getId();
		this.hardstartTime = i.getStartTime();
		this.hardendTime = i.getEndTime();
		this.softstartTime = i.getStartTime();
		this.softendTime = i.getEndTime();
		this.departureTime=i.departureTime;
		this.arrivalTime=i.arrivalTime;
		this.reqQualification = i.getReqQualification();
		this.serviceTime = i.getReqTime();
		this.walkingTime=i.walkingTime;
//		vehicleDepartureTime=i.getVehicleDepartureTime();
//		this.vehicleArrivalTime=i.vehicleArrivalTime;
		waitingTime=i.waitingTime;
		double sizeTW=hardendTime-hardstartTime;
		this.sortETWSizeCriterion=(hardstartTime)*(sizeTW);
		this.sortLTWSizeCriterion=(hardendTime)*(sizeTW);
		this.startServiceTime=serviceStartTime;
		endServiceTime=i.endServiceTime;
		isPatient=i.isPatient();// type of job in the system- patient job at patient home
		isMedicalCentre=i.isMedicalCentre;// type of job in the system- patient job at medical centre home
		isClient=i.isClient;// type of job in the system- patient job at client home
		idUser=i.idUser;
		this.loadUnloadRegistrationTime=i.getloadUnloadRegistrationTime();
		totalPeople=i.getTotalPeople();
		subJobKey=i.subJobKey;
		if(i.subJobPair!=null) {
		subJobPair=new Jobs(i.subJobPair);
		}
		subJobs= new HashMap<>();
		for(SubJobs j:i.getSubJobs().values()){
			subJobs.put(j.getId(), new SubJobs(j));
		}
		deltaArrivalDeparture=i.deltaArrivalDeparture;
		deltaArrivalStartServiceTime=i.deltaArrivalStartServiceTime;
		deltarStartServiceTimeEndServiceTime=i.deltarStartServiceTimeEndServiceTime;
	}

	public Jobs(Jobs i) {
		this.id = i.getId();
		this.hardstartTime = i.getStartTime();
		this.hardendTime = i.getEndTime();
		this.softstartTime = i.getStartTime();
		this.softendTime = i.getEndTime();
		this.reqQualification = i.getReqQualification();
		this.serviceTime = i.getReqTime();
//		this.vehicleArrivalTime=i.vehicleArrivalTime;
		this.departureTime=i.departureTime;
		this.walkingTime=i.walkingTime;
		this.arrivalTime=i.arrivalTime;
		this.departureTime=i.departureTime;
//		vehicleDepartureTime=i.getVehicleDepartureTime();
		this.loadUnloadRegistrationTime=i.getloadUnloadRegistrationTime();
		double sizeTW=hardendTime-hardstartTime;
		this.sortETWSizeCriterion=(hardstartTime)*(sizeTW);
		this.sortLTWSizeCriterion=(hardendTime)*(sizeTW);
		this.startServiceTime=i.getstartServiceTime();
		isPatient=i.isPatient();// type of job in the system- patient job at patient home
		isMedicalCentre=i.isMedicalCentre;// type of job in the system- patient job at medical centre home
		isClient=i.isClient;// type of job in the system- patient job at client home
		endServiceTime=i.endServiceTime;
		subJobKey=i.subJobKey;
		idUser=i.idUser;
		totalPeople=i.getTotalPeople();
		if(i.subJobPair!=null) {
			subJobPair=new Jobs(i.subJobPair);}
		walkingRoute=i.getWalkingRoute();
		deltaArrivalDeparture=i.deltaArrivalDeparture;
		deltaArrivalStartServiceTime=i.deltaArrivalStartServiceTime;
		deltarStartServiceTimeEndServiceTime=i.deltarStartServiceTimeEndServiceTime;
		
	}

	/* SET METHODS */
	public void setdeltaArrivalDeparture(double a) {this.deltaArrivalDeparture = a;}
	public void setdeltaArrivalStartServiceTime(double a) {this.deltaArrivalStartServiceTime = a;}
	public void setdeltarStartServiceTimeEndServiceTime(double a) {this.deltarStartServiceTimeEndServiceTime = a;}
	
	public void setAssignedJobToMedicalCentre(ArrayList<Jobs> jobs) {this.assignedJob = jobs;}
	public void setServerd(boolean isServerd) {this.isServerd = isServerd;}
	public void setTotalPeople(int i) {	
		subJobKey=creatingKey(i);
		this.totalPeople = i;}
	public void setId(int id) {this.id = id;}
	public void setWalkingTime(double walking) {this.walkingTime = walking;}
	public void setloadUnloadRegistrationTime(double time) {this.loadUnloadRegistrationTime = time;}
	public void setloadUnloadTime(double time) {this.loadUnloadTime = time;}
	public void setStartServiceTime(double B) {this.startServiceTime = (int) Math.ceil(B);}
	public void setEndServiceTime(double B) {this.endServiceTime = B;}
	public void setStartTime(double B) {this.hardstartTime = (int) Math.ceil(B);}
	public void setEndTime(double B) {	this.hardendTime = (int) Math.ceil(B);}
	public void setserviceTime(double B) {	this.serviceTime = (int) Math.ceil(B);}
	public void setClient(boolean client) {isClient= client;}
	public void setPatient(boolean patient) {this.isPatient = patient;}
	public void setMedicalCentre(boolean mc) {this.isMedicalCentre = mc;}
	public void setReqQualification(int qualification) {reqQualification = qualification;}
	public void setarrivalTime(double arrival) {arrivalTime=arrival;}
//	public void setvehicleArrivalTime(double arrival) {vehicleArrivalTime=arrival;}
	public void setdepartureTime(double departure) {departureTime=departure;}
//	public void setVehicledepartureTime(double departure) {vehicleDepartureTime=departure;}
	public void setIDcouple(int couple) {idCouple=couple;}
	public void setWaitingTime(double w) {this.waitingTime = w;}
	public void setWalkingRoute(LinkedList<Jobs> w) {this.walkingRoute = w;}
	public void setWaitingTime(double arrivalTime, double startService ) {
		waitingTime=0;
		if(arrivalTime<startService) {
			this.waitingTime = startService-arrivalTime;	
		}
	}

	
	/* GET METHODS */
	public boolean isServerd() {return isServerd;}
	public boolean isPatient() {return isPatient;}
	public boolean isMedicalCentre() {return isMedicalCentre;	}
	public boolean isClient() {return isClient;}	
	public double getdeltaArrivalDeparture() {return deltaArrivalDeparture;}
	public double getdeltaArrivalStartServiceTime() {return deltaArrivalStartServiceTime;}
	public double getdeltarStartServiceTimeEndServiceTime() {return deltarStartServiceTimeEndServiceTime;}

	public String getSubJobKey() { return subJobKey;}
	public int getId() { return id;}
	public double getstartServiceTime() {return startServiceTime;}
	public double getendServiceTime() {return endServiceTime;}
	public double getStartTime() {return hardstartTime;}
	public double getArrivalTime() {return arrivalTime;}
	public double getDepartureTime() {return departureTime;}
	public double getloadUnloadRegistrationTime() {return loadUnloadRegistrationTime;}
	public double getloadUnloadTime() {return loadUnloadTime;}
//	public double getVehicleArrivalTime() {return vehicleArrivalTime;}
	public LinkedList<Jobs> getWalkingRoute() {return walkingRoute;}
//	public double getVehicleDepartureTime() {return vehicleDepartureTime;}
	public int getIDcouple() {return idCouple;}
	public double getEndTime() {return hardendTime;}
	public double getSoftStartTime() {return this.softstartTime;}
	public double getSoftEndTime() {return this.softendTime;}
	public double getsortETWSizeCriterion() {return sortETWSizeCriterion;}
	public double getsortLTWSizeCriterion() {return this.sortLTWSizeCriterion;}
	public int getReqQualification() {return reqQualification;}
	public double getReqTime() {return serviceTime;}
	public int getTotalPeople() {return totalPeople;}
	public double getWaitingTime() {return waitingTime;}
	public Jobs getsubJobPair() {return subJobPair;}
	public int getIdUser() {return idUser;}
	public double getWalkingTime() {return walkingTime;}
	public ArrayList<Jobs> getAssignedJobToMedicalCentre() {return assignedJob;}
	public HashMap<Integer,SubJobs> getSubJobs() {return subJobs;}

	public double getTW() { return hardendTime-hardstartTime; }

	// Setters
	public void setTimeWindowsDropOffMedicalCentre(int registrationTime) {
		double startTime=this.getStartTime();
		this.setStartTime(startTime-registrationTime);
		this.setEndTime(startTime-registrationTime);
	}

	public void setTimeWindowsPickUpMedicalCentre(double startTime, int maxDetourDirectConnection, double cumulativeWaitingTime) {
		this.setEndTime(startTime-maxDetourDirectConnection);
		// aca se considera el waiting time pensando en el tiempo max permitido que el paciente puede estar en el centro médico
		this.setStartTime(this.getEndTime());
	}
	
	public void setIdUser(int id) {idUser= id;}
	public void setPair(Jobs pickUp) {this.subJobPair=pickUp;
	if(pickUp.isMedicalCentre) {
		pickUp.setIdUser(this.id);
	}
	else {
		pickUp.setIdUser(pickUp.getId());
	}
	}

	private String creatingKey(int i) {
		String key="";

		if(i>0) {
			key="P"+this.getId();
		}
		else {
			key="D"+this.getId();
		}
		if(this.isMedicalCentre()) {
			key=key+this.getIdUser();
		}
		return key;
	}


	public String extendedData() {
		return String.format("(id = %d , RQ = %d\n ST: %d,  RT: %d, ET: %d + [PB: %d])",id,reqQualification,hardstartTime,serviceTime,hardendTime,hardendTime - serviceTime);
	}

	public static Comparator<Jobs> SORT_BY_ENDTW = new Comparator<Jobs>() {
		@Override
		public int compare(Jobs o1, Jobs o2) {
			if (o1.getEndTime() > o2.getEndTime())
				return 1;
			if (o1.getEndTime() < o2.getEndTime())
				return -1;
			return 0;
		}

	};
	
	public static Comparator<Jobs> SORT_BY_STARTSERVICETIME = new Comparator<Jobs>() {
		@Override
		public int compare(Jobs o1, Jobs o2) {
			if (o1.getstartServiceTime() > o2.getstartServiceTime() )
				return 1;
			if (o1.getstartServiceTime()  < o2.getstartServiceTime() )
				return -1;
			return 0;
		}

	};
	
	public static Comparator<Jobs> SORT_BY_ARRIVALTIME = new Comparator<Jobs>() {
		@Override
		public int compare(Jobs o1, Jobs o2) {
			if (o1.getArrivalTime() > o2.getArrivalTime() )
				return 1;
			if (o1.getArrivalTime()  < o2.getArrivalTime() )
				return -1;
			return 0;
		}

	};


	public static Comparator<Jobs> SKILLS = new Comparator<Jobs>() {
		@Override
		public int compare(Jobs o1, Jobs o2) {
			if (o1.getReqQualification() < o2.getReqQualification())
				return 1;
			if (o1.getReqQualification() < o2.getReqQualification())
				return -1;
			return 0;
		}

	};

	public static Comparator<Jobs> SORT_BY_STARTW = new Comparator<Jobs>() {
		@Override
		public int compare(Jobs o1, Jobs o2) {
			if (o1.getStartTime() > o2.getStartTime())
				return 1;
			if (o1.getStartTime() < o2.getStartTime())
				return -1;
			return 0;
		}
	};

	public static Comparator<Jobs> TWSIZE_Early = new Comparator<Jobs>() {
		@Override
		public int compare(Jobs o1, Jobs o2) {
			if (o1.getsortETWSizeCriterion() > o2.getsortETWSizeCriterion())
				return 1;
			if (o1.getsortETWSizeCriterion() < o2.getsortETWSizeCriterion())
				return -1;
			return 0;
		}

	};

	public static Comparator<Jobs> TWSIZE_Latest = new Comparator<Jobs>() {
		@Override
		public int compare(Jobs o1, Jobs o2) {
			if (o1.getsortLTWSizeCriterion() > o2.getsortLTWSizeCriterion())
				return 1;
			if (o1.getsortLTWSizeCriterion() < o2.getsortLTWSizeCriterion())
				return -1;
			return 0;
		}

	};





	@Override
	public String toString() {
		String s = "";
		s = s.concat("\nJob Id: " + this.id);
		s = s.concat("\nstartTime: " + this.hardstartTime);
		s = s.concat("\nendTime: " + this.hardendTime);
		s = s.concat("\nservice start Time: " + this.startServiceTime);
		s = s.concat("\nArrival Time: " + (this.arrivalTime));
		s = s.concat("\nDeparture Time: " + (this.departureTime));
		s = s.concat("\nreqQualification: " + (this.reqQualification));
		s = s.concat("\nserviceTime: " + (this.serviceTime));
		s = s.concat("\n preparation time: " + (this.loadUnloadRegistrationTime));
		s = s.concat("\npeople on board: " + this.totalPeople);
		return s;
	}




}
