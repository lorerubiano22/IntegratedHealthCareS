
// Warming the sequence of jobs_ el problema aca es que estoy confundiendo la sequencia de los trabajo con la posición de cada trabajo
import java.util.LinkedList;

import com.dashoptimization.XPRB;
import com.dashoptimization.XPRBexpr;
import com.dashoptimization.XPRBprob;
import com.dashoptimization.XPRBvar;

/**
 * @author Lorena
 *
 */
/**
 * @author Lorena
 *
 */
public class ExactAllocationDummyExample {
	int n = 0; // total of jobs
	public LinkedList<Jobs> jobsList;
	public LinkedList<Jobs> proposedSequence;
	private Test test;
	private Inputs input;

	// slots selection
	XPRBvar[] WR;// Variable <-Binary. 1 if the slot is selected i is assigned to position j
	// [i][j]
	XPRBexpr slotJob; // Constraint: Each position is once over all slots

	// sequence
	XPRBvar[][] x;// Variable <-Binary. 1 if the job is i is assigned to position j [i][j]
	XPRBvar[] y; // Variable <- start time of service
	XPRBexpr lobj;// Objective function
	XPRBexpr job;// Constraint: Each job has a position
	XPRBexpr position; // Constraint: Each position has one job
	XPRBexpr job2job; // Constraint: Maximum travel time between two jobs
	XPRBexpr maxRoute; // Constraint: Maximum route length
	XPRBexpr timeWindow; // Constraint: Time window
	XPRB bcl;
	XPRBprob p;

	public ExactAllocationDummyExample(Test t, Inputs inp) {
		test = t;
		input = inp;
		bcl = new XPRB();
		p = bcl.newProb("Schedulling");
	}

	void solving(SubRoute wr) {
		if (!wr.getJobSequence().isEmpty()) {

			// 1. Copying the jobs list
			jobsList = new LinkedList<Jobs>();
			for (Jobs j : wr.getJobSequence()) {
				jobsList.add(new Jobs(j));
			}
			n = jobsList.size();

			// 3. Creating a the variable list
			creatingVariables();

			// 4. Objective function
			for (int i = 0; i < n - 1; i++) {
				for (int j = 1; j < n; j++) {
					lobj.add(x[i][j].mul(input.getWalkCost().getCost(i, j)));
					p.setObj(lobj);
				}
			}

			// 5. Constraints

			// Each job has a position<- job = new XPRBexpr();
			for (int i = 0; i < n; i++) {
				job = new XPRBexpr();
				for (int j = 0; j < n; j++) {
					job.add(x[i][j].mul(1));
				}
				p.newCtr("Job", job.eql(1));
			}

			// Each position has one job<- position = new XPRBexpr();
			for (int j = 0; j < n; j++) {
				position = new XPRBexpr();
				for (int i = 0; i < n; i++) {
					position.add(x[i][j].mul(1));
				}
				p.newCtr("Position", position.eql(1));
			}

			// Maximum travel time between two jobs<- job2job = new XPRBexpr();


			// Maximum route length <-maxRoute = new XPRBexpr();



			/* SOLVING */
			p.setSense(XPRB.MINIM); /* Choose the sense of the optimization */
			p.mipOptimize(""); /* Solve the MIP-problem */

			System.out.println("Objective: " + p.getObjVal()); /* Get objective value */

			if ((p.getMIPStat() == XPRB.MIP_SOLUTION) || (p.getMIPStat() == XPRB.MIP_OPTIMAL)) {

				System.out.println("Model solved: " + p.getObjVal());
			} else {
				System.out.println("Model not solved");

			}

		}
	}

