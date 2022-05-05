import java.util.concurrent.Semaphore;
import java.io.File;
import java.util.Random;
import java.util.Scanner;

public class ProcessSim {

	public static void main(String args[]) {
		
		if(args.length != 2) {
			System.out.println("arguments not met.");
		}
		else {
			//2 arguments exist path
			//create a priority queue and 
			
			File f = new File(args[0]);
			if(f.exists()) {
				Scanner kb = new Scanner(f);//take file and create scanner to parse
				while(kb.hasNextLine()) {//checks if 
					String line = kb.nextLine();
				}
			}
		}
		
		
	}
	
	public static class Process{
		int arrival;
		int id;
		public Process(int i, String s) {
			this.id = i;
			
		}
	}
	
	/*public static class Process extends Thread{
		int arrival;
		int id;
		public Process(int i) {
			this.id = i;
		}
	}
	
	public static class Time extends Thread{
		int time;
		int id;
		public Time(int i,int time) {
			this.time = time; 
			this.id = i;
		}
	}*/
}
