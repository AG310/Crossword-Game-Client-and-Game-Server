package Server;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.print.attribute.standard.Severity;

public class ChatRoom {
	private Vector<ServerThread> serverThreads;
	private Vector<Lock> lockVec;
	private Vector<Condition> conditionVec;
	private Vector<Integer> playerScores;
	private int numPlayers=0;
	private boolean gameOver =false;
	private boolean allPlayersReady=false;
	private boolean cont=false;
	private CrosswordServer crosswordObj;
	private ArrayList<Word> acrList;
	private ArrayList<Word> dowList;
	public int currPlayer=0;

	public ChatRoom(int port) {
		ServerSocket ss= null;
		try {
			ss = new ServerSocket(port);
			System.out.println("Listening on port: "+port+" \nWaiting for players...");
			serverThreads = new Vector<ServerThread>();
			lockVec = new Vector<Lock>();
			conditionVec = new Vector<Condition>();
			playerScores = new Vector<Integer>();
			Socket s=null;
			while(true) {
				if(serverThreads.isEmpty()) { //if no players yet need to initalize some stuff based on player response
					s= ss.accept();
					System.out.println("Connection from "+ s.getInetAddress());
					Lock lock = new ReentrantLock();
					lockVec.add(lock);
					Condition cond = lock.newCondition();
					conditionVec.add(cond);
					ServerThread st = new ServerThread(s, this, lock, cond, lockVec.size()-1);
					serverThreads.add(st);
					crosswordObj= new CrosswordServer();
					acrList=crosswordObj.getUsedAcc();
					dowList=crosswordObj.getUsedDown();
					playerScores.setSize(numPlayers);
				}
				else if(serverThreads.size()<numPlayers) { //other players join
					s= ss.accept();
					System.out.println("Connection from "+ s.getInetAddress());
					Lock lock = new ReentrantLock();
					lockVec.add(lock);
					Condition cond = lock.newCondition();
					conditionVec.add(cond);
					ServerThread st = new ServerThread(s, this, lock, cond, lockVec.size()-1);
					serverThreads.add(st);
				}
				else { //game begins
					if(serverThreads.size()==numPlayers) {
						for(int i=0; i<=numPlayers; i++) {
							playerScores.add(0);
						}
						broadcast("The game is beginning.", null);
						System.out.println("Sending game board");
						allPlayersReady=true;
						bcBoard(crosswordObj.residualBoard, null);
						showHints();
						if(!gameOver) {
							lockVec.get(0).lock();
							conditionVec.get(0).signal();
							lockVec.get(0).unlock();	
						}
						while(!gameOver) {
							if(acrList.isEmpty() && dowList.isEmpty()) {
								gameOver=true;
							}
						}
						numPlayers=0;
						gameOver =false;
						allPlayersReady=false;
						serverThreads.clear();
						lockVec.clear();
						conditionVec.clear();
						playerScores.clear();
					}
				}
				
			}
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}finally {
			try {
				if(ss!=null) ss.close();
			} catch (Exception e2) {
				System.out.println(e2.getMessage());
			}
		}
	}
	
	public void setNumPlayers(int num) {
		numPlayers=num;
	}
	public int getNumPlayers() {
		return numPlayers;
	}
	public int getPlayersIn() {
		return serverThreads.size();
	}
	public boolean getPlayersReady() {
		return allPlayersReady;
	}
	public void terminate() { //terminate at end
			for(ServerThread st : serverThreads) {
					st.sendMessage("TERMINATE");
			}
	}
	public void broadcast(String message, ServerThread currST) {
		if(message!=null) {
			System.out.println(message);
			for(ServerThread st : serverThreads) {
				if(st!=currST) {
					st.sendMessage(message);
				}
			}
		}
	}
	public void showHints() { // show cross hints
		for(ServerThread st : serverThreads) {
			crosswordObj.printHints(st);
		}
	}
	public void bcBoard(String[][] board, ServerThread currST) { //broadcast board
		if(board!=null) {
			String line = "";
			for(ServerThread st : serverThreads) {
				if(st!=currST) {
					for(int i=crosswordObj.getPositions().get(0); i<=crosswordObj.getPositions().get(1); i++) {
						for(int j = crosswordObj.getPositions().get(2); j<=crosswordObj.getPositions().get(3); j++) {
							line+=crosswordObj.residualBoard[i][j];
						}
						st.sendMessage(line);
						line="";
					}
				}
			}
		}
	}
	
