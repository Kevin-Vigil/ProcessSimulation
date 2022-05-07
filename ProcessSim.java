import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;

public class ProcessSim {
	
	static int time = 0; //will be used to sync all classes
	
	static enum States{ //the events that the scheduler will push to the process
		ARRIVE,
		BLOCK,
		EXIT,
		UNBLOCK,
		TIMEOUT
	}
	
	static enum Algs{
		FCFS,
		FEEDBACK,
		SRT,
		VRR,
		HRRN
	}
	
	static List<Process> processes;
	
	public static void main(String args[]) throws FileNotFoundException {
		
		processes = new ArrayList<Process>();
		
		
		if(args.length != 2) {//if there are not 2 arguements in the command line, then this program ends
			System.out.println("arguments not met.");
		}
		else {//if there are 2 arguements, the program will run
			
			Queue<Events> initQ = new PriorityQueue<Events>(); // does initial processing
			int idInd = 0;//sets ids for all processes
			
			File f = new File(args[0]);//opens the file for process to read
			if(f.exists()) {
				Scanner kb = new Scanner(f);//take file and create scanner to parse
				while(kb.hasNextLine()) {//checks if 
					int arrive = kb.nextInt();//grabs arrival time
					String line = kb.nextLine();//gets process activities
					line = line.substring(1,line.length());//eliminates space before start of activty line
					Process p = new Process(idInd, line);//creates the process with all activities
					//System.out.println(p);//test
					
					processes.add(p);
					
					initQ.add(new Events(arrive, States.ARRIVE, idInd++));//creates initial arrival
					
					
				}
				kb.close();
			}
			
			List<String> info = new ArrayList<String>();
			//read the next file for the scheduling algorithm
			f = new File(args[1]);
			if(f.exists()) {
				Scanner kb = new Scanner(f);//take file and create scanner to parse
				while(kb.hasNextLine()) {//checks if there is a next line
					String line = kb.nextLine();
					
					info.add(line);
					
				}
				kb.close();
			}
			
			Scheduler sch = new Scheduler(initQ, info);//sets up the scheduler
			sch.executeP();//runs the scheduler
			
			double meanTT = 0;
			double meanNTT = 0;
			double full_avgRT = 0;
			if(processes.get(0).getFinTime() == -1) {
				System.out.println("Algorithm not supported yet.");
			}
			else {
				for(int i = 0; i < processes.size(); i++) {//printing process
					Process p = processes.get(i);
					meanTT += p.getTT();
					meanNTT += p.getNTT();
					full_avgRT += p.getAvgRT();
					System.out.printf("Process %d: \n", p.getID());
					System.out.printf("%28s: %d%n", "Arrival Time", p.getArriveTime());
					System.out.printf("%28s: %d%n", "Start Time", p.getStartTime());
					System.out.printf("%28s: %d%n", "Finish Time", p.getFinTime());
					System.out.printf("%28s: %d%n", "Service Time", p.getSerTime());
					System.out.printf("%28s: %d%n", "Turnaround Time", (int)p.getTT());
					System.out.printf("%28s: %.2f%n", "Normalized Turnaround Time", p.getNTT());
					System.out.printf("%28s: %.2f%n", "Average Response Time", p.getAvgRT());
					System.out.println("---------------------------------------------");
				}
				meanTT /= processes.size();
				meanNTT /= processes.size();
				full_avgRT /= processes.size();//ending print
				System.out.printf("%32s: %.1f%n", "Mean Turnaround Time", meanTT);
				System.out.printf("%32s: %.2f%n", "Mean Normalized Turnaround Time", meanNTT);
				System.out.printf("%32s: %.2f%n", "Mean Average Response Time", full_avgRT);
			}
			
			
		}
		
		
	}
	
	public static class Scheduler{
		
		List<Events> history;//records the history of events for debugging or viewing
		Queue<Events> eventQ;//maintains all events to be run, once queue is empty, scheduler is complete
		int cpu;//will give the id of the process that currently has the CPU
		Algs alg;//what algorithm is being used
		
		int priorities;//used for FEEDBACK, is always set in case
		int quantum;//used for all algorithms, but set differently depending on alg
		boolean serviceGiven;//used for SRT and HRRN, is always set
		double alpha;//used for SRT and HRRN, is always set
		
		int cpu_prevPri; //for Feedback
		
