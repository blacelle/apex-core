/**
 * The MIT License
 * Copyright (c) 2014 Benoit Lacelle
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package blasd.apex.jconsole;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.sun.tools.jconsole.JConsolePlugin;

/**
 * Demonstrate how to start a custom JConsole
 * 
 * @author Benoit Lacelle
 *
 */
public class BufferMonitorJPanel extends JPanel {
	private static final long serialVersionUID = -1872450950763813263L;

	// SLF4J in not available in the JConsole
	protected static final Logger LOGGER = Logger.getLogger(BufferMonitorJPanel.class.getName());

	public static final String DIRECT_BUFFER_NAME = "java.nio:type=BufferPool,name=direct";
	public static final String MAPPED_BUFFER_NAME = "java.nio:type=BufferPool,name=mapped";

	protected final MBeanServerConnection connection;

	protected final NavigableMap<Date, Long> dateToMemoryUsed = new ConcurrentSkipListMap<>();

	private static final double COEF_TOP_MARGIN = 1.1;

	private static final int PADDING = 25;
	private static final int LABEL_PADDING = 25;
	private static final Color LINE_COLOR = new Color(44, 102, 230, 180);
	private static final Color POINT_COLOR = new Color(100, 100, 100, 180);
	private static final Color GRID_COLOR = new Color(200, 200, 200, 200);
	private static final Stroke GRAPH_STROKE = new BasicStroke(2f);
	private static final int POINT_WIDTH = 4;
	private static final int NUMBER_Y_DIVISIONS = 10;

	private static final double TWENTY_DOUBLE = 20.0;
	private static final long THREE = 3;
	private static final long FIVE = 5;

	private static final int KB = 1024;

