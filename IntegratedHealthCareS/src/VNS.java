import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import umontreal.iro.lecuyer.rng.LFSR113;
import umontreal.iro.lecuyer.rng.RandomStreamBase;

public class VNS {
	DrivingRoutes drivingRoutes;
	private Inputs inp; // input problem
	private Test test; // input problem
	private Random rng;

	public VNS(DrivingRoutes dr, Test t, Inputs i) {
		drivingRoutes=dr;
		test=t;
		inp=i;
		rng=new Random(t.getSeed());
		RandomStreamBase stream = new LFSR113(); // L'Ecuyer stream
		t.setRandomStream(stream);
	}

	public Solution solvns(Solution copySolution) {
		Solution sol=null;
		Solution solCopy=new Solution(copySolution);
		
		for(int i=0; i<100 ;i++){
			sol= shaking(new Solution (solCopy));
			updateSolution(sol);
			sol.checkingSolution(inp,test,drivingRoutes.getobsInWalkingRoute());
			
			System.out.println("shaking");
			System.out.println(sol.toString());
			sol= swaping(sol);
			updateSolution(sol);
			sol.checkingSolution(inp,test,drivingRoutes.getobsInWalkingRoute());
			updateSolution(sol);
			System.out.println("swaping");
			System.out.println(sol.toString());
			solCopy=new Solution(sol);

// operators
			
			
		}
		return sol;
	}

	


	private void updateSolution(Solution sol) {
		// creación de partes

		SubJobs depotStart=new SubJobs(inp.getNodes().get(0));

		for(Route r:sol.getRoutes()) {
			totalPersonalFromDepot(depotStart,r);
			settingParts(depotStart,r);

		}
		// cuantificación de costos

	}

	private void settingParts(SubJobs depotStart, Route r) {
		r.getPartsRoute().clear();
		double amountParamedic=r.getAmountParamedic();
		double amountHomeCareStaff=r.getHomeCareStaff();
		double patients=0;
		SubJobs depotEnd=new SubJobs(inp.getNodes().get(0));
		Parts part= new Parts();
		r.getPartsRoute().add(part);
		part.getListSubJobs().add(depotStart);
		part= new Parts();
		for(SubJobs s:r.getSubJobsList()) {
			part.getListSubJobs().add(s);
			if(s.isClient()) {
				amountHomeCareStaff+=s.getTotalPeople();
			}
			else {
				patients=s.getTotalPeople();
				amountParamedic=amountParamedic+patients;
			}
			if(amountParamedic==0 && amountHomeCareStaff==0) {
				r.getPartsRoute().add(part);
				part= new Parts();

			}
		}
		if(amountParamedic!=0 && amountHomeCareStaff!=0 && !part.getListSubJobs().isEmpty()) {
			r.getPartsRoute().add(part);
		}
		part= new Parts();
		r.getPartsRoute().add(part);
		part.getListSubJobs().add(depotEnd);
		System.out.println("print");
	}

	private void totalPersonalFromDepot(SubJobs depotStart, Route r) {
		double amountParamedic=0;
		double amountHomeCareStaff=0;
		double dropOffHomeCare=0;
		double pickUppatient=0;

		for(SubJobs j:r.getSubJobsList()) {
			if(j.isClient()) {
				dropOffHomeCare+=j.getTotalPeople();
				if(amountHomeCareStaff<Math.abs(dropOffHomeCare)) {
					amountHomeCareStaff++;
				}
			}
			if(j.isPatient() ) {
				pickUppatient+=j.getTotalPeople();
				if(amountParamedic<Math.abs(pickUppatient)) {
					amountParamedic++;
				}
			}
		}
		r.setAmountParamedic(amountParamedic);
		r.setHomeCareStaff(amountHomeCareStaff);
		depotStart.setTotalPeople((int)(amountParamedic+amountHomeCareStaff));
	}

