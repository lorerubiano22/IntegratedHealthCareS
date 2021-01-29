import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;



/**
 * @author Lorena
 *
 */
public class Algorithm {
	private int iterations=1;
	private final Test test;
	private final Inputs input;
	private WalkingRoutes subroutes;
	private LinkedList<SubRoute> walkingList;
	private DrivingRoutes drivingRoute;
	private Solution initialSolution=null;
	private Solution solution=null;
	private Solution newSolution=null;
	private Solution bestSolution=null;
	private Random rn;
	private  HashMap<String,Couple> subJobsList= new HashMap<String,Couple>();




	public Algorithm(Test t, Inputs i, Random r) {

		test = t;
		rn=r;
		input = i;
		walkingList = new LinkedList<SubRoute>();
		double objective=0;
		subroutes = new WalkingRoutes(rn,input, t, i.getNodes()); // stage 1: Creation of walking routes

		boolean diversification= false;
		System.out.println("Stop " +test.getTestTime());
		for(int iter=0;iter<test.getTestTime();iter++) {
			if(iter==7) {
				System.out.println("Stop");
			}
			//if(solution== null ||diversification) {
				walkingList = new LinkedList<SubRoute>();
				if(subroutes.getWalkingRoutes()!=null) {
					selectionWalkingRoutes(false);
				}
				updateListJobs();// jobs couple - class SubJobs // las couples sólo sirven para la lista de clients (como consequencia de las walking routes)
				drivingRoute = new DrivingRoutes(input, r, t,subJobsList,walkingList); // stage 2: Creation of driving routes
			//}
			drivingRoute.generateAfeasibleSolution(iter,solution,diversification);
			drivingRoute.getSol().setWalkingRoutes(walkingList);
			newSolution= new Solution(drivingRoute.getSol());
			newSolution.getRoutes().sort(Route.SORT_BY_departureTimeDepot);
			System.out.println(newSolution.toString());
			objective=0;
			if(bestSolution==null) {
				objective=Double.MAX_VALUE;
			}
			else {
				objective=bestSolution.getobjectiveFunction();
			}
			if(newSolution.getobjectiveFunction()<objective) {
				System.out.println(" itertarion "+ iter);
				if(iter==2) {
					System.out.println(" itertarion "+ iter);
				}
				setBestSolution(drivingRoute.getSol());
				setInitialSolution(drivingRoute.getSol().getShift());
				//solution=null;
			}
			else {
				if(newSolution.getobjectiveFunction()>objective) {// darle una oportunidad a la mala solución
					//while(continueSearching) {
					drivingRoute.assigningRoutesToDrivers(iter,new Solution(newSolution));
					solution= new Solution(drivingRoute.getSol());
					solution.getRoutes().sort(Route.SORT_BY_departureTimeDepot);
					objective=0;
					if(bestSolution==null) {
						objective=Double.MAX_VALUE;
					}
					else {
						objective=bestSolution.getobjectiveFunction();
					}
					if(solution.getobjectiveFunction()<newSolution.getobjectiveFunction()) {
						newSolution=new Solution(solution);
						if(newSolution.getobjectiveFunction()<objective) {
							setBestSolution(drivingRoute.getSol());
							setInitialSolution(drivingRoute.getSol().getShift());
							diversification=false;
						}
					}
					else {
						diversification=true;
					}
					//}
				}
				else {
					diversification=true;
				}
			}


		}



	}

	private boolean correctCouple(HashMap<String, Couple> subJobsList2, LinkedList<SubRoute> walkingList2) {
		boolean correctCoupe= false;
		// pick-up Nodes

		for(SubRoute r:walkingList2) {
			SubJobs start=new SubJobs(r.getJobSequence().get(0));
			String key="P"+start.getId();
			if(subJobsList2.containsKey(key)) {
				Couple j=subJobsList2.get(key);
				System.out.println("Stop");
			}
		}
		for(SubRoute r:walkingList2) {
			SubJobs start=new SubJobs(r.getJobSequence().get(r.getJobSequence().size()-1));
			String key="D"+start.getId();
			if(subJobsList2.containsKey(key)) {
				System.out.println("Stop");
			}
		}
		// Drop-off nodes
		return correctCoupe;
	}

	private void selectionWalkingRoutes(boolean b) {
		if(!subroutes.getWalkingRoutes().isEmpty()) {
			if(!b) {
				int totalWalkingRoutes = this.rn.nextInt(subroutes.getWalkingRoutes().size()-1);
				//int totalWalkingRoutes = 1;
				int id=-1;
				for(int i=0;i<totalWalkingRoutes;i++) {
					int r2 = this.rn.nextInt(subroutes.getWalkingRoutes().size()-1);
					SubRoute wr=subroutes.getWalkingRoutes().get(r2);

					if(!walkingList.contains(wr)) {
						id++;	
						wr.setSlotID(id);
						walkingList.add(wr);
					}
					else {
						i--;
					}

				}
			}
			else {
				for(int i=0;i<subroutes.getWalkingRoutes().size();i++) {
					SubRoute wr=subroutes.getWalkingRoutes().get(i);
					if(!walkingList.contains(wr)) {
						walkingList.add(wr);
					}			
				}

			}
			int id=-1;
			for(SubRoute wr:walkingList) {
				id++;
				wr.setSlotID(id);
			}
		}
	}

