import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;



/**
 * @author Lorena
 *
 */
public class Algorithm {
	private final Test test;
	private final Inputs input;
	private WalkingRoutes subroutes;
	private DrivingRoutes routes;
	private  ArrayList<Couple> subJobsList= new ArrayList<Couple>();




	public Algorithm(Test t, Inputs i, Random r) {
		test = t;
		input = i;
		subroutes = new WalkingRoutes(input, r, t, i.getNodes()); // stage 1: Creation of walking routes
		updateListJobs();// jobs couple - class SubJobs
		routes = new DrivingRoutes(input, r, t,subJobsList); // stage 2: Creation of driving routes
		Interaction stages= new Interaction(routes,subJobsList, input, r, t);// Iteration between stage 1 und stage 2: from the current walking routes split and define new ones
		routes= stages.getBestRoutes();
		subroutes= stages.getBestWalkingRoutes();
	}



	private void walkingRouteDestruction() {
		// TODO Auto-generated method stub

	}



	private void updateListJobs() {
		// stage 0: set the jobs which are not in a walking route
		ArrayList<Couple> clientJobs= createClientJobs(); // TO DO
		ArrayList<Couple> patientJobs= createPatientsJobs(); // TO DO

		for(Couple j:clientJobs) {
			System.out.println(j.toString());
			subJobsList.add(j);
			
		}

		for(Couple j:patientJobs) {
			System.out.println(j.toString());
			subJobsList.add(j);
		}	
		
		System.out.println("Couple");
		for(Couple j:subJobsList) {
			System.out.println(j.toString());
		}
		
		System.out.println("End");
	}

	private ArrayList<Couple> createPatientsJobs() {

		// Los pacientes estan vinculados con el centro médico // 1
		ArrayList<Couple> coupleFromPatientsRequest= new ArrayList<Couple>();
		for(Jobs j: input.getpatients()) {

			// patient home -----going ----> Medical centre
			Jobs presentJob= new Jobs(j);// Patient home - pick up
			presentJob.setTotalPeople(1); // 1 persona porque se recoge sólo al paciente
			Jobs futureJob= new Jobs(j.getsubJobPair().getId(), j.getStartTime(),j.getEndTime(), j.getReqQualification(), j.getReqTime()); // medical centre - drop-off 
			presentJob.setPair(futureJob);
			int directConnectionDistance= input.getCarCost().getCost(presentJob.getId(), futureJob.getId()); // setting the time for picking up the patient at home patient
			futureJob.setTotalPeople(-2); // setting el numero de personas en el servicio // es un dos porque es el paramedico y el pacnete al mismo tiempo
			Couple pairPatientMedicalCentre=creatingPairPatientMedicalCentre(presentJob,futureJob, directConnectionDistance);
			presentJob.setStartServiceTime(presentJob.getEndTime()); // set the service start time
			presentJob.setReqserviceTime(test.getloadTimePatient());
			futureJob.setStartServiceTime(j.getStartTime()); // set the service start time
			futureJob.setReqserviceTime(test.getloadTimePatient()+j.getReqTime());
			
			System.out.println(pairPatientMedicalCentre.toString());
			
			coupleFromPatientsRequest.add(pairPatientMedicalCentre);

			// patient home <-----returning ---- Medical centre  - copying for returning patient to patients home
			Jobs medicalCentrepickUpJob=  creationPickupTask(j);// Patient home - pick up <- setting the attributes such as time windows
			medicalCentrepickUpJob.setTotalPeople(2); // pick-up patient and paramedic
			Jobs homePatientdropOffJob = creatingDropOffTask(medicalCentrepickUpJob,j); 	// medical centre - drop-off  <- setting the attributes such as time windows
			homePatientdropOffJob.setTotalPeople(-1); // pick-up patient and paramedic
			directConnectionDistance= input.getCarCost().getCost(medicalCentrepickUpJob.getId(), homePatientdropOffJob.getId());
			homePatientdropOffJob.setTotalPeople(-1); // drop-off patient
			medicalCentrepickUpJob.setPair(homePatientdropOffJob);
			Couple pairMedicalCentrePatient=creatingPairMedicalCentrePatient(presentJob,futureJob, directConnectionDistance);
			System.out.println(pairMedicalCentrePatient.toString());
			coupleFromPatientsRequest.add(pairMedicalCentrePatient);

		}
		return coupleFromPatientsRequest;
	}