	private Solution swaping(Solution solCopy) {
		Solution sol= new Solution();
		LinkedList<Route> routes= solCopy.getRoutes();
		LinkedList<Route> routesListToMerge= new LinkedList<Route>();
		int totalRoutesToMerge=2;
		for(int i=0; i<totalRoutesToMerge;i++) {
			int index =-1;
			boolean otherRoute =true;
			double paramedic=0;
			double homeCareStaff=0;
			int iteration=-1;
			do {
				iteration++;
				index = this.rng.nextInt(routes.size()-1);	
				if(!routesListToMerge.isEmpty()) {
					paramedic=routesListToMerge.get(0).getAmountParamedic();
					homeCareStaff=routesListToMerge.get(0).getHomeCareStaff();
					if(routes.get(index).getAmountParamedic()==paramedic && routes.get(index).getHomeCareStaff()==homeCareStaff) {
						otherRoute =true;
					}
				}
				if(iteration>100) {
					break;
				}
			}
			while(routesListToMerge.contains(routes.get(index)) || !otherRoute);	
			otherRoute =false;
			routesListToMerge.add(routes.get(index));
		}

		LinkedList<Route> newRoutes=mergingRoutes(routesListToMerge);
		// en su defecto se forman dos rutas
		if(!newRoutes.isEmpty()) {
			for(Route r: routesListToMerge) {
				routes.remove(r);
			}
			for(Route r: newRoutes) {
				routes.add(r);
			}
		}
		for(Route r: routes) {
			sol.getRoutes().add(r);
		}
		return sol;

	}

	private Solution shaking(Solution copySolution) {
		// aleatoriamente se seleccionan dos rutas
		Solution sol= new Solution();
		LinkedList<Route> routes= copySolution.getRoutes();
		LinkedList<Route> routesListToMerge= new LinkedList<Route>();
		int totalRoutesToMerge=2;
		for(int i=0; i<totalRoutesToMerge;i++) {
			int index =0;
			do {
				index = this.rng.nextInt(routes.size()-1);

			}
			while(routesListToMerge.contains(routes.get(index)));	
			routesListToMerge.add(routes.get(index));
		}
		// se intentan unificar
		LinkedList<Route> newRoutes=mergingRoutes(routesListToMerge);
		// en su defecto se forman dos rutas
		if(!newRoutes.isEmpty()) {
			for(Route r: routesListToMerge) {
				routes.remove(r);
			}
			for(Route r: newRoutes) {
				routes.add(r);
			}
		}
		for(Route r: routes) {
			sol.getRoutes().add(r);
		}
		return sol;
	}

	private LinkedList<Route> mergingRoutes(LinkedList<Route> routesListToMerge) {
		LinkedList<Route> newRoutes= new LinkedList<Route>();
		Route newRoute= new Route();
		newRoutes.add(newRoute);
		ArrayList<SubJobs> patients= selectingPatients(routesListToMerge);
		patients.sort(Jobs.SORT_BY_STARTW);
		ArrayList<SubJobs> clients= selectingClients(routesListToMerge);
		clients.sort(Jobs.SORT_BY_STARTW);
		HashMap<String,SubJobs> jobsToInsert= listJobs(patients,clients);

		assigmentPatients(patients,jobsToInsert,newRoute,newRoutes);
		assigmentClients(clients,jobsToInsert,newRoute,newRoutes);

		if(newRoutes.size()>routesListToMerge.size()) {
			newRoutes.clear();	
		}

		return newRoutes;
	}

	private void assigmentClients(ArrayList<SubJobs> clients, HashMap<String, SubJobs> jobsToInsert, Route newRoute,
			LinkedList<Route> newRoutes) {
		ArrayList<SubJobs> clientsHardTimeWindow= selectingclientsHardTimeWindow(clients);
		ArrayList<SubJobs> clientsPickUps= selectingclientsPickUps(clients);
		clientsHardTimeWindow.sort(Jobs.SORT_BY_STARTW);

		for(SubJobs p:clientsHardTimeWindow) { // assigning clients hard time window
			if(jobsToInsert.containsKey(p.getSubJobKey())) {
				insertingClients(newRoute,p,newRoutes);
				updatingList(jobsToInsert,p);
			}

		}
		for(SubJobs p:clientsPickUps) { // assigning clients hard time window
			if(jobsToInsert.containsKey(p.getSubJobKey())) {
				insertingClients(newRoute,p,newRoutes);
				updatingList(jobsToInsert,p);
			}
		}

	}

	private ArrayList<SubJobs> selectingclientsPickUps(ArrayList<SubJobs> clients) {
		ArrayList<SubJobs> clientsHardTimeWindow= new ArrayList<SubJobs>();
		for(SubJobs j: clients) {
			if(j.getTotalPeople()>0){
				clientsHardTimeWindow.add(j);
			}
		}
		return clientsHardTimeWindow;
	}

