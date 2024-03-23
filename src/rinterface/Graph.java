package rinterface;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Panel;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class Graph extends JPanel {
	
	private XYSeries thrIn;
	private XYSeries thrOut;
	private int proba0 = 0;
	private int proba1 = 10;
	

	public Graph(String name, String value) {
		JFreeChart graph = ChartFactory.createXYLineChart(name, "time", value, createData());
		thrIn.add(0,0);
		thrOut.add(0,0);
		ChartPanel panel = new ChartPanel(graph);
		panel.setPreferredSize(new Dimension(580,400));
		
		
		this.add(panel);
	}
	
	public XYDataset createData() {
		//formiranje grafika koji prikazuje ulazne i izlazne protoke
		thrIn = new XYSeries("Input throughput");
		thrOut = new XYSeries("Output throughput");
		XYSeriesCollection collection = new XYSeriesCollection();
		collection.addSeries(thrIn);
		collection.addSeries(thrOut);
		return collection;
	}
	
	public void addInParam(double thr, int time) {
		thrIn.add(time, thr);
		repaint();
	}
	
	public void addOutParam(double thr, int time) {
		thrOut.add(time, thr);
		repaint();
	}
	
	public void printData() {
		int items = thrIn.getItemCount();
		System.out.println("*******");
		for(int i = 0; i < items; i++) {
			System.out.println(thrIn.getX(i));
			System.out.println(thrIn.getY(i));
		}
		System.out.println("*******");
	}
	
}
