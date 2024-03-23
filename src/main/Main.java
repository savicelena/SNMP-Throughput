package main;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.SocketException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import rinterface.RInterface;

public class Main extends Frame{
	
	private static Main obj = null;
	
	public static String r1Loopback = "192.168.10.1";
	public static String r2Loopback = "192.168.20.1";
	public static String r3Loopback = "192.168.30.1";
	
	private static List<RInterface> r1interfaces;
	private static List<RInterface> r2interfaces ;
	private static List<RInterface> r3interfaces;
	private static List<RInterface> interfaces = new ArrayList<RInterface>();
	
	private static List<Panel> panels;
	private static List<Checkbox> checkBox;
	private int forListener = 0;
	public static int selected = -1;
	
	
	private static String strInUcst = "Number of input ucast packets: ";
	private static String strOutUcst = "Number of output ucast packets: ";
	private static String strInNUcst = "Number of input non-ucast packets: ";
	private static String strOutNUcst = "Number of output non-ucast packets: ";
	private static Label labelInUcst = new Label(strInUcst + 0);
	private static Label labelOutUcst = new Label(strOutUcst + 0);
	private static Label labelInNUcst = new Label(strInNUcst + 0);
	private static Label labelOutNUcst = new Label(strOutNUcst + 0);
	
	
	private Main() {
		super("Prikaz protoka");
		this.setBounds(0, 0, 1700, 850);
		this.setResizable(false);
		
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				dispose();
			}
		});
		
		addComponents();
		
		this.setVisible(true);
	}
	
	public static Main getInstance() {
		if (obj == null) {
			obj = new Main();
		}
		return obj;
	}
	
	
	public static void main(String[] args) {
		
		r1interfaces = RInterface.addInterfacesOfRouter("r1", r1Loopback);
		r2interfaces = RInterface.addInterfacesOfRouter("r2", r2Loopback);
		r3interfaces = RInterface.addInterfacesOfRouter("r3", r3Loopback);
		
		
		for(int i = 0; i < r1interfaces.size(); i++) {
			interfaces.add(r1interfaces.get(i));
		}
		for(int i = 0; i < r2interfaces.size(); i++) {
			interfaces.add(r2interfaces.get(i));
		}
		for(int i = 0; i < r3interfaces.size(); i++) {
			interfaces.add(r3interfaces.get(i));
		}
		
		//zapocinje se pracenje protoka na svim interfejsima
		for(int i = 0; i < interfaces.size(); i++) {
			interfaces.get(i).start();
		}
		
	
		Main frame = Main.getInstance();
		
		
		try {
			for(int i = 0; i < interfaces.size(); i++) {
				interfaces.get(i).join();
			}
			//interfaces.get(2).join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public void addComponents() {
		//na glavnom panelu se nalazi panel sa nazivom i checkbox-ovima
		//u slucaju izbora nekog chexkbox-a prikazuju se grafici protoka na interfejsu
		Panel mainPanel = new Panel();
		mainPanel.setBackground(Color.lightGray);
		mainPanel.setLayout(new GridLayout(2, 1, 0, 5));
		Label name = new Label("SNMP interface throughput monitoring", Label.CENTER);
		name.setFont(new Font("Times New Roman", Font.BOLD, 20));
		Panel supporting = new Panel();
		supporting.setLayout(new GridLayout(4, 1, 0, 5));
		supporting.add(name);
		Label checkChoose = new Label("Choose one of the active interfaces: ", Label.LEFT);
		checkChoose.setFont(new Font("Times New Roman", Font.BOLD, 14));
		supporting.add(checkChoose);
		panels = new ArrayList<Panel>();
		checkBox = new ArrayList<Checkbox>();
		CheckboxGroup checkGroup = new CheckboxGroup();
		for(int i = 0; i < interfaces.size(); i++) {
			//dodavanje checkbox-a za svaki interfejs
			RInterface ri = interfaces.get(i);
			checkBox.add(new Checkbox(ri.getRouter() + ": " + ri.getIntAddr(), false, checkGroup));
			//formiranje panela na kome ce se nalaziti grafici relevantni za interfejs
			Panel panelGraph = new Panel();
			panelGraph.setLayout(new GridLayout(1, 3, 15, 15));
			panelGraph.add(ri.getGraphUcast());
			panelGraph.add(ri.getGraphThr());
			panelGraph.add(ri.getGraphNUcast());
			
			panels.add(panelGraph);
		}
		
		Panel checkBoxPanel = new Panel();
		checkBoxPanel.setLayout(new GridLayout(3, 5, 0, 5));
		for(int i = 0; i < checkBox.size(); i++) {
			Checkbox ch = checkBox.get(i);
			ch.setFont(new Font(("Times New Roman"), Font.BOLD, 14));
			checkBoxPanel.add(ch);
			ch.addItemListener( (ie) -> {
				if(ie.getStateChange() == ItemEvent.SELECTED) {
					//uklanjanje grafika interfejsa koji nije izabran
					for(; forListener < panels.size(); forListener++) {
						if(panels.get(forListener).getParent() == mainPanel) {
							mainPanel.remove(panels.get(forListener));
						}
					}
					forListener = 0;
					//dobijanje labele checkbox-a koji je izabran i izdvajanje ip adrese
					//System.out.println(ie.getItem());
					String cbName = (String)ie.getItem();
					String[] splitted = cbName.split(" ");
					cbName = splitted[1];
					//pretraga interfejsa koji je izabran radi prikaza grafa
					String toCompare = checkBox.get(0).getLabel().split(" ")[1];
					while(!cbName.equals(toCompare)) {
						forListener++;
						toCompare = checkBox.get(forListener).getLabel().split(" ")[1];
					}
				
					selected = forListener;
					Main.updateLabels(forListener);
					mainPanel.add(panels.get(forListener));
					this.revalidate();
					this.pack();
					
					
				}
			});
			
		}
		
		Panel forPcktNum = new Panel();
		forPcktNum.setLayout(new BorderLayout());
		Panel ucast = new Panel(new GridLayout(2,1,5,5));
		Panel nucast = new Panel(new GridLayout(2,1,5,5));
		
		ucast.add(labelInUcst);
		ucast.add(labelOutUcst);
		nucast.add(labelInNUcst);
		nucast.add(labelOutNUcst);
		Font font = new Font("Times New Roman", Font.BOLD, 12);
		labelInUcst.setFont(font);
		labelOutUcst.setFont(font);
		labelInNUcst.setFont(font);
		labelOutNUcst.setFont(font);
		forPcktNum.add(ucast, BorderLayout.LINE_START);
		forPcktNum.add(nucast, BorderLayout.LINE_END);
		
		supporting.add(checkBoxPanel);
		supporting.add(forPcktNum);
		mainPanel.add(supporting);
		this.add(mainPanel);
		
	}
	
	public static void updateLabels(int id) {
		labelInUcst.setText(strInUcst + interfaces.get(id).getPcktUcastIn());
		labelOutUcst.setText(strOutUcst + interfaces.get(id).getPcktUcastOut());
		labelInNUcst.setText(strInNUcst + interfaces.get(id).getPcktNUcastIn());
		labelOutNUcst.setText(strOutNUcst + interfaces.get(id).getPcktNUcastOut());
	}
}
