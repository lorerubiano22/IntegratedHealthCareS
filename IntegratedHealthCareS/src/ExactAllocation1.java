// Warming the sequence of jobs_ el problema aca es que estoy confundiendo la sequencia de los trabajo con la posición de cada trabajo
import java.util.LinkedList;

import com.dashoptimization.XPRB;
import com.dashoptimization.XPRBexpr;
import com.dashoptimization.XPRBprob;
import com.dashoptimization.XPRBvar;

import com.dashoptimization.*;


public class ExactAllocation1 {
	int n=0; // total of jobs 
	public LinkedList<Jobs> jobsList;
	public LinkedList<Jobs> proposedSequence;
	private Test test;
	private Inputs input;
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

	public ExactAllocation1(Test t, Inputs inp) {
		test=t;
		input=inp;
		bcl = new XPRB();
		p=bcl.newProb("Scedulling"); 
	}

	void solving(SubRoute wr) {
		if(!wr.getJobSequence().isEmpty()) {

			// 1. Copying the jobs list 
			jobsList=new LinkedList<Jobs>();
			for(Jobs j:wr.getJobSequence()) {
				jobsList.add(new Jobs(j));
			}
			n=jobsList.size();

		
			// 3. Creating a the variable list
			creatingVariables();

			// 4. Objective function
			for(int i=0;i<n-1;i++) {
				for(int j=1;j<n;j++) {
					lobj.add(x[i][j].mul(input.getWalkCost().getCost(i,j))); 
					p.setObj(lobj); 
				}
			}

			// 5. Constraints
	

			// Each job has a position<- job = new XPRBexpr();
			for(int i=0;i<n;i++) {
				job = new XPRBexpr();
				for(int j=0;j<n;j++) {
					job.add(x[i][j].mul(1));
				}
				p.newCtr("Job", job.eql(1));
			}

			// Each position has one job<- position = new XPRBexpr();
			for(int j=0;j<n;j++) {
				position = new XPRBexpr();
				for(int i=0;i<n;i++) {
					job.add(x[i][j].mul(1));
				}
				p.newCtr("Position", position.eql(1));
			}

			// Maximum travel time between two jobs<- job2job = new XPRBexpr();
//			for(int j=0;j<n;j++) {
//				for(int i=0;i<n;i++) {
//					job2job = new XPRBexpr();
//					job.add(x[i][j].mul(input.getWalkCost().getCost(i, j)));
//				}
//				p.newCtr("Position", job2job.lEql(test.getWalking2jobs()));
//			}
			
			
			// Maximum route length <-maxRoute = new XPRBexpr();
//			for(int j=0;j<n;j++) {
//				for(int i=0;i<n;i++) {
//					job2job = new XPRBexpr();
//					job.add(x[i][j].mul(input.getWalkCost().getCost(i, j)));
//				}
//				p.newCtr("Position", job2job.lEql(test.getWalking2jobs()));
//			}
//			
			// Time window<- timeWindow = new XPRBexpr();
			/*SOLVING*/
			p.setSense(XPRB.MINIM);            /* Choose the sense of the optimization */
			p.mipOptimize("");                 /* Solve the MIP-problem */

			System.out.println("Objective: " + p.getObjVal());  /* Get objective value */

		
			if((p.getMIPStat()==XPRB.MIP_SOLUTION) || (p.getMIPStat()==XPRB.MIP_OPTIMAL)) {
					
			System.out.println("Model solved: "+p.getObjVal());
			}
			else {
				System.out.println("Model not solved");
			
			}
			
		}
	}


	private void creatingVariables() {
		x= new XPRBvar[jobsList.size()][jobsList.size()];
		for(int i=0; i<n;i++ ) {
			for(int j=0; j<n;j++ ) {
				x[i][j]=p.newVar("x_("+i+","+j+")", XPRB.BV);  
			}
		}
		y = new XPRBvar[jobsList.size()];
		for(int i=0;i<n;i++) {
			y[i] = p.newVar("w_("+i+")", XPRB.SC, 0, n);    
		}
		lobj = new XPRBexpr();
	}




}