		public Scheduler(Queue<Events> initQueue, List<String> i) {
			this.cpu_prevPri = 0;//sets the feedback tracker
			String algS = i.remove(0);//pops the head item to determine the algorithm
			cpu = -1;
			if(algS.equals("FCFS")) {//depending on which algorithm it is, configuration will be done accordingly
				this.alg = Algs.FCFS;
				this.eventQ = initQueue;
				
				//this is just setting variables so that everything is assigned, not used if FCFS
				this.serviceGiven = true;
				this.priorities = 1;
				this.alpha = 1;
				this.quantum = 99999999;
			}
			else if(algS.equals("FEEDBACK")) {
				this.alg = Algs.FEEDBACK;
				this.eventQ = initQueue;
				
				//parses info arrayList
				while(!i.isEmpty()) {
					String[] l = i.remove(0).split("=");
					if(l[0].equals("num_priorities")) {
						this.priorities = Integer.parseInt(l[1]);
					}
					else {
						this.quantum = Integer.parseInt(l[1]);
					}
				}
				
				//not used, just setting
				this.serviceGiven = true;
				this.alpha = 1;
			}
			else if(algS.equals("SRT")) {
				this.alg = Algs.SRT;
				this.eventQ = initQueue;
				this.priorities = 1;
				
				//parses info arrayList
				while(!i.isEmpty()) {
					String[] l = i.remove(0).split("=");
					if(l[0].equals("service_given")) {
						this.serviceGiven = l[1].equals("true") ? true : false;
					}
					else {
						this.alpha = Double.parseDouble(l[1]);
					}
				}
				
				//unused
				this.quantum = 99999999;
			}
			else if(algS.equals("HRRN")) {
				this.alg = Algs.HRRN;
				this.eventQ = initQueue;
				this.priorities = 1;

				//parses info arrayList
				while(!i.isEmpty()) {
					String[] l = i.remove(0).split("=");
					if(l[0].equals("service_given")) {
						this.serviceGiven = l[1].equals("true") ? true : false;
					}
					else {
						this.alpha = Double.parseDouble(l[1]);
					}
				}
				
				//unused
				this.quantum = 99999999;
			}
			else {
				this.alg = Algs.VRR;
				this.eventQ = initQueue;
				this.priorities = 2;
				
				String[] l = i.remove(0).split("=");
				this.quantum = Integer.parseInt(l[1]);
				
				//unused
				this.serviceGiven = true;
				this.alpha = 1;
			}
			history = new ArrayList<Events>();
		}
		
		public void executeP() {//sets time to 0 and determines which scheduler to go through
			time = 0;
			switch(this.alg) {
				case FCFS: 
					fcfs();
					break;
					
				case VRR:
					vrr();
					break;
					
				case HRRN:
					for(Process p: processes) {
						p.setServiceG(this.serviceGiven);
						p.setAlpha(this.alpha);
					}
					hrrn();
					break;
				
				case FEEDBACK:
					feedback();
					break;
					
				case SRT:
					for(Process p: processes) {
						p.setServiceG(this.serviceGiven);
						p.setAlpha(this.alpha);
					}
					//System.out.println("Algorithm not yet supported.");
					break;
			}
		}
		
		public boolean arr(Algs a, int iden) {//general process arrival, just used to remove lines of code
			if(a != Algs.SRT) {//if the algorithm is not SRT, then run this code
				Process curr = processes.get(iden);//var set
				boolean end = curr.getLastAct();//if on the last activity, set true, otherwise, set false
				boolean finishable = (curr.getDur() <= this.quantum) ? true : false; //if burst time is shorter than the quantum, then set true, otherwise, false
				end = curr.getLastAct();
				if(cpu == -1) {
					cpu = iden;//used for setting the process to the CPU
					cpu_prevPri = 0;//sets the priority of the process for FEEDBACK Algorithm
					curr.arrive();//sets the process as arrived
					curr.dispatch();//sets the process on the cpu
					if(end) {//if on last activity, changes whether exit or block
						if(finishable) {//uses the quantum to determine if process can run before timeout
							eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
						}
						else {//if quantum is shorter, run for quantum time and timeout
							eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
						}
						return true;
					}
					else if(finishable){//if burst time is shorter, run for burst time and block
						eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
						return true;
					}
					else {//if end is false and finishable is false, run for quantum time and timeout
						eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
						return true;
					}
				}
			}
			return false;//does not perform the general arrival
			
		}
		
		//DUE TO A LOT OF REDUNDANT CODE, ALL COMMENTS EXPLAINING CERTAIN THINGS WILL BE IN FCFS AND THE REST OF THE ALGORITHMS
		//WILL HAVE SPARCE COMMENTING, ONLY COMMENTING ON SLIGHT CHANGES
		