	protected BufferMonitorJPanel(MBeanServerConnection connection) {
		this.connection = connection;
		try {
			if (connection.isRegistered(new ObjectName(DIRECT_BUFFER_NAME))) {
				LOGGER.log(Level.FINE, DIRECT_BUFFER_NAME + " is registered");
			}
		} catch (MalformedObjectNameException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public BufferMonitorJPanel(JConsolePlugin jconsolePlugin) {
		this(jconsolePlugin.getContext().getMBeanServerConnection());
	}

	public void refreshValues() {
		try {
			Long memoryUsed = (Long) connection.getAttribute(new ObjectName(DIRECT_BUFFER_NAME), "MemoryUsed");

			if (memoryUsed != null) {
				dateToMemoryUsed.put(new Date(), memoryUsed);
			}
		} catch (AttributeNotFoundException | InstanceNotFoundException | MalformedObjectNameException | MBeanException
				| ReflectionException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		if (dateToMemoryUsed.isEmpty()) {
			return;
		}

		Date firstDate = dateToMemoryUsed.firstKey();
		Date lastDate = dateToMemoryUsed.lastKey();

		double xScale =
				((double) getWidth() - 2 * PADDING - LABEL_PADDING) / (lastDate.getTime() - firstDate.getTime());

		// Add 10% in order not to have a flat at the very top of the window
		long range = (long) ((getMaxScore() - getMinScore()) * COEF_TOP_MARGIN);
		double yScale;
		if (range == 0L) {
			yScale = 1;
		} else {
			yScale = ((double) getHeight() - 2 * PADDING - LABEL_PADDING) / range;
		}

		List<Point> graphPoints = new ArrayList<>();
		for (Entry<Date, Long> entry : dateToMemoryUsed.entrySet()) {
			int x1 = (int) ((entry.getKey().getTime() - firstDate.getTime()) * xScale + PADDING + LABEL_PADDING);
			int y1 = (int) ((getMaxScore() - entry.getValue()) * yScale + PADDING);
			graphPoints.add(new Point(x1, y1));
		}

		// draw white background
		g2.setColor(Color.WHITE);
		g2.fillRect(PADDING + LABEL_PADDING,
				PADDING,
				getWidth() - (2 * PADDING) - LABEL_PADDING,
				getHeight() - 2 * PADDING - LABEL_PADDING);
		g2.setColor(Color.BLACK);

		// create hatch marks and grid lines for y axis.
		for (int i = 0; i <= NUMBER_Y_DIVISIONS; i++) {
			int x0 = PADDING + LABEL_PADDING;
			int x1 = POINT_WIDTH + PADDING + LABEL_PADDING;
			int y0 = getHeight() - ((i * (getHeight() - PADDING * 2 - LABEL_PADDING)) / NUMBER_Y_DIVISIONS + PADDING
					+ LABEL_PADDING);
			int y1 = y0;
			if (dateToMemoryUsed.size() > 0) {
				g2.setColor(GRID_COLOR);
				g2.drawLine(PADDING + LABEL_PADDING + 1 + POINT_WIDTH, y0, getWidth() - PADDING, y1);
				g2.setColor(Color.BLACK);
				long memory = range * i
				// * ONE_HUNDRED
						/ NUMBER_Y_DIVISIONS;
				String yLabel;

				if (memory > KB * KB * KB) {
					yLabel = Long.toString(memory / (KB * KB * KB)) + "GB";
				} else if (memory > KB * KB) {
					yLabel = Long.toString(memory / (KB * KB)) + "MB";
				} else {
					yLabel = Long.toString(memory);
				}
				FontMetrics metrics = g2.getFontMetrics();
				int labelWidth = metrics.stringWidth(yLabel);
				g2.drawString(yLabel, x0 - labelWidth - FIVE, y0 + (metrics.getHeight() / 2) - THREE);
			}
			g2.drawLine(x0, y0, x1, y1);
		}

		// and for x axis
		for (int i = 0; i < dateToMemoryUsed.size(); i++) {
			if (dateToMemoryUsed.size() > 1) {
				int x0 = i * (getWidth() - PADDING * 2 - LABEL_PADDING) / (dateToMemoryUsed.size() - 1) + PADDING
						+ LABEL_PADDING;
				int x1 = x0;
				int y0 = getHeight() - PADDING - LABEL_PADDING;
				int y1 = y0 - POINT_WIDTH;
				if (i % ((int) (dateToMemoryUsed.size() / TWENTY_DOUBLE) + 1) == 0) {
					g2.setColor(GRID_COLOR);
					g2.drawLine(x0, getHeight() - PADDING - LABEL_PADDING - 1 - POINT_WIDTH, x1, PADDING);
					g2.setColor(Color.BLACK);
					String xLabel = i + "";
					FontMetrics metrics = g2.getFontMetrics();
					int labelWidth = metrics.stringWidth(xLabel);
					g2.drawString(xLabel, x0 - labelWidth / 2, y0 + metrics.getHeight() + THREE);
				}
				g2.drawLine(x0, y0, x1, y1);
			}
		}

		// create x and y axes
		g2.drawLine(PADDING + LABEL_PADDING, getHeight() - PADDING - LABEL_PADDING, PADDING + LABEL_PADDING, PADDING);
		g2.drawLine(PADDING + LABEL_PADDING,
				getHeight() - PADDING - LABEL_PADDING,
				getWidth() - PADDING,
				getHeight() - PADDING - LABEL_PADDING);

		Stroke oldStroke = g2.getStroke();
		g2.setColor(LINE_COLOR);
		g2.setStroke(GRAPH_STROKE);
		for (int i = 0; i < graphPoints.size() - 1; i++) {
			int x1 = graphPoints.get(i).x;
			int y1 = graphPoints.get(i).y;
			int x2 = graphPoints.get(i + 1).x;
			int y2 = graphPoints.get(i + 1).y;
			g2.drawLine(x1, y1, x2, y2);
		}

		g2.setStroke(oldStroke);
		g2.setColor(POINT_COLOR);
		for (int i = 0; i < graphPoints.size(); i++) {
			int x = graphPoints.get(i).x - POINT_WIDTH / 2;
			int y = graphPoints.get(i).y - POINT_WIDTH / 2;
			int ovalW = POINT_WIDTH;
			int ovalH = POINT_WIDTH;
			g2.fillOval(x, y, ovalW, ovalH);
		}
	}

	private long getMinScore() {
		return 0L;
	}

	private long getMaxScore() {
		long maxScore = 0L;
		for (Long score : dateToMemoryUsed.values()) {
			maxScore = Math.max(maxScore, score);
		}
		return maxScore;
	}

	private static BufferMonitorJPanel createAndShowGui() {
		BufferMonitorJPanel mainPanel = new BufferMonitorJPanel(ManagementFactory.getPlatformMBeanServer());
		mainPanel.setPreferredSize(new Dimension(KB, KB / 2));
		JFrame frame = new JFrame("DrawGraph");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(mainPanel);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		return mainPanel;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				BufferMonitorJPanel panel = createAndShowGui();

				refreshLaterIndefinitely(panel);
			}
		});
	}

	protected static void refreshNow(BufferMonitorJPanel panel) {
		new BufferMonitorUpdater(panel).execute();
	}

	protected static void refreshLaterIndefinitely(final BufferMonitorJPanel panel) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				refreshNow(panel);

				// Allocate 1MB
				ByteBuffer.allocateDirect(KB * KB);

				try {
					Thread.sleep(KB);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				refreshLaterIndefinitely(panel);
			}
		});
	}
}