	public void signal(ServerThread currST, String guess) { //signal
		if(acrList.isEmpty() && dowList.isEmpty()) { //checks if over
			int maxIndex=0;
			int maxScore=-1;
			int maxCounter=0;
			System.out.println("The game has concluded.\r\n" + 
					"Sending scores.");
			broadcast("\nFinal Scores", null);
			for(int i=0; i<numPlayers; i++) {
				broadcast("Player "+(i+1)+" scored "+playerScores.get(i)+" points", null);
				if(playerScores.get(i)>maxScore) {
					maxScore=playerScores.get(i);
					maxIndex=i;
				}
			}
			for(int i=0; i<numPlayers; i++) {
				if(playerScores.get(i)==maxScore) {
					maxCounter++;
				}
			}
			if(maxCounter>1) {
				broadcast("There was a tie!", null);
			}
			else
				broadcast("Player "+(maxIndex+1)+" is the winner!", null);
			terminate();
		}
		else { //if not over checks player response and seeks to validate it if correct player keeps playing if not onto next player
			int nextIndex;
			int index = currST.getIndex();
			boolean correct = false;
			guess=guess.trim();
			String [] parameters= guess.split("\\|");
			String dir = parameters[1].equals("a")? "across" : "down";
			broadcast("Player "+(index+1)+" guessed "+parameters[2]+" for "+parameters[0]+" "+dir, currST);
			if(dir.equals("across")) {
				for(int i=0; i<acrList.size(); i++) {
					if(acrList.get(i).getNumber()==Integer.parseInt(parameters[0]) && acrList.get(i).getAnswer().toLowerCase().equals(parameters[2].toLowerCase())) {
						correct=true;
						playerScores.set(index, playerScores.get(index)+1);
						crosswordObj.placeResid(acrList.get(i).getAnswer(), acrList.get(i).getRowIndex(), acrList.get(i).getColIndex(), true, acrList.get(i).getNumber());
						acrList.remove(i);
					}
				}
			}
			else {
				for(int i=0; i<dowList.size(); i++) {
					if(dowList.get(i).getNumber()==Integer.parseInt(parameters[0]) && dowList.get(i).getAnswer().toLowerCase().equals(parameters[2].toLowerCase())) {
						correct=true;
						playerScores.set(index, playerScores.get(index)+1);
						crosswordObj.placeResid(dowList.get(i).getAnswer(), dowList.get(i).getRowIndex(), dowList.get(i).getColIndex(), false, dowList.get(i).getNumber());
						dowList.remove(i);
					}
				}
			}
			if(correct) {
				broadcast("That is correct", null);
			}
			else
				broadcast("That is incorrect", null);
			if(currST.getIndex()==lockVec.size()-1) {
				nextIndex = 0;
			}
			else
				nextIndex=currST.getIndex()+1;
			cont=true;
			bcBoard(crosswordObj.residualBoard, null);
			showHints();
			if(acrList.isEmpty() && dowList.isEmpty()) {
				gameOver=true;
			}
			if(!correct) {
				currPlayer=nextIndex;
				serverThreads.get(nextIndex).sendMessage("It is your turn to play!");
				lockVec.get(nextIndex).lock();
				conditionVec.get(nextIndex).signal();
				lockVec.get(nextIndex).unlock();
			}
		}
	}
	
	public static void main(String[] args) {
		ChatRoom cr = new ChatRoom(3456);
	}

	public boolean getGameOn() {
		return gameOver;
	}

	public boolean getCont() {
		return cont;
	}

	public void setCont(boolean cont) {
		this.cont = cont;
	}
	
	public ArrayList<Word> getAcrList() {
		return acrList;
	}

	public ArrayList<Word> getDowList() {
		return dowList;
	}
	
	public void setGameOver(boolean go) {
		gameOver=go;
	}
}
