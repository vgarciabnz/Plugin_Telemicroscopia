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

}