		public void fcfs() {//runs the fcfs algorithm, due to lots of code, comments will be sparce after this function
			
			Queue<Integer> q = new LinkedList<Integer>(); // creates a queue with 1 level of priority, this changes depending on the algorithm
			while(!(eventQ.isEmpty())) {//will check if the events queue is empty and run until empty
				Events next = eventQ.poll();//grabs next event
				//System.out.println(next);//used for testing
				Process curr = processes.get(next.getID());//var setting for readability
				if(next.getT() >= time) {//if the time is behind the event time, time will jump
					time = next.getT();
				}
				boolean end;
				
				switch(next.getState()) {//gets state of event and executes state on process
					case ARRIVE:
						if(!arr(this.alg, next.getID())){//runs general arrival method, if false, process is added to ready queue
							curr.arrive();
							q.add(next.getID());
						}
						
						history.add(next);//adds event to history
						break;
						
					case BLOCK: //if process reaches end of burst, blocks and either exits, or runs IO
						curr.block();//blocks the process
						end = curr.getLastAct();
						if(end) {//if on last activity, exit or if false, unblock event generation
							eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
						}
						else{
							eventQ.add(new Events(time + curr.getDur(), States.UNBLOCK, cpu));
						}
						
						if(!q.isEmpty()) {//finds new process to put on cpu(if ready queue is not empty)
							cpu = q.poll();//takes process and puts on cpu
							curr = processes.get(cpu);//var set
							curr.dispatch();//starts running process on cpu
							end = curr.getLastAct();//if process is on last activity
							if(end) {//if true, generate exit event
								eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
							}
							else {//if false, generate block event
								eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
							}
						}
						else {//if queue is empty, set cpu to -1
							cpu = -1;
						}
						
						
						history.add(next);
						break;
						
					case EXIT://if process finishes all activities
						curr.exit();//gives process exit and generates all stats of that process
						
						if(!q.isEmpty()) {//same as before, finds new process for cpu
							cpu = q.poll();
							curr = processes.get(cpu);
							curr.dispatch();
							end = curr.getLastAct();
							if(end) {
								eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
							}
							else {
								eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
							}
						}
						else {
							cpu = -1;
						}
						
						
						history.add(next);
						break;
						
					case UNBLOCK://once IO has been completed, process will unblock
						curr.unblock();//unblocks the process
						q.add(next.getID());//puts process on the ready queue
						if(cpu == -1) {//if the cpu has nothing, then find next process
							cpu = q.poll();
							curr = processes.get(cpu);
							curr.dispatch();
							end = curr.getLastAct();
							if(end) {
								eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
							}
							else {
								eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
							}
						}
						history.add(next);
						break;
						
					case TIMEOUT:
						curr.timeout();//times the process out
						q.add(next.getID());//adds to ready queue
						if(!q.isEmpty()) {//finds next process
							cpu = q.poll();
							curr = processes.get(cpu);
							curr.dispatch();
							end = curr.getLastAct();
							if(end) {
								eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
							}
							else {
								eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
							}
						}
						history.add(next);
						break;
						
					default:
						break;
						
				}
			}
			
		}
		
		@SuppressWarnings("unchecked")
		public void vrr() {
			
			
			Queue<Integer>[] q = new Queue[2];
			q[0] = new LinkedList<Integer>();
			q[1] = new LinkedList<Integer>();
			/*while(!complete) {
				
			}*/
			while(!(eventQ.isEmpty())) {
				Events next = eventQ.poll();
				//System.out.print(next);
				if(next.getT() >= time) {
					time = next.getT();
				}
				Process curr = processes.get(next.getID());
				boolean end = curr.getLastAct();
				boolean finishable = (curr.getDur() <= this.quantum) ? true : false;
				
				switch(next.getState()) {
					case ARRIVE:
						if(!arr(this.alg, next.getID())){
							curr.arrive();
							q[1].add(next.getID());
						}
						
						history.add(next);
						break;
						
					case BLOCK:
						curr.block();
						end = curr.getLastAct();
						if(end) {
							eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
						}
						else{
							eventQ.add(new Events(time + curr.getDur(), States.UNBLOCK, cpu));
						}
						//System.out.println("\nProcess " + curr.getID()+": "+curr.getDur() + "\n"
								//+ "Queue 0: " + q[0] + "\nQueue 1: " + q[1] +"\n");
						
						cpu = -1;
						for(int i = 0; i < 2; i++) {
							if(!q[i].isEmpty() && cpu == -1) {
								cpu = q[i].poll();
								curr = processes.get(cpu);
								curr.dispatch();
								end = curr.getLastAct();
								finishable = (curr.getDur() <= this.quantum) ? true : false;
								if(end) {
									if(finishable) {
										eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
									}
									else {
										eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
									}
									
								}
								else if(finishable){
									eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
								}
								else {
									eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
								}
							}
						}
						
						history.add(next);
						break;
						
					case EXIT:
						curr.exit();
						cpu = -1;
						for(int i = 0; i < this.priorities; i++) {
							if(!q[i].isEmpty() && cpu == -1) {
								cpu = q[i].poll();
								curr = processes.get(cpu);
								curr.dispatch();
								end = curr.getLastAct();
								finishable = (curr.getDur() <= this.quantum) ? true : false;
								if(end) {
									if(finishable) {
										eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
									}
									else {
										eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
									}
									
								}
								else if(finishable){
									eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
								}
								else {
									eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
								}
							}
						}
						
						history.add(next);
						break;
						
					case UNBLOCK:
						curr.unblock();
						q[0].add(next.getID());
						//System.out.println("\nProcess " + curr.getID()+": "+curr.getDur() + "\n"
								//+ "Queue 0: " + q[0] + "\nQueue 1: " + q[1] +"\n");
						if(cpu == -1) {
							for(int i = 0; i < 2; i++) {
								if(!q[i].isEmpty() && cpu == -1) {
									cpu = q[i].poll();
									curr = processes.get(cpu);
									curr.dispatch();
									end = curr.getLastAct();
									finishable = (curr.getDur() <= this.quantum) ? true : false;
									if(end) {
										if(finishable) {
											eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
										}
										else {
											eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
										}
										
									}
									else if(finishable){
										eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
									}
									else {
										eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
									}
								}
							}
						}
						history.add(next);
						break;
						
					case TIMEOUT:
						curr.timeout();
						//System.out.println("\nProcess " + curr.getID()+": "+curr.getDur() + "\n"
								//+ "Queue 0: " + q[0] + "\nQueue 1: " + q[1] +"\n");
						q[1].add(next.getID());
						cpu = -1;
						for(int i = 0; i < 2; i++) {
							if(!q[i].isEmpty() && cpu == -1) {
								cpu = q[i].poll();
								//System.out.println("polling " + cpu);
								curr = processes.get(cpu);
								curr.dispatch();
								end = curr.getLastAct();
								finishable = (curr.getDur() <= this.quantum) ? true : false;
								if(end) {
									if(finishable) {
										eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
									}
									else {
										eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
									}
									
								}
								else if(finishable){
									eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
								}
								else {
									eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
								}
							}
						}
						history.add(next);
						break;
						
					default:
						break;
						
				}
			}
			
		}
		
