package Server;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient extends Thread {
	private BufferedReader br = null;
	private PrintWriter pw = null;
	private String hostname;
	private String port;
	
	public ChatClient() {
		Socket s = null;
		try {
			Scanner scan = new Scanner(System.in);
			System.out.println("Welcome to 201 Crossword!\nEnter the server hostname: ");
			hostname=scan.nextLine();
			System.out.println("Enter the server port: ");
			port=scan.nextLine();
			s= new Socket(hostname,  Integer.parseInt(port));
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			pw = new PrintWriter(s.getOutputStream());
			this.start();
			while(true) {
				String line=scan.nextLine();
				pw.println(line);
				pw.flush();
			}
			
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}finally {
			try {
				if(s!=null) s.close();
			} catch (Exception e2) {
				System.out.println(e2.getMessage());
			}
		}
	}
	
	public void run() {
		try {
			while(true) {
				String line = br.readLine();
				if(line.equals("TERMINATE")) {
					System.exit(0);
				}
				System.out.println(line);
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public static void main(String[] args) {
		ChatClient cc= new ChatClient();

	}

}
