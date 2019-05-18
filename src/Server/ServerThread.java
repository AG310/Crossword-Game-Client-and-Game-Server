package Server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import javax.swing.text.AbstractDocument.BranchElement;

public class ServerThread extends Thread {
		private BufferedReader br = null;
		private PrintWriter pw = null;
		private ChatRoom cr = null;
		private Lock lock;
		private Condition condition;
		private int index;
		private boolean isFirst;
		public ServerThread(Socket s, ChatRoom cr, Lock lock, Condition condition, int index) {
			try {
				if(index==0) isFirst=true; //if first player 
				this.index = index;
				this.lock = lock;
				this.condition = condition;
				this.cr= cr;
				this.br = new BufferedReader(new InputStreamReader(s.getInputStream()));
				this.pw= new PrintWriter(s.getOutputStream());
				if(index!=0) {
					sendMessage("There is game waiting for you\n");
					for(int i= 0; i<index; i++) {
						sendMessage("Player "+(i+1)+" has already joined");
					}
				}
				cr.broadcast("Player "+(index+1)+" has joined from "+s.getInetAddress(), this);
				this.start();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
		public void run() {
			try {
				String line = "";
				while(true) {
					lock.lock();
					if(!cr.getPlayersReady()) {  //if all players in or not
						if (!isFirst) {
							condition.await();
						} else {
							isFirst = false;
							sendMessage("Enter the number of players: ");
							while((line=br.readLine())==null) {
							}
							cr.setNumPlayers(Integer.parseInt(line));
						}
						System.out.println("Number of players: "+cr.getNumPlayers());
						if(cr.getPlayersIn()!=cr.getNumPlayers()) {
							for(int i = 1; i<cr.getNumPlayers(); i++) {
								sendMessage("Waiting for player: "+(i+1));
							}
						}
					}
					else if(cr.getPlayersReady() || cr.getPlayersIn()==cr.getNumPlayers()) { //if players in
						if(cr.currPlayer!=index) { //only the one whos turn it is can play
							condition.await();
						}
						if(cr.getAcrList().isEmpty() && cr.getDowList().isEmpty()) { //when over terminate
							cr.setGameOver(true);
							cr.signal(this, "TERMINATE");
							System.exit(0);
							
						}
						cr.broadcast("Player "+(index+1)+"'s turn", this);
						String dir = getDirection();
						Integer num = getNum();
						String guess = getGuess(dir, num);  //form player answer and pass it
						String answer=num+"|"+dir+"|"+guess;
						lock.unlock();
						cr.signal(this, answer);
						guess = "";
					}
				}
			} catch (IOException e) {
			System.out.println("ioe: "+e.getMessage());
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally {
				//lock.unlock();
			}
		}
		
		public Integer getNum() throws IOException {  //methods to form player answer
			String num;
			Integer number = null;
			boolean correctNum= false;
			sendMessage("Which number? ");
			while((num=br.readLine())==null) {
			}
			try {
				 number = Integer.parseInt(num);
			}catch (Exception e) {
				while(!correctNum) {
					sendMessage("That is not a valid number!");	
					sendMessage("Which number? ");
					while((num=br.readLine())==null) {
					}
					try {
						number=Integer.parseInt(num);
						correctNum=true;
					}catch (Exception e1) {
					correctNum=false;
					}
				}
			}
			return number;
		}
		
		public String getDirection() throws IOException {
			String dir="";
			sendMessage("Would you like to answer a question\r\n" + 
					"across (a) or down (d)?");
			while((dir=br.readLine())==null) {
			}
			while(!dir.equals("a")&&!dir.equals("d")) {
				sendMessage("That is not a valid direction!");
				sendMessage("Would you like to answer a question\r\n" + 
						"across (a) or down (d)?");
				while((dir=br.readLine())==null) {
				}
			}
			if(dir.equals("a") && cr.getAcrList().size()==0) {
				while(!dir.equals("d")) {
					sendMessage("That is not a valid direction!");
					sendMessage("Would you like to answer a question\r\n" + 
							"across (a) or down (d)?");
					while((dir=br.readLine())==null) {
					}
				}
			}
			if(dir.equals("d") && cr.getDowList().size()==0) {
				while(!dir.equals("a")) {
					sendMessage("That is not a valid direction!");
					sendMessage("Would you like to answer a question\r\n" + 
							"across (a) or down (d)?");
					while((dir=br.readLine())==null) {
					}
				}
			}
			return dir;
		}
		
		public String getGuess(String dir, Integer num) throws IOException {
			String guess;
			String temp = "What is your guess for "+num;
			if(dir.equals("a")) {
				temp+=" across?";
			}
			else
				temp+=" down?";
			sendMessage(temp);
			while((guess=br.readLine())==null) {
			}
			return guess;
		}
		
		public void sendMessage(String message) {
			pw.println(message);
			pw.flush();
		}
		public int getIndex() {
			return index;
		}
}