		public void hrrn() {//TODO
			
			boolean[] ready = new boolean[processes.size()]; //finds which processes are in the "ready queue" all set to false
			double[] rr = new double[processes.size()]; //used to determine highest response ratio and run that on CPU
			for(int i = 0; i < ready.length; i++) {
				ready[i] = false;
				rr[i] = -1.0;
			}
			
			while(!(eventQ.isEmpty())) {//honestly i dont know how i did it but its perfect lol
				Events next = eventQ.poll();
				//System.out.print(next);
				if(next.getT() >= time) {
					time = next.getT();
				}
				Process curr = processes.get(next.getID());
				boolean end = curr.getLastAct();
				boolean finishable = (curr.getDur() <= this.quantum) ? true : false;
				
				for(int i = 0; i < processes.size(); i++) {
					if(ready[i]) {
						processes.get(i).update();
						rr[i] = processes.get(i).generateRR(rr[i]);
					}
					
					
				}
				
				switch(next.getState()) {
					case ARRIVE:
						if(!arr(this.alg, next.getID())){
							curr.arrive();
							ready[next.getID()] = true;
							rr[next.getID()] = curr.generateRR(curr.getDur());
						}
						
						history.add(next);
						break;
						
					case BLOCK:
						curr.block();
						end = curr.getLastAct();
						if(end) {
							eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
						}
						else{
							eventQ.add(new Events(time + curr.getDur(), States.UNBLOCK, cpu));
						}
						//System.out.println("\nProcess " + curr.getID()+": "+curr.getDur() + "\n"
								//+ "Queue 0: " + q[0] + "\nQueue 1: " + q[1] +"\n");
						
						cpu = -1;
						if(cpu == -1) {
							double highest = -100.0;
							for(int i = 0; i < rr.length; i++) {
								if(ready[i] && rr[i] > highest) {
									highest = rr[i];
									cpu = i;
								}
							}
							if(cpu != -1) {
								curr = processes.get(cpu);
								curr.dispatch();
								ready[cpu] = false;
								end = curr.getLastAct();
								finishable = (curr.getDur() <= this.quantum) ? true : false;
								if(end) {
									if(finishable) {
										eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
									}
									else {
										eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
									}
									
								}
								else if(finishable){
									eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
								}
								else {
									eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
								}
							}
							
						}
						
						
						history.add(next);
						break;
						
					case EXIT:
						curr.exit();
						cpu = -1;
						
						if(cpu == -1) {
							double highest = -100.0;
							for(int i = 0; i < rr.length; i++) {
								if(ready[i] && rr[i] > highest) {
									highest = rr[i];
									cpu = i;
								}
							}
							if(cpu != -1) {
								curr = processes.get(cpu);
								curr.dispatch();
								ready[cpu] = false;
								end = curr.getLastAct();
								finishable = (curr.getDur() <= this.quantum) ? true : false;
								if(end) {
									if(finishable) {
										eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
									}
									else {
										eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
									}
									
								}
								else if(finishable){
									eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
								}
								else {
									eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
								}
							}
							
						}
						
						
						history.add(next);
						break;
						
					case UNBLOCK:
						curr.unblock();
						ready[next.getID()] = true;
						rr[next.getID()] = curr.generateRR(rr[next.getID()]);
						//System.out.println("\nProcess " + curr.getID()+": "+curr.getDur() + "\n"
								//+ "Queue 0: " + q[0] + "\nQueue 1: " + q[1] +"\n");
						if(cpu == -1) {
							double highest = -100.0;
							for(int i = 0; i < rr.length; i++) {
								if(ready[i] && rr[i] > highest) {
									highest = rr[i];
									cpu = i;
								}
							}
							if(cpu != -1) {
								curr = processes.get(cpu);
								curr.dispatch();
								ready[cpu] = false;
								end = curr.getLastAct();
								finishable = (curr.getDur() <= this.quantum) ? true : false;
								if(end) {
									if(finishable) {
										eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
									}
									else {
										eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
									}
									
								}
								else if(finishable){
									eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
								}
								else {
									eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
								}
							}
							
						}
						history.add(next);
						break;
						
					case TIMEOUT:
						//not used in hrrn
						break;
						
					default:
						break;
						
				}
			}
			
		}
		
