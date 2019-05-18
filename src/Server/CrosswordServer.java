 package Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Vector;

public class CrosswordServer {
	private ArrayList<Integer> positions=new ArrayList<Integer>();
	ArrayList<Word> accWords;
	ArrayList<Word> downWords;
	ArrayList<Word> usedAcc;
	ArrayList<Word> usedDown;
	char[][] board;
	String[][] residualBoard;
	int randomIndex=-1;
	int fileTries=0;
	
	public Vector<Word> getWordVect(){  //gets a vector of all word in the form of Word objects also validates game files
		Vector<Word> wordVec = new Vector<Word>();
		int number;
		String word;
		int aCount=0;
		int dCount=0;
		boolean isAcross=true;
		BufferedReader br =null;
		File directory = new File(" ");
		String path =directory.getAbsolutePath().trim()+"gamedata";
		File dir = new File(path);
		File[] files = dir.listFiles();
		int dirLength=files.length;
		if(fileTries==dirLength) {
			System.out.println("No valid game files exist. Please add one.");
			System.exit(1);
		}
		fileTries++;
		if(randomIndex==-1) {
			randomIndex = (int) (Math.random() * dirLength);
		}
		else {
			randomIndex++;
			randomIndex=(int) (randomIndex%dirLength);
		}
		String selectedFile = dir.getAbsolutePath()+"\\"+dir.list()[randomIndex];
		String fileError = "This file "+dir.list()[randomIndex]+" is not formatted properly.\r\n";
		try {
			br = new BufferedReader(new FileReader(selectedFile));
			String line;
			while((line = br.readLine()) != null) {
				if(line.equals("ACROSS")) {
					line=line.trim();
					if(line.split("\\s+").length!=1) {
						System.out.println(fileError);
						return null;
					}
					isAcross=true;
					aCount++;
				}
				else if(line.equals("DOWN")) {
					line=line.trim();
					if(line.split("\\s+").length!=1) {
						System.out.println(fileError);
						return null;
					}
					isAcross=false;
					dCount++;
				}
				else {
					String [] parameters= line.split("\\|");
					if(parameters.length < 3) {
						System.out.println(fileError + 
								"There are not enough parameters on line "+line+".");
						return null;
					}
					word= parameters[1];
					String description = parameters[2];
					try {
						number=Integer.parseInt(parameters[0]);
					}catch(NumberFormatException e) {
						System.out.println(fileError+"Unable to convert "+parameters[0]+" to double");
						return null;
					}
					Word newWord = new Word(number, word, description, isAcross);
					for(int i=0; i<wordVec.size(); i++) {
						Word temp = wordVec.get(i);
						if(temp.getNumber()==newWord.getNumber()) {
							if(temp.getAnswer().charAt(0)!=newWord.getAnswer().charAt(0)) {
								System.out.println("Two words with the same number start with different letters");
								return null;
							}
						}
					}
					wordVec.add(newWord);
				}
			}
		} catch(Exception e) {
			System.out.println("Exception: "+e.getMessage());
		}finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if(aCount+dCount!=2) {
			System.out.println(fileError+"invalid number of direction declarators. ie: ACROSS or DOWN");
			return null;
		}
		return wordVec;
	} 
	
	public int getBoardSize(Vector<Word> wordVec) { //gets initial board size before cut
		int horizSum = 0;
		int vertSum = 0;
		for(Word w : wordVec) {
			if(w.isAcross()==true) {
				horizSum+=w.getAnswer().length();
			}
			else {
				vertSum+=w.getAnswer().length();
			}
		}
		int sum = 2*(horizSum+vertSum);
		return (sum);
	}
	
	public void placeWords(String word, char[][] board, int row, int column, boolean isAcross) { //places a word on the board 
		if(isAcross) {
			for(int i = column; i<column+word.length();i++ ) {
				board[row][i]=word.charAt(i-column);
			}
		}
		else {
			for(int i = row; i<row+word.length();i++ ) {
				board[i][column]=word.charAt(i-row);
			}
		}
	} 
	
