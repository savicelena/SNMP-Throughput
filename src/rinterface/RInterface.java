package rinterface;

import java.io.IOException;
import java.net.SocketException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jfree.data.xy.XYDataset;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import main.Main;

public class RInterface extends Thread{
	
	public static int thrNum = 0;
	
	public static int values = 7;
	public static OID ifIndex = new OID(".1.3.6.1.2.1.2.2.1.1");
	
	public static OID ifInOctets = new OID(".1.3.6.1.2.1.2.2.1.10");
	public static OID ifOutOctets = new OID(".1.3.6.1.2.1.2.2.1.16");
	public static OID ifInUcastPkts = new OID(".1.3.6.1.2.1.2.2.1.11");
	public static OID ifInNUcastPkts = new OID(".1.3.6.1.2.1.2.2.1.12");
	public static OID ifOutUcastPkts = new OID(".1.3.6.1.2.1.2.2.1.17");
	public static OID ifOutNUcastPkts = new OID(".1.3.6.1.2.1.2.2.1.18");
	
	public static OID ipAddr = new OID(".1.3.6.1.2.1.4.20.1.1");
	public static OID ifNumber = new OID(".1.3.6.1.2.1.4.20.1.2");
	public static OID ifOperStatus = new OID(".1.3.6.1.2.1.2.2.1.8");
	
	public static int port = 161;
	
	
	private String router;
	private String intAddr;
	private int ifNum;
	private String loopback;
	private Graph graphThr;
	private Graph graphUcast;
	private Graph graphNUcast;
	private int id;
	
	private int pcktUcastIn;
	private int pcktUcastOut;
	private int pcktNUcastIn;
	private int pcktNUcastOut;
	
	public RInterface(String r, String in, String ia, String lp){
		this.router = r;
		this.ifNum = Integer.parseInt(in);
		this.intAddr = ia;
		this.loopback = lp;
		graphThr = new Graph("Interface throughput", "bits");
		graphUcast = new Graph("Unicast throughput", "packets");
		graphNUcast = new Graph("Non-unicast throughput", "packets");
		this.id = thrNum++;
	}
	


	public static int getThrNum() {
		return thrNum;
	}

	public static int getValues() {
		return values;
	}

	public int getIdThr() {
		return id;
	}

	public int getPcktUcastIn() {
		return pcktUcastIn;
	}

	public int getPcktUcastOut() {
		return pcktUcastOut;
	}

	public int getPcktNUcastIn() {
		return pcktNUcastIn;
	}

	public int getPcktNUcastOut() {
		return pcktNUcastOut;
	}

	public String getIntAddr() {
		return intAddr;
	}

	public int getIfNum() {
		return ifNum;
	}
	
	public String getRouter() {
		return router;
	}

	public String getLoopback() {
		return loopback;
	}
	
	public Graph getGraphThr() {
		return graphThr;
	}
	
	public Graph getGraphUcast() {
		return graphUcast;
	}
	
	public Graph getGraphNUcast() {
		return graphNUcast;
	}



