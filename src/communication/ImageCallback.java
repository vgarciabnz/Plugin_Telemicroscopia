package communication;

import java.awt.image.BufferedImage;

import au.edu.jcu.v4l4j.CaptureCallback;
import au.edu.jcu.v4l4j.VideoFrame;
import au.edu.jcu.v4l4j.exceptions.V4L4JException;

public class ImageCallback implements CaptureCallback{

	BufferedImage currentImage = null;
	Object frameReceived = new Object();
	
	@Override
	public void exceptionReceived(V4L4JException arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void nextFrame(VideoFrame frame) {
		if (this.currentImage == null){
			currentImage = frame.getBufferedImage();
			synchronized(frameReceived){
				frameReceived.notify();
			}
		}
		frame.recycle();
	}
	
	public BufferedImage getCurrentImage(){
		synchronized(frameReceived){
			try {
				//Wait until the first frame arrives
				frameReceived.wait();
				return currentImage;
			} catch (InterruptedException e) {
				e.printStackTrace();
				return null;
			}
		}
	}
	
}