	public void placeResid(String word,int row, int column, boolean isAcross, int number) { //places word on residual board for gameplay
		word=word.toUpperCase();
		word=number+word;
		if(isAcross) {
			for(int i = column; i<column+word.length();i++ ) {
				if(i==column) {
					residualBoard[row][i]=Character.toString(word.charAt(i-column))+Character.toString(word.charAt(i-column+1))+" ";
					i++;
				}
				else
					residualBoard[row][i-1]=" "+Character.toString(word.charAt(i-column))+" ";
			}
		}
		else {
			for(int i = row; i<row+word.length();i++ ) {
				if(i==row) {
					residualBoard[i][column]=Character.toString(word.charAt(i-row))+Character.toString(word.charAt(i-row+1))+" ";
					i++;
				}
				else
					residualBoard[i-1][column]=" "+Character.toString(word.charAt(i-row))+" ";
			}
		}
	} 
	
	public void putBackWord(ArrayList<Word> usedAcc, ArrayList<Word> usedDown, char[][] board){ //puts words back for backtracking
		for(int i=0; i<usedAcc.size(); i++) {
			placeWords(usedAcc.get(i).getAnswer(), board, usedAcc.get(i).getRowIndex(),usedAcc.get(i).getColIndex(),true);
		}
		for(int i=0; i<usedDown.size(); i++) {
			placeWords(usedDown.get(i).getAnswer(), board, usedDown.get(i).getRowIndex(),usedDown.get(i).getColIndex(),false);
		}
	}
	
	public boolean isSafe(String word, char[][] board, int row, int column, boolean isAcross) { //checks if a position
		int sameCounter =0;
		int interfs=0;
		if(isAcross) {
			if(column!=0 && column+word.length()<=board.length-1) {
			if(board[row][column-1]!='0' || board[row][column+word.length()]!='0') {
				return false; //if char right before or after where we want to place word
			}
			}
			for(int i = column; i<column+word.length();i++ ) {
				if(board[row][i]==word.charAt(i-column)) {
					sameCounter++; //if its the same letter increase count
					
				}
				else if(board[row][i]!=word.charAt(i-column) && board[row][i]!='0') {
					return false; //if char conflict then return false
				}
				
				if(row!=0) {
					if(board[row-1][i]!='0') {  //has a padding around word except where the other word joins it
						interfs++;
					}
				}
				if(row!=board.length-1) {
					if(board[row+1][i]!='0') {
						interfs++;
					}
				}
			}
		}
		else {
			if(row!=0 && row+word.length()<=board.length-1) {
			if(board[row-1][column]!='0' || board[row+word.length()][column]!='0') {
				return false; //if char right before or after where we want to place word
			}
			}
			for(int i = row; i<row+word.length();i++ ) {
				if(board[i][column]==word.charAt(i-row)) {
					sameCounter++; //if its the same letter increase count
				}
				else if(board[i][column]!=word.charAt(i-row) && board[i][column]!='0') {
					return false; //if char conflict then return false
				}
				
				if(column!=0) {
					if(board[i][column-1]!='0') {
						interfs++;
					}
				}
				if(column!=board.length-1) {
					if(board[i][column+1]!='0') {
						interfs++;
					}
				}
			}
		}
		if(sameCounter==0) { //if not overlaying any word return false
			return false;
		}
		if(interfs>2) { //check padding
			return false;
		}
		return true;
	}
	