	private void setBestSolution(Solution sol) {

		if(solution==null) {
			solution=new Solution (sol);
			//newSolution=new Solution (sol);
			bestSolution=new Solution (sol);
			addingWaitingTime(solution);
			solution.setId(iterations);
			double durationW=0;
			for(SubRoute r:sol.getWalkingRoute()) {
				durationW+=r.getTotalTravelTime();
			}
			solution.setWalkingTime(durationW);
			setInitialSolution(drivingRoute.getInitialSol());

			addingWaitingTime(bestSolution);
			bestSolution.setId(iterations);

			bestSolution.setWalkingTime(durationW);
			setInitialSolution(drivingRoute.getInitialSol());
		}
		else {
			newSolution=new Solution (sol);
			addingWaitingTime(sol);
			newSolution.setId(iterations);
			newSolution.setWalkingTime(subroutes.getTotalTravelTime());
			if(newSolution.getobjectiveFunction()<solution.getobjectiveFunction()) {
				bestSolution=new Solution (newSolution);
				addingWaitingTime(bestSolution);
				bestSolution.setId(iterations);
				double durationW=0;
				for(SubRoute r:sol.getWalkingRoute()) {
					durationW+=r.getTotalTravelTime();
				}
				bestSolution.setWalkingTime(durationW);
				solution=new Solution (newSolution);
				addingWaitingTime(solution);
				solution.setId(iterations);
				solution.setWalkingTime(subroutes.getTotalTravelTime());
				setInitialSolution(drivingRoute.getInitialSol());  
			}
		}
	}

	public void setInitialSolution(Solution initialSolution) {
		this.initialSolution = initialSolution;
		addingWaitingTime(initialSolution);
		//		initialSolution.setId(iterations);
		//		double durationW=0;
		//		for(SubRoute r:initialSolution.getWalkingRoute()) {
		//			durationW+=r.getDurationWalkingRoute();
		//		}
		//		initialSolution.setWalkingTime(durationW);
		//initialSolution.setWalkingTime(subroutes.getTotalTravelTime());
	}

	private void addingWaitingTime(Solution initialSolution2) {
		double waitingSolution=initialSolution2.getWaitingTime();
		initialSolution2.setWaitingTime(waitingSolution);		

	}








	private void updateListJobs() {
		subJobsList.clear();
		// stage 0: set the jobs which are not in a walking route - * Hard time window
		ArrayList<Couple> clientJobs= createClientJobs(); //  (Drop-off home care staff at client home)* -------- (Pick up home care staff at client home)


		ArrayList<Couple> patientJobs= createPatientsJobs(); //  (Pick up patient at patient home)------(Drop-off paramedic and patient at medical centre)*
		// creating the list of subJobs <- each subjob could be also considered as a stop
		creatingSubjobsList(clientJobs,patientJobs);
	}

	private void creatingSubjobsList(ArrayList<Couple> clientJobs, ArrayList<Couple> patientJobs) {
		// una couple vincula dos lugares
		int i=-1;
		for(Couple j:clientJobs) {
			i++;
			if(i==67) {
				System.out.println("Error");
			}
			System.out.println(j.toString());
			j.setIdCouple(i);
			j.getPresent().setIDcouple(i);
			if(j.getPresent().getId()==70) {
				System.out.println("Error");
			}
			if(j.getPresent().getId()==38) {
				System.out.println("Error");
			}
			j.getFuture().setIDcouple(i);
			subJobsList.put(j.getPresent().getSubJobKey(),j);
			subJobsList.put(j.getFuture().getSubJobKey(),j);
			if(j.getPresent().getStartTime()>j.getFuture().getStartTime()) {
				System.out.println("Error");
			}

		}

		for(Couple j:patientJobs) {
			i++;
			System.out.println(j.toString());
			j.setIdCouple(i);
			subJobsList.put(j.getPresent().getSubJobKey(),j);
			subJobsList.put(j.getFuture().getSubJobKey(),j);
			if(j.getPresent().getStartTime()>j.getFuture().getStartTime()) {
				System.out.println("Error");
			}
		}
	}



