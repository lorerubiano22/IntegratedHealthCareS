import java.io.File;
import java.util.List;
import java.util.Random;

public class Main {
	final static String inputFolder = "inputs";
    final static String outputFolder = "outputs";
    final static String testFolder = "tests";
    final static String fileNameTest = "tests2run.txt";
    final static String sufixFileOutput = "_outputs.txt";
    public static void main( String[] args ) {
        System.out.println("****  WELCOME TO THIS PROGRAM  ****");
        long programStart = ElapsedTime.systemTime();
        int counter = 0;
        /* 1. GET THE LIST OF TESTS TO RUN FORM "test2run.txt" */ //Parameter file not yet included, hard coded in problem!
        String testsFilePath = testFolder + File.separator + fileNameTest;
        List<Test> testsList = TestsReader.getTestsList(testsFilePath);

        /* 2. FOR EACH TEST (instanceName + testParameters) IN THE LIST... */
        for(Test currentTest: testsList)
        {   
        	
            Random rng = new Random(currentTest.getSeed()); // Random number generator
            System.out.println("\nSTARTING TEST " + (++counter) + " OF " + testsList.size());

            // 2.1 GET THE INSTANCE INPUTS (DATA ON NODES AND VEHICLES)
            // "instanceName_input_nodes.txt" contains data on nodes
            String inputSource = inputFolder + File.separator +
                    currentTest.getInstanceName() + File.separator;
            Inputs inputs = InputsReader.readInputs(inputSource);
            long t = System.nanoTime();
            String outputsFilePath = outputFolder + File.separator +
                    currentTest.getInstanceName() + "_" + currentTest.getSeed() +  sufixFileOutput;
            // 2.2. USE THE MULTI-START ALGORITHM TO SOLVE THE INSTANCE
           Algorithm algorithm = new Algorithm(currentTest, inputs, rng);
           Outputs output = new Outputs(algorithm);
           Double endTime=(System.nanoTime() - t) / Math.pow(10, 6);
           output.sendToFile(outputsFilePath,endTime);
        
           // algorithm.solve(outputsFilePath);
            System.out.println("Taked:"+endTime);
        }

        /* 3. END OF PROGRAM */
        System.out.println("\n****  END OF PROGRAM, CHECK OUTPUTS FILES  ****");
            long programEnd = ElapsedTime.systemTime();
            System.out.println("Total elapsed time = "
                + ElapsedTime.calcElapsedHMS(programStart, programEnd));
    }

}
