import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;



public class Algorithm {
	private final Test test;
	private final Inputs input;
	private final WalkingRoutes subroutes;
	private final DrivingRoutes routes;
	private  ArrayList<Couple> subJobsList= new ArrayList<Couple>();
	public Algorithm(Test t, Inputs i, Random r) {
		test = t;
		input = i;
		subroutes = new WalkingRoutes(input, r, t, i.getNodes());
		udateListJobs();// jobs couple - class SubJobs
		routes = new DrivingRoutes(input, r, t, i.getNodes());
		setSolution(subroutes,routes);
	}



	private void udateListJobs() {
		// stage 0: set the jobs which are not in a walking route
		HashMap<Integer,Jobs> jobsInWalkingRoutes= new HashMap<>();
		for(SubRoute r:subroutes.getWalkingRoutes()) {
			for(Jobs j:r.getJobSequence()) {
				jobsInWalkingRoutes.put(j.getId(), j);
			}
		}

		// stage 1: convert walking route in big jobs
		for(SubRoute r:subroutes.getWalkingRoutes()) {
			if(r.getPickUpNode()==null) {
				System.out.println("stop ID "+r.getSlotID());
			}
			// definition of the couples
			Jobs pickUp=r.getPickUpNode();
			Jobs dropOff=r.getPickUpNode();
			pickUp.setPair(dropOff);
		}


		for(Jobs j:input.getNodes()) {
			if(j.getsubJobPair()!=null) {
				Couple separateJobs= new Couple(j,j.getsubJobPair());
				subJobsList.add(separateJobs);}
			else {
				// when the current job is a medical centre it doesnt have a pair
				Couple separateJobs= new Couple(j,j);
			}
		}

	}

	private void setSolution(WalkingRoutes subroutes2, DrivingRoutes routes2) {
		// TODO Auto-generated method stub

	}

	public void solve(String outputsFilePath) {
	}
}