	/**
	 * @param medicalCentrepickUpJob  <- es el lugar en donde es recogido el paramedico y el paciente
	 * @param j <- es el lugar en donde sera dejado el paciente
	 * @param directConnectionDistance <- distancia a viajar con un vehículo desde el centro médico hasta la casa del paciente
	 * medicalCentrepickUpJob<= present job <- Drop-off the patient at the patient home
	 * Nota: las horas de este trabajo se establecen de acuerdo a la hora en que el paramedico y el paciente son recogidos del centro médico
	 * int earlyTime= hora en que es recogido el paramedico y paciente+ maxDetourDuration;
	 * int lateTime= hora en que es recogido el paramedico y paciente+ maxDetourDuration + max tiempo de espera;
	 * @return drop-off node
	 */
	private Jobs creatingDropOffTask(Jobs medicalCentrepickUpJob,Jobs j) {
		int directConnectionDistance= input.getCarCost().getCost(medicalCentrepickUpJob.getId(), j.getId());
		double maxDetourDuration = (int) directConnectionDistance*(test.getDetour()); // dynamic according to the direct 
		int earlyTime=medicalCentrepickUpJob.getStartTime()+(int) maxDetourDuration;
		int lateTime=medicalCentrepickUpJob.getStartTime()+(int) maxDetourDuration+ test.getCumulativeWaitingTime();
		Jobs homePatientdropOffJob= new Jobs(j.getId(),earlyTime,lateTime,j.getReqQualification(),0); 	// medical centre - drop-off 
		return homePatientdropOffJob;
	}

	/*
	presentJob<- es el nodo en donde el paciente es recogido para ser llevado al centro médico
	futureJob<- es el centro médico en donde será dejado el paciente
	 */
	private Couple creatingPairMedicalCentrePatient(Jobs presentJob, Jobs futureJob, int directConnectionDistance) {

		// 1. Calculate the time for piking up the patient and paramedic
		double maxDetourDuration = (int) directConnectionDistance*(test.getDetour()); // dynamic according to the direct connections
		int directConection= Math.max((int)maxDetourDuration,directConnectionDistance);

		// DEFINITION OF FUTURE TASK<- DROP-OFF PATIENT AND PARAMEDIC AT MEDICAL CENTRE
		// the time for the future task is modified to consider the registration time
		// start time= doctor appointment - registration time
		futureJob.setStartTime(futureJob.getStartTime()-test.getRegistrationTime());

		// DEFINITION OF PRESENT TASK<- PICK UP PATIENT AT MEDICAL CENTRE
		// 2. Set the time for pick up patient at the patient home = doctor appointment time  - max(detour, direct connection) - load time - tiempo de registro
		int fixedTime=futureJob.getStartTime()-directConection- test.getloadTimePatient(); // doctor appointment
		presentJob.setStartTime(fixedTime);


		// DEFINITION OF A COUPLE
		// 3. creation of the coupe
		Couple presentCouple= new Couple(presentJob,futureJob, directConnectionDistance,test.getDetour());

		return presentCouple;
	}



	private Jobs creationPickupTask(Jobs j) {
		// j contains the patient informmation
		// setting houir to pick up paramedic and patient at medical centre
		//earlyTime = service start time + d(i) 
		int earlyTime= j.getstartServiceTime()+j.getReqTime();
		//laterTime = service start time + d(i) 
		int laterTime= j.getstartServiceTime()+j.getReqTime() + test.getCumulativeWaitingTime();
		//(int id, int startTime, int endTime, int reqQualification, int reqTime)
		Jobs medicalCentrepickUpJob= new Jobs(j.getsubJobPair().getId(), earlyTime,laterTime,j.getReqQualification(),0);// Patient home - pick up
		return medicalCentrepickUpJob;
	}



	private Couple creatingPairPatientMedicalCentre(Jobs presentJob, Jobs futureJob, int directConnectionDistance) {
		// 1. Calculate the time for piking up the patient
		double maxDetourDuration =  directConnectionDistance*(test.getDetour()); // dynamic according to the direct connections
		int directConection= Math.max((int)maxDetourDuration,directConnectionDistance);
		// DEFINITION OF FUTURE TASK<- DROP-OFF PATIENT AND PARAMEDIC AT MEDICAL CENTRE
		// the time for the future task is modified to consider the registration time
		// start time= doctor appointment - registration time
		futureJob.setTimeWindowsDropOffMedicalCentre(test.getRegistrationTime());
		
		// DEFINITION OF PRESENT TASK<- PICK UP PATIENT AT MEDICAL CENTRE
		// 2. Set the time for pick up patient at the patient home = doctor appointment time  - max(detour, direct connection) - load time - tiempo de registro
		presentJob.setTimeWindowsPickUpMedicalCentre(futureJob.getStartTime(),directConection,test.getCumulativeWaitingTime());
	

		// DEFINITION OF A COUPLE
		// 3. creation of the coupe
		Couple presentCouple= new Couple(presentJob,futureJob, directConnectionDistance, test.getDetour());
		return presentCouple;
	}





