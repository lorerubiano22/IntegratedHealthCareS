
// Warming the sequence of jobs_ el problema aca es que estoy confundiendo la sequencia de los trabajo con la posición de cada trabajo
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
public class ExactDrivingRoutes {
	int n = 0; // total of jobs
	public LinkedList<Jobs> jobsList;
	public LinkedList<Jobs> proposedSequence;
	private Test test;
	private Inputs input;
	private ArrayList<Route> selectedRoutes= new ArrayList<Route>(); // list of selected walking routes
	private SubRoute subR;
	int[][] coverage;
	// slots selection
	XPRBvar[] DR;// Variable <-Binary. 1 if the slot is selected i is assigned to position j
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



	public ExactDrivingRoutes(Solution newSol) {
		bcl = new XPRB();
		p = bcl.newProb("Selecting routes");
		
		HashMap<String, SubJobs> list= new HashMap<>();
		for(Route r: newSol.getRoutes()) {
			for(SubJobs j:r.getSubJobsList()) {
				list.put(j.getSubJobKey(), j);
			}
		}
		
		ArrayList<SubJobs> listSubJobs= new ArrayList<SubJobs>();
		for(SubJobs j:list.values()) {
			listSubJobs.add(j);
		}
		
		// coverage
		coverage = generatingCoverageMatrix(listSubJobs,newSol.getRoutes());
		
		selectionDrivingRoutes(listSubJobs,newSol.getRoutes());
		
	}


	private int[][] generatingCoverageMatrix(ArrayList<SubJobs> listSubJobs, LinkedList<Route> linkedList) {
		int[][] coverage = new int[linkedList.size()][listSubJobs.size()];
		for(int i=0;i<linkedList.size();i++) {
			Route r= linkedList.get(i);
			for(int j=0;j<listSubJobs.size();j++) {
				SubJobs s=listSubJobs.get(j);
				if(r.getJobsDirectory().containsKey(s.getSubJobKey())) {
				coverage[i][j]=1;}
			}
		}
		return coverage;
	}


	/*
	 * Model to select slots
	 */
	public void selectionDrivingRoutes(ArrayList<SubJobs> listSubJobs, LinkedList<Route> routes) {
		try
		{

			bcl = new XPRB();
			p = bcl.newProb("Slots selection");


			// 1. Creation of variables
			creatingVariables(routes);

			// 2. Objective function
			for (int i = 0; i < routes.size(); i++) {
				lobj.add(DR[i].mul(routes.get(i).getDurationRoute()));
				p.setObj(lobj);
			}

			// 3: Constraint
			// all jobs have to be once in the set of slots selected
			for (int j = 0; j < listSubJobs.size(); j++) {
				slotJob = new XPRBexpr();
				for (int i = 0; i < routes.size(); i++) {
					if(coverage[i][j]==1) {
						slotJob.add(DR[i].mul(1));}
				}
				p.newCtr("subJob", slotJob.eql(1));
			}



			p.print();          /* Problem formulation */
			p.exportProb(XPRB.MPS,"drivingRoutes"); /* Output the matrix in MPS format */
			p.exportProb(XPRB.LP,"drivingRoutes");  /* Output the matrix in LP format */
			p.setSense(XPRB.MINIM); /* Choose the sense of the optimization */
			//p.setSense(XPRB.MAXIM); /* Choose the sense of the optimization */
			p.mipOptimize(""); /* Solve the MIP-problem */

			System.out.println("Objective: " + p.getObjVal()); /* Get objective value */

			if ((p.getMIPStat() == XPRB.MIP_SOLUTION) || (p.getMIPStat() == XPRB.MIP_OPTIMAL)) {
				System.out.println("Model solved: " + p.getObjVal());
				/* Print out the solutions found */
				for (int i = 0; i < routes.size(); i++) {
					if (DR[i].getSol() > 0) {
						System.out.print(DR[i].getName() + ":" + DR[i].getSol() + " ");
						//subR= new SubRoute();
						//int jobPosition=0;
						//					for(Jobs j:jobSlots.get(i).getJobSequence()) {
						//						subR.addJobSequence(j, jobPosition, j.getstartServiceTime());
						//						jobPosition++;
						//					}
						this.selectedRoutes.add(routes.get(i));
					}
				}
			} else {
				System.out.println("Model not solved");

			}
		}
		catch(IOException e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}


	
	private void creatingVariables(LinkedList<Route> listSubJobs) {
		DR = new XPRBvar[listSubJobs.size()];
		for (int i = 0; i < listSubJobs.size(); i++) {
			DR[i] = p.newVar("DR_(" + i + ")", XPRB.BV);
		}
		lobj = new XPRBexpr();
	}


	// getters

	public ArrayList<Route> getDrivingRoutes() {return selectedRoutes;}
}