		@SuppressWarnings("unchecked")
		public void feedback() {
			cpu_prevPri = 0;
			Queue<Integer>[] q = new Queue[this.priorities];
			for(int i = 0; i < q.length; i++) {
				q[i] = new LinkedList<Integer>();
			}
			
			/*while(!complete) {
				
			}*/
			while(!(eventQ.isEmpty())) {
				Events next = eventQ.poll();
				//System.out.print(next);
				Process curr = processes.get(next.getID());
				if(next.getT() >= time) {
					time = next.getT();
				}
				boolean end;
				boolean finishable = (curr.getDur() <= this.quantum) ? true : false;
				
				switch(next.getState()) {
					case ARRIVE:
						if(!arr(this.alg, next.getID())){
							curr.arrive();
							q[0].add(next.getID());
						}
						
						history.add(next);
						break;
						
					case BLOCK:
						curr.block();
						end = curr.getLastAct();
						if(end) {
							eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
						}
						else{
							eventQ.add(new Events(time + curr.getDur(), States.UNBLOCK, cpu));
						}
						//System.out.println("\nProcess " + curr.getID()+": "+curr.getDur() + "\n"
								//+ "Queue 0: " + q[0] + "\nQueue 1: " + q[1] +"\nQueue 2: " + q[2] + "\n");
						
						cpu = -1;
						for(int i = 0; i < this.priorities; i++) {
							if(!q[i].isEmpty() && cpu == -1) {
								cpu = q[i].poll();
								//System.out.println("polling " + cpu);
								curr = processes.get(cpu);
								curr.dispatch();
								cpu_prevPri = i;
								end = curr.getLastAct();
								finishable = (curr.getDur() <= this.quantum) ? true : false;
								if(end) {
									if(finishable) {
										eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
									}
									else {
										eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
									}
									
								}
								else if(finishable){
									eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
								}
								else {
									eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
								}
							}
						}
						
						history.add(next);
						break;
						
					case EXIT:
						curr.exit();
						cpu = -1;
						for(int i = 0; i < this.priorities; i++) {
							if(!q[i].isEmpty() && cpu == -1) {
								cpu = q[i].poll();
								//System.out.println("polling " + cpu);
								curr = processes.get(cpu);
								curr.dispatch();
								cpu_prevPri = i;
								end = curr.getLastAct();
								finishable = (curr.getDur() <= this.quantum) ? true : false;
								if(end) {
									if(finishable) {
										eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
									}
									else {
										eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
									}
									
								}
								else if(finishable){
									eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
								}
								else {
									eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
								}
							}
						}
						
						history.add(next);
						break;
						
					case UNBLOCK:
						curr.unblock();
						q[0].add(next.getID());
						//System.out.println("\nProcess " + curr.getID()+": "+curr.getDur() + "\n"
								//+ "Queue 0: " + q[0] + "\nQueue 1: " + q[1] +"\nQueue 2: " + q[2] + "\n");
						if(cpu == -1) {
							for(int i = 0; i < this.priorities; i++) {
								if(!q[i].isEmpty() && cpu == -1) {
									cpu = q[i].poll();
									//System.out.println("polling " + cpu);
									curr = processes.get(cpu);
									curr.dispatch();
									cpu_prevPri = i;
									end = curr.getLastAct();
									finishable = (curr.getDur() <= this.quantum) ? true : false;
									if(end) {
										if(finishable) {
											eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
										}
										else {
											eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
										}
										
									}
									else if(finishable){
										eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
									}
									else {
										eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
									}
								}
							}
						}
						history.add(next);
						break;
						
					case TIMEOUT:
						curr.timeout();
						if(cpu_prevPri < q.length-1) {
							q[cpu_prevPri+1].add(next.getID());
						}
						else {
							q[cpu_prevPri].add(next.getID());
						}
						//System.out.println("\nProcess " + curr.getID()+": "+curr.getDur() + "\n"
								//+ "Queue 0: " + q[0] + "\nQueue 1: " + q[1] +"\nQueue 2: " + q[2] + "\n");
						cpu = -1;
						for(int i = 0; i < this.priorities; i++) {
							if(!q[i].isEmpty() && cpu == -1) {
								cpu = q[i].poll();
								//System.out.println("polling " + cpu);
								curr = processes.get(cpu);
								curr.dispatch();
								cpu_prevPri = i;
								end = curr.getLastAct();
								finishable = (curr.getDur() <= this.quantum) ? true : false;
								if(end) {
									if(finishable) {
										eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
									}
									else {
										eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
									}
									
								}
								else if(finishable){
									eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
								}
								else {
									eventQ.add(new Events(time + this.quantum, States.TIMEOUT, cpu));
								}
							}
						}
						history.add(next);
						break;
						
					default:
						break;
						
				}
			}
			
		}
		
