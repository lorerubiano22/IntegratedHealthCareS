import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Scanner;

public class TestsReader {
    public static ArrayList<Test> getTestsList(String testsFilePath) {   
        ArrayList<Test> list = new ArrayList<Test>();
        try
        {   
            FileReader reader = new FileReader(testsFilePath);
            Scanner in = new Scanner(reader);
            in.useLocale(Locale.US);
             while( in.hasNextLine() )
            {   
                String s = in.next();
                if (s.charAt(0) == '#') // this is a comment line
                    in.nextLine(); // skip comment lines
                else
                {   
                    String instanceName = s; // e.g.: A-n32-k5
                    int maxTime = in.nextInt();
                    long seed = in.nextLong();
                    double workingTime=in.nextDouble();
                    double routeLenght=in.nextDouble();
                    double walking2jobs=in.nextDouble();
                    double detour=1-in.nextDouble();
                    double cumulativeWalkingTime=in.nextDouble();
                    int cumulativeWaitingTime=in.nextInt();
                    int loadPatient= in.nextInt();
                    int loadHomeCare= in.nextInt();
                    int patientRegistrationTime=in.nextInt();
                    Test aTest = new Test(instanceName, maxTime, seed,workingTime, routeLenght,walking2jobs,  detour,  cumulativeWalkingTime, cumulativeWaitingTime,loadPatient, loadHomeCare , patientRegistrationTime);
                    list.add(aTest);
                }
            }
            in.close();
        }
        catch (IOException exception)
        {   
            System.out.println("Error processing tests file: " + exception);
        }
        return list;
    }
}