	private ArrayList<Couple> createPatientsJobs() {
		// Los pacientes estan vinculados con el centro médico // 1
		ArrayList<Couple> coupleFromPatientsRequest= new ArrayList<Couple>();
		for(Jobs j: input.getpatients().values()) {
			if(j.getId()==54){
				System.out.println("error");
			}
			
			//D653
			// patient home -----going ----> Medical centre
			//0. creation of couple
			Couple pairPatientMedicalCentre= creatingCouplePatientHomeToMedicalCentre(j); 
			// 1. fixing time windows
			computingTimeWindowPatientMedicalCentreSubJob(pairPatientMedicalCentre,j);
			// 2. fixing number of people involved
			settingPeopleInSubJob(pairPatientMedicalCentre, +1,-2); //- drop-off #personas + pick up #personas
			// 3. checking information
			System.out.println(pairPatientMedicalCentre.toString());
			// 4. adding couple
			if(pairPatientMedicalCentre.getPresent().getstartServiceTime()>pairPatientMedicalCentre.getFuture().getstartServiceTime()) { // control
				System.out.println("error");
			}
			// patient medical centre ---- going ----> patient home

			Couple pairMedicalCentrePatient= creatingPairMedicalCentrePatient(pairPatientMedicalCentre);
			//creatingCouplePatientHomeToMedicalCentre(j); 
			coupleFromPatientsRequest.add(pairMedicalCentrePatient);
			coupleFromPatientsRequest.add(pairPatientMedicalCentre);

			settingPeopleInSubJob(pairMedicalCentrePatient, +2,-1);

			if(pairMedicalCentrePatient.getPresent().getstartServiceTime()>pairMedicalCentrePatient.getFuture().getstartServiceTime()) { // control
				System.out.println("error");
			}
		}
		return coupleFromPatientsRequest;
	}










	private void settingPeopleInSubJob(Couple pairPatientMedicalCentre, int i, int j) {
		pairPatientMedicalCentre.getPresent().setTotalPeople(i); // 1 persona porque se recoge sólo al paciente
		pairPatientMedicalCentre.getFuture().setTotalPeople(j); // setting el numero de personas en el servicio // es un dos porque es el paramedico y el pacnete al mismo tiempo	
	}



	private void computingTimeWindowPatientMedicalCentreSubJob(Couple pairPatientMedicalCentre, Jobs j) {
		// Time window
		// 1. Future Job: drop-off patient and paramedic at medical centre
		// required time before the service starts
		pairPatientMedicalCentre.getFuture().setloadUnloadTime(test.getloadTimePatient());
		pairPatientMedicalCentre.getFuture().setloadUnloadRegistrationTime(test.getRegistrationTime());


		// Service time: start time and duration service
		pairPatientMedicalCentre.getFuture().setStartServiceTime(j.getEndTime()); // start time
		pairPatientMedicalCentre.getFuture().setserviceTime(j.getReqTime()); // duration service
		// set arrival at node drop off medical centre
		pairPatientMedicalCentre.getFuture().setarrivalTime(pairPatientMedicalCentre.getFuture().getstartServiceTime()-(pairPatientMedicalCentre.getFuture().getloadUnloadRegistrationTime()+pairPatientMedicalCentre.getFuture().getloadUnloadTime()));
		pairPatientMedicalCentre.getFuture().setEndServiceTime(pairPatientMedicalCentre.getFuture().getstartServiceTime()+pairPatientMedicalCentre.getFuture().getReqTime());

		pairPatientMedicalCentre.getFuture().setdepartureTime(pairPatientMedicalCentre.getFuture().getendServiceTime());


		// changing TW

		pairPatientMedicalCentre.getFuture().setStartTime(Math.max(0, j.getStartTime()-test.getCumulativeWaitingTime())); // earliest
		
	//	pairPatientMedicalCentre.getFuture().setEndTime(j.getStartTime()); // latest
		pairPatientMedicalCentre.getFuture().setEndTime(j.getEndTime()); // latest



		// 2. Present Job: pick patient up time is set assuming a direct connection from patient home to medical centre
		// required time before the service starts
		pairPatientMedicalCentre.getPresent().setloadUnloadTime(test.getloadTimePatient());
		//double tv=input.getCarCost().getCost(pairPatientMedicalCentre.getPresent().getId()-1, pairPatientMedicalCentre.getFuture().getId()-1)*test.getDetour();	
		double tv=input.getCarCost().getCost(pairPatientMedicalCentre.getPresent().getId()-1, pairPatientMedicalCentre.getFuture().getId()-1);	

		pairPatientMedicalCentre.getPresent().setStartTime(pairPatientMedicalCentre.getFuture().getStartTime()-tv);
		pairPatientMedicalCentre.getPresent().setEndTime(pairPatientMedicalCentre.getFuture().getEndTime()-tv);
		pairPatientMedicalCentre.getPresent().setSoftStartTime(pairPatientMedicalCentre.getFuture().getStartTime()-tv);
		pairPatientMedicalCentre.getPresent().setEndServiceTime(pairPatientMedicalCentre.getFuture().getEndTime()-tv);
		// Service time required
		pairPatientMedicalCentre.getPresent().setserviceTime(0);
		// Service time: start time and duration service
		pairPatientMedicalCentre.getPresent().setStartServiceTime(pairPatientMedicalCentre.getFuture().getArrivalTime()-tv);
		pairPatientMedicalCentre.getPresent().setEndServiceTime(pairPatientMedicalCentre.getPresent().getstartServiceTime());
		pairPatientMedicalCentre.getPresent().setarrivalTime(pairPatientMedicalCentre.getPresent().getendServiceTime()-pairPatientMedicalCentre.getPresent().getloadUnloadTime());
		pairPatientMedicalCentre.getPresent().setdepartureTime(pairPatientMedicalCentre.getPresent().getendServiceTime());





		// present
		double deltaArrivalDeparture=pairPatientMedicalCentre.getPresent().getDepartureTime()-pairPatientMedicalCentre.getPresent().getArrivalTime();
		double deltaArrivalStartServiceTime=pairPatientMedicalCentre.getPresent().getstartServiceTime()-pairPatientMedicalCentre.getPresent().getArrivalTime();
		double deltarStartServiceTimeEndServiceTime=pairPatientMedicalCentre.getPresent().getendServiceTime()-pairPatientMedicalCentre.getPresent().getstartServiceTime();
		pairPatientMedicalCentre.getPresent().setdeltaArrivalDeparture(deltaArrivalDeparture);
		pairPatientMedicalCentre.getPresent().setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		pairPatientMedicalCentre.getPresent().setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);