		public void srt() {//TODO
			
			boolean[] ready = new boolean[processes.size()]; //finds which processes are in the "ready queue" all set to false
			double[] srt = new double[processes.size()]; //used to determine highest response ratio and run that on CPU
			for(int i = 0; i < ready.length; i++) {
				ready[i] = false;
				srt[i] = -1.0;
			}
			
			while(!(eventQ.isEmpty())) {//honestly i dont know how i did it but its perfect lol
				Events next = eventQ.poll();
				System.out.print(next);
				if(next.getT() >= time) {
					time = next.getT();
				}
				Process curr = processes.get(next.getID());
				boolean end = curr.getLastAct();
				boolean finishable = (curr.getDur() <= this.quantum) ? true : false;
				
				for(int i = 0; i < processes.size(); i++) {
					if(ready[i]) {
						processes.get(i).update();
						srt[i] = processes.get(i).generateSRT(srt[i]);
					}
					
					
				}
				
				switch(next.getState()) {
					case ARRIVE:
						ready[next.getID()] = true;
						srt[next.getID()] = curr.generateSRT(curr.getDur());
						curr.arrive();
						if(cpu == -1) {
							cpu = next.getID();
							curr = processes.get(cpu);
							curr.dispatch();
							ready[cpu] = false;
							end = curr.getLastAct();
							Events pee = next;
							if(!eventQ.isEmpty()) {
								pee = eventQ.peek();
							}
							finishable = true;
							if(pee.getState() == States.ARRIVE || pee.getState() == States.UNBLOCK)
								finishable = (curr.getDur() <= pee.getT()) ? true : false;
							if(end) {
								if(finishable) {
									eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
								}
								else {
									eventQ.add(new Events(time + pee.getT(), States.TIMEOUT, cpu));
								}
								
							}
							else if(finishable){
								eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
							}
							else {
								eventQ.add(new Events(time + pee.getT(), States.TIMEOUT, cpu));
							}
							
							
							
						}
						else {
							double lowest = 10000.0;
							int nextP = 0;
							for(int i = 0; i < srt.length; i++) {
								if(ready[i] && srt[i] < lowest) {
									lowest = srt[i];
									nextP = 0;
								}
							}
							if(cpu != -1) {
								curr = processes.get(cpu);
								curr.dispatch();
								ready[cpu] = false;
								end = curr.getLastAct();
								Events pee = next;
								if(!eventQ.isEmpty()) {
									pee = eventQ.peek();
								}
								finishable = true;
								if(pee.getState() == States.ARRIVE || pee.getState() == States.UNBLOCK)
									finishable = (curr.getDur() <= pee.getT()) ? true : false;
								if(end) {
									if(finishable) {
										eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
									}
									else {
										eventQ.add(new Events(time + pee.getT(), States.TIMEOUT, cpu));
									}
									
								}
								else if(finishable){
									eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
								}
								else {
									eventQ.add(new Events(time + pee.getT(), States.TIMEOUT, cpu));
								}
							}
						}
						history.add(next);
						break;
						
					case BLOCK:
						curr.block();
						end = curr.getLastAct();
						if(end) {
							eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
						}
						else{
							eventQ.add(new Events(time + curr.getDur(), States.UNBLOCK, cpu));
						}
						//System.out.println("\nProcess " + curr.getID()+": "+curr.getDur() + "\n"
								//+ "Queue 0: " + q[0] + "\nQueue 1: " + q[1] +"\n");
						ready[cpu] = false;
						cpu = -1;
						if(cpu == -1) {
							double lowest = 10000.0;
							for(int i = 0; i < srt.length; i++) {
								if(ready[i] && srt[i] < lowest) {
									lowest = srt[i];
									cpu = i;
								}
							}
							if(cpu != -1) {
								curr = processes.get(cpu);
								curr.dispatch();
								ready[cpu] = false;
								end = curr.getLastAct();
								Events pee = next;
								if(!eventQ.isEmpty()) {
									pee = eventQ.peek();
								}
								finishable = true;
								if(pee.getState() == States.ARRIVE || pee.getState() == States.UNBLOCK)
									finishable = (curr.getDur() <= pee.getT()) ? true : false;
								if(end) {
									if(finishable) {
										eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
									}
									else {
										eventQ.add(new Events(time + pee.getT(), States.TIMEOUT, cpu));
									}
									
								}
								else if(finishable){
									eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
								}
								else {
									eventQ.add(new Events(time + pee.getT(), States.TIMEOUT, cpu));
								}
							}
							
						}
						
						
						history.add(next);
						break;
						
					case EXIT:
						curr.exit();
						
						cpu = -1;
						if(cpu == -1) {
							double lowest = 10000.0;
							for(int i = 0; i < srt.length; i++) {
								if(ready[i] && srt[i] < lowest) {
									lowest = srt[i];
									cpu = i;
								}
							}
							if(cpu != -1) {
								curr = processes.get(cpu);
								curr.dispatch();
								ready[cpu] = false;
								end = curr.getLastAct();
								Events pee = next;
								if(!eventQ.isEmpty()) {
									pee = eventQ.peek();
								}
								finishable = true;
								if(pee.getState() == States.ARRIVE || pee.getState() == States.UNBLOCK)
									finishable = (curr.getDur() <= pee.getT()) ? true : false;
								if(end) {
									if(finishable) {
										eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
									}
									else {
										eventQ.add(new Events(time + pee.getT(), States.TIMEOUT, cpu));
									}
									
								}
								else if(finishable){
									eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
								}
								else {
									eventQ.add(new Events(time + pee.getT(), States.TIMEOUT, cpu));
								}
							}
							
						}
						
						
						history.add(next);
						break;
						
					case UNBLOCK:
						curr.unblock();
						ready[next.getID()] = true;
						srt[next.getID()] = curr.generateSRT(srt[next.getID()]);
						//System.out.println("\nProcess " + curr.getID()+": "+curr.getDur() + "\n"
								//+ "Queue 0: " + q[0] + "\nQueue 1: " + q[1] +"\n");
						if(cpu == -1) {
							double lowest = 10000.0;
							for(int i = 0; i < srt.length; i++) {
								if(ready[i] && srt[i] < lowest) {
									lowest = srt[i];
									cpu = i;
								}
							}
							if(cpu != -1) {
								curr = processes.get(cpu);
								curr.dispatch();
								ready[cpu] = false;
								end = curr.getLastAct();
								Events pee = next;
								if(!eventQ.isEmpty()) {
									pee = eventQ.peek();
								}
								finishable = true;
								if(pee.getState() == States.ARRIVE || pee.getState() == States.UNBLOCK)
									finishable = (curr.getDur() <= pee.getT()) ? true : false;
								if(end) {
									if(finishable) {
										eventQ.add(new Events(time + curr.getDur(), States.EXIT, cpu));
									}
									else {
										eventQ.add(new Events(time + pee.getT(), States.TIMEOUT, cpu));
									}
									
								}
								else if(finishable){
									eventQ.add(new Events(time + curr.getDur(), States.BLOCK, cpu));
								}
								else {
									eventQ.add(new Events(time + pee.getT(), States.TIMEOUT, cpu));
								}
							}
							
						}
						history.add(next);
						break;
						
					case TIMEOUT:
						//not used in hrrn
						break;
						
					default:
						break;
						
				}
			}
			
		}
	}
	
