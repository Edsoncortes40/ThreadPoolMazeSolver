package cmsc433.p3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cmsc433.p3.SkippingMazeSolver.SolutionFound;

/**
 * This file needs to hold your solver to be tested. 
 * You can alter the class to extend any class that extends MazeSolver.
 * It must have a constructor that takes in a Maze.
 * It must have a solve() method that returns the datatype List<Direction>
 *   which will either be a reference to a list of steps to take or will
 *   be null if the maze cannot be solved.
 */
public class StudentMTMazeSolver extends SkippingMazeSolver
{
	private ExecutorService ex = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
	private CompletionService<List<Direction>> completion = new ExecutorCompletionService<List<Direction>>(ex);
	private int maxThreads = Runtime.getRuntime().availableProcessors();
	
	//***********************Private class that implements Callable***********************************
	private class MiniDFSSolver implements Callable<List<Direction>>{
		private Choice startChoice;
		private LinkedList<Choice> initialPath;
		
		public MiniDFSSolver(Choice startChoice, LinkedList<Choice> initialPath){
			this.startChoice = startChoice;
			this.initialPath = initialPath;
		}

		public List<Direction> call() {
	        Choice ch;
	        LinkedList<Choice> choiceStack = new LinkedList<Choice>();
	        
	        try
	        {
	            choiceStack.push(startChoice);
	            while (!choiceStack.isEmpty())
	            {
	                ch = choiceStack.peek();
	                if (ch.isDeadend())
	                {
	                    // backtrack.
	                    choiceStack.pop();
	                    if (!choiceStack.isEmpty()) choiceStack.peek().choices.pop();
	                    continue;
	                }
	                choiceStack.push(follow(ch.at, ch.choices.peek()));
	            }
	            // No solution found.
	            return null;
	        }
	        catch (SolutionFound e)
	        {
	        	//merge initial with current choice stack
	        	initialPath.pop();
	        	choiceStack.addAll(initialPath);
	            Iterator<Choice> iter = choiceStack.iterator();
	            LinkedList<Direction> solutionPath = new LinkedList<Direction>();
	            while (iter.hasNext())
	            {
	            	ch = iter.next();
	                solutionPath.push(ch.choices.peek());
	            }
	            
	            //System.out.println("Result: " + solutionPath);
	            //if (maze.display != null) maze.display.updateDisplay();
	            return pathToFullPath(solutionPath);
	        }
		}
	}
	//***********************Private class that implements Callable Ends*******************************
	
	
    public StudentMTMazeSolver(Maze maze)
    {
        super(maze);
    }

    public List<Direction> solve()
    {	
    	LinkedList<Choice> leadingPath = new LinkedList<Choice>();
    	ArrayList<LinkedList<Choice>> initialPaths = new ArrayList<LinkedList<Choice>>();
    	int pathsFound = 0;
    	Choice ch;
    	int choicesCount = 0;
    	//=========find maxThreads number of starting paths==========
    	try 
    	{
			leadingPath.push(firstChoice(maze.getStart()));
			
			while(pathsFound < maxThreads)
	    	{
	    		ch = leadingPath.peek(); 
	    		choicesCount = ch.choices.size();
	    		
	    		if(ch.isDeadend()) {
	    			maxThreads = pathsFound;
	    		}
	    	
	    		if(pathsFound == maxThreads - 1) 
	    		{
	    			//make the current path its own unique path
    				initialPaths.add(pathsFound, leadingPath); 
    				pathsFound++;	    			
	    		}else if(pathsFound == maxThreads - 2 && choicesCount == 3)
	    		{
	    			//weird case, one thread wont be used
	    			maxThreads = maxThreads - 1;
	    			initialPaths.add(pathsFound, leadingPath); 
	    			pathsFound++;
	    		}else
	    		{
	    			if(choicesCount == 2) 
	    			{
	    				//push first choice to a initial path
	    				if(!follow(ch.at, ch.choices.peek()).isDeadend()){
	    					Choice tempCh = new Choice(ch.at, ch.from, new LinkedList<Direction>(ch.choices));
	    					LinkedList<Choice> path1 = new LinkedList<Choice>(leadingPath);
	    					path1.pop();
	    					path1.push(tempCh);
	    					path1.push(follow(tempCh.at, tempCh.choices.peek())); 
	    					initialPaths.add(pathsFound, path1);
	    					pathsFound++;
	    					//System.out.println("path1 path choices after ch pop: " + path1.peek().choices);
	    				}
	    				
	    				ch.choices.pop();
	    			
	    				
	    				//System.out.println("Leading path choices: " + leadingPath.peek().choices);
	    				//continue in while loop through the other choice
    					leadingPath.push(follow(ch.at, ch.choices.peek()));
    					
	    			}else if(choicesCount == 3) 
	    			{
	    				//push first choice to a initial path
	    				if(!follow(ch.at, ch.choices.peek()).isDeadend()){
	    					LinkedList<Choice> path1 = new LinkedList<Choice>(leadingPath);
	    					Choice tempCh = new Choice(ch.at, ch.from, new LinkedList<Direction>(ch.choices));
	    					path1.pop();
	    					path1.push(tempCh);
	    					path1.push(follow(tempCh.at, tempCh.choices.peek())); 
	    					initialPaths.add(pathsFound, path1);
	    					pathsFound++;
	    				}
	    				
	    				ch.choices.pop();
	    				
	    				//push second choice to a initial path
	    				if(!follow(ch.at, ch.choices.peek()).isDeadend()){
	    					LinkedList<Choice> path2 = new LinkedList<Choice>(leadingPath);
	    					Choice tempCh = new Choice(ch.at, ch.from, new LinkedList<Direction>(ch.choices));
	    					path2.pop();
	    					path2.push(tempCh);
	    					path2.push(follow(tempCh.at, tempCh.choices.peek()));
	    					initialPaths.add(pathsFound, path2);
	    					pathsFound++;
	    				}
	    				
	    				ch.choices.pop();
	    				
	    				//continue in while loop through third choice
    					leadingPath.push(follow(ch.at, ch.choices.peek()));
	    			}else 
	    			{
	    				System.out.println("Path : " + pathsFound);
	    				maxThreads = pathsFound;
	    				System.out.println("error, choice count == " + choicesCount);
	    			}
	    		}
	    	}
			
	    }catch (SolutionFound e) {
			Iterator<Choice> iter = leadingPath.iterator();
	        LinkedList<Direction> solutionPath = new LinkedList<Direction>();
	        while (iter.hasNext())
	        {
            	ch = iter.next();
                solutionPath.push(ch.choices.peek());
            }

            if (maze.display != null) maze.display.updateDisplay();
            return pathToFullPath(solutionPath);
		}
    	
    	
		//========initial paths found, start the threads on each initial path===========
    	
			for(int i = 0; i < maxThreads; i++) 
			{
				MiniDFSSolver path = new MiniDFSSolver(initialPaths.get(i).peek(), initialPaths.get(i));
				completion.submit(path);
			}
			
			//get result, if null do nothing, else merge with its initial list and return
			try 
			{	
				for(int i = 0; i < maxThreads; i++) 
				{
					Future<List<Direction>> futureResult = completion.take();
					List<Direction> result = futureResult.get();
					
					if(result != null) {
						ex.shutdown();
						return result;
					}
				}	
			}catch(InterruptedException e){
				Thread.currentThread().interrupt();
				System.out.println("InterruptedException Thrown!");
			}catch(ExecutionException e) {
				System.out.println("Execution Exception thrown");
			}
			
			//result was not found so return null!
				ex.shutdown();
				return null;
    }

   
}
