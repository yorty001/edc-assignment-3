
import nz.sodium.*;
import swidgets.*;
import javax.swing.*;
import java.awt.*;

public class GpsGUI {
	
	
	private static JPanel trackerPanel(Stream<GpsEvent> s) {     
        Cell<String> numCell = s.map(ev -> ev.name).hold("waiting...");
        Cell<String> latCell = s.map(ev -> "Lat: " + ev.latitude).hold("waiting...");
        Cell<String> longCell = s.map(ev -> "Long: " + ev.longitude).hold("waiting...");
        
        SLabel numLabel = new SLabel(numCell);
        SLabel latLabel = new SLabel(latCell);
        SLabel longLabel = new SLabel(longCell);
		
		JPanel panel = new JPanel();
		panel.add(numLabel);
        panel.add(latLabel);
        panel.add(longLabel);
		return panel;
	}

	public static void main(String[] args) {
		GpsService serv = new GpsService();
        Stream<GpsEvent>[] streams = serv.getEventStreams();
           
        JFrame frame = new JFrame("GPS");
        frame.setLayout(new GridLayout(5,2));
        frame.setSize(600,400);
        
        for(Stream<GpsEvent> s : streams) {
        	frame.add(trackerPanel(s));
        }
        
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

	}

}