	private ArrayList<Couple> createClientJobs() {
		ArrayList<Couple> coupleFromWalkingRoutes= new ArrayList<Couple>();
		// WALKING ROUTES. 	// Convert walking route in big jobs
		// 1. Ignoring the jobs which are already in a walking route
		HashMap<Integer,Jobs> jobsInWalkingRoutes= new HashMap<>(); // store the list of job in the walking routes
		for(SubRoute r:subroutes.getWalkingRoutes()) {
			for(Jobs j:r.getJobSequence()) {
				jobsInWalkingRoutes.put(j.getId(), j); // storing the job in the walking route
			}
			if(r.getDropOffNode()!=null && r.getPickUpNode()!=null) {
				Jobs present=creatinngPresentJobFromWR(r.getDropOffNode());
				present.setTotalPeople(-1); // drop-off home care staff
				Jobs future=creatinngFutureJobFromWR(r.getPickUpNode());
				int directConnectionDistance= input.getCarCost().getCost(present.getId(), future.getId());
				future.setTotalPeople(1); // pick-up home care staff
				present.setPair(future);
				coupleFromWalkingRoutes.add(new Couple(present,future, r, directConnectionDistance,test.getDetour()));
			}
		}


		// REMAINING JOBS
		for(Jobs j: input.getclients()) {
			// Creating the request for picking up the nurse
			if(!jobsInWalkingRoutes.containsKey(j.getId())) { // only jobs which are not in a walking route	
				Jobs presentJob= new Jobs(j);
				presentJob.setTotalPeople(-1);
				Jobs futureJob= creatingTheFeatureJob(j); // this a copy of the current job
				futureJob.setTotalPeople(1);
				int directConnectionDistance= input.getCarCost().getCost(presentJob.getId(), futureJob.getId());
				presentJob.setPair(futureJob);
				coupleFromWalkingRoutes.add(new Couple(presentJob,futureJob, directConnectionDistance,test.getDetour()));
			}
		}
		// create in the class Couple a constructor for setting the walking routes
		return coupleFromWalkingRoutes;
	}



	private Jobs creatinngFutureJobFromWR(Jobs pickUpNode) {
		int startTime= pickUpNode.getstartServiceTime(); // early time window = start time service + time requested // lastest=  start time service + time requested + max waiting time
		int endTime= pickUpNode.getstartServiceTime()+pickUpNode.getSurplusTime(); // early time window = start time service + time requested // lastest=  start time service + time requested + max waiting time
		Jobs future= new Jobs(pickUpNode.getId(),startTime,endTime ,pickUpNode.getReqQualification(), 0); // Jobs(int id, int startTime, int endTime, int reqQualification,int reqTime)
		return future;
	}



	private Jobs creatinngPresentJobFromWR(Jobs dropOffNode) {
		int startTime= dropOffNode.getstartServiceTime(); // early time window = start time service + time requested // lastest=  start time service + time requested + max waiting time
		int endTime= dropOffNode.getstartServiceTime()+dropOffNode.getSurplusTime(); // early time window = start time service + time requested // lastest=  start time service + time requested + max waiting time
		Jobs present= new Jobs(dropOffNode.getId(),startTime,endTime ,dropOffNode.getReqQualification(), 0); // Jobs(int id, int startTime, int endTime, int reqQualification,int reqTime)
		return present;
	}



	private Jobs creatingTheFeatureJob(Jobs j) {

		int startTime= j.getstartServiceTime()+j.getReqTime(); // early time window = start time service + time requested // lastest=  start time service + time requested + max waiting time
		int endTime= j.getstartServiceTime()+j.getReqTime()+test.getCumulativeWaitingTime(); // early time window = start time service + time requested // lastest=  start time service + time requested + max waiting time
		Jobs future= new Jobs(j.getId(),startTime,endTime ,j.getReqQualification(), 0); // Jobs(int id, int startTime, int endTime, int reqQualification,int reqTime)
		return future;
	}



	// Getters
	public WalkingRoutes getSubroutes() {
		return subroutes;
	}



	public DrivingRoutes getRoutes() {
		return routes;
	}




}