	private void creatingVariables() {
		x = new XPRBvar[jobsList.size()][jobsList.size()];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				x[i][j] = p.newVar("x_(" + i + "," + j + ")", XPRB.BV);
			}
		}
		y = new XPRBvar[jobsList.size()];
		for (int i = 0; i < n; i++) {
			y[i] = p.newVar("w_(" + i + ")", XPRB.SC, 0, n);
		}
		lobj = new XPRBexpr();
	}

	/* Model to select slots */
	public void selectionWalkingRoutes(LinkedList<SubRoute> jobSlots) {
		bcl = new XPRB();
		p = bcl.newProb("Slots selection");

		LinkedList<Jobs> dummy=testing(jobSlots);


		// 0. Input data
		int[][] coverage = new int[jobSlots.size()][dummy.size()];
		// TO DO
		generationCoverageMatrix(jobSlots, coverage); // coverage matrix slot vs jobs

		int[] jobsList = new int[dummy.size()]; // vector of job to in the slots
		jobsInWalkingRoutes(jobsList,dummy);

		// 1. Creation of variables
		creatingVariables(jobSlots);

		// 2. Objective function
		for (int i = 0; i < jobSlots.size(); i++) {
			lobj.add(WR[i].mul(jobSlots.get(i).getTotalTravelTime()));
			p.setObj(lobj);
		}

		// 3: Constraint

		for (int j = 0; j < dummy.size(); j++) {
			System.out.print("\n job "+ dummy.get(j).getId() +"position " + j );
			slotJob = new XPRBexpr();
			for (int i = 0; i < jobSlots.size(); i++) {
				System.out.print("\n coverage "+ coverage[i][input.getNodes().get(j).getId()-1]);
				slotJob.add(WR[i].mul(coverage[i][input.getNodes().get(j).getId()-1]));
			}
			p.newCtr("slot", slotJob.eql(jobsList[j]));
		}

		p.setSense(XPRB.MINIM); /* Choose the sense of the optimization */
		p.mipOptimize(""); /* Solve the MIP-problem */

		System.out.println("Objective: " + p.getObjVal()); /* Get objective value */

		if ((p.getMIPStat() == XPRB.MIP_SOLUTION) || (p.getMIPStat() == XPRB.MIP_OPTIMAL)) {
			System.out.println("Model solved: " + p.getObjVal());
			/* Print out the solutions found */
			for (int i = 0; i < jobSlots.size(); i++) {
				if(WR[i].getSol()>0) {
					System.out.print(WR[i].getName() + ":" + WR[i].getSol() + " ");
				}
			}
			
		} 
		else {
			System.out.println("Model not solved");

		}



	}

	private LinkedList<Jobs> testing(LinkedList<SubRoute> jobSlots) {
		LinkedList<Jobs> dummy= new LinkedList<Jobs> ();
		jobSlots.clear();
		Jobs n0=new Jobs(0,0,480,1,10);
		dummy.add(n0);
		Jobs n1=new Jobs(1,0,480,1,10);
		dummy.add(n1);
		Jobs n2=new Jobs(2,0,480,1,10);
		dummy.add(n2);
		Jobs n3=new Jobs(3,0,480,1,10);
		dummy.add(n3);
		Jobs n4=new Jobs(4,0,480,1,10);
		dummy.add(n4);
		Jobs n5=new Jobs(5,0,480,1,10);
		dummy.add(n5);

		
		
		SubRoute subR= new SubRoute();
		subR.setSlotID(0);
		subR.addJobSequence(n0, 0, 0);
		subR.addJobSequence(n1, 1, 0);
		subR.addJobSequence(n2, 2, 0);
		subR.setTotalTravelTime(250);
		jobSlots.add(subR);

		subR= new SubRoute();
		subR.setSlotID(1);
		subR.addJobSequence(n2, 0, 0);
		subR.addJobSequence(n1, 1, 0);
		subR.addJobSequence(n0, 2, 0);
		
		
		subR.setTotalTravelTime(50);
		jobSlots.add(subR);

		subR= new SubRoute();
		subR.setSlotID(2);
		subR.addJobSequence(n3, 0, 0);
		subR.addJobSequence(n4, 1, 0);
		subR.addJobSequence(n5, 2, 0);
		subR.setTotalTravelTime(70);
		jobSlots.add(subR);

		subR= new SubRoute();
		subR.setSlotID(3);
		subR.addJobSequence(n4, 0, 0);
		subR.addJobSequence(n3, 1, 0);
		subR.addJobSequence(n5, 2, 0);
		subR.setTotalTravelTime(190);
		jobSlots.add(subR);
		
		
		subR= new SubRoute();
		subR.setSlotID(4);
		jobSlots.add(subR);
		subR.setTotalTravelTime(20);
		subR= new SubRoute();
		subR.setSlotID(5);
		subR.addJobSequence(n0, 0, 0);
		jobSlots.add(subR);
		subR.setTotalTravelTime(20);
		subR= new SubRoute();
		subR.setSlotID(6);
		subR.addJobSequence(n1, 0, 0);
		jobSlots.add(subR);
		subR.setTotalTravelTime(20);
		subR= new SubRoute();
		subR.setSlotID(7);
		subR.addJobSequence(n2, 0, 0);
		jobSlots.add(subR);
		subR.setTotalTravelTime(20);
		
		
		return dummy;
	}

	private void jobsInWalkingRoutes(int[] jobsList, LinkedList<Jobs> dummy) {
		for (int i = 0; i < dummy.size(); i++) {
			jobsList[dummy.get(i).getId()] = 1;
		}
	}

	private void generationCoverageMatrix(LinkedList<SubRoute> jobSlots, int[][] coverage) {
		for(SubRoute slot:jobSlots) {
			for(Jobs j:slot.getJobSequence()) {
				coverage[slot.getSlotID()][j.getId()]=1;
			}
		}

	}

	private void creatingVariables(LinkedList<SubRoute> jobSlots) {
		WR = new XPRBvar[jobSlots.size()];
		for (int i = 0; i < jobSlots.size(); i++) {
			WR[i] = p.newVar("WR_(" + i + ")", XPRB.BV);
		}
		lobj = new XPRBexpr();
	}

}
