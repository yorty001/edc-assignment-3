import java.util.Timer;
import nz.sodium.*;
import swidgets.*;
import javax.swing.*;
import java.awt.*;
import java.util.TimerTask;

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
        
        
        Stream<GpsEvent> allEvents = streams[0];
        for (int i = 1;  i < streams.length; i++) {
        	allEvents = allEvents.merge(streams[i], (a, b) -> b);
        }

        Stream<String> eventText = allEvents.map(ev -> ev.name + "," + ev.latitude + "," + ev.longitude + "," +ev.altitude );
        StreamSink<String> clearSink = new StreamSink<>();
        Cell<String> lastText = eventText.hold("");
        Timer timer = new Timer();
        eventText.listen(text -> timer.schedule(new TimerTask(){
            public void run() { clearSink.send(text); }
        }, 3000));

        Stream<String> clears = clearSink
            .snapshot(lastText, (scheduled, current) -> scheduled.equals(current) ? "" : null)
            .filter(x -> x != null);

        Cell<String> latestEventCell = eventText.merge(clears, (a, b) -> b).hold("waiting...");
        
        SLabel updateLabel = new SLabel(latestEventCell);
           
        JFrame frame = new JFrame("GPS");
        JPanel trackersPanel = new JPanel(new GridLayout(5,2));
        
        
        for(Stream<GpsEvent> s : streams) {
        	trackersPanel.add(trackerPanel(s));
        }
        
        frame.setSize(600,400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(trackersPanel, BorderLayout.CENTER);
        frame.add(updateLabel, BorderLayout.SOUTH);
        frame.setVisible(true);
	}

}
