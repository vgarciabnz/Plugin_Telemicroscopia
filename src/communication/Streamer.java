package communication;

public interface Streamer extends Runnable {

	/**
	 * This method must return the header HTML code needed to display the stream,
	 * typically javascript code.
	 * 
	 * @return
	 */
	public String getHeadHTMLText();

	/**
	 * This method must return the body HTML code needed to display the stream in the
	 * web browser. The video must be embedded in a HTML object with
	 * class='stream'
	 * 
	 * @return
	 */
	public String getBodyHTMLText();
	
	/**
	 * This method checks if all the programs needed by the streamer are installed. If any of them
	 * is not present, the method throws an Exception with the name of the program as message. 
	 * @return true - All required programs are present
	 */
	public boolean isStreamerReady() throws Exception;

}
