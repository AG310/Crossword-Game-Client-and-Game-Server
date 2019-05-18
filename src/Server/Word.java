package Server;

public class Word {
	private String answer;
	private boolean isAcross;
	private int number;
	private int rowIndex= -1;
	private int colIndex= -1;
	private String description;
	public Word(int num, String word, String desc, boolean across) {
		number = num;
		answer = word;
		isAcross = across;
		description=desc;
	}
	public String getDesc() {
		return description;
	}
	public String getAnswer() {
		return answer;
	}
	public boolean isAcross() {
		return isAcross;
	}
	public int getNumber() {
		return number;
	}
	public int getRowIndex() {
		return rowIndex;
	}
	public void setRowIndex(int rowIndex) {
		this.rowIndex = rowIndex;
	}
	public int getColIndex() {
		return colIndex;
	}
	public void setColIndex(int colIndex) {
		this.colIndex = colIndex;
	}

}