	public static List<RInterface> addInterfacesOfRouter(String router, String loopback){
		List <RInterface> interfaces = new ArrayList<RInterface>();
		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString("si2019"));
		//postavljanje adrese rutera za pristup putem SNMP-a
		target.setAddress(GenericAddress.parse("udp:" + loopback + "/" + port));
		target.setVersion(SnmpConstants.version2c);
		Snmp snmp;
		try {
			snmp = new Snmp(new DefaultUdpTransportMapping());
			snmp.listen();
			
			//formiranje Protocol Data Unit-a za dobijanje adresa aktivnih interfejsa
			PDU pduAddr = new PDU();
			VariableBinding vbAddr = new VariableBinding(ipAddr);
			pduAddr.add(vbAddr);
			
			PDU pduIfNum = new PDU();
			VariableBinding vbIfNum = new VariableBinding(ifNumber);
			pduIfNum.add(vbIfNum);
			
			
			while(true) {
				ResponseEvent responseAddr = snmp.getNext(pduAddr, target);
				ResponseEvent responseIfNum = snmp.getNext(pduIfNum, target);
				
				
				PDU pduResponseAddr = responseAddr.getResponse();
				if(pduResponseAddr.getVariable(ipAddr) == null) break;
				//System.out.println(pduResponseAddr.getVariable(oidAddr).toString());
				String ip = pduResponseAddr.getVariable(ipAddr).toString();
				PDU pduResponseIfNum = responseIfNum.getResponse();
				//System.out.println(pduResponseIfNum.getVariable(oidIfNum));
				String num = pduResponseIfNum.getVariable(ifNumber).toString();
				//za svaki interfejs koji formiramo pamtimo njegovu ip adresu i broj
				interfaces.add(new RInterface(router, num, ip, loopback));
				pduAddr = pduResponseAddr;
				pduIfNum = pduResponseIfNum;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return interfaces;
	}
	
	@Override
	public void run() {
		int time = 0;
		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString("si2019"));
		target.setAddress(GenericAddress.parse("udp:" + loopback + "/" + port));
		target.setVersion(SnmpConstants.version2c);
		Snmp snmp;
		try {
			snmp = new Snmp(new DefaultUdpTransportMapping());
			snmp.listen();
			OID[] oids = {ifIndex, ifInOctets, ifOutOctets, ifInUcastPkts, ifInNUcastPkts, ifOutUcastPkts, ifOutNUcastPkts};
			PDU[] pdus = new PDU[values];
			ResponseEvent[] responses = new ResponseEvent[values];
			int[] val0 = new int[values];
			int[] val1 = new int[values];
			
			pdus[0] = new PDU();
			pdus[0].add(new VariableBinding(oids[0]));
			int counter = 0;
			val0[0] = val1[0] = ifNum;
			//brojanje koliko redova tabele moramo da prodjemo da bismo dosli do naseg interfejsa
			while(true) {
				responses[0] = snmp.getNext(pdus[0], target);
				counter++;
				if(Integer.parseInt(responses[0].getResponse().getVariable(oids[0]).toString()) == ifNum) break;
				pdus[0] = responses[0].getResponse();
			}
			int iteration = 0;
			while(true) {
				//ocitavanje podataka na svakih 10 sekundi
				for (; iteration < 2; iteration++) {
					for(int i = 1; i < values; i++) {
						pdus[i] = new PDU();
						pdus[i].add(new VariableBinding(oids[i]));
					}
					//trazimo vrednosti koje odgovaraju nasem interfejsu
					for(int i = 0; i < counter; i++) {
						for(int j = 1; j < values; j++) {
							responses[j] = snmp.getNext(pdus[j], target);
						}
						for(int j = 1; j < values; j++) {
							pdus[j] = responses[j].getResponse();
						}
					}
					
					for(int i = 1; iteration == 0 && i < values; i++) {
						val0[i] = Integer.parseInt(responses[i].getResponse().getVariable(oids[i]).toString());
					}
					for(int i = 1; iteration == 1 && i < values; i++) {
						val1[i] = Integer.parseInt(responses[i].getResponse().getVariable(oids[i]).toString());
					}
					
					if(iteration == 0) Thread.sleep(10000);
				}
				//racunanje protoka i dodavanje vrednosti na grafove
				time += 10;
				float throughputIn = 8*(val1[1]-val0[1])/10;
				float throughputOut = 8*(val1[2]-val0[2])/10;
				int pcktNumIn = (val1[3]-val0[3]) + (val1[4]-val0[4]);
				int pcktNumOut = (val1[5]-val0[5]) + (val1[6]-val0[6]);
				pcktUcastIn = val1[3]-val0[3];
				pcktUcastOut = val1[5] - val0[5];
				graphUcast.addInParam(pcktUcastIn, time);
				graphUcast.addOutParam(pcktUcastOut, time);
				pcktNUcastIn = val1[4]-val0[4];
				pcktNUcastOut = val1[6] - val0[6];
				graphNUcast.addInParam(pcktNUcastIn, time);
				graphNUcast.addOutParam(pcktNUcastOut, time);
				//System.out.println("Ulazni protok: " + throughputIn);
				//System.out.println("Izlazni protok: " + throughputOut);
				graphThr.addInParam(throughputIn, time);
				graphThr.addOutParam(throughputOut, time);
				//System.out.println("Ulazni paketi: " + pcktNumIn);
				//System.out.println("Ulazni unicast paketi: " + pcktUcastIn);
				//System.out.println("Izlazni ucast paketi: " + pcktUcastOut);
				//System.out.println("Ulazni non unicast paketi: " + pcktNUcastIn);
				//System.out.println("Izlazni non unicast paketi: " + pcktNUcastOut);
				//System.out.println("Izlazni paketi: " + pcktNumOut);
				//System.out.println(time);
				for(int i = 0; i < values; i++) {
					val0[i] = val1[i];
				}
				iteration = 1;
				if(this.id == Main.selected) Main.updateLabels(this.id);
				Thread.sleep(10000);
				//graphUcast.printData();
			}
			
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Override
	public String toString() {
		return "Router: " + router + " interface: " + intAddr + " num: " + ifNum + " loopback: " + loopback;
	}

	

}
