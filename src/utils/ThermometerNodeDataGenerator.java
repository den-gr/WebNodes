package utils;

import java.util.Iterator;
import java.util.stream.Stream;

/**
 * Helps in creating of nodes data.
 */
public class ThermometerNodeDataGenerator {
	Iterator<Integer> idGenerator = Stream.iterate(0, i -> i + 1).iterator();
	private final int max_cols;
	private final int stepX;
	private final int stepY;
	private int lastX = -1;
	private int lastY = 0;
	
	public ThermometerNodeDataGenerator(int cols, int stepX, int stepY) {
		this.max_cols = cols;
		this.stepX = stepX;
		this.stepY = stepY;
	}
	
	public ThermometerNodeData getNewNodeData() {
		return new ThermometerNodeData();
	}
	
	/** 
	 * Allow easy access to created node data
	 */
	public class ThermometerNodeData{
		public final int id;
		public final int x;
		public final int y;
		
		public ThermometerNodeData() {
			int tmpx; 
			int tmpy;
			if(++lastX >= max_cols) {
				lastX = 0;
				lastY++;
			}
			tmpx = lastX;
			tmpy = lastY;
			
			this.id = idGenerator.next();
			this.x = tmpx * stepX;
			this.y = tmpy * stepY;
		}
	}
}