	public static class Events implements Comparable<Events>{
		
		int startTime;
		States event;
		int processID;

		
		public Events(int s, States e, int d) {
			this.startTime = s;
			this.event = e;
			this.processID = d;
		}
		
		//get functions
		public States getState() {
			return this.event;
		}
		
		public int getID() {
			return this.processID;
		}
		
		public int getT() {
			return this.startTime;
		}
		
		//comparison for priority queue
		@Override
		public int compareTo(ProcessSim.Events o) {
			if(this.startTime < o.startTime) return -1;
			else if(this.startTime > o.startTime) return 1;
			else if(this.event == States.UNBLOCK && o.event == States.TIMEOUT) return -1;
			else if(this.event == States.TIMEOUT && o.event == States.UNBLOCK) return 1;
			else if(this.event == States.EXIT) return -1;
			else return 0;
		}
		
		//for printing, mostly testing
		public String toString() {
			String toRet = "Start time: " + this.startTime + "\tEvent: " + this.event + "\t Process: " + this.processID + "\n";
			return toRet;
		}
	}
	
	public static class Process{
		//completed in the constructor
		int id;//sets process id
		String[] type;//will either be "CPU" or "IO"
		int[] durTime;//will be duration of corresponding process type
		int pI;//index for which activity the process is on
		
		boolean serviceGiven;//gets set for srt and hrrn
		double alpha;//gets set for srt and hrrn
		double pred_s;
		
		//for stat keeping
		int cpuStartTime;//used to find difference of time
		int readyQT;//used to find difference of time
		int readyQCounter;//counts amount of times sent to ready queue
		int resTime;//counts the amount of time in ready queue
		boolean onCPU;
		boolean inReady;
		