	private void insertingClients(Route route, SubJobs p, LinkedList<Route> newRoutes) {
		boolean inserted= false;
		SubJobs j1=(SubJobs)this.drivingRoutes.getCoupleList().get(p.getSubJobKey()).getStartEndNodes().get(1);
		SubJobs j2=(SubJobs)this.drivingRoutes.getCoupleList().get(p.getSubJobKey()).getStartEndNodes().get(0);

		if(route.getSubJobsList().isEmpty()) {	
			route.getSubJobsList().add(j1);
			route.getSubJobsList().add(j2);
		}
		else { // iterando sobre la ruta
			inserted=iteratingOverRoute(route,j1,j2);
		}
		if(!inserted) {
			Route newRoute= new Route();
			newRoutes.add(newRoute);
			newRoute.getSubJobsList().add(j1);
			newRoute.getSubJobsList().add(j2);
		}
	}

	private ArrayList<SubJobs> selectingclientsHardTimeWindow(ArrayList<SubJobs> clients) {
		ArrayList<SubJobs> clientsHardTimeWindow= new ArrayList<SubJobs>();
		for(SubJobs j: clients) {
			if(j.getTotalPeople()<0){
				clientsHardTimeWindow.add(j);
			}
		}
		return clientsHardTimeWindow;
	}

	private void assigmentPatients(ArrayList<SubJobs> patients, HashMap<String, SubJobs> jobsToInsert, Route newRoute,
			LinkedList<Route> newRoutes) {
		for(SubJobs p:patients) { // assigning patients
			if(p.getSubJobKey().equals("P71")) {
				System.out.println("stop");
			}
			if(jobsToInsert.containsKey(p.getSubJobKey())) {
				insertingPatients(newRoute,p,newRoutes);
				updatingList(jobsToInsert,p);
			}

		}
	}


	private void updatingList(HashMap<String, SubJobs> jobsToInsert, SubJobs p) {
		SubJobs j1=(SubJobs)this.drivingRoutes.getCoupleList().get(p.getSubJobKey()).getStartEndNodes().get(1);
		SubJobs j2=(SubJobs)this.drivingRoutes.getCoupleList().get(p.getSubJobKey()).getStartEndNodes().get(0);
		jobsToInsert.remove(j1.getSubJobKey());
		jobsToInsert.remove(j2.getSubJobKey());
	}

	private HashMap<String, SubJobs> listJobs(ArrayList<SubJobs> patients, ArrayList<SubJobs> clients) {
		HashMap<String,SubJobs> jobsToInsert= new HashMap<>();
		for(SubJobs p: patients) {
			jobsToInsert.put(p.getSubJobKey(),p);
		}
		for(SubJobs p: clients) {
			jobsToInsert.put(p.getSubJobKey(),p);
		}
		return jobsToInsert;
	}

	private void insertingPatients(Route route, SubJobs p, LinkedList<Route> newRoutes) {
		boolean inserted= false;
		SubJobs j1=(SubJobs)this.drivingRoutes.getCoupleList().get(p.getSubJobKey()).getStartEndNodes().get(1);
		SubJobs j2=(SubJobs)this.drivingRoutes.getCoupleList().get(p.getSubJobKey()).getStartEndNodes().get(0);

		if(route.getSubJobsList().isEmpty()) {	
			insertingCouple(route,-1,-1,j1,j2,inp,test);
		}
		else { // iterando sobre la ruta
			inserted=iteratingOverRoute(route,j1,j2);
		}
		if(!inserted) {
			Route newRoute= new Route();
			insertingCouple(newRoute,-1,-1,j1,j2,inp,test);
			newRoutes.add(newRoute);
		}


	}

	private void insertingCouple(Route newRoute, int positionj1, int positionj2, SubJobs j1, SubJobs j2, Inputs inp2, Test test2) {
		if(positionj1==-1 && positionj2==-1) {
			newRoute.getSubJobsList().add(j1);
			newRoute.getSubJobsList().add(j2);}
		else {
			newRoute.getSubJobsList().add(positionj1,j1);
			newRoute.getSubJobsList().add(positionj2,j2);
		}
		Edge e= new Edge(j1,j2,inp,test);
		newRoute.getEdges().put(e.getEdgeKey(), e);
	}

	private boolean iteratingOverRoute(Route route, SubJobs j1, SubJobs j2) {
		boolean inserted= false;
		inserted=early(route,j1,j2);
		if(!inserted) { // inserted as the first job
			inserted=latest(route,j1,j2);
		}
		if(!inserted) { // in the middle of the route
			inserted=insideRoute(route,j1,j2);
		}
		else {
			// infeasibility
		}
		return inserted;
	}