	public boolean crosswordMaker(ArrayList<Word> accList, ArrayList<Word> downList, ArrayList<Word> usedAcc, ArrayList<Word> usedDown,char[][] board, boolean firstWord, boolean isAcross) {
		if(accList.isEmpty() && downList.isEmpty()) {  //the grand backtracking algorithm
			return true;							//checks same starts and every position on the board other wise
		}											//for both across and down words. not as slow as you would expect its actually pretty fast
		if(isAcross && accList.isEmpty()) {
			isAcross = false;
		}
		if(!isAcross && downList.isEmpty()) {
			isAcross=true;
		}
		boolean sameStart= false;	
		int sameRowInd=0;
		int sameColInd=0;
		if(isAcross) {
			for(int i = 0; i<accList.size(); i++) {
				for(int j = 0; j<usedDown.size(); j++) {
					if(accList.get(i).getNumber()==usedDown.get(j).getNumber()) {
						sameStart = true;
						sameRowInd=usedDown.get(i).getRowIndex();
						sameColInd=usedDown.get(i).getColIndex();
						break;
					}
				}
				if(sameStart) {
					accList.get(i).setRowIndex(sameRowInd);
					accList.get(i).setColIndex(sameColInd);
					if(isSafe(accList.get(i).getAnswer(), board, sameRowInd, sameColInd, true)){
						placeWords(accList.get(i).getAnswer(), board, sameRowInd, sameColInd, true);
						Word backWord = accList.get(i);
						usedAcc.add(accList.get(i));
						accList.remove(i);
						boolean backFirst = firstWord;
						firstWord =false;
						if(crosswordMaker(accList, downList, usedAcc, usedDown, board, firstWord, false)) {
							return true;
						}
						else {
							int numZeros=backWord.getAnswer().length();
							String replace="";
							for(int z = 0; z<numZeros; z++) {
								replace+='0';
							}
							placeWords(replace, board, sameRowInd, sameColInd, true);
							backWord.setRowIndex(-1);
							backWord.setColIndex(-1);
							accList.add(backWord);
							usedAcc.remove(backWord);
							firstWord = backFirst;
							putBackWord(usedAcc, usedDown, board);
						}
					}
					else {
						accList.get(i).setRowIndex(-1);
						accList.get(i).setColIndex(-1);
						return false;
					}
				}
				else if(firstWord){
					accList.get(i).setRowIndex(board.length/2);
					accList.get(i).setColIndex(board.length/2);
					placeWords(accList.get(i).getAnswer(), board, board.length/2, board.length/2, true);
					Word backWord = accList.get(i);
					usedAcc.add(accList.get(i));
					accList.remove(i);
					boolean backFirst = firstWord;
					firstWord =false;
					if(crosswordMaker(accList, downList, usedAcc, usedDown, board, firstWord, false)) {
						return true;
					}
					else {
						int numZeros=backWord.getAnswer().length();
						String replace="";
						for(int z = 0; z<numZeros; z++) {
							replace+='0';
						}
						placeWords(replace, board, board.length/2, board.length/2, true);
						usedAcc.remove(backWord);
						backWord.setRowIndex(-1);
						backWord.setColIndex(-1);
						accList.add(backWord);
						firstWord = backFirst;
						putBackWord(usedAcc, usedDown, board);
					}
				}
				else {
					for(int k =0; k<board.length; k++) { //ROWS OF BOARD
						for(int h =0; h<=board.length-accList.get(i).getAnswer().length(); h++) {
							accList.get(i).setRowIndex(k);
							accList.get(i).setColIndex(h);
							if(isSafe(accList.get(i).getAnswer(), board, k, h, true)) {
								placeWords(accList.get(i).getAnswer(), board, k, h, true);
								Word backWord = accList.get(i);
								usedAcc.add(accList.get(i));
								accList.remove(i);
								boolean backFirst = firstWord;
								firstWord =false;
								if(crosswordMaker(accList, downList, usedAcc, usedDown, board, firstWord, false)) {
									return true;
								}
								else {
									int numZeros=backWord.getAnswer().length();
									String replace="";
									for(int z = 0; z<numZeros; z++) {
										replace+='0';
									}
									placeWords(replace, board, k, h, true);
									usedAcc.remove(backWord);
									backWord.setRowIndex(-1);
									backWord.setColIndex(-1);
									accList.add(backWord);
									firstWord = backFirst;
									putBackWord(usedAcc, usedDown, board);
								}
								
							}
						}
					}
				}
			}
		}
		else if(!isAcross) {
			for(int i = 0; i<downList.size(); i++) {
				for(int j = 0; j<usedAcc.size(); j++) {
					if(downList.get(i).getNumber()==usedAcc.get(j).getNumber()) {
						sameStart = true;
						sameRowInd=usedAcc.get(j).getRowIndex();
						sameColInd=usedAcc.get(j).getColIndex();
						break;
					}
				}
				if(sameStart) {
					downList.get(i).setRowIndex(sameRowInd);
					downList.get(i).setColIndex(sameColInd);
					if(isSafe(downList.get(i).getAnswer(), board, sameRowInd, sameColInd, false)){
						placeWords(downList.get(i).getAnswer(), board, sameRowInd, sameColInd, false);
						Word backWord = downList.get(i);
						usedDown.add(downList.get(i));
						downList.remove(i);
						boolean backFirst = firstWord;
						firstWord =false;
						if(crosswordMaker(accList, downList, usedAcc, usedDown, board, firstWord, true)) {
							return true;
						}
						else {
							int numZeros=backWord.getAnswer().length();
							String replace="";
							for(int z = 0; z<numZeros; z++) {
								replace+='0';
							}
							placeWords(replace, board, sameRowInd, sameColInd, false);
							downList.add(backWord);
							usedDown.remove(backWord);
							firstWord = backFirst;
							putBackWord(usedAcc, usedDown, board);
						}
					}
					else {
						downList.get(i).setRowIndex(-1);
						downList.get(i).setColIndex(-1);
						return false;
					}
				}
				else if(firstWord){
					downList.get(i).setRowIndex(board.length/2);
					downList.get(i).setColIndex(board.length/2);
					placeWords(downList.get(i).getAnswer(), board, board.length/2, board.length/2, false);
					Word backWord = downList.get(i);
					usedDown.add(downList.get(i));
					downList.remove(i);
					boolean backFirst = firstWord;
					firstWord =false;
					if(crosswordMaker(accList, downList, usedAcc, usedDown, board, firstWord, true)) {
						return true;
					}
					else {
						int numZeros=backWord.getAnswer().length();
						String replace="";
						for(int z = 0; z<numZeros; z++) {
							replace+='0';
						}
						placeWords(replace, board, board.length/2, board.length/2, false);
						usedDown.remove(backWord);
						backWord.setRowIndex(-1);
						backWord.setColIndex(-1);
						downList.add(backWord);
						firstWord = backFirst;
						putBackWord(usedAcc, usedDown, board);
					}
				}
				else {
					for(int k =0; k<=board.length-downList.get(i).getAnswer().length(); k++) { //ROWS OF BOARD
						for(int h =0; h<board.length; h++) {
							downList.get(i).setRowIndex(k);
							downList.get(i).setColIndex(h);
							if(isSafe(downList.get(i).getAnswer(), board, k, h, isAcross)) {
								placeWords(downList.get(i).getAnswer(), board, k, h, false);
								Word backWord = downList.get(i);
								usedDown.add(downList.get(i));
								downList.remove(i);
								boolean backFirst = firstWord;
								firstWord =false;
								if(crosswordMaker(accList, downList, usedAcc, usedDown, board, firstWord, true)) {
									return true;
								}
								else {
									int numZeros=backWord.getAnswer().length();
									String replace="";
									for(int z = 0; z<numZeros; z++) {
										replace+='0';
									}
									placeWords(replace, board, k, h, false);
									usedDown.remove(backWord);
									backWord.setRowIndex(-1);
									backWord.setColIndex(-1);
									downList.add(backWord);
									firstWord = backFirst;
									putBackWord(usedAcc, usedDown, board);
								}
								
							}
						}
					}
				}
			}
			
		}
		return false;
		
	}
	public void getPositions(char[][] board) { //gets postions to cut down board when printing it
		//0=top 1= bottom 2=left 3=right
		positions.add(0);
		positions.add(0);
		positions.add(0);
		positions.add(0);
		outerloop:
		for(int i=0; i<board.length; i++) {
			for(int j=0; j<board.length; j++) {
				if(board[i][j]!='0') {
					positions.add(0, i);
					break outerloop;
				}
			}
		}
		
		outerloop:
		for(int i=board.length-1; i>=0; i--) {
			for(int j=board.length-1; j>=0; j--) {
				if(board[i][j]!='0') {
					positions.add(1, i);
					break outerloop;
				}
			}
		}
		
		 outerloop:
				for(int i=0; i<board.length; i++) {
					for(int j=0; j<board.length; j++) {
						if(board[j][i]!='0') {
							positions.add(2, i);
							break outerloop;
						}
					}
				}
		
		outerloop:
		for(int i=board.length-1; i>=0; i--) {
			for(int j=board.length-1; j>=0; j--) {
				if(board[j][i]!='0') {
					positions.add(3, i);
					break outerloop;
				}
			}
		}
		
	}
	