		// future
		deltaArrivalDeparture=pairPatientMedicalCentre.getFuture().getDepartureTime()-pairPatientMedicalCentre.getFuture().getArrivalTime();
		deltaArrivalStartServiceTime=pairPatientMedicalCentre.getFuture().getstartServiceTime()-pairPatientMedicalCentre.getFuture().getArrivalTime();
		deltarStartServiceTimeEndServiceTime=pairPatientMedicalCentre.getFuture().getendServiceTime()-pairPatientMedicalCentre.getFuture().getstartServiceTime();
		pairPatientMedicalCentre.getFuture().setdeltaArrivalDeparture(deltaArrivalDeparture);
		pairPatientMedicalCentre.getFuture().setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		pairPatientMedicalCentre.getFuture().setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);

	}

	private Couple creatingCouplePatientHomeToMedicalCentre(Jobs j) {
		// j- location: Patiente home
		// j.getsubJobPair()- location: Medical centre
		Jobs presentJob= new Jobs(j);// Patient home - pick up
		presentJob.setPatient(true);
		presentJob.setMedicalCentre(false);
		presentJob.setloadUnloadTime(test.getloadTimePatient());
		
		Jobs futureJob= new Jobs(j.getsubJobPair().getId(), j.getStartTime(),j.getEndTime(), j.getReqQualification(), j.getReqTime()); // medical centre - drop-off 
		futureJob.setloadUnloadTime(test.getloadTimePatient());
		futureJob.setloadUnloadRegistrationTime(test.getRegistrationTime());
		futureJob.setMedicalCentre(true);
		futureJob.setPatient(false);
	
		presentJob.setPair(futureJob);
		int directConnectionDistance= input.getCarCost().getCost(presentJob.getId()-1, futureJob.getId()-1); // setting the time for picking up the patient at home patient
		Couple pairPatientMedicalCentre=creatingPairPatientMedicalCentre(presentJob,futureJob, directConnectionDistance);

		// present
		double deltaArrivalDeparture=presentJob.getDepartureTime()-presentJob.getArrivalTime();
		double deltaArrivalStartServiceTime=presentJob.getstartServiceTime()-presentJob.getArrivalTime();
		double deltarStartServiceTimeEndServiceTime=presentJob.getendServiceTime()-presentJob.getstartServiceTime();
		
		presentJob.setdeltaArrivalDeparture(deltaArrivalDeparture);
		presentJob.setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		presentJob.setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);


		// future
		deltaArrivalDeparture=futureJob.getDepartureTime()-futureJob.getArrivalTime();
		deltaArrivalStartServiceTime=futureJob.getstartServiceTime()-futureJob.getArrivalTime();
		deltarStartServiceTimeEndServiceTime=futureJob.getendServiceTime()-futureJob.getstartServiceTime();
		
		futureJob.setdeltaArrivalDeparture(deltaArrivalDeparture);
		futureJob.setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		futureJob.setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);

		return pairPatientMedicalCentre;
	}


	/*
	presentJob<- es el nodo en donde el paciente es recogido para ser llevado al centro médico
	futureJob<- es el centro médico en donde será dejado el paciente
	 */
	private Couple creatingPairMedicalCentrePatient(Couple pairPatientMedicalCentre) {

		// 1. Calculate the time for piking up the patient and paramedic
		// DEFINITION OF FUTURE TASK<- DROP-OFF PATIENT AND PARAMEDIC AT MEDICAL CENTRE
		// the time for the future task is modified to consider the registration time
		// start time= doctor appointment - registration time

		Jobs present= new Jobs(pairPatientMedicalCentre.getFuture());// pick up at medical centre
		present.setMedicalCentre(true);
		present.setPatient(false);
		settingInformationPatientPickUpMC(present);	
		Jobs future= new Jobs(pairPatientMedicalCentre.getPresent());// drop off patient home
		future.setPatient(true);
		future.setMedicalCentre(false);
		settingInformationPatientDropOffHome(present,future);	

		double tv= input.getCarCost().getCost(present.getId()-1, future.getId()-1);
		// DEFINITION OF A COUPLE
		// 3. creation of the coupe
		Couple presentCouple= new Couple(present,future, tv,test.getDetour());



		// delta

		// present
		double deltaArrivalDeparture=presentCouple.getPresent().getDepartureTime()-presentCouple.getPresent().getArrivalTime();
		double deltaArrivalStartServiceTime=presentCouple.getPresent().getstartServiceTime()-presentCouple.getPresent().getArrivalTime();
		double deltarStartServiceTimeEndServiceTime=presentCouple.getPresent().getendServiceTime()-presentCouple.getPresent().getstartServiceTime();
		presentCouple.getPresent().setdeltaArrivalDeparture(deltaArrivalDeparture);
		presentCouple.getPresent().setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		presentCouple.getPresent().setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);


		// future
		deltaArrivalDeparture=presentCouple.getFuture().getDepartureTime()-presentCouple.getFuture().getArrivalTime();
		deltaArrivalStartServiceTime=presentCouple.getFuture().getstartServiceTime()-presentCouple.getFuture().getArrivalTime();
		deltarStartServiceTimeEndServiceTime=presentCouple.getFuture().getendServiceTime()-presentCouple.getFuture().getstartServiceTime();
		presentCouple.getFuture().setdeltaArrivalDeparture(deltaArrivalDeparture);
		presentCouple.getFuture().setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		presentCouple.getFuture().setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);

		return presentCouple;
	}


	private void settingInformationPatientDropOffHome(Jobs present, Jobs dropOffPatientHome) {
		dropOffPatientHome.setTotalPeople(-1);
		dropOffPatientHome.setPatient(true);
		dropOffPatientHome.setMedicalCentre(false);
		dropOffPatientHome.setloadUnloadTime(test.getloadTimePatient());
		// 1. Setting the start service time -- startServiceTime
		//double travel=input.getCarCost().getCost(present.getId()-1, dropOffPatientHome.getId()-1)*test.getDetour(); // es necesario considerar el travel time porque involucra dos locaciones
		double travel=input.getCarCost().getCost(present.getId()-1, dropOffPatientHome.getId()-1); // es necesario considerar el travel time porque involucra dos locaciones

		dropOffPatientHome.setStartTime(present.getStartTime()+travel);
		dropOffPatientHome.setEndTime(present.getEndTime()+travel);// departure from patient home - el tiempo de viaje - el tiempo necesario para cargar los pacientes al vehículo
		dropOffPatientHome.setSoftStartTime(present.getStartTime()+travel);
		dropOffPatientHome.setEndServiceTime(present.getEndTime()+travel);
		//dropOffPatientHome.setEndTime(Double.MAX_VALUE);// departure from patient home - el tiempo de viaje - el tiempo necesario para cargar los pacientes al vehículo

		dropOffPatientHome.setStartServiceTime(dropOffPatientHome.getEndTime());
		dropOffPatientHome.setEndServiceTime(dropOffPatientHome.getstartServiceTime());
		dropOffPatientHome.setarrivalTime(dropOffPatientHome.getstartServiceTime());
		dropOffPatientHome.setdepartureTime(dropOffPatientHome.getendServiceTime()+dropOffPatientHome.getloadUnloadTime());
		dropOffPatientHome.setserviceTime(0);

	}

	private void settingInformationPatientPickUpMC(Jobs pickUpMedicalCentre) {
		pickUpMedicalCentre.setTotalPeople(2); // 5. Setting the total people (+) pick up   (-) drop-off
		pickUpMedicalCentre.setMedicalCentre(true);
		pickUpMedicalCentre.setPatient(false);
		pickUpMedicalCentre.setloadUnloadTime(test.getloadTimePatient());
		pickUpMedicalCentre.setStartTime(pickUpMedicalCentre.getendServiceTime());
		pickUpMedicalCentre.setEndTime(pickUpMedicalCentre.getendServiceTime()+test.getCumulativeWaitingTime());
		//pickUpMedicalCentre.setEndTime(pickUpMedicalCentre.getStartTime()+test.getCumulativeWaitingTime());
		//pickUpMedicalCentre.setEndTime(Double.MAX_VALUE);
		// 1. Setting the start service time -- startServiceTime
		pickUpMedicalCentre.setStartServiceTime(pickUpMedicalCentre.getEndTime());
		pickUpMedicalCentre.setarrivalTime(pickUpMedicalCentre.getstartServiceTime());
		// 3. Set el fin del servicio
		pickUpMedicalCentre.setEndServiceTime(pickUpMedicalCentre.getstartServiceTime());
		pickUpMedicalCentre.setdepartureTime(pickUpMedicalCentre.getendServiceTime()+pickUpMedicalCentre.getloadUnloadTime());
		pickUpMedicalCentre.setserviceTime(0);
		// delta

	}


	private Couple creatingPairPatientMedicalCentre(Jobs presentJob, Jobs futureJob, int directConnectionDistance) {
		// 1. Calculate the time for piking up the patient
		// DEFINITION OF FUTURE TASK<- DROP-OFF PATIENT AND PARAMEDIC AT MEDICAL CENTRE
		// the time for the future task is modified to consider the registration time
		// start time= doctor appointment - registration time
		futureJob.setTimeWindowsDropOffMedicalCentre(test.getRegistrationTime());
		futureJob.setloadUnloadTime(test.getloadTimePatient());
		futureJob.setloadUnloadRegistrationTime(test.getRegistrationTime());
		// DEFINITION OF PRESENT TASK<- PICK UP PATIENT AT MEDICAL CENTRE
		// 2. Set the time for pick up patient at the patient home = doctor appointment time  - max(detour, direct connection) - load time - tiempo de registro
		presentJob.setTimeWindowsPickUpMedicalCentre(futureJob.getStartTime(),directConnectionDistance,test.getCumulativeWaitingTime());


		// DEFINITION OF A COUPLE
		// 3. creation of the coupe
		Couple presentCouple= new Couple(presentJob,futureJob, directConnectionDistance, test.getDetour());
		return presentCouple;
	}


	private ArrayList<Couple> createClientJobs() {
		ArrayList<Couple> coupleFromWalkingRoutes= new ArrayList<Couple>();
		HashMap<Integer,Jobs> jobsInWalkingRoutes= clientInWalkingRoutes(); // store the list of job in the walking routes
		// 1. WALKING ROUTES-- Convert walking route in big jobs
		convertingWalkingRoutesInOneTask(coupleFromWalkingRoutes);
		// Individual client JOBS
		if(!input.getclients().isEmpty()) {
			for(Jobs j: input.getclients().values()) {
				if(j.getId()==70) {
					System.out.println("stop");
				}
				// Creating the request for picking up the nurse
				if(!jobsInWalkingRoutes.containsKey(j.getId())) { // only jobs which are not in a walking route	
					Jobs presentJob= new Jobs(j);


					presentJob.setClient(true);
					presentJob.setloadUnloadTime(test.getloadTimeHomeCareStaff());
					settingPresentTimeClient(presentJob);
					Jobs futureJob= creatingSubPairJOb(j);

					//0. creation of couple
					Couple pickUpDropOff= creatingCoupleforIndividualJobs(presentJob,futureJob); // individula jobs <- not walking routes

					if(presentJob.getId()==70 || futureJob.getId()==70 ) {
						String key="P70";
					}

					// 1. fixing time windows
					//computingTimeatClientHomeSubJob(pickUpDropOff,j); // considering the load un loading time
					// 2. fixing number of people involved
					settingPeopleInSubJob(pickUpDropOff, -1,+1); //- drop-off #personas + pick up #personas
					// 3. setting time information
					//settingTimeInformation(pickUpDropOff);
					// 4. adding couple
					if(presentJob.getstartServiceTime()>futureJob.getstartServiceTime()) { // control
						System.out.println("error");
					}
					coupleFromWalkingRoutes.add(pickUpDropOff);
				}
			}
		}
		// create in the class Couple a constructor for setting the walking routes
		return coupleFromWalkingRoutes;
	}


	private void settingPresentTimeClient(Jobs presentJob) {
		presentJob.setTotalPeople(-1);
		presentJob.setClient(true);
		presentJob.setloadUnloadTime(test.getloadTimeHomeCareStaff());
		// 1. Setting the start service time -- startServiceTime
		presentJob.setStartServiceTime(presentJob.getEndTime());
		// 2. Set ArrivalTime
		presentJob.setarrivalTime(presentJob.getstartServiceTime()-presentJob.getloadUnloadTime());
		// 3. Set el fin del servicio
		presentJob.setEndServiceTime(presentJob.getstartServiceTime()+presentJob.getReqTime());	
		presentJob.setdepartureTime(presentJob.getendServiceTime());
	}



	private Jobs creatingSubPairJOb(Jobs j) {
		double pickUpTimeEarly=j.getstartServiceTime()+j.getReqTime();
		//double pickUpTimeLate=j.getstartServiceTime()+j.getReqTime()+test.getCumulativeWaitingTime();
		double pickUpTimeLate=j.getstartServiceTime()+j.getReqTime();
		Jobs futureJob= new Jobs(j.getId(),pickUpTimeEarly,pickUpTimeLate,j.getReqQualification(),j.getReqTime()); 
		futureJob.setClient(true);
		futureJob.setloadUnloadTime(test.getloadTimeHomeCareStaff());
		j.setPair(futureJob);
		// set start service time
		futureJob.setStartServiceTime(pickUpTimeLate);
		futureJob.setEndServiceTime(pickUpTimeLate);
		futureJob.setarrivalTime(futureJob.getstartServiceTime());
		futureJob.setdepartureTime(futureJob.getendServiceTime()+futureJob.getloadUnloadTime());
		return futureJob;
	}


	private void computingTimeatClientHomeSubJob(Couple DropOffpickUp, Jobs j) {
		// 1. Calculate the time for drop-Off home care staff at client home
		double dropOffTimeEarly=DropOffpickUp.getPresent().getStartTime()-test.getloadTimeHomeCareStaff();
		double dropOffTimeLate=DropOffpickUp.getPresent().getEndTime()-test.getloadTimeHomeCareStaff();
		DropOffpickUp.getPresent().setStartTime(dropOffTimeEarly);
		DropOffpickUp.getPresent().setEndTime(dropOffTimeLate);
		// 2. Calculate the time for pick-Up home care staff at client home
		double pickUpTimeEarly=DropOffpickUp.getFuture().getstartServiceTime()+DropOffpickUp.getFuture().getReqTime();
		//double pickUpTimeLate=DropOffpickUp.getFuture().getstartServiceTime()+DropOffpickUp.getFuture().getReqTime()+test.getCumulativeWaitingTime();
		double pickUpTimeLate=DropOffpickUp.getFuture().getstartServiceTime()+DropOffpickUp.getFuture().getReqTime();

		DropOffpickUp.getFuture().setStartTime(pickUpTimeEarly);
		DropOffpickUp.getFuture().setEndTime(pickUpTimeLate);
	}


	private Couple creatingCoupleforIndividualJobs(Jobs presentJob, Jobs futureJob) {
		presentJob.setPair(futureJob);

		///
		futureJob.setTotalPeople(1);
		futureJob.setClient(true);	
		futureJob.setserviceTime(0); //los nodos pick up contienen la información de los nodos
		// Setting the TW
		double tv=input.getCarCost().getCost(presentJob.getId()-1, futureJob.getId()-1);
		//double tv=input.getCarCost().getCost(presentJob.getId()-1, futureJob.getId()-1)*test.getDetour();
		futureJob.setStartTime(presentJob.getendServiceTime()+tv);
		//futureJob.setEndTime(futureJob.getStartTime()+test.getCumulativeWaitingTime()); // considering waiting time
		futureJob.setEndTime(futureJob.getStartTime()); // considering waiting time
		//futureJob.setEndTime(Double.MAX_VALUE);
		// modificar el tiempo requerido para el trabajo+	
		// 1. Setting the start service time -- startServiceTime
		futureJob.setStartServiceTime(futureJob.getEndTime());
		// 3. Set el fin del servicio
		futureJob.setEndServiceTime(futureJob.getstartServiceTime());
		// 2. Set ArrivalTime-<- la enferemera puede ser recogida una vez esta haya terminado el servicio
		futureJob.setarrivalTime(futureJob.getstartServiceTime());
		futureJob.setdepartureTime(futureJob.getendServiceTime()+futureJob.getloadUnloadTime());
		/////


		double deltaArrivalDeparture=presentJob.getDepartureTime()-presentJob.getArrivalTime();
		double deltaArrivalStartServiceTime=presentJob.getstartServiceTime()-presentJob.getArrivalTime();
		double deltarStartServiceTimeEndServiceTime=presentJob.getendServiceTime()-presentJob.getstartServiceTime();
		presentJob.setdeltaArrivalDeparture(deltaArrivalDeparture);
		presentJob.setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		presentJob.setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);
		//		// delta tiempo: future
		deltaArrivalDeparture=futureJob.getDepartureTime()-futureJob.getArrivalTime();
		deltaArrivalStartServiceTime=futureJob.getstartServiceTime()-futureJob.getArrivalTime();
		deltarStartServiceTimeEndServiceTime=futureJob.getendServiceTime()-futureJob.getstartServiceTime();
		futureJob.setdeltaArrivalDeparture(deltaArrivalDeparture);
		futureJob.setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		futureJob.setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);



		Couple presentCouple= new Couple(presentJob,futureJob);
		return presentCouple;
	}

	void convertingWalkingRoutesInOneTask(ArrayList<Couple> coupleFromWalkingRoutes) {
		if(walkingList!=null) {
			for(SubRoute r:walkingList) {
				if(r.getDropOffNode()!=null && r.getPickUpNode()!=null) {
					double walkingRouteLength=r.getDurationWalkingRoute();

					// 0. creation of subjobs and fixing time windows 
					if(r.getDropOffNode().getId()==3) {
						System.out.println("couple ");
					}
					Jobs present=creatinngPresentJobFromWR(r.getDropOffNode(),walkingRouteLength);
					r.getDropOffNode().setAssignedJobToMedicalCentre(r.getJobSequence());
					present.setAssignedJobToMedicalCentre(r.getJobSequence());
					Jobs future=creatinngFutureJobFromWR(present, r.getPickUpNode());
					//1. creation of couple
					Couple pairPickUpDropOffHCS=creatingCoupleClientHome(present,future); 

					// 2. fixing number of people involved
					settingPeopleInSubJob(pairPickUpDropOffHCS, -1,+1); //- drop-off #personas + pick up #personas

					// 4. adding couple
					coupleFromWalkingRoutes.add(pairPickUpDropOffHCS);
					// 3. checking information
					System.out.println("couple ");
					System.out.println(pairPickUpDropOffHCS.toString());
					System.out.println("couple ");
					if(present.getstartServiceTime()>future.getstartServiceTime()) { // control
						System.out.println("error");
					}

				}
			}
		}
	}

	public Couple creatingCoupleClientHome(Jobs presentJob, Jobs futureJob) {
		presentJob.setPair(futureJob);
		int directConnectionDistance= input.getCarCost().getCost(presentJob.getId()-1, futureJob.getId()-1); // setting the time for picking up the patient at home patient
		Couple pairPatientMedicalCentre=creatingPairPickUpDeliveryHCS(presentJob,futureJob, directConnectionDistance);
		return pairPatientMedicalCentre;
	}

	private Couple creatingPairPickUpDeliveryHCS(Jobs presentJob, Jobs futureJob, int directConnectionDistance) {
		Couple presentCouple= new Couple(presentJob,futureJob, directConnectionDistance, test.getDetour());
		return presentCouple;
	}

	private HashMap<Integer, Jobs> clientInWalkingRoutes() {
		HashMap<Integer,Jobs> jobsInWalkingRoutes= new HashMap<Integer,Jobs>();
		if(walkingList!=null) {
			for(SubRoute r:walkingList) {
				if(r.getJobSequence().size()>1) {
					for(Jobs j:r.getJobSequence()) {
						jobsInWalkingRoutes.put(j.getId(), j); // storing the job in the walking route
					}
				}
			}
		}
		return jobsInWalkingRoutes;
	}

	private Jobs creatinngFutureJobFromWR(Jobs present,Jobs pickUpNode) {
		// homeCarePickUp(dropOff,pickUp);

		Jobs future= new Jobs(pickUpNode.getId(),0,0 ,pickUpNode.getReqQualification(), 0); // Jobs(int id, int startTime, int endTime, int reqQualification,int reqTime)


		future.setTotalPeople(1);
		future.setClient(true);	
		future.setserviceTime(0); //los nodos pick up contienen la información de los nodos
		// Setting the TW
		double tv=input.getCarCost().getCost(present.getId()-1, pickUpNode.getId()-1);
		//double tv=input.getCarCost().getCost(present.getId()-1, pickUpNode.getId()-1)*test.getDetour();
		future.setStartTime(present.getendServiceTime()+tv);
		//future.setEndTime(future.getStartTime()+test.getCumulativeWaitingTime()); // considering waiting time
		future.setEndTime(future.getStartTime()); // considering waiting time

		// modificar el tiempo requerido para el trabajo+	
		// 1. Setting the start service time -- startServiceTime
		future.setStartServiceTime(future.getEndTime());
		// 3. Set el fin del servicio
		future.setEndServiceTime(future.getstartServiceTime());
		// 2. Set ArrivalTime-<- la enferemera puede ser recogida una vez esta haya terminado el servicio
		future.setarrivalTime(future.getstartServiceTime());
		future.setdepartureTime(future.getendServiceTime()+future.getloadUnloadTime());
		/// poner el tiempo de servicio en cero
		future.setserviceTime(0);
		// present
		double deltaArrivalDeparture=future.getDepartureTime()-future.getArrivalTime();
		double deltaArrivalStartServiceTime=future.getstartServiceTime()-future.getArrivalTime();
		double deltarStartServiceTimeEndServiceTime=future.getendServiceTime()-future.getstartServiceTime();
		future.setdeltaArrivalDeparture(deltaArrivalDeparture);
		future.setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		future.setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);
		return future;
	}



	private Jobs creatinngPresentJobFromWR(Jobs dropOffNode, double walkingRouteLength) {
		// start when the service
		Jobs present= new Jobs(dropOffNode.getId(),dropOffNode.getStartTime(),dropOffNode.getEndTime() ,dropOffNode.getReqQualification(), walkingRouteLength); // Jobs(int id, int startTime, int endTime, int reqQualification,int reqTime)

		///
		present.setTotalPeople(-1);
		present.setClient(true);
		present.setloadUnloadTime(test.getloadTimeHomeCareStaff());
		// 1. Setting the start service time -- startServiceTime
		present.setStartServiceTime(present.getEndTime());
		// 2. Set ArrivalTime
		present.setarrivalTime(present.getstartServiceTime()-present.getloadUnloadTime());
		// 3. Set el fin del servicio
		present.setEndServiceTime(present.getstartServiceTime()+walkingRouteLength);	
		present.setdepartureTime(present.getendServiceTime()+present.getloadUnloadTime());


		///
		// present
		double deltaArrivalDeparture=present.getDepartureTime()-present.getArrivalTime();
		double deltaArrivalStartServiceTime=present.getstartServiceTime()-present.getArrivalTime();
		double deltarStartServiceTimeEndServiceTime=present.getendServiceTime()-present.getstartServiceTime();
		present.setdeltaArrivalDeparture(deltaArrivalDeparture);
		present.setdeltaArrivalStartServiceTime(deltaArrivalStartServiceTime);
		present.setdeltarStartServiceTimeEndServiceTime(deltarStartServiceTimeEndServiceTime);
		return present;
	}


	// Getters
	public LinkedList<SubRoute> getSubroutes() {return walkingList;}



	public DrivingRoutes getRoutes() {return drivingRoute;}
	public Solution getInitialSolution() {return initialSolution;}
	public Solution getSolution() {return bestSolution;}



}
