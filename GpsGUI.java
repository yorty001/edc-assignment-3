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

    private static String eventToText(GpsEvent ev) {
        return ev.name + "," + ev.latitude + "," +ev.longitude + "," + ev.altitude;
    }

    private static Stream<GpsEvent> inRange(Stream<GpsEvent> s, Cell<double[]> range) {
        return s.snapshot(range, (ev, r) -> ev.latitude >= r[0] && ev.latitude <= r[1] && ev.longitude >= r[2] && ev.longitude <= r[3] ? ev : null).filter(x -> x != null);
        }

	public static void main(String[] args) {
		GpsService serv = new GpsService();
        Stream<GpsEvent>[] streams = serv.getEventStreams();
        
        
        Stream<GpsEvent> allEvents = streams[0];
        for (int i = 1;  i < streams.length; i++) {
        	allEvents = allEvents.merge(streams[i], (a, b) -> b);
        }

        Stream<String> eventText = allEvents.map(ev -> eventToText(ev));
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
           
        STextField latMin = new STextField("-90");
        STextField latMax = new STextField("90");
        STextField longMin = new STextField("-180");
        STextField longMax = new STextField("180");

        JLabel latMinLabel = new JLabel("Lat Min");
        JLabel latMaxLabel = new JLabel("Lat Max");
        JLabel longMinLabel = new JLabel("Long Min");
        JLabel longMaxLabel = new JLabel("Long Max");

        SButton setButton = new SButton("Set filter");
        JPanel controlPanel = new JPanel(new GridLayout(0,2));
        controlPanel.add(latMinLabel);
        controlPanel.add(latMin);
        controlPanel.add(latMaxLabel);
        controlPanel.add(latMax);
        controlPanel.add(longMinLabel);
        controlPanel.add(longMin);
        controlPanel.add(longMaxLabel);
        controlPanel.add(longMax);
        controlPanel.add(setButton);

        JFrame frame = new JFrame("GPS");
        frame.add(controlPanel, BorderLayout.NORTH);

        Cell<double[]> rangeCell = setButton.sClicked
            .snapshot(latMin.text, latMax.text, longMin.text, longMax.text,
                (click, a, b, c, d) -> new double[] {Double.parseDouble(a), Double.parseDouble(b), Double.parseDouble(c), Double.parseDouble(d)} ).hold(new double [] {-90, 90, -180, 180});

        Cell<String> filteredCell = inRange(allEvents, rangeCell).map(ev -> eventToText(ev)).hold("waiting...");
        SLabel filteredLabel = new SLabel(filteredCell);
        
        Cell<String> rangeTextCell = rangeCell.map(r -> "Lat: " + r[0] + " to " + r[1] + "  Long: " + r[2] + " to " + r[3]);
        SLabel rangeLabel = new SLabel(rangeTextCell);

        JPanel trackersPanel = new JPanel(new GridLayout(5,2));
        JPanel bottomPanel = new JPanel(new GridLayout(0, 1));
        bottomPanel.add(updateLabel);
        bottomPanel.add(rangeLabel);
        bottomPanel.add(filteredLabel);

        
        for(Stream<GpsEvent> s : streams) {
        	trackersPanel.add(trackerPanel(s));
        }
        
        frame.setSize(600,400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(trackersPanel, BorderLayout.CENTER);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.setVisible(true);
	}

}