	public void printBoard(char[][]board) { //prints board to test
		getPositions(board);
		for(int i=positions.get(0); i<=positions.get(1); i++) {
			for(int j = positions.get(2); j<=positions.get(3); j++) {
				System.out.print(board[i][j]);
			}
			System.out.println();
		}
	}
	
	public void printResid() {  //print residual board for gamePlay used to test
		getPositions(board);
		for(int i=positions.get(0); i<=positions.get(1); i++) {
			for(int j = positions.get(2); j<=positions.get(3); j++) {
				System.out.print(residualBoard[i][j]);
			}
			System.out.println();
			System.out.println();
		}
	}
	
	public char[][] createGameBoard(Vector<Word> wordVec) {  //this ties everything together 
		int size = getBoardSize(wordVec);
		board = new char[size][size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				board[i][j]='0';
			}
		}
		accWords = new ArrayList<Word>();
		downWords = new ArrayList<Word>();
		usedAcc = new ArrayList<Word>();
		usedDown = new ArrayList<Word>();
		for (Word word : wordVec) {
			if(word.isAcross()) {
				accWords.add(word);
			}
			else
				downWords.add(word);
		}
		boolean makeBoard = crosswordMaker(accWords, downWords, usedAcc, usedDown, board, true, true);
		getPositions(board);
		return board;
	}
	
	public void getUsed() { //get words with final indexes
		for(int i=0; i<usedAcc.size(); i++) {
			System.out.println(usedAcc.get(i).getAnswer()+" "+usedAcc.get(i).getRowIndex()+" "+usedAcc.get(i).getColIndex());
		}
		for(int i=0; i<usedDown.size(); i++) {
			System.out.println(usedDown.get(i).getAnswer());
		}
	}
	
	public void makeResidBoard(char[][] board) { //makes resid board for game
		residualBoard = new String[board.length][board.length];
		for(int i=0; i<residualBoard.length; i++) {
			for(int j=0; j<residualBoard.length; j++) {
				if(board[i][j]!='0') {
					residualBoard[i][j]=" _ ";
				}
				else {
					residualBoard[i][j]="   ";
				}
			}
		}
		for(int i=0; i<usedAcc.size(); i++) {
			residualBoard[usedAcc.get(i).getRowIndex()][usedAcc.get(i).getColIndex()]=usedAcc.get(i).getNumber()+"_ ";
		}
		for(int i=0; i<usedDown.size(); i++) {
			residualBoard[usedDown.get(i).getRowIndex()][usedDown.get(i).getColIndex()]=usedDown.get(i).getNumber()+"_ ";
		}
	}
	
	public void printHints(ServerThread st) { //prints hints for game
		st.sendMessage("Across");
		for(int i =0; i<usedAcc.size(); i++) {
			st.sendMessage(usedAcc.get(i).getNumber()+" "+usedAcc.get(i).getDesc());
		}
		st.sendMessage("Down");
		for(int i =0; i<usedDown.size(); i++) {
			st.sendMessage(usedDown.get(i).getNumber()+" "+usedDown.get(i).getDesc());
		}
	}
	
	public ArrayList<Word> getUsedAcc() {
		return usedAcc;
	}

	public ArrayList<Word> getUsedDown() {
		return usedDown;
	}

	public CrosswordServer() { //constructor
		System.out.println("Reading random game file.");
		Vector<Word> wordVec = getWordVect();
		while(wordVec==null) {
			System.out.println("Reading random game file.");
			wordVec = getWordVect();
		}
		System.out.println("File read successfully!");
		board = createGameBoard(wordVec);
		makeResidBoard(board);
	}


	public ArrayList<Integer> getPositions() {
		return positions;
	}

	public void setPositions(ArrayList<Integer> positions) {
		this.positions = positions;
	}

}