	private boolean insideRoute(Route route, SubJobs j1, SubJobs j2) {
		boolean inserted= false;
		for(int i=0;i<route.getSubJobsList().size()-1;i++) {
			SubJobs a=route.getSubJobsList().get(i);
			SubJobs b=route.getSubJobsList().get(i+1);
			if(a.getDepartureTime()<=j1.getArrivalTime() && j1.getDepartureTime()<=b.getArrivalTime()) {
				inserted=insertingB(route,i,j1,j2);
			}
		}
		return inserted;
	}

	private boolean insertingB(Route route, int startIteration, SubJobs j1, SubJobs j2) {
		boolean inserted= false;		
		SubJobs nextJob=route.getSubJobsList().get(startIteration+1);
		if(j2.getDepartureTime()<nextJob.getArrivalTime() || j2.getArrivalTime()>nextJob.getDepartureTime() ) {
			if(j2.getDepartureTime()<nextJob.getArrivalTime()) {
				if(vehicleCapacity(route,j1,startIteration+1,j2,startIteration+2)) {
					inserted= true;
				}		
			}
			else{// iterating over Route
				for(int i=startIteration+1;i<route.getSubJobsList().size()-1;i++) {
					SubJobs a=route.getSubJobsList().get(i);
					SubJobs b=route.getSubJobsList().get(i+1);		
					if(a.getDepartureTime()<=j2.getArrivalTime() && j2.getDepartureTime()<=b.getArrivalTime()) {
						if(vehicleCapacity(route,j1,startIteration+1,j2,i+1)) {
							inserted= true;
						}
						if(inserted) {
							//route.getSubJobsList().add(startIteration+1,j1);
							//route.getSubJobsList().add(i+2,j2);
							insertingCouple(route,startIteration+1,i+2,j1,j2,inp,test);
							break;
						}
					}
				}
			}
		}
		return inserted;
	}

	private boolean vehicleCapacity(Route route, SubJobs j1, int positionJ1, SubJobs j2, int positionJ2) {
		boolean inserted= false;	
		LinkedList<SubJobs> newSubJobsList= new LinkedList<SubJobs>();
		for(int i=0;i<route.getSubJobsList().size();i++) {
			SubJobs job=route.getSubJobsList().get(i);
			if(i==positionJ1) {
				newSubJobsList.add(j1);
			}
			if(i==positionJ2) {
				newSubJobsList.add(j2);
			}
			newSubJobsList.add(job);
		}

		int passegers=0;
		int action=0;
		for(SubJobs s: newSubJobsList) {
			action=s.getTotalPeople();
			passegers+=action;
			if(Math.abs(passegers)>inp.getVehicles().get(0).getMaxCapacity()) {
				inserted=false;
				break;
			}
		}
		if(Math.abs(passegers)>inp.getVehicles().get(0).getMaxCapacity()) {
			inserted=false;
		}
		else {
			inserted=true;
		}

		return inserted;
	}

	private boolean latest(Route route, SubJobs j1, SubJobs j2) {
		boolean inserted= false;
		SubJobs lastJob=route.getSubJobsList().get(route.getSubJobsList().size()-1);
		if(lastJob.getDepartureTime()<=j1.getArrivalTime()) { // inserted as the first job
			inserted=true;
			insertingCouple(route,-1,-1,j1,j2,inp,test);
			//route.getSubJobsList().add(j1);
			//route.getSubJobsList().add(j2);

		}
		return inserted;
	}

	private boolean early(Route route, SubJobs j1, SubJobs j2) {
		boolean inserted= false;
		SubJobs firstJob=route.getSubJobsList().get(0);
		if(j2.getDepartureTime()<=firstJob.getArrivalTime()) { // inserted as the first job
			inserted=true;
			insertingCouple(route,0,1,j1,j2,inp,test);
			//route.getSubJobsList().add(0,j2);
			//route.getSubJobsList().add(0,j1);
		}

		// cambiando la hora de j2
		//double tvj2firstJob

		return inserted;
	}

	private ArrayList<SubJobs> selectingClients(LinkedList<Route> newRoutes) {
		ArrayList<SubJobs> clients= new ArrayList<SubJobs>();
		for(Route r: newRoutes) {

			for(SubJobs j:r.getSubJobsList()) {
				if(j.isClient()) {
					clients.add(j);
				}
			}

		}
		return clients;
	}

	private ArrayList<SubJobs> selectingPatients(LinkedList<Route> newRoutes) {
		ArrayList<SubJobs> patients= new ArrayList<SubJobs>();
		for(Route r: newRoutes) {

			for(SubJobs j:r.getSubJobsList()) {
				if(!j.isClient()) {
					patients.add(j);}
			}

		}
		return patients;
	}

}