		//stats
		int arrivalTime;//the time the process arrives
		int startTime;//the time the process started its first activity
		int finishTime;//the time the process ends
		int serTime;//counts the amount of time in the CPU
		int turnaroundTime;//finish time - start time
		double normTT;//turnaround time/ service time
		double avgRT;//restime/readyQCounter
		
		public Process(int t, String line) {
			this.resTime = 0;
			this.serTime = 0;
			this.id = t;
			String[] pro = line.split(" ");
			this.type = new String[pro.length/2];
			this.durTime = new int[pro.length/2];
			int pIndex = 0;
			for(int i = 0; i < pro.length-1; i += 2) {
				this.type[pIndex] = pro[i];
				this.durTime[pIndex] = Integer.parseInt(pro[i+1]);
				pIndex++;
			}
			this.pI = 0;
			this.readyQCounter = 0;
			this.startTime = -1;
			this.serviceGiven = true;
			this.pred_s = -1;
			this.finishTime = -1;
			 
		}
		
		//get methods
		public int getID() {
			return this.id;
		}
		public int getDur() {
			return durTime[pI];
		}
		public boolean getLastAct() {
			return (this.pI == this.type.length-1) ? true : false;
		}
		public int getArriveTime() {
			return this.arrivalTime;
		}
		public int getStartTime() {
			return this.startTime;
		}
		public int getFinTime() {
			return this.finishTime;
		}
		public int getSerTime() {
			return this.serTime;
		}
		public double getTT() {
			return this.turnaroundTime;
		}
		public double getNTT() {
			return this.normTT;
		}
		public double getAvgRT() {
			return this.avgRT;
		}
		public double generateRR(double sp) {
			double s = this.pred_s;
			if(this.pred_s == -1.0) {
				this.pred_s = getDur();
				return getDur();
			}
			else if(serviceGiven) {
				this.pred_s = getDur();
				s = getDur();
			}
			else {
				s *= this.alpha;
				s += ((1-this.alpha)*sp);
			}
			
			return ((this.resTime+s)/s);
		}
		public double generateSRT(double sp) {
			double s = sp;
			if(this.pred_s == -1.0) {
				this.pred_s = getDur();
				return getDur();
			}
			else if(serviceGiven) {
				this.pred_s = getDur();
				return getDur();
			}
			else {
				s *= this.alpha;
				s += ((1-this.alpha)*this.pred_s);
			}
			return s;
			
		}
		
		public void setServiceG(boolean sG) {
			this.serviceGiven = sG;
		}
		public void setAlpha(double a) {
			this.alpha = a;
		}
		
		public void update() {
			if(this.finishTime == -1) {
				
				if(!onCPU && inReady) {
					this.resTime += time - this.readyQT;
					this.readyQT = time;
				}
			}
		}
		public void arrive() {
			this.arrivalTime = time;
			this.readyQT = time;
			this.readyQCounter++;
			this.inReady = true;
			this.onCPU = false;
		}
		
		public void dispatch() {
			this.cpuStartTime = time;
			this.resTime += time - this.readyQT;
			if(this.startTime == -1) {
				this.startTime = time;
			}
			this.inReady = false;
			this.onCPU = true;
		}
		
		public void block() {
			this.serTime += time - this.cpuStartTime;
			durTime[this.pI++] = 0;
			
			this.inReady = false;
			this.onCPU = false;
		}
		
		public void timeout() {
			this.serTime += time - this.cpuStartTime;
			this.durTime[this.pI] -= time - this.cpuStartTime;
			this.pred_s = time - this.cpuStartTime;
			this.readyQT = time;
			this.readyQCounter++;
			
			this.inReady = true;
			this.onCPU = false;
		}
		
		public void unblock() {
			this.readyQT = time;
			this.readyQCounter++;
			this.durTime[this.pI++] = 0;
			
			this.inReady = true;
			this.onCPU = false;
			
		}

		public void exit() {
			this.serTime += time - this.cpuStartTime;
			this.finishTime = time;
			this.turnaroundTime = this.finishTime - this.arrivalTime;
			this.normTT = (double)this.turnaroundTime / (double)this.serTime;
			this.avgRT = (double)this.resTime / (double)this.readyQCounter;
		}
		
		
		
		//printing mostly for testing
		public String toString() {
			String toRet = "Process " + this.id + ":\n\t\tArrival Time: " + this.arrivalTime
					+ "\n\t\tStart Time: " + this.startTime + "\n\t\tFinish Time: " + this.finishTime
					+ "\n\t\tService Time: " + this.serTime + "\n\t\tTurnaround Time: " + this.turnaroundTime
					+ "\n\t\tNormalized Turnaround Time: " + this.normTT + "\n\t\tAverage Response Time: " + this.avgRT;
			return toRet;
			
		}
	}
}
