import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DrivingRoutes {

	private Inputs inp; // input problem
	private Test test; // input problem
	private Random rn;
	private  ArrayList<Couple> subJobsList= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsHighestQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsMediumQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobsLowestQualification= new ArrayList<Couple>();
	private  ArrayList<Couple> subJobspatients= new ArrayList<Couple>();

	public DrivingRoutes(Inputs i, Random r, Test t, ArrayList<Couple> subJobsList) {
		inp=i;
		test=t;
		rn=r;
		this.subJobsList=subJobsList;
	}

	public void generateAfeasibleSolution() {
		// 1. Initial feasible solution
		Solution initialSol= createInitialSolution();
		// a solution is a set of routes
		// 2. VNS
		// Local search



	}

	private Solution createInitialSolution() {
		Solution initialSol= new Solution();
		// create a combination of couples for home care staff: match req qualification
		combinationClientjob(initialSol);
		// create a combination of couples for home care staff and patients: match req qualification

		// Assignment combination to vehicles and shift

		return initialSol;
	}

	private void combinationClientjob(Solution initialSol) {
		// 0.classified couples according the req qualification
		for(int qualification=0;qualification<=inp.getMaxQualificationLevel();qualification++) {
			for(Couple c:subJobsList) {
				if(c.getQualification()==qualification && qualification==0) {
					subJobspatients.add(c);
				}
				if(c.getQualification()==qualification && qualification==1) {
					subJobsLowestQualification.add(c);
				}
				if(c.getQualification()==qualification && qualification==2) {
					this.subJobsMediumQualification.add(c);
				}
				if(c.getQualification()==qualification && qualification==3) {
					this.subJobsHighestQualification.add(c);
				}
			}
		}
		ArrayList<ArrayList<Couple>> newCouples= new ArrayList<ArrayList<Couple>>();
		
		
		// 1. Select the client jobs which requires the highest qualification level =3
		ArrayList<Couple> hcs3level= assignJob3Levels(newCouples) ; // it contains jobs which requires a qualification level = 1, 2 or 3
		// 2. Select the client jobs which requires the second highest qualification level =2

		ArrayList<Couple> hcs2level= assignJob2Levels(newCouples) ; // // it contains jobs which requires a qualification level = 1 or 2

		// 3. Select the client jobs which requires the lowest qualification level =1

		ArrayList<Couple> hcs1level= assignJobLevels(newCouples) ; // // it contains jobs which requires a qualification level = 1

	}

	private ArrayList<Couple> assignJobLevels() {
		// TODO Auto-generated method stub
		return null;
	}

	private ArrayList<Couple> assignJob2Levels() {
		// TODO Auto-generated method stub
		return null;
	}

	private ArrayList<Couple> assignJob3Levels(ArrayList<ArrayList<Couple>> newCouples) {
		// couple structure |drop-off|---------<pick-Up>
		ArrayList<Couple> hcs3level= new ArrayList<Couple>();
		for(Couple i:this.subJobsList) {
		hcs3level= new ArrayList<Couple>();
		ArrayList<Jobs> option1= combination1(); //|couple1|---------<couple1>------|couple2|---------<couple2>
		ArrayList<Jobs> option2= new ArrayList<Jobs>(); //|couple1|------|couple2|---------<couple2>---------<couple1>
		ArrayList<Jobs> option3= new ArrayList<Jobs>(); //|couple1|------|couple2|---------<couple1>---------<couple2>
		ArrayList<Jobs> option4= new ArrayList<Jobs>(); //|couple2|---------<couple2>------|couple1|---------<couple1>
		ArrayList<Jobs> option5= new ArrayList<Jobs>(); //|couple2|------|couple1|---------<couple1>---------<couple2>
		ArrayList<Jobs> option6= new ArrayList<Jobs>(); //|couple2|------|couple1|---------<couple2>---------<couple1>
		}
		return 	hcs3level;
	}

	private ArrayList<Jobs> combination1() {
		ArrayList<Jobs> option1= new ArrayList<Jobs>(); 
		return option1;
	}


}
